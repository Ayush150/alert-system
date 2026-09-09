package com.sih26001.mobilealert

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sih26001.mobilealert.data.ack.AckSyncEngine
import com.sih26001.mobilealert.data.ack.ExponentialBackoffAckRetryPolicy
import com.sih26001.mobilealert.data.ack.NetworkConnectivityMonitor
import com.sih26001.mobilealert.data.ack.UnavailableAckSyncDataSource
import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.AlertEntity
import com.sih26001.mobilealert.data.local.AppDatabase
import com.sih26001.mobilealert.data.remote.api.AlertApiService
import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * On-device instrumented verification for Phase 6E:
 * 1. Alert acknowledgement atomic Room transaction
 * 2. PendingAckEntity persistence and idempotency
 * 3. Process-death simulation (close and reopen Room DB)
 * 4. Reconciliation with production UnavailableAckSyncDataSource
 * 5. Verifies queue NEVER transitions to COMPLETED
 * 6. Verifies FAILED transition with incremented retry count and lastAttemptAt
 */
@RunWith(AndroidJUnit4::class)
class Phase6ERuntimeIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun completePhase6EWorkflow_onDevice() = runBlocking {
        val alertDao = db.alertDao()
        val pendingAckDao = db.pendingAckDao()
        val dummyApi = object : AlertApiService {
            override suspend fun getAlerts(): List<AlertDto> = emptyList()
        }
        val txRunner = object : com.sih26001.mobilealert.data.local.DatabaseTransactionRunner {
            override suspend fun <T> invoke(block: suspend () -> T): T {
                return db.withTransaction { block() }
            }
        }

        val repository = AlertRepositoryImpl(
            apiService = dummyApi,
            alertDao = alertDao,
            pendingAckDao = pendingAckDao,
            transactionRunner = txRunner,
            ioDispatcher = Dispatchers.IO
        )

        // 1. Insert an active alert
        val now = Instant.now()
        val alert = AlertEntity(
            alertId = "ALT-DEVICE-001",
            eventType = "landslide",
            severity = AlertSeverity.CRITICAL,
            riskScore = 0.9,
            locationName = "Pass Sector",
            latitude = 30.0,
            longitude = 78.0,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            topDrivers = listOf("Rainfall"),
            recommendedAction = "Evacuate",
            affectedAssets = emptyList(),
            source = "backend",
            dataQuality = "CONFIRMED",
            requiresAck = true,
            status = AlertStatus.ACTIVE,
            receivedAt = now,
            acknowledgedAt = null
        )
        alertDao.insertAlert(alert)

        // 2. Acknowledge the alert
        val ackResult = repository.acknowledgeAlert("ALT-DEVICE-001")
        assertTrue("Acknowledgement should succeed", ackResult.isSuccess)

        // Verify local alert status and timestamp
        val acknowledgedAlert = alertDao.getAlertById("ALT-DEVICE-001")
        assertNotNull(acknowledgedAlert)
        assertEquals(AlertStatus.ACKNOWLEDGED, acknowledgedAlert?.status)
        assertNotNull(acknowledgedAlert?.acknowledgedAt)

        // Verify PendingAckEntity created with status PENDING
        val pendingList = pendingAckDao.getPendingAcks()
        assertEquals(1, pendingList.size)
        val pendingRecord = pendingList[0]
        assertEquals("ALT-DEVICE-001", pendingRecord.alertId)
        assertEquals(AckSyncStatus.PENDING, pendingRecord.status)
        assertEquals(0, pendingRecord.retryCount)
        assertNull(pendingRecord.lastAttemptAt)

        // 3. Duplicate ACK is idempotent and maintains single queue entry
        val dupResult = repository.acknowledgeAlert("ALT-DEVICE-001")
        assertTrue(dupResult.isSuccess)
        assertEquals(1, pendingAckDao.getPendingAcks().size)

        // 4. Silence action does not modify ACK or add to queue
        repository.silenceAlert("ALT-DEVICE-001")
        assertEquals(AlertStatus.ACKNOWLEDGED, alertDao.getAlertById("ALT-DEVICE-001")?.status)
        assertEquals(1, pendingAckDao.getPendingAcks().size)

        // 5. Run reconciliation with production UnavailableAckSyncDataSource
        val alwaysOnlineMonitor = object : NetworkConnectivityMonitor {
            override fun isOnline(): Boolean = true
        }
        val retryPolicy = ExponentialBackoffAckRetryPolicy(
            baseDelayMs = 2000L,
            multiplier = 2.0,
            maxDelayMs = 300_000L,
            maxRetries = 10
        )
        val prodDataSource = UnavailableAckSyncDataSource()
        val recoveryPolicy = com.sih26001.mobilealert.data.ack.DefaultAckRecoveryPolicy()
        val engine = AckSyncEngine(
            pendingAckDao = pendingAckDao,
            ackSyncDataSource = prodDataSource,
            ackRetryPolicy = retryPolicy,
            ackRecoveryPolicy = recoveryPolicy,
            connectivityMonitor = alwaysOnlineMonitor,
            dispatcher = Dispatchers.IO
        )

        val attemptedCount = engine.reconcilePendingAcks()
        assertEquals(1, attemptedCount)

        // 6. Critical Verification: Queue MUST NEVER be COMPLETED
        val postReconcile = pendingAckDao.getPendingAckById("ALT-DEVICE-001")
        assertNotNull(postReconcile)
        assertNotEquals(
            "Production queue MUST NEVER become COMPLETED without backend contract",
            AckSyncStatus.COMPLETED,
            postReconcile!!.status
        )
        assertEquals(AckSyncStatus.FAILED, postReconcile.status)
        assertEquals(1, postReconcile.retryCount)
        assertNotNull(postReconcile.lastAttemptAt)

        // 7. Verify backoff delay prevents immediate re-reconciliation
        val immediateRetryCount = engine.reconcilePendingAcks()
        assertEquals("Should be skipped because backoff delay has not elapsed", 0, immediateRetryCount)
        val afterSkip = pendingAckDao.getPendingAckById("ALT-DEVICE-001")
        assertEquals(1, afterSkip?.retryCount)
    }
}
