package com.strhodler.utxopocket.data.wallet

import androidx.room.withTransaction
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.data.db.scheduleFullScan
import com.strhodler.utxopocket.data.db.toDomain
import com.strhodler.utxopocket.data.db.toStorage
import com.strhodler.utxopocket.data.db.UtxoPocketDatabase
import com.strhodler.utxopocket.data.wallet.SyncGapInitializer.seedSyncGapIfMissing
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.DescriptorType
import com.strhodler.utxopocket.domain.model.DescriptorValidationResult
import com.strhodler.utxopocket.domain.model.DescriptorWarning
import com.strhodler.utxopocket.domain.model.NodeStatus
import com.strhodler.utxopocket.domain.model.SyncOperation
import com.strhodler.utxopocket.domain.model.WalletColor
import com.strhodler.utxopocket.domain.model.WalletCreationRequest
import com.strhodler.utxopocket.domain.model.WalletCreationResult
import com.strhodler.utxopocket.domain.model.toBdkNetwork
import com.strhodler.utxopocket.domain.repository.WalletNameAlreadyExistsException
import com.strhodler.utxopocket.domain.repository.WalletSyncPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.bitcoindevkit.Descriptor

internal class WalletProvisioningManager(
    private val walletDao: WalletDao,
    private val database: UtxoPocketDatabase,
    private val walletSyncPreferencesRepository: WalletSyncPreferencesRepository,
    private val refreshWallet: suspend (Long, SyncOperation) -> Unit,
    private val ioDispatcher: CoroutineDispatcher,
    private val maxFullScanStopGap: Int
) {

    suspend fun validateDescriptor(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult = withContext(ioDispatcher) {
        validateDescriptorInternal(descriptor, changeDescriptor, network)
    }

    suspend fun addWallet(request: WalletCreationRequest): WalletCreationResult =
        withContext(ioDispatcher) {
            val name = request.name.trim()
            if (name.isEmpty()) {
                return@withContext WalletCreationResult.Failure("Wallet name is required.")
            }

            val validation = validateDescriptorInternal(
                descriptor = request.descriptor,
                changeDescriptor = request.changeDescriptor,
                network = request.network
            )
            if (validation !is DescriptorValidationResult.Valid) {
                val message = when (validation) {
                    is DescriptorValidationResult.Invalid -> validation.reason
                    DescriptorValidationResult.Empty -> "Descriptor is required."
                    else -> GENERIC_DESCRIPTOR_ERROR
                }
                return@withContext WalletCreationResult.Failure(message)
            }

            val networkName = request.network.name
            if (walletDao.countByName(networkName, name) > 0) {
                return@withContext WalletCreationResult.Failure("A wallet with this name already exists for the selected network.")
            }
            if (walletDao.countByDescriptor(networkName, validation.descriptor) > 0) {
                return@withContext WalletCreationResult.Failure("This descriptor is already registered for the selected network.")
            }

            val (statusValue, statusError) = NodeStatus.Idle.toStorage()
            val entityTemplate = WalletEntity(
                name = name,
                descriptor = validation.descriptor,
                changeDescriptor = validation.changeDescriptor,
                network = networkName,
                balanceSats = 0,
                transactionCount = 0,
                lastSyncStatus = statusValue,
                lastSyncError = statusError,
                lastSyncTime = null,
                sharedDescriptors = request.sharedDescriptors,
                viewOnly = validation.isViewOnly
            )

            return@withContext runCatching {
                val id = database.withTransaction {
                    val nextSortOrder = (walletDao.getMaxSortOrder(networkName) ?: -1) + 1
                    walletDao.insert(entityTemplate.copy(sortOrder = nextSortOrder))
                }
                val inserted = walletDao.findById(id)
                if (inserted != null) {
                    seedSyncGapIfMissing(inserted, walletSyncPreferencesRepository)
                    WalletCreationResult.Success(inserted.toDomain())
                } else {
                    WalletCreationResult.Failure("Wallet inserted but could not be loaded.")
                }
            }.getOrElse { error ->
                WalletCreationResult.Failure(error.message ?: "Failed to create wallet.")
            }
        }

    suspend fun updateWalletColor(id: Long, color: WalletColor) = withContext(ioDispatcher) {
        walletDao.updateColor(id, color.storageKey)
    }

    suspend fun forceFullRescan(walletId: Long, stopGap: Int) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        val normalizedStopGap = stopGap.coerceIn(1, maxFullScanStopGap)
        walletDao.upsert(entity.scheduleFullScan(normalizedStopGap))
        refreshWallet(walletId, SyncOperation.FullRescan)
    }

    suspend fun renameWallet(id: Long, name: String) = withContext(ioDispatcher) {
        val entity = walletDao.findById(id) ?: throw IllegalArgumentException("Wallet not found: $id")
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Wallet name cannot be blank." }
        val duplicateCount = walletDao.countByNameExcluding(entity.network, trimmed, id)
        if (duplicateCount > 0) {
            throw WalletNameAlreadyExistsException(trimmed)
        }
        walletDao.updateWalletName(id, trimmed)
    }

    suspend fun reorderWallets(
        network: BitcoinNetwork,
        orderedWalletIds: List<Long>
    ) = withContext(ioDispatcher) {
        val requestedIds = orderedWalletIds.distinct()
        if (requestedIds.size < 2) return@withContext

        val networkName = network.name
        database.withTransaction {
            val currentIds = walletDao.getWalletsSnapshot(networkName).map { it.id }
            if (requestedIds.toSet() != currentIds.toSet()) return@withTransaction

            requestedIds.forEachIndexed { index, walletId ->
                walletDao.updateWalletSortOrder(
                    id = walletId,
                    network = networkName,
                    sortOrder = index
                )
            }
        }
    }

    private fun validateDescriptorInternal(
        descriptor: String,
        changeDescriptor: String?,
        network: BitcoinNetwork
    ): DescriptorValidationResult {
        val sanitizedDescriptor = descriptor.trim()
        if (sanitizedDescriptor.isEmpty()) {
            return DescriptorValidationResult.Empty
        }
        val sanitizedChange = changeDescriptor
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (containsPrivateMaterial(sanitizedDescriptor) ||
            sanitizedChange?.let(::containsPrivateMaterial) == true
        ) {
            return descriptorInvalid(
                "Descriptor contains private key material. Only watch-only descriptors are supported."
            )
        }

        val bdkNetwork = network.toBdkNetwork()
        val parsedDescriptor = try {
            Descriptor(
                descriptor = sanitizedDescriptor,
                network = bdkNetwork
            )
        } catch (error: Throwable) {
            return descriptorInvalid(error.message)
        }

        try {
            val normalizedDescriptor = sanitizedDescriptor
            val isMultipath = parsedDescriptor.isMultipath()

            if (isMultipath && sanitizedChange != null) {
                return descriptorInvalid(
                    "Remove the separate change descriptor when using a BIP-389 multipath descriptor."
                )
            }

            if (isMultipath) {
                val singleDescriptors = runCatching { parsedDescriptor.toSingleDescriptors() }
                    .getOrElse { error ->
                        return descriptorInvalid(
                            "Multipath descriptor could not be expanded: ${error.message ?: "unknown error"}"
                        )
                    }
                try {
                    if (singleDescriptors.size != 2) {
                        return descriptorInvalid(
                            "Multipath descriptor must expand to exactly two branches (external/change)."
                        )
                    }
                } finally {
                    singleDescriptors.forEach { it.destroy() }
                }
            }

            val normalizedChange = sanitizedChange?.let { change ->
                val parsedChange = runCatching {
                    Descriptor(
                        descriptor = change,
                        network = bdkNetwork
                    )
                }.getOrElse { error ->
                    return descriptorInvalid(
                        "Change descriptor invalid: ${error.message ?: "unknown error"}"
                    )
                }
                parsedChange.destroy()
                change
            }

            val derivedHasWildcard = isMultipath ||
                hasWildcard(normalizedDescriptor) ||
                normalizedChange?.let(::hasWildcard) == true

            if (!isMultipath && !derivedHasWildcard) {
                return descriptorInvalid(
                    "Descriptor must include a wildcard derivation (`*`) or use a BIP-389 multipath branch (`/<0;1>/*`)."
                )
            }

            if (!isMultipath && derivedHasWildcard && normalizedChange == null) {
                return descriptorInvalid(
                    "A change descriptor is required for HD descriptors. Provide a BIP-389 multipath descriptor (`/<0;1>/*`) or a dedicated change descriptor."
                )
            }

            val warnings = mutableSetOf<DescriptorWarning>()
            if (!derivedHasWildcard) {
                warnings += DescriptorWarning.MISSING_WILDCARD
            }
            if (!isMultipath) {
                if (normalizedChange == null) {
                    warnings += DescriptorWarning.MISSING_CHANGE_DESCRIPTOR
                } else {
                    if (!hasWildcard(normalizedChange)) {
                        warnings += DescriptorWarning.CHANGE_DESCRIPTOR_NOT_DERIVABLE
                    }
                    if (EXTERNAL_PATH_REGEX.containsMatchIn(normalizedDescriptor) &&
                        !CHANGE_PATH_REGEX.containsMatchIn(normalizedChange)
                    ) {
                        warnings += DescriptorWarning.CHANGE_DESCRIPTOR_MISMATCH
                    }
                }
            }

            val descriptorType = DescriptorType.fromDescriptorString(normalizedDescriptor)
            val isViewOnly = !isMultipath && normalizedChange == null && !derivedHasWildcard

            return DescriptorValidationResult.Valid(
                descriptor = normalizedDescriptor,
                changeDescriptor = normalizedChange,
                type = descriptorType,
                hasWildcard = derivedHasWildcard,
                warnings = warnings.toList(),
                isMultipath = isMultipath,
                isViewOnly = isViewOnly
            )
        } finally {
            parsedDescriptor.destroy()
        }
    }

    private fun containsPrivateMaterial(descriptor: String): Boolean {
        if (descriptor.contains("xprv", ignoreCase = true)) return true
        if (descriptor.contains("tprv", ignoreCase = true)) return true
        if (EXTENDED_PRIVATE_KEY_REGEX.containsMatchIn(descriptor)) return true
        if (WIF_PRIVATE_KEY_REGEX.containsMatchIn(descriptor)) return true
        return false
    }

    private fun hasWildcard(descriptor: String): Boolean = descriptor.contains("*")

    private fun String?.orDescriptorError(): String =
        this?.takeIf { it.isNotBlank() } ?: GENERIC_DESCRIPTOR_ERROR

    private fun descriptorInvalid(reason: String?): DescriptorValidationResult.Invalid =
        DescriptorValidationResult.Invalid(reason.orDescriptorError())

    private companion object {
        private const val GENERIC_DESCRIPTOR_ERROR =
            "Invalid or malformed descriptor; review the imported descriptor or the compatibility wiki article."
        private val EXTENDED_PRIVATE_KEY_REGEX =
            Regex("\\b[acdfklmnstuvxyz]prv[0-9a-z]+", RegexOption.IGNORE_CASE)
        private val WIF_PRIVATE_KEY_REGEX =
            Regex("\\b(?:[59][1-9A-HJ-NP-Za-km-z]{50}|[KLc][1-9A-HJ-NP-Za-km-z]{50,51})\\b")
        private val EXTERNAL_PATH_REGEX = Regex("/0+/?\\*")
        private val CHANGE_PATH_REGEX = Regex("/1+/?\\*")
    }
}
