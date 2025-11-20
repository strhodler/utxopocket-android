package com.strhodler.utxopocket.presentation.tor

import android.util.Log
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.NodeConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.model.hasActiveSelection
import com.strhodler.utxopocket.domain.model.requiresTor
import com.strhodler.utxopocket.domain.service.TorManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class TorLifecycleController @Inject constructor(
    private val scope: CoroutineScope,
    private val torManager: TorManager,
    private val refreshWallets: suspend (BitcoinNetwork) -> Unit,
    private val nodeConfigFlow: Flow<NodeConfig>,
    private val networkFlow: Flow<BitcoinNetwork>,
    private val networkStatusFlow: Flow<Boolean>
) {

    private var lastTorWasRunning = false
    private var pendingTorRestart = false
    private var lastNetworkOnline = true
    private var pendingTorStop: Job? = null
    private var pendingTorStart: Job? = null
    private var pendingTorRestartJob: Job? = null

    fun start() {
        scope.launch {
            combine(
                torManager.status,
                nodeConfigFlow,
                networkFlow,
                networkStatusFlow
            ) { status, config, network, online ->
                TorLifecycleSnapshot(
                    status = status,
                    config = config,
                    network = network,
                    networkOnline = online
                )
            }.collect { snapshot ->
                handleSnapshot(snapshot)
            }
        }
    }

    private fun handleSnapshot(snapshot: TorLifecycleSnapshot) {
        val requiresTor = snapshot.config.requiresTor(snapshot.network)
        val torStatus = snapshot.status
        val torRunning = torStatus is TorStatus.Running
        val torConnecting = torStatus is TorStatus.Connecting
        val torActive = torRunning || torConnecting

        if (!snapshot.networkOnline) {
            cancelPendingTorStart()
            pendingTorRestart = requiresTor && torActive
            lastTorWasRunning = torRunning && requiresTor
            lastNetworkOnline = false
            return
        }

        if (requiresTor && pendingTorRestart) {
            ensureTorRestarted()
        }

        if (requiresTor && (torStatus is TorStatus.Stopped || torStatus is TorStatus.Error)) {
            ensureTorStarted()
        } else if (!requiresTor) {
            cancelPendingTorStart()
        }

        if (!requiresTor && torActive) {
            scheduleTorStop()
        } else {
            cancelPendingTorStop()
        }

        val recoveredFromOffline = !lastNetworkOnline
        val shouldTriggerRefresh = requiresTor &&
            torRunning &&
            snapshot.config.hasActiveSelection(snapshot.network) &&
            (!lastTorWasRunning || recoveredFromOffline || pendingTorRestart) &&
            pendingTorRestartJob?.isActive != true &&
            !pendingTorRestart
        if (shouldTriggerRefresh) {
            scope.launch {
                refreshWallets(snapshot.network)
            }
        }
        lastTorWasRunning = requiresTor && torRunning
        if (torRunning) {
            pendingTorRestart = false
        }
        lastNetworkOnline = true
    }

    private fun scheduleTorStop() {
        if (pendingTorStop?.isActive == true) return
        pendingTorStop = scope.launch {
            runCatching { torManager.stop() }
                .onFailure { error ->
                    Log.w(TAG, "Unable to stop Tor when it is no longer required", error)
                }
            pendingTorStop = null
        }
    }

    private fun cancelPendingTorStop() {
        pendingTorStop?.cancel()
        pendingTorStop = null
    }

    private fun ensureTorStarted() {
        if (pendingTorStart?.isActive == true) return
        pendingTorStart = scope.launch {
            runCatching { torManager.start() }
                .onFailure { error ->
                    Log.w(TAG, "Unable to start Tor when it is required", error)
                }
            pendingTorStart = null
        }
    }

    private fun ensureTorRestarted() {
        if (pendingTorRestartJob?.isActive == true) return
        pendingTorRestartJob = scope.launch {
            pendingTorRestart = true
            runCatching { torManager.stop() }
                .onFailure { error ->
                    Log.w(TAG, "Unable to stop Tor for restart after network recovery", error)
                }
            waitForTorState { state ->
                state is TorStatus.Stopped || state is TorStatus.Error
            }
            runCatching { torManager.start() }
                .onFailure { error ->
                    Log.w(TAG, "Unable to start Tor after restart attempt", error)
                }
            val ready = waitForTorState { state ->
                state is TorStatus.Running || state is TorStatus.Error || state is TorStatus.Stopped
            }
            pendingTorRestart = ready !is TorStatus.Running
            pendingTorRestartJob = null
        }
    }

    private fun cancelPendingTorStart() {
        pendingTorStart?.cancel()
        pendingTorStart = null
    }

    private suspend fun waitForTorState(predicate: (TorStatus) -> Boolean): TorStatus? =
        withTimeoutOrNull(RESTART_TIMEOUT_MILLIS) {
            torManager.status.first { state -> predicate(state) }
        }

    private data class TorLifecycleSnapshot(
        val status: TorStatus,
        val config: NodeConfig,
        val network: BitcoinNetwork,
        val networkOnline: Boolean
    )

    private companion object {
        private const val TAG = "TorLifecycleController"
        private const val RESTART_TIMEOUT_MILLIS = 30_000L
    }
}
