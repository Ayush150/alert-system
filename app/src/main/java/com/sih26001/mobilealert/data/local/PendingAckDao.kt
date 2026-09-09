package com.sih26001.mobilealert.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingAckDao {

    @Query("SELECT * FROM pending_acks ORDER BY acknowledgedAt ASC")
    fun observePendingAcks(): Flow<List<PendingAckEntity>>

    @Query("SELECT * FROM pending_acks ORDER BY acknowledgedAt ASC")
    fun getPendingAcks(): List<PendingAckEntity>

    @Query("SELECT * FROM pending_acks WHERE status != 'COMPLETED' ORDER BY acknowledgedAt ASC")
    fun getEligiblePendingAcks(): List<PendingAckEntity>

    @Query("SELECT * FROM pending_acks WHERE alertId = :alertId")
    fun getPendingAckById(alertId: String): PendingAckEntity?

    @Query("SELECT status FROM pending_acks WHERE alertId = :alertId")
    fun observeAckSyncStatus(alertId: String): Flow<AckSyncStatus?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnore(entity: PendingAckEntity): Long

    @Update
    fun update(entity: PendingAckEntity)

    @Query("DELETE FROM pending_acks WHERE alertId = :alertId")
    fun delete(alertId: String)
}
