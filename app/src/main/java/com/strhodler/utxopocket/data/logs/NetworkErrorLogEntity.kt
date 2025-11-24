package com.strhodler.utxopocket.data.logs

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_error_logs")
data class NetworkErrorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val appVersion: String,
    val androidVersion: String,
    val networkType: String?,
    val operation: String,
    val endpointType: String,
    val transport: String,
    val hostMask: String?,
    val hostHash: String?,
    val port: Int?,
    val usedTor: Boolean,
    val torBootstrapPercent: Int?,
    val errorKind: String?,
    val errorMessage: String,
    val durationMs: Long?,
    val retryCount: Int?,
    val nodeSource: String
)
