package com.strhodler.utxopocket.data.wallet

import com.strhodler.utxopocket.data.db.WalletDao
import com.strhodler.utxopocket.di.IoDispatcher
import com.strhodler.utxopocket.domain.model.Bip329ImportResult
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import com.strhodler.utxopocket.domain.repository.WalletLabelRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class DefaultWalletLabelRepository @Inject constructor(
    walletDao: WalletDao,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : WalletLabelRepository {

    private val walletLabelManager = WalletLabelRepositorySupport.createManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher
    )

    override suspend fun updateUtxoLabel(walletId: Long, txid: String, vout: Int, label: String?) {
        walletLabelManager.updateUtxoLabel(walletId, txid, vout, label)
    }

    override suspend fun updateTransactionLabel(walletId: Long, txid: String, label: String?) {
        walletLabelManager.updateTransactionLabel(walletId, txid, label)
    }

    override suspend fun updateUtxoSpendable(walletId: Long, txid: String, vout: Int, spendable: Boolean?) {
        walletLabelManager.updateUtxoSpendable(walletId, txid, vout, spendable)
    }

    override suspend fun exportWalletLabels(walletId: Long): WalletLabelExport =
        walletLabelManager.exportWalletLabels(walletId)

    override suspend fun importWalletLabels(
        walletId: Long,
        payload: ByteArray,
        overwriteExisting: Boolean
    ): Bip329ImportResult = walletLabelManager.importWalletLabels(
        walletId = walletId,
        payload = payload,
        overwriteExisting = overwriteExisting
    )
}

internal object WalletLabelRepositorySupport {
    private const val MAX_LABEL_LENGTH = 255
    private val WHITESPACE_REGEX = Regex("\\s+")

    internal fun createManager(
        walletDao: WalletDao,
        ioDispatcher: CoroutineDispatcher
    ): WalletLabelManager = WalletLabelManager(
        walletDao = walletDao,
        ioDispatcher = ioDispatcher,
        sanitizeLabel = ::sanitizeLabel,
        originsCompatible = WalletDescriptorOriginUtils::originsCompatible
    )

    internal fun sanitizeLabel(value: String?): String? {
        if (value == null) return null
        val normalized = WHITESPACE_REGEX.replace(value, " ").trim()
        if (normalized.isEmpty()) return null
        return normalized.take(MAX_LABEL_LENGTH)
    }
}
