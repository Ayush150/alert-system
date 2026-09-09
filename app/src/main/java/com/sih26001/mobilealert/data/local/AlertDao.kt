package com.sih26001.mobilealert.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.sih26001.mobilealert.domain.model.AlertStatus
import java.time.Instant

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY issuedAt DESC")
    fun observeAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE status IN (:activeStatuses) ORDER BY issuedAt DESC")
    fun observeActiveAlerts(activeStatuses: List<String>): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE alertId = :id")
    fun observeAlertById(id: String): Flow<AlertEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAlerts(alerts: List<AlertEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET status = :status WHERE alertId = :id")
    fun updateStatus(id: String, status: AlertStatus)

    @Query("UPDATE alerts SET acknowledgedAt = :timestamp WHERE alertId = :id")
    fun updateAcknowledgedAt(id: String, timestamp: Instant)
}
