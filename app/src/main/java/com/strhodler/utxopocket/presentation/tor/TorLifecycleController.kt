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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TorLifecycleController @Inject constructor(
    private val scope: CoroutineScope,
    private val torManager: TorManager,
    private val refreshWallets: suspend (BitcoinNetwork) -> Unit,
    private val nodeConfigFlow: Flow<NodeConfig>,
    private val networkFlow: Flow<BitcoinNetwork>,
    private val networkStatusFlow: Flow<Boolean>,
    private val torVisibilityState: MutableStateFlow<Boolean>
) {

    private var lastTorWasRunning = false
    private var pendingTorStop: Job? = null
    private var pendingTorStart: Job? = null

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
        torVisibilityState.value = requiresTor || torActive

        if (!snapshot.networkOnline) {
            cancelPendingTorStart()
            lastTorWasRunning = torRunning && requiresTor
            return
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

        val shouldTriggerRefresh = requiresTor &&
            torRunning &&
            !lastTorWasRunning &&
            snapshot.config.hasActiveSelection(snapshot.network)
        if (shouldTriggerRefresh) {
            scope.launch {
                refreshWallets(snapshot.network)
            }
        }
        lastTorWasRunning = requiresTor && torRunning
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

    private fun cancelPendingTorStart() {
        pendingTorStart?.cancel()
        pendingTorStart = null
    }

    private data class TorLifecycleSnapshot(
        val status: TorStatus,
        val config: NodeConfig,
        val network: BitcoinNetwork,
        val networkOnline: Boolean
    )

    private companion object {
        private const val TAG = "TorLifecycleController"
    }
}
