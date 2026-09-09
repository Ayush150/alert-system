package com.sih26001.mobilealert.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Instrumented test verifying the Room database configuration (version 2),
 * verifying both AlertDao and PendingAckDao operate correctly on device.
 *
 * Prototype Note: For this prototype development phase, fallbackToDestructiveMigration()
 * is permitted when upgrading from version 1 to 2.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private lateinit var db: AppDatabase
    private lateinit var alertDao: AlertDao
    private lateinit var pendingAckDao: PendingAckDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
        alertDao = db.alertDao()
        pendingAckDao = db.pendingAckDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun database_stores_and_reads_alerts_and_pending_acks() = runBlocking {
        val now = Instant.now()
        val alert = AlertEntity(
            alertId = "ALT-ROOM-001",
            eventType = "landslide",
            severity = AlertSeverity.CRITICAL,
            riskScore = 0.85,
            locationName = "Sector 4",
            latitude = 30.12,
            longitude = 78.34,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            topDrivers = listOf("Rainfall"),
            recommendedAction = "Evacuate",
            affectedAssets = emptyList(),
            source = "Test",
            dataQuality = "GOOD",
            requiresAck = true,
            status = AlertStatus.ACTIVE,
            receivedAt = now,
            acknowledgedAt = null
        )

        alertDao.insertAlert(alert)
        val readAlert = alertDao.getAlertById("ALT-ROOM-001")
        assertNotNull(readAlert)
        assertEquals("ALT-ROOM-001", readAlert?.alertId)
        assertEquals(AlertStatus.ACTIVE, readAlert?.status)

        // Test PendingAckDao persistence
        val pendingAck = PendingAckEntity(
            alertId = "ALT-ROOM-001",
            acknowledgedAt = now,
            retryCount = 0,
            lastAttemptAt = null,
            status = AckSyncStatus.PENDING
        )
        pendingAckDao.insertOrIgnore(pendingAck)

        val pendingList = pendingAckDao.getPendingAcks()
        assertEquals(1, pendingList.size)
        assertEquals("ALT-ROOM-001", pendingList[0].alertId)
        assertEquals(AckSyncStatus.PENDING, pendingList[0].status)

        // Test duplicate insert is ignored
        val secondInsert = pendingAckDao.insertOrIgnore(pendingAck)
        assertEquals(-1L, secondInsert)
        assertEquals(1, pendingAckDao.getPendingAcks().size)

        // Test update
        val inFlight = pendingAck.copy(status = AckSyncStatus.IN_FLIGHT, retryCount = 1)
        pendingAckDao.update(inFlight)
        val updated = pendingAckDao.getPendingAckById("ALT-ROOM-001")
        assertEquals(AckSyncStatus.IN_FLIGHT, updated?.status)
        assertEquals(1, updated?.retryCount)

        // Test eligible query excludes COMPLETED
        val eligibleList = pendingAckDao.getEligiblePendingAcks()
        assertEquals(1, eligibleList.size)
        assertEquals("ALT-ROOM-001", eligibleList[0].alertId)

        val completed = inFlight.copy(status = AckSyncStatus.COMPLETED)
        pendingAckDao.update(completed)
        val eligibleAfterCompleted = pendingAckDao.getEligiblePendingAcks()
        assertTrue(eligibleAfterCompleted.isEmpty())

        // Test delete
        pendingAckDao.delete("ALT-ROOM-001")
        assertTrue(pendingAckDao.getPendingAcks().isEmpty())
    }
}
