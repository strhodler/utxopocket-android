package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.data.db.WalletEntity
import com.strhodler.utxopocket.domain.model.AddressUsage
import com.strhodler.utxopocket.domain.model.WalletAddress
import com.strhodler.utxopocket.domain.model.WalletAddressDetail
import com.strhodler.utxopocket.domain.model.WalletAddressType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.use

internal class WalletAddressManager(
    private val walletDao: WalletDao,
    private val sessionRunner: WalletSessionRunner,
    private val ioDispatcher: CoroutineDispatcher,
    private val logTag: String
) {

    suspend fun listUnusedAddresses(
        walletId: Long,
        type: WalletAddressType,
        limit: Int
    ): List<WalletAddress> = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext emptyList()
        if (type == WalletAddressType.CHANGE &&
            (entity.viewOnly || !entity.hasChangeBranch())
        ) {
            return@withContext emptyList()
        }
        sessionRunner.withWallet(entity = entity, sealAfterUse = true) { wallet, persister, _ ->
            val keychain = type.toKeychainKind()
            val usedAddresses = walletDao.addressesWithHistory(walletId).map { it.trim() }.toSet()
            val fundedAddresses = walletDao.addressesWithFunds(walletId).map { it.trim() }.toSet()
            val targetDepth = (usedAddresses.size + limit - 1).coerceAtLeast(limit - 1)
            runCatching { wallet.revealAddressesTo(keychain, targetDepth.toUInt()) }
                .onSuccess { revealed -> revealed.forEach { it.destroy() } }
            wallet.persist(persister)

            val candidates = mutableListOf<WalletAddress>()
            val seen = mutableSetOf<String>()

            fun appendIfEligible(addressValue: String, derivationIndex: Int) {
                val trimmed = addressValue.trim()
                if (trimmed.isEmpty()) return
                if (usedAddresses.contains(trimmed) || fundedAddresses.contains(trimmed)) return
                if (!seen.add(trimmed)) return
                candidates += WalletAddress(
                    value = trimmed,
                    type = type,
                    derivationPath = derivationPath(type, derivationIndex),
                    derivationIndex = derivationIndex
                )
            }

            wallet.listUnusedAddresses(keychain).forEach { info ->
                try {
                    val addressValue = info.address.use { it.toString() }
                    val derivationIndex = info.index.toInt()
                    appendIfEligible(addressValue, derivationIndex)
                } finally {
                    info.destroy()
                }
            }

            var didRevealExtra = false
            var safety = 0
            while (candidates.size < limit && safety < limit * 4) {
                val next = runCatching { wallet.revealNextAddress(keychain) }.getOrNull() ?: break
                try {
                    didRevealExtra = true
                    val addressValue = next.address.use { it.toString() }
                    val derivationIndex = next.index.toInt()
                    appendIfEligible(addressValue, derivationIndex)
                } finally {
                    next.destroy()
                }
                safety++
            }

            if (didRevealExtra) {
                wallet.persist(persister)
            }

            candidates
                .sortedBy { it.derivationIndex }
                .take(limit)
        }
    }

    suspend fun revealNextAddress(
        walletId: Long,
        type: WalletAddressType
    ): WalletAddress? = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext null
        if (type == WalletAddressType.CHANGE &&
            (entity.viewOnly || !entity.hasChangeBranch())
        ) {
            return@withContext null
        }
        sessionRunner.withWallet(entity = entity, sealAfterUse = true) { wallet, persister, _ ->
            val keychain = type.toKeychainKind()
            val next = runCatching { wallet.revealNextAddress(keychain) }.getOrNull()
                ?: return@withWallet null
            try {
                val addressValue = next.address.use { it.toString().trim() }
                if (addressValue.isBlank()) return@withWallet null
                val derivationIndex = next.index.toInt()
                wallet.persist(persister)
                WalletAddress(
                    value = addressValue,
                    type = type,
                    derivationPath = derivationPath(type, derivationIndex),
                    derivationIndex = derivationIndex
                )
            } finally {
                next.destroy()
            }
        }
    }

    suspend fun getAddressDetail(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ): WalletAddressDetail? = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext null
        try {
            sessionRunner.withWallet<WalletAddressDetail?>(entity = entity, sealAfterUse = true) { wallet, persister, _ ->
                val keychain = type.toKeychainKind()
                runCatching { wallet.revealAddressesTo(keychain, derivationIndex.toUInt()) }
                    .onSuccess { reveals ->
                        reveals.forEach { it.destroy() }
                        wallet.persist(persister)
                    }
                wallet.peekAddress(keychain, derivationIndex.toUInt()).use { info ->
                    val (addressValue, scriptHex) = info.address.use { addr ->
                        val value = addr.toString()
                        val script = addr.scriptPubkey().use { script ->
                            script.toBytes().map { it.toUByte() }.toHexString()
                        }
                        value to script
                    }
                    val usageCount = walletDao.countOutputsByAddress(walletId, addressValue)
                    val usage = when {
                        usageCount <= 0 -> AddressUsage.NEVER
                        usageCount == 1 -> AddressUsage.ONCE
                        else -> AddressUsage.MULTIPLE
                    }
                    val descriptorTemplate = runCatching {
                        val detailKeychain = type.toKeychainKind()
                        wallet.publicDescriptor(detailKeychain)
                    }.getOrNull()
                    val derivedDescriptor = descriptorTemplate
                        ?.replace("*", derivationIndex.toString())
                        .orEmpty()
                    WalletAddressDetail(
                        value = addressValue,
                        type = type,
                        derivationPath = derivationPath(type, derivationIndex),
                        derivationIndex = derivationIndex,
                        scriptPubKey = scriptHex,
                        descriptor = derivedDescriptor,
                        usage = usage,
                        usageCount = usageCount
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            SecureLog.w(logTag, error) { "Failed to resolve address detail" }
            null
        }
    }

    suspend fun markAddressAsUsed(
        walletId: Long,
        type: WalletAddressType,
        derivationIndex: Int
    ) = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext
        if (type == WalletAddressType.CHANGE &&
            (entity.viewOnly || !entity.hasChangeBranch())
        ) {
            return@withContext
        }
        sessionRunner.withWallet(entity = entity, sealAfterUse = true) { wallet, persister, _ ->
            val keychain = type.toKeychainKind()
            wallet.markUsed(keychain, derivationIndex.toUInt())
            runCatching { wallet.revealNextAddress(keychain) }
                .onSuccess { it.destroy() }
            wallet.persist(persister)
            Unit
        }
    }

    suspend fun highestUsedIndices(walletId: Long): Pair<Int?, Int?> = withContext(ioDispatcher) {
        val entity = walletDao.findById(walletId) ?: return@withContext null to null
        val external = walletDao.maxDerivationIndexForOutputs(walletId, WalletAddressType.EXTERNAL.name)
            ?: entity.lastActiveExternalIndex
        val change = walletDao.maxDerivationIndexForOutputs(walletId, WalletAddressType.CHANGE.name)
            ?: entity.lastActiveChangeIndex
        external to change
    }

    private fun WalletAddressType.toKeychainKind(): KeychainKind = when (this) {
        WalletAddressType.EXTERNAL -> KeychainKind.EXTERNAL
        WalletAddressType.CHANGE -> KeychainKind.INTERNAL
    }

    private fun derivationPath(type: WalletAddressType, index: Int): String =
        when (type) {
            WalletAddressType.EXTERNAL -> "0/$index"
            WalletAddressType.CHANGE -> "1/$index"
        }

    private fun List<UByte>.toHexString(): String = buildString(size) {
        for (value in this@toHexString) {
            append((value.toInt() and 0xFF).toString(16).padStart(2, '0'))
        }
    }

    private fun WalletEntity.hasChangeBranch(): Boolean =
        !changeDescriptor.isNullOrBlank() || (!viewOnly && MULTIPATH_SEGMENT_REGEX.containsMatchIn(descriptor))

    private companion object {
        private val MULTIPATH_SEGMENT_REGEX = Regex("/<[^>]+>/")
    }
}
