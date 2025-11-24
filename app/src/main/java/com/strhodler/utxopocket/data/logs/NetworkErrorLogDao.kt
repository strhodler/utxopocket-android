package com.strhodler.utxopocket.data.logs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkErrorLogDao {

    @Query("SELECT * FROM network_error_logs ORDER BY timestamp DESC, id DESC")
    fun observeLogs(): Flow<List<NetworkErrorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NetworkErrorLogEntity): Long

    @Query("DELETE FROM network_error_logs")
    suspend fun clear()

    @Query("DELETE FROM network_error_logs WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM network_error_logs")
    suspend fun count(): Int

    @Query("SELECT id FROM network_error_logs ORDER BY timestamp ASC, id ASC LIMIT :count")
    suspend fun oldestIds(count: Int): List<Long>

    @Query("DELETE FROM network_error_logs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
