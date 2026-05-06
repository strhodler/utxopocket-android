package com.strhodler.utxopocket.data.wallet

import androidx.room.withTransaction
import com.strhodler.utxopocket.data.db.PendingBip329LabelEntity
import com.strhodler.utxopocket.data.db.UtxoCanvasDao
import com.strhodler.utxopocket.data.db.UtxoCanvasItemEntity
import com.strhodler.utxopocket.data.db.UtxoCollectionEntity
import com.strhodler.utxopocket.data.db.UtxoCollectionMembershipEntity
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.wallet.backup.BACKUP_MAX_KDF_ITERATIONS
import com.strhodler.utxopocket.data.wallet.backup.BACKUP_MIN_KDF_ITERATIONS
import com.strhodler.utxopocket.data.wallet.backup.BACKUP_SALT_BYTES
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupAppPreferences
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupCanvasItem
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupCollection
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupCollectionMembership
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupDecodeResult
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupJsonCodec
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupLabels
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupPayload
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupPendingLabel
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupTransactionFilter
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupUtxoFilter
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupWallet
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupWalletDetailPreferences
import com.strhodler.utxopocket.data.wallet.backup.WalletBackupWalletMeta
import com.strhodler.utxopocket.domain.model.AppLanguage
import com.strhodler.utxopocket.domain.model.BalanceRange
import com.strhodler.utxopocket.domain.model.BalanceUnit
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.ThemePreference
import com.strhodler.utxopocket.domain.model.ThemeProfile
import com.strhodler.utxopocket.domain.model.UtxoCanvasItemType
import com.strhodler.utxopocket.domain.model.UtxoCollectionColor
import com.strhodler.utxopocket.domain.model.WalletBackupExportData
import com.strhodler.utxopocket.domain.model.WalletBackupExportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupExportResult
import com.strhodler.utxopocket.domain.model.WalletBackupFailure
import com.strhodler.utxopocket.domain.model.WalletBackupImportRequest
import com.strhodler.utxopocket.domain.model.WalletBackupImportResult
import com.strhodler.utxopocket.domain.model.WalletBackupImportSummary
import com.strhodler.utxopocket.domain.model.WalletBackupPreview
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewRequest
import com.strhodler.utxopocket.domain.model.WalletBackupPreviewResult
import com.strhodler.utxopocket.domain.model.WalletDetailTransactionFilter
import com.strhodler.utxopocket.domain.model.WalletDetailUtxoFilter
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.WalletDetailPreferencesRepository
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class WalletBackupManager(
    private val walletDao: WalletDao,
    private val utxoCanvasDao: UtxoCanvasDao,
    private val database: UtxoPocketDatabase,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val walletDetailPreferencesRepository: WalletDetailPreferencesRepository,
    private val validateDescriptor: suspend (
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ) -> DescriptorValidationResult,
    private val removeWalletStorage: (walletId: Long, network: BitcoinNetwork) -> Unit,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun exportEncryptedBackup(request: WalletBackupExportRequest): WalletBackupExportResult =
        withContext(ioDispatcher) {
            val passphrase = request.passphrase.copyOf()
            try {
                if (passphrase.isEmpty()) {
                    return@withContext WalletBackupExportResult.Failure(
                        WalletBackupFailure.InvalidPayload("Backup passphrase is required")
                    )
                }

                val wallets = walletDao.getAllWallets()
                    .sortedWith(compareBy<WalletEntity> { it.sortOrder }.thenBy { it.name.lowercase(Locale.US) })

                val walletIdByRef = linkedMapOf<String, Long>()
                val backupWallets = wallets.mapIndexed { index, entity ->
                    val walletRef = "wallet-${index + 1}"
                    walletIdByRef[walletRef] = entity.id
                    toBackupWallet(entity = entity, walletRef = walletRef)
                }

                val appPreferences = WalletBackupAppPreferences(
                    preferredNetwork = appPreferencesRepository.preferredNetwork.first().name,
                    themePreference = appPreferencesRepository.themePreference.first().name,
                    themeProfile = appPreferencesRepository.themeProfile.first().name,
                    appLanguage = appPreferencesRepository.appLanguage.first().languageTag,
                    balanceUnit = appPreferencesRepository.balanceUnit.first().name,
                    balancesHidden = appPreferencesRepository.balancesHidden.first(),
                    hapticsEnabled = appPreferencesRepository.hapticsEnabled.first(),
                    walletBalanceRange = appPreferencesRepository.walletBalanceRange.first().name,
                    showBalanceChart = appPreferencesRepository.showBalanceChart.first(),
                    pinShuffleEnabled = appPreferencesRepository.pinShuffleEnabled.first(),
                    advancedMode = appPreferencesRepository.advancedMode.first(),
                    dustThresholdSats = appPreferencesRepository.dustThresholdSats.first()
                )

                val detailPreferences = backupWallets.map { wallet ->
                    val walletId = walletIdByRef[wallet.walletRef]
                        ?: throw IllegalStateException("Missing wallet id for backup reference")
                    val detail = walletDetailPreferencesRepository.observe(walletId).first()
                    WalletBackupWalletDetailPreferences(
                        walletRef = wallet.walletRef,
                        transactionSort = detail.transactionSort.name,
                        showPending = detail.showPending,
                        utxoSort = detail.utxoSort.name,
                        transactionFilter = WalletBackupTransactionFilter(
                            showLabeled = detail.transactionFilter.showLabeled,
                            showUnlabeled = detail.transactionFilter.showUnlabeled,
                            showReceived = detail.transactionFilter.showReceived,
                            showSent = detail.transactionFilter.showSent
                        ),
                        utxoFilter = WalletBackupUtxoFilter(
                            showLabeled = detail.utxoFilter.showLabeled,
                            showUnlabeled = detail.utxoFilter.showUnlabeled,
                            showSpendable = detail.utxoFilter.showSpendable,
                            showNotSpendable = detail.utxoFilter.showNotSpendable
                        ),
                        balanceRange = detail.balanceRange.name,
                        showBalanceChart = detail.showBalanceChart
                    )
                }

                val payload = WalletBackupPayload(
                    wallets = backupWallets,
                    appPreferences = appPreferences,
                    walletDetailPreferences = detailPreferences
                )

                val createdAtMillis = System.currentTimeMillis()
                val calibratedIterations = calibrateKdfIterations(passphrase)
                val encoded = WalletBackupJsonCodec.encode(
                    payload = payload,
                    passphrase = passphrase,
                    iterations = calibratedIterations,
                    createdAtMillis = createdAtMillis,
                    secureRandom = SecureRandom()
                )

                WalletBackupExportResult.Success(
                    WalletBackupExportData(
                        fileName = defaultBackupFileName(createdAtMillis),
                        payload = encoded,
                        createdAtMillis = createdAtMillis,
                        walletCount = backupWallets.size
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                WalletBackupExportResult.Failure(
                    WalletBackupFailure.IoFailure(error.message ?: "Failed to export encrypted backup")
                )
            } finally {
                passphrase.fill('\u0000')
            }
        }

    suspend fun previewEncryptedBackup(request: WalletBackupPreviewRequest): WalletBackupPreviewResult =
        withContext(ioDispatcher) {
            val passphrase = request.passphrase.copyOf()
            try {
                val decoded = WalletBackupJsonCodec.decode(
                    encoded = request.payload,
                    passphrase = passphrase
                )
                when (decoded) {
                    is WalletBackupDecodeResult.Failure -> {
                        WalletBackupPreviewResult.Failure(decoded.failure)
                    }

                    is WalletBackupDecodeResult.Success -> {
                        WalletBackupPreviewResult.Success(
                            WalletBackupPreview(
                                createdAtMillis = decoded.createdAtMillis,
                                walletCount = decoded.payload.wallets.size,
                                walletNames = decoded.payload.wallets.map { it.meta.name },
                                hasAppPreferences = decoded.payload.appPreferences != null,
                                hasWalletDetailPreferences = decoded.payload.walletDetailPreferences.isNotEmpty()
                            )
                        )
                    }
                }
            } finally {
                passphrase.fill('\u0000')
            }
        }

    suspend fun importEncryptedBackup(request: WalletBackupImportRequest): WalletBackupImportResult =
        withContext(ioDispatcher) {
            val passphrase = request.passphrase.copyOf()
            try {
                val decoded = WalletBackupJsonCodec.decode(
                    encoded = request.payload,
                    passphrase = passphrase
                )
                val payload = when (decoded) {
                    is WalletBackupDecodeResult.Failure -> {
                        return@withContext WalletBackupImportResult.Failure(decoded.failure)
                    }

                    is WalletBackupDecodeResult.Success -> decoded.payload
                }

                validatePayloadDescriptors(payload)?.let { failure ->
                    return@withContext WalletBackupImportResult.Failure(failure)
                }

                val existingWallets = walletDao.getAllWallets()
                val walletRefMap = mutableMapOf<String, Long>()
                database.withTransaction {
                    walletDao.clearAllTransactionOutputs()
                    walletDao.clearAllTransactionInputs()
                    walletDao.clearAllTransactions()
                    walletDao.clearAllUtxos()
                    walletDao.clearAllPendingLabels()
                    walletDao.deleteAllWallets()

                    utxoCanvasDao.clearAllCanvasItems()
                    utxoCanvasDao.clearAllMemberships()
                    utxoCanvasDao.clearAllCollections()

                    payload.wallets.forEach { wallet ->
                        val insertedId = walletDao.insert(wallet.toWalletEntity())
                        walletRefMap[wallet.walletRef] = insertedId

                        val pendingEntries = wallet.toPendingLabelEntities(insertedId)
                        if (pendingEntries.isNotEmpty()) {
                            walletDao.upsertPendingLabels(pendingEntries)
                        }

                        insertWalletCollections(
                            walletId = insertedId,
                            collections = wallet.collections,
                            memberships = wallet.collectionMemberships,
                            canvasItems = wallet.canvasItems
                        )
                    }
                }

                existingWallets.forEach { wallet ->
                    val network = runCatching { BitcoinNetwork.valueOf(wallet.network) }.getOrNull() ?: return@forEach
                    removeWalletStorage(wallet.id, network)
                }

                applyAppPreferences(payload.appPreferences)?.let { failure ->
                    return@withContext WalletBackupImportResult.Failure(failure)
                }
                applyWalletDetailPreferences(
                    entries = payload.walletDetailPreferences,
                    walletRefMap = walletRefMap
                )?.let { failure ->
                    return@withContext WalletBackupImportResult.Failure(failure)
                }

                val txLabelCount = payload.wallets.sumOf { it.labels.transactionLabels.size }
                val utxoLabelCount = payload.wallets.sumOf { it.labels.utxoLabels.size }
                val pendingLabelCount = payload.wallets.sumOf { it.labels.pendingBip329.size }
                val collectionCount = payload.wallets.sumOf { it.collections.size }
                val membershipCount = payload.wallets.sumOf { it.collectionMemberships.size }
                val canvasCount = payload.wallets.sumOf { it.canvasItems.size }

                WalletBackupImportResult.Success(
                    WalletBackupImportSummary(
                        walletsImported = payload.wallets.size,
                        queuedTransactionLabels = txLabelCount,
                        queuedUtxoLabels = utxoLabelCount,
                        queuedPendingLabels = pendingLabelCount,
                        collectionsImported = collectionCount,
                        collectionMembershipsImported = membershipCount,
                        canvasItemsImported = canvasCount
                    )
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: IllegalArgumentException) {
                WalletBackupImportResult.Failure(
                    WalletBackupFailure.InvalidPayload(error.message ?: "Invalid backup payload")
                )
            } catch (error: Exception) {
                WalletBackupImportResult.Failure(
                    WalletBackupFailure.IoFailure(error.message ?: "Failed to import encrypted backup")
                )
            } finally {
                passphrase.fill('\u0000')
            }
        }

    private suspend fun toBackupWallet(entity: WalletEntity, walletRef: String): WalletBackupWallet {
        val txLabels = walletDao.getTransactionLabels(entity.id)
            .mapNotNull { projection ->
                sanitizeLabel(projection.label)?.let { sanitized ->
                    com.strhodler.utxopocket.data.wallet.backup.WalletBackupTransactionLabel(
                        txid = projection.txid,
                        label = sanitized
                    )
                }
            }
        val utxoLabels = walletDao.getUtxoMetadata(entity.id)
            .mapNotNull { projection ->
                val sanitized = sanitizeLabel(projection.label)
                if (sanitized == null && projection.spendable == null) {
                    null
                } else {
                    com.strhodler.utxopocket.data.wallet.backup.WalletBackupUtxoLabel(
                        txid = projection.txid,
                        vout = projection.vout,
                        label = sanitized,
                        spendable = projection.spendable
                    )
                }
            }
        val pending = walletDao.getPendingLabels(entity.id)
            .map { pendingLabel ->
                WalletBackupPendingLabel(
                    type = pendingLabel.type,
                    ref = pendingLabel.ref,
                    keyPath = pendingLabel.keyPath.ifBlank { null },
                    label = sanitizeLabel(pendingLabel.label),
                    spendable = pendingLabel.spendable,
                    hasSpendable = pendingLabel.hasSpendable,
                    overwriteExisting = pendingLabel.overwriteExisting
                )
            }

        val collections = utxoCanvasDao.getCollectionsSnapshot(entity.id)
        val collectionIdToName = collections.associate { it.id.toString() to it.name }
        val exportedCollections = collections.map { collection ->
            WalletBackupCollection(
                name = collection.name,
                colorKey = collection.colorKey
            )
        }
        val memberships = utxoCanvasDao.getMembershipsSnapshot(entity.id)
            .mapNotNull { membership ->
                val name = collections.firstOrNull { it.id == membership.collectionId }?.name ?: return@mapNotNull null
                WalletBackupCollectionMembership(
                    txid = membership.txid,
                    vout = membership.vout,
                    collectionName = name
                )
            }
        val canvasItems = utxoCanvasDao.getCanvasItemsSnapshot(entity.id)
            .mapNotNull { item ->
                val refId = if (item.itemType == UtxoCanvasItemType.COLLECTION.name) {
                    collectionIdToName[item.refId] ?: return@mapNotNull null
                } else {
                    item.refId
                }
                WalletBackupCanvasItem(
                    itemType = item.itemType,
                    refId = refId,
                    positionIndex = item.positionIndex.coerceAtLeast(0)
                )
            }

        return WalletBackupWallet(
            walletRef = walletRef,
            meta = WalletBackupWalletMeta(
                name = entity.name,
                network = entity.network,
                descriptor = normalizeDescriptor(entity.descriptor) ?: entity.descriptor,
                changeDescriptor = normalizeDescriptor(entity.changeDescriptor),
                sharedDescriptors = entity.sharedDescriptors,
                viewOnly = entity.viewOnly,
                color = entity.color,
                sortOrder = entity.sortOrder.coerceAtLeast(0)
            ),
            labels = WalletBackupLabels(
                transactionLabels = txLabels,
                utxoLabels = utxoLabels,
                pendingBip329 = pending
            ),
            collections = exportedCollections,
            collectionMemberships = memberships,
            canvasItems = canvasItems
        )
    }

    private suspend fun validatePayloadDescriptors(payload: WalletBackupPayload): WalletBackupFailure? {
        payload.wallets.forEach { wallet ->
            val network = runCatching { BitcoinNetwork.valueOf(wallet.meta.network.uppercase(Locale.US)) }
                .getOrNull()
                ?: return WalletBackupFailure.InvalidPayload("Unknown wallet network: ${wallet.meta.network}")
            val validation = validateDescriptor(
                normalizeDescriptor(wallet.meta.descriptor) ?: wallet.meta.descriptor,
                normalizeDescriptor(wallet.meta.changeDescriptor),
                network
            )
            if (validation !is DescriptorValidationResult.Valid) {
                val reason = when (validation) {
                    is DescriptorValidationResult.Invalid -> validation.reason
                    DescriptorValidationResult.Empty -> "Descriptor is empty"
                    DescriptorValidationResult.Idle -> "Descriptor validation did not complete"
                }
                return WalletBackupFailure.DescriptorValidation(
                    walletName = wallet.meta.name,
                    reason = reason
                )
            }
        }
        return null
    }

    private fun WalletBackupWallet.toWalletEntity(): WalletEntity {
        return WalletEntity(
            id = 0,
            name = sanitizeWalletName(meta.name),
            descriptor = normalizeDescriptor(meta.descriptor)
                ?: throw IllegalArgumentException("Wallet descriptor cannot be blank"),
            changeDescriptor = normalizeDescriptor(meta.changeDescriptor),
            network = meta.network.uppercase(Locale.US),
            balanceSats = 0L,
            transactionCount = 0,
            lastSyncStatus = "IDLE",
            lastSyncError = null,
            lastSyncTime = null,
            requiresFullScan = true,
            sharedDescriptors = meta.sharedDescriptors,
            fullScanStopGap = null,
            lastFullScanTime = null,
            viewOnly = meta.viewOnly,
            color = meta.color.ifBlank { com.strhodler.utxopocket.domain.model.WalletColor.DEFAULT.storageKey },
            sortOrder = meta.sortOrder.coerceAtLeast(0),
            syncSessionId = null,
            syncTipHeight = null,
            syncTipHash = null,
            syncApplied = true,
            syncStartedAt = null,
            syncCompletedAt = null,
            lastActiveExternalIndex = null,
            lastActiveChangeIndex = null
        )
    }

    private fun WalletBackupWallet.toPendingLabelEntities(walletId: Long): List<PendingBip329LabelEntity> {
        val entries = linkedMapOf<String, PendingBip329LabelEntity>()

        labels.transactionLabels.forEach { txLabel ->
            val sanitized = sanitizeLabel(txLabel.label) ?: return@forEach
            val entity = PendingBip329LabelEntity(
                walletId = walletId,
                type = "tx",
                ref = txLabel.txid,
                label = sanitized,
                spendable = null,
                hasSpendable = false,
                keyPath = "",
                overwriteExisting = true
            )
            entries[pendingKey(entity)] = entity
        }

        labels.utxoLabels.forEach { utxoLabel ->
            val sanitized = sanitizeLabel(utxoLabel.label)
            val hasSpendable = utxoLabel.spendable != null
            if (sanitized == null && !hasSpendable) {
                return@forEach
            }
            val entity = PendingBip329LabelEntity(
                walletId = walletId,
                type = "output",
                ref = "${utxoLabel.txid}:${utxoLabel.vout}",
                label = sanitized,
                spendable = utxoLabel.spendable,
                hasSpendable = hasSpendable,
                keyPath = "",
                overwriteExisting = true
            )
            entries[pendingKey(entity)] = entity
        }

        labels.pendingBip329.forEach { pending ->
            val type = pending.type.trim().lowercase(Locale.US)
            val ref = pending.ref.trim()
            if (type.isEmpty() || ref.isEmpty()) {
                return@forEach
            }
            val sanitized = sanitizeLabel(pending.label)
            val hasSpendable = pending.hasSpendable || pending.spendable != null
            if (sanitized == null && !hasSpendable) {
                return@forEach
            }
            val entity = PendingBip329LabelEntity(
                walletId = walletId,
                type = type,
                ref = ref,
                label = sanitized,
                spendable = if (hasSpendable) pending.spendable else null,
                hasSpendable = hasSpendable,
                keyPath = pending.keyPath?.trim().orEmpty(),
                overwriteExisting = pending.overwriteExisting
            )
            entries[pendingKey(entity)] = entity
        }

        return entries.values.toList()
    }

    private suspend fun insertWalletCollections(
        walletId: Long,
        collections: List<WalletBackupCollection>,
        memberships: List<WalletBackupCollectionMembership>,
        canvasItems: List<WalletBackupCanvasItem>
    ) {
        val now = System.currentTimeMillis()
        val collectionIdByName = linkedMapOf<String, Long>()
        collections.forEach { collection ->
            val trimmedName = collection.name.trim()
            if (trimmedName.isBlank()) {
                throw IllegalArgumentException("Collection name cannot be blank")
            }
            val key = trimmedName.lowercase(Locale.US)
            if (collectionIdByName.containsKey(key)) {
                throw IllegalArgumentException("Duplicate collection name in backup payload: $trimmedName")
            }
            val insertedId = utxoCanvasDao.insertCollection(
                UtxoCollectionEntity(
                    walletId = walletId,
                    name = trimmedName,
                    colorKey = UtxoCollectionColor.fromStorageKey(collection.colorKey).storageKey,
                    createdAt = now,
                    updatedAt = now
                )
            )
            collectionIdByName[key] = insertedId
        }

        val membershipEntities = linkedMapOf<String, UtxoCollectionMembershipEntity>()
        memberships.forEach { membership ->
            val collectionId = collectionIdByName[membership.collectionName.trim().lowercase(Locale.US)]
                ?: throw IllegalArgumentException("Membership references unknown collection: ${membership.collectionName}")
            val key = "${membership.txid}:${membership.vout}"
            membershipEntities[key] = UtxoCollectionMembershipEntity(
                walletId = walletId,
                txid = membership.txid,
                vout = membership.vout,
                collectionId = collectionId,
                createdAt = now
            )
        }
        if (membershipEntities.isNotEmpty()) {
            utxoCanvasDao.upsertMemberships(membershipEntities.values.toList())
        }

        val canvasByKey = linkedMapOf<String, UtxoCanvasItemEntity>()
        canvasItems.forEach { item ->
            val itemType = item.itemType.trim().uppercase(Locale.US)
            when (itemType) {
                UtxoCanvasItemType.UTXO.name -> {
                    val key = "$itemType:${item.refId}"
                    canvasByKey[key] = UtxoCanvasItemEntity(
                        walletId = walletId,
                        itemType = itemType,
                        refId = item.refId,
                        positionIndex = item.positionIndex.coerceAtLeast(0)
                    )
                }

                UtxoCanvasItemType.COLLECTION.name -> {
                    val collectionId = collectionIdByName[item.refId.trim().lowercase(Locale.US)]
                        ?: throw IllegalArgumentException(
                            "Canvas collection item references unknown collection: ${item.refId}"
                        )
                    val normalizedRef = collectionId.toString()
                    val key = "$itemType:$normalizedRef"
                    canvasByKey[key] = UtxoCanvasItemEntity(
                        walletId = walletId,
                        itemType = itemType,
                        refId = normalizedRef,
                        positionIndex = item.positionIndex.coerceAtLeast(0)
                    )
                }

                else -> {
                    throw IllegalArgumentException("Unknown canvas item type: ${item.itemType}")
                }
            }
        }
        if (canvasByKey.isNotEmpty()) {
            val reindexed = canvasByKey.values
                .sortedBy { it.positionIndex }
                .mapIndexed { index, entity ->
                    entity.copy(positionIndex = index)
                }
            utxoCanvasDao.upsertCanvasItems(reindexed)
        }
    }

    private suspend fun applyAppPreferences(
        appPreferences: WalletBackupAppPreferences?
    ): WalletBackupFailure? {
        if (appPreferences == null) return null

        val preferredNetwork = parseEnum<BitcoinNetwork>(appPreferences.preferredNetwork)
            ?: return WalletBackupFailure.InvalidPayload("Invalid preferred network")
        val themePreference = parseEnum<ThemePreference>(appPreferences.themePreference)
            ?: return WalletBackupFailure.InvalidPayload("Invalid theme preference")
        val themeProfile = parseEnum<ThemeProfile>(appPreferences.themeProfile)
            ?: return WalletBackupFailure.InvalidPayload("Invalid theme profile")
        val appLanguage = AppLanguage.fromLanguageTagOrNull(appPreferences.appLanguage)
            ?: return WalletBackupFailure.InvalidPayload("Invalid app language")
        val balanceUnit = parseEnum<BalanceUnit>(appPreferences.balanceUnit)
            ?: return WalletBackupFailure.InvalidPayload("Invalid balance unit")
        val walletBalanceRange = parseEnum<BalanceRange>(appPreferences.walletBalanceRange)
            ?: return WalletBackupFailure.InvalidPayload("Invalid wallet balance range")

        return try {
            appPreferencesRepository.setPreferredNetwork(preferredNetwork)
            appPreferencesRepository.setThemePreference(themePreference)
            appPreferencesRepository.setThemeProfile(themeProfile)
            appPreferencesRepository.setAppLanguage(appLanguage)
            appPreferencesRepository.setBalanceUnit(balanceUnit)
            appPreferencesRepository.setBalancesHidden(appPreferences.balancesHidden)
            appPreferencesRepository.setHapticsEnabled(appPreferences.hapticsEnabled)
            appPreferencesRepository.setWalletBalanceRange(walletBalanceRange)
            appPreferencesRepository.setShowBalanceChart(appPreferences.showBalanceChart)
            appPreferencesRepository.setPinShuffleEnabled(appPreferences.pinShuffleEnabled)
            appPreferencesRepository.setAdvancedMode(appPreferences.advancedMode)
            appPreferencesRepository.setDustThresholdSats(appPreferences.dustThresholdSats.coerceAtLeast(0L))
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WalletBackupFailure.IoFailure(error.message ?: "Failed to apply app preferences")
        }
    }

    private suspend fun applyWalletDetailPreferences(
        entries: List<WalletBackupWalletDetailPreferences>,
        walletRefMap: Map<String, Long>
    ): WalletBackupFailure? {
        return try {
            entries.forEach { entry ->
                val walletId = walletRefMap[entry.walletRef]
                    ?: return WalletBackupFailure.InvalidPayload("Unknown wallet reference in detail preferences")
                val transactionSort = parseEnum<WalletTransactionSort>(entry.transactionSort)
                    ?: return WalletBackupFailure.InvalidPayload("Invalid wallet transaction sort")
                val utxoSort = parseEnum<WalletUtxoSort>(entry.utxoSort)
                    ?: return WalletBackupFailure.InvalidPayload("Invalid wallet UTXO sort")
                val balanceRange = parseEnum<BalanceRange>(entry.balanceRange)
                    ?: return WalletBackupFailure.InvalidPayload("Invalid wallet detail balance range")

                walletDetailPreferencesRepository.setTransactionSort(walletId, transactionSort)
                walletDetailPreferencesRepository.setShowPending(walletId, entry.showPending)
                walletDetailPreferencesRepository.setUtxoSort(walletId, utxoSort)
                walletDetailPreferencesRepository.setTransactionFilter(
                    walletId = walletId,
                    filter = WalletDetailTransactionFilter(
                        showLabeled = entry.transactionFilter.showLabeled,
                        showUnlabeled = entry.transactionFilter.showUnlabeled,
                        showReceived = entry.transactionFilter.showReceived,
                        showSent = entry.transactionFilter.showSent
                    )
                )
                walletDetailPreferencesRepository.setUtxoFilter(
                    walletId = walletId,
                    filter = WalletDetailUtxoFilter(
                        showLabeled = entry.utxoFilter.showLabeled,
                        showUnlabeled = entry.utxoFilter.showUnlabeled,
                        showSpendable = entry.utxoFilter.showSpendable,
                        showNotSpendable = entry.utxoFilter.showNotSpendable
                    )
                )
                walletDetailPreferencesRepository.setBalanceRange(walletId, balanceRange)
                walletDetailPreferencesRepository.setShowBalanceChart(walletId, entry.showBalanceChart)
            }
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WalletBackupFailure.IoFailure(error.message ?: "Failed to apply wallet detail preferences")
        }
    }

    private fun sanitizeWalletName(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "Wallet"
        return trimmed.take(MAX_WALLET_NAME_LENGTH)
    }

    private fun sanitizeLabel(value: String?): String? {
        if (value == null) return null
        val normalized = WHITESPACE_REGEX.replace(value, " ").trim()
        if (normalized.isEmpty()) return null
        return normalized.take(MAX_LABEL_LENGTH)
    }

    private fun normalizeDescriptor(value: String?): String? {
        if (value == null) return null
        val normalized = value.replace("\r\n", "\n").replace('\r', '\n').trim()
        return normalized.ifEmpty { null }
    }

    private fun pendingKey(entity: PendingBip329LabelEntity): String =
        "${entity.type}|${entity.ref}|${entity.keyPath}"

    private fun calibrateKdfIterations(passphrase: CharArray): Int {
        val salt = ByteArray(BACKUP_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val baseline = BACKUP_MIN_KDF_ITERATIONS
        val start = System.nanoTime()
        deriveCalibrationKey(passphrase, salt, baseline)
        val elapsedMs = ((System.nanoTime() - start) / 1_000_000.0).coerceAtLeast(1.0)
        val projected = (baseline * (KDF_TARGET_MILLIS / elapsedMs)).toInt()
        return projected.coerceIn(BACKUP_MIN_KDF_ITERATIONS, BACKUP_MAX_KDF_ITERATIONS)
    }

    private fun deriveCalibrationKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun defaultBackupFileName(createdAtMillis: Long): String {
        val timestamp = BACKUP_TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(createdAtMillis))
        return "utxopocket-watch-only-backup-$timestamp.ubak"
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String): T? =
        runCatching { enumValueOf<T>(value.trim()) }.getOrNull()

    private companion object {
        private const val MAX_WALLET_NAME_LENGTH = 64
        private const val MAX_LABEL_LENGTH = 255
        private const val KDF_TARGET_MILLIS = 350.0
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val BACKUP_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withLocale(Locale.US)
                .withZone(ZoneOffset.UTC)
    }
}
