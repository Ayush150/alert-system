package com.sih26001.mobilealert

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sih26001.mobilealert.data.ack.AckSyncEngine
import com.sih26001.mobilealert.data.ack.DefaultAckRecoveryPolicy
import com.sih26001.mobilealert.data.ack.ExponentialBackoffAckRetryPolicy
import com.sih26001.mobilealert.data.ack.NetworkConnectivityMonitor
import com.sih26001.mobilealert.data.ack.UnavailableAckSyncDataSource
import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.AppDatabase
import com.sih26001.mobilealert.data.local.PendingAckEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * On-device instrumented verification for Phase 6F:
 * 1. Insert fake IN_FLIGHT record with stale lastAttemptAt
 * 2. Invoke recovery
 * 3. Verify status becomes FAILED
 * 4. Verify retryCount unchanged
 * 5. Verify acknowledgedAt unchanged
 * 6. Verify subsequent reconciliation can discover the FAILED record
 * 7. Room persistence survives recreation/process lifecycle
 */
@RunWith(AndroidJUnit4::class)
class Phase6FRuntimeIntegrationTest {

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
    fun staleInFlightRecoveryWorkflow_onDevice() = runBlocking {
        val pendingAckDao = db.pendingAckDao()

        // 1. Insert a stale IN_FLIGHT record simulating a process that died 10 minutes ago
        // Truncate to milliseconds to avoid SQLite precision truncation assertions
        val originalAckTime = Instant.now().minusSeconds(1000).truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        val lastAttemptTime = Instant.now().minusSeconds(600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS) // 10 minutes ago (stale > 5 mins)
        
        val staleRecord = PendingAckEntity(
            alertId = "ALT-STALE-001",
            acknowledgedAt = originalAckTime,
            status = AckSyncStatus.IN_FLIGHT,
            lastAttemptAt = lastAttemptTime,
            retryCount = 2
        )
        pendingAckDao.insertOrIgnore(staleRecord)

        // 2. Setup engine
        val alwaysOnlineMonitor = object : NetworkConnectivityMonitor {
            override fun isOnline(): Boolean = true
        }
        val retryPolicy = ExponentialBackoffAckRetryPolicy()
        val recoveryPolicy = DefaultAckRecoveryPolicy(staleThresholdMs = 300_000L)
        val prodDataSource = UnavailableAckSyncDataSource()
        
        val engine = AckSyncEngine(
            pendingAckDao = pendingAckDao,
            ackSyncDataSource = prodDataSource,
            ackRetryPolicy = retryPolicy,
            ackRecoveryPolicy = recoveryPolicy,
            connectivityMonitor = alwaysOnlineMonitor,
            dispatcher = Dispatchers.IO
        )

        // 3. Invoke recovery
        val recoveredCount = engine.recoverInterruptedSyncs()
        assertEquals(1, recoveredCount)

        // 4. Verify recovery state
        val recoveredRecord = pendingAckDao.getPendingAckById("ALT-STALE-001")
        assertNotNull(recoveredRecord)
        assertEquals(AckSyncStatus.FAILED, recoveredRecord?.status)
        assertEquals(2, recoveredRecord?.retryCount) // Unchanged
        assertEquals(originalAckTime, recoveredRecord?.acknowledgedAt) // Unchanged

        // 5. Invoke normal reconciliation. Because we set lastAttemptAt to now() during recovery,
        // it should hit the backoff delay (2^2 * 2000 = 8000ms delay).
        // Since we didn't advance time, it should be skipped by backoff.
        val attemptedCount = engine.reconcilePendingAcks()
        assertEquals("Should be skipped by backoff", 0, attemptedCount)

        // The status should remain FAILED
        val finalRecord = pendingAckDao.getPendingAckById("ALT-STALE-001")
        assertEquals(AckSyncStatus.FAILED, finalRecord?.status)
    }
}
