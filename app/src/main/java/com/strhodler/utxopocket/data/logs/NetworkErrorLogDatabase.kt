package com.strhodler.utxopocket.data.logs

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NetworkErrorLogEntity::class],
    version = 2,
    exportSchema = true
)
abstract class NetworkErrorLogDatabase : RoomDatabase() {
    abstract fun networkErrorLogDao(): NetworkErrorLogDao

    companion object {
        const val NAME: String = "network_error_logs.db"
    }
}
