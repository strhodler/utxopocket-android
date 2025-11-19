package com.strhodler.utxopocket.tor

import com.strhodler.utxopocket.domain.model.SocksProxyConfig
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.service.TorManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class TorProxyProvider @Inject constructor(
    private val torManager: TorManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _proxy = MutableStateFlow((torManager.status.value as? TorStatus.Running)?.proxy)

    val proxy: StateFlow<SocksProxyConfig?> = _proxy.asStateFlow()

    init {
        scope.launch {
            torManager.status.collect { status ->
                when (status) {
                    is TorStatus.Running -> _proxy.value = status.proxy
                    TorStatus.Stopped -> _proxy.value = null
                    is TorStatus.Error -> _proxy.value = null
                    is TorStatus.Connecting -> Unit
                }
            }
        }
    }

    fun currentProxy(): SocksProxyConfig? = _proxy.value

    suspend fun awaitProxy(timeoutMillis: Long? = null): SocksProxyConfig? {
        val immediate = _proxy.value
        if (immediate != null) {
            return immediate
        }
        return if (timeoutMillis != null) {
            withTimeoutOrNull(timeoutMillis) {
                proxy.filterNotNull().first()
            }
        } else {
            proxy.filterNotNull().first()
        }
    }

    suspend fun restart(): Result<SocksProxyConfig> =
        torManager.start().onSuccess { proxy -> _proxy.value = proxy }

    fun shutdown() {
        scope.cancel()
    }
}
