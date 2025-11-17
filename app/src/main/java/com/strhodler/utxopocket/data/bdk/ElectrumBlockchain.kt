package com.strhodler.utxopocket.data.bdk

import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import java.io.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.ServerFeaturesRes
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.ElectrumClient
import org.bitcoindevkit.use
import org.bitcoindevkit.FullScanScriptInspector
import org.bitcoindevkit.Script
import org.bitcoindevkit.SyncScriptInspector

/**
 * Lightweight connection manager that keeps a single ElectrumClient instance alive for the
 * lifetime of a sync cycle and standardises retry behaviour.
 */
class ElectrumBlockchain(
    private val endpoint: ElectrumEndpoint,
    private val proxy: SocksProxyConfig?
) : Closeable {

    data class Metadata(
        val serverInfo: ServerFeaturesRes?,
        val blockHeight: Long?,
        val feeRateSatPerVb: Double?
    )

    private val clientLock = Any()
    private var client: ElectrumClient? = null

    private val maxAttempts: Int = endpoint.retry.coerceAtLeast(1)

    suspend fun fetchMetadata(): Metadata = executeWithRetry("metadata") { client ->
        val serverInfo = runCatching { client.serverFeatures() }.getOrNull()
        val headers = runCatching { client.blockHeadersSubscribe() }.getOrNull()
        val feeRateSatPerVb = runCatching { client.estimateFee(1u) }
            .getOrNull()
            ?.takeIf { value ->
                !value.isNaN() &&
                    !value.isInfinite() &&
                    value > 0.0
            }
        Metadata(
            serverInfo = serverInfo,
            blockHeight = headers?.height?.toLong(),
            feeRateSatPerVb = feeRateSatPerVb
        )
    }

    suspend fun syncWallet(
        wallet: Wallet,
        shouldRunFullScan: Boolean,
        hasChangeKeychain: Boolean,
        cancellationSignal: SyncCancellationSignal
    ) = executeWithRetry("walletSync") { client ->
        ensureActive(cancellationSignal)
        val syncPrefs = endpoint.sync
        val fullScanStopGap = syncPrefs.fullScanStopGap.coerceAtLeast(1)
        val revealDepth = (fullScanStopGap - 1).toUInt()
        val fullScanBatchSize = syncPrefs.fullScanBatchSize.coerceAtLeast(1)
        val incrementalBatchSize = syncPrefs.incrementalBatchSize.coerceAtLeast(1)

        if (shouldRunFullScan) {
            wallet.revealAddressesTo(KeychainKind.EXTERNAL, revealDepth)
            ensureActive(cancellationSignal)
            if (hasChangeKeychain) {
                wallet.revealAddressesTo(KeychainKind.INTERNAL, revealDepth)
                ensureActive(cancellationSignal)
            }
            wallet.startFullScan().use { builder ->
                val preparedBuilder = builder.inspectSpksForAllKeychains(
                    createFullScanInspector(cancellationSignal)
                )
                preparedBuilder.build().use { request ->
                    ensureActive(cancellationSignal)
                    client.fullScan(
                        request = request,
                        batchSize = fullScanBatchSize.toULong(),
                        stopGap = fullScanStopGap.toULong(),
                        fetchPrevTxouts = false
                    ).use { update ->
                        ensureActive(cancellationSignal)
                        wallet.applyUpdate(update)
                    }
                }
            }
        } else {
            wallet.startSyncWithRevealedSpks().use { builder ->
                val preparedBuilder = builder.inspectSpks(createSyncInspector(cancellationSignal))
                preparedBuilder.build().use { request ->
                    ensureActive(cancellationSignal)
                    client.sync(
                        request = request,
                        batchSize = incrementalBatchSize.toULong(),
                        fetchPrevTxouts = false
                    ).use { update ->
                        ensureActive(cancellationSignal)
                        wallet.applyUpdate(update)
                    }
                }
            }
        }
    }

    private fun createFullScanInspector(signal: SyncCancellationSignal): FullScanScriptInspector =
        object : FullScanScriptInspector {
            @Suppress("UNUSED_PARAMETER")
            override fun inspect(
                keychain: KeychainKind,
                derivationIndex: UInt,
                script: Script
            ) {
                ensureActive(signal)
            }
        }

    private fun createSyncInspector(signal: SyncCancellationSignal): SyncScriptInspector =
        object : SyncScriptInspector {
            @Suppress("UNUSED_PARAMETER")
            override fun inspect(script: Script, derivationIndex: ULong) {
                ensureActive(signal)
            }
        }

    private fun ensureActive(signal: SyncCancellationSignal) {
        if (signal.shouldCancel()) {
            throw CancellationException("Sync cancelled because app entered background")
        }
    }

    private suspend fun <T> executeWithRetry(
        operation: String,
        block: (ElectrumClient) -> T
    ): T {
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            val result = runCatching { block(acquireClient()) }
            if (result.isSuccess) {
                return result.getOrThrow()
            }
            val error = result.exceptionOrNull()
            if (error is CancellationException) {
                throw error
            }
            lastError = error
            resetClient()
            if (attempt < maxAttempts - 1) {
                delay(backoffDelayMillis(attempt))
            }
        }
        throw lastError ?: IllegalStateException("$operation failed without additional details.")
    }

    private fun acquireClient(): ElectrumClient =
        synchronized(clientLock) {
            client ?: createClient().also { client = it }
        }

    private fun createClient(): ElectrumClient = if (proxy != null) {
        ElectrumClient(
            endpoint.url,
            "${proxy.host}:${proxy.port}"
        )
    } else {
        ElectrumClient(
            endpoint.url,
            null
        )
    }

    private fun resetClient() {
        val cached = synchronized(clientLock) {
            client.also { client = null }
        }
        cached?.close()
    }

    private fun backoffDelayMillis(attempt: Int): Long {
        val base = 500L
        return base * (attempt + 1L)
    }

    override fun close() {
        resetClient()
    }
}
