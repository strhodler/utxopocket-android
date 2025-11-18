package com.strhodler.utxopocket.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.strhodler.utxopocket.domain.model.ConnectivityState
import com.strhodler.utxopocket.domain.service.ConnectivityMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DefaultConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context
) : ConnectivityMonitor {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService()

    private val _state = MutableStateFlow(readConnectivity())
    override val state: StateFlow<ConnectivityState> = _state.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateState()
        }

        override fun onLost(network: Network) {
            updateState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateState()
        }
    }

    init {
        connectivityManager?.let { manager ->
            runCatching {
                manager.registerNetworkCallback(
                    NetworkRequest.Builder().build(),
                    callback
                )
            }.onFailure {
                updateState()
            }
        }
    }

    private fun updateState() {
        _state.value = readConnectivity()
    }

    private fun readConnectivity(): ConnectivityState {
        val manager = connectivityManager ?: return ConnectivityState()
        val network = manager.activeNetwork ?: return ConnectivityState()
        val capabilities = manager.getNetworkCapabilities(network) ?: return ConnectivityState()
        val isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val onLocalNetwork = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val onVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val onCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        return ConnectivityState(
            isOnline = isOnline,
            onLocalNetwork = onLocalNetwork,
            onVpn = onVpn,
            onCellular = onCellular
        )
    }
}
