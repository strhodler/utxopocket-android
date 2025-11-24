package com.strhodler.utxopocket.domain.model

enum class NetworkLogOperation {
    TorBootstrap,
    NodeConnect,
    NodeSync,
    Broadcast,
    Ping
}

enum class NetworkEndpointType {
    Onion,
    Clearnet,
    Local,
    Preset,
    Unknown
}

enum class NetworkTransport {
    SSL,
    TCP,
    Unknown
}

enum class NetworkNodeSource {
    Public,
    Custom,
    None,
    Unknown
}

data class NetworkErrorLog(
    val id: Long,
    val timestamp: Long,
    val appVersion: String,
    val androidVersion: String,
    val networkType: String?,
    val operation: NetworkLogOperation,
    val endpointType: NetworkEndpointType,
    val transport: NetworkTransport,
    val hostMask: String?,
    val hostHash: String?,
    val port: Int?,
    val usedTor: Boolean,
    val torBootstrapPercent: Int?,
    val errorKind: String?,
    val errorMessage: String,
    val durationMs: Long?,
    val retryCount: Int?,
    val nodeSource: NetworkNodeSource
)

data class NetworkErrorLogEvent(
    val operation: NetworkLogOperation,
    val endpoint: String?,
    val transport: NetworkTransport? = null,
    val usedTor: Boolean,
    val error: Throwable,
    val durationMs: Long? = null,
    val retryCount: Int? = null,
    val torStatus: TorStatus? = null,
    val endpointTypeHint: NetworkEndpointType? = null,
    val nodeSource: NetworkNodeSource? = null,
    val networkType: String? = null
)
