package com.strhodler.utxopocket.data.logs

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService
import com.strhodler.utxopocket.domain.model.NetworkEndpointType
import com.strhodler.utxopocket.domain.model.NetworkErrorLog
import com.strhodler.utxopocket.domain.model.NetworkErrorLogEvent
import com.strhodler.utxopocket.domain.model.NetworkLogOperation
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.model.NetworkTransport
import com.strhodler.utxopocket.domain.model.TorStatus
import com.strhodler.utxopocket.domain.node.EndpointKind
import com.strhodler.utxopocket.domain.node.EndpointScheme
import com.strhodler.utxopocket.domain.node.NodeEndpointClassifier
import com.strhodler.utxopocket.data.logs.NetworkLogSanitizer.maskHost
import com.strhodler.utxopocket.data.logs.NetworkLogSanitizer.sanitizeMessage
import com.strhodler.utxopocket.data.logs.NetworkLogSanitizer.rootCause
import com.strhodler.utxopocket.domain.repository.AppPreferencesRepository
import com.strhodler.utxopocket.domain.repository.NetworkErrorLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

@Singleton
class DefaultNetworkErrorLogRepository @Inject constructor(
    private val dao: NetworkErrorLogDao,
    private val appPreferencesRepository: AppPreferencesRepository,
    @param:ApplicationContext private val context: Context
) : NetworkErrorLogRepository {

    override val logs: Flow<List<NetworkErrorLog>> =
        dao.observeLogs().map { list -> list.map { it.toDomain() } }

    override val loggingEnabled: Flow<Boolean> = appPreferencesRepository.networkLogsEnabled
    override val infoSheetSeen: Flow<Boolean> = appPreferencesRepository.networkLogsInfoSeen

    private val appVersion by lazy { resolveAppVersion() }
    private val androidVersion by lazy { Build.VERSION.RELEASE ?: "unknown" }

    override suspend fun setLoggingEnabled(enabled: Boolean) {
        appPreferencesRepository.setNetworkLogsEnabled(enabled)
        if (!enabled) {
            dao.clear()
        }
    }

    override suspend fun clear() {
        dao.clear()
    }

    override suspend fun setInfoSheetSeen(seen: Boolean) {
        appPreferencesRepository.setNetworkLogsInfoSeen(seen)
    }

    override suspend fun record(event: NetworkErrorLogEvent) {
        val enabled = appPreferencesRepository.networkLogsEnabled.first()
        if (!enabled) return

        val sanitized = sanitize(event) ?: return
        dao.insert(sanitized)
        dao.deleteOlderThan(System.currentTimeMillis() - RETENTION_MILLIS)
        trimToLimit()
    }

    private suspend fun trimToLimit() {
        val count = dao.count()
        if (count <= MAX_ENTRIES) return
        val excess = count - MAX_ENTRIES
        val ids = dao.oldestIds(excess)
        if (ids.isNotEmpty()) {
            dao.deleteByIds(ids)
        }
    }

    private fun sanitize(event: NetworkErrorLogEvent): NetworkErrorLogEntity? {
        val endpointInfo = extractEndpointInfo(event.endpoint)
        val hostMask = endpointInfo?.maskedHost ?: fallbackHostLabel(event)
        val hostHash = endpointInfo?.hostHash
        val transport = (event.transport ?: endpointInfo?.transport)?.name ?: NetworkTransport.Unknown.name
        val endpointType = (event.endpointTypeHint ?: endpointInfo?.endpointType)
            ?.name ?: NetworkEndpointType.Unknown.name
        val errorKind = event.error.rootCause().javaClass.simpleName.takeUnless { it.isNullOrBlank() }
        val errorMessage = sanitizeMessage(event.error, endpointInfo?.host ?: hostMask)

        val torStatus = event.torStatus
        val torBootstrapPercent = when (torStatus) {
            is TorStatus.Connecting -> torStatus.progress
            else -> null
        }

        return NetworkErrorLogEntity(
            timestamp = System.currentTimeMillis(),
            appVersion = appVersion,
            androidVersion = androidVersion,
            networkType = event.networkType ?: currentNetworkType(),
            operation = event.operation.name,
            endpointType = endpointType,
            transport = transport,
            hostMask = hostMask,
            hostHash = hostHash,
            port = endpointInfo?.port,
            usedTor = event.usedTor,
            torBootstrapPercent = torBootstrapPercent,
            errorKind = errorKind,
            errorMessage = errorMessage,
            durationMs = event.durationMs,
            retryCount = event.retryCount,
            nodeSource = (event.nodeSource ?: NetworkNodeSource.Unknown).name
        )
    }

    private fun extractEndpointInfo(raw: String?): EndpointInfo? {
        if (raw.isNullOrBlank()) return null
        val normalized = runCatching { NodeEndpointClassifier.normalize(raw) }.getOrNull() ?: return null
        val masked = maskHost(normalized.host)
        val endpointType = when (normalized.kind) {
            EndpointKind.ONION -> NetworkEndpointType.Onion
            EndpointKind.LOCAL -> NetworkEndpointType.Local
            EndpointKind.PUBLIC -> NetworkEndpointType.Clearnet
        }
        val transport = when (normalized.scheme) {
            EndpointScheme.SSL -> NetworkTransport.SSL
            EndpointScheme.TCP -> NetworkTransport.TCP
        }
        return EndpointInfo(
            host = normalized.host,
            maskedHost = masked?.first,
            hostHash = masked?.second,
            port = normalized.port,
            transport = transport,
            endpointType = endpointType
        )
    }

    private fun fallbackHostLabel(event: NetworkErrorLogEvent): String? =
        when {
            event.networkType == "offline" -> "offline"
            event.operation == NetworkLogOperation.TorBootstrap -> "tor"
            else -> "none"
        }

    private fun currentNetworkType(): String? {
        val connectivityManager: ConnectivityManager = context.getSystemService() ?: return null
        val active = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(active) ?: return null
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }

    private fun resolveAppVersion(): String {
        return runCatching {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pkgInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            "$versionName ($versionCode)"
        }.getOrElse {
            "unknown"
        }
    }

    private data class EndpointInfo(
        val host: String,
        val maskedHost: String?,
        val hostHash: String?,
        val port: Int?,
        val transport: NetworkTransport,
        val endpointType: NetworkEndpointType
    )

    companion object {
        private const val MAX_ENTRIES = 200
        private const val RETENTION_DAYS = 14L
        private const val RETENTION_MILLIS = RETENTION_DAYS * 24 * 60 * 60 * 1000
    }
}

private fun NetworkErrorLogEntity.toDomain(): NetworkErrorLog {
    return NetworkErrorLog(
        id = id,
        timestamp = timestamp,
        appVersion = appVersion,
        androidVersion = androidVersion,
        networkType = networkType,
        operation = enumValueOrDefault(operation, NetworkLogOperation.NodeConnect),
        endpointType = enumValueOrDefault(endpointType, NetworkEndpointType.Unknown),
        transport = enumValueOrDefault(transport, NetworkTransport.Unknown),
        hostMask = hostMask,
        hostHash = hostHash,
        port = port,
        usedTor = usedTor,
        torBootstrapPercent = torBootstrapPercent,
        errorKind = errorKind,
        errorMessage = errorMessage,
        durationMs = durationMs,
        retryCount = retryCount,
        nodeSource = enumValueOrDefault(nodeSource, NetworkNodeSource.Unknown)
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, default: T): T =
    runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
