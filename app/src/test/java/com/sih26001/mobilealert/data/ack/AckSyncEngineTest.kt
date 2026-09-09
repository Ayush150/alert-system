package com.sih26001.mobilealert.data.ack

import com.sih26001.mobilealert.data.local.AckSyncStatus
import com.sih26001.mobilealert.data.local.PendingAckDao
import com.sih26001.mobilealert.data.local.PendingAckEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class AckSyncEngineTest {

    private lateinit var fakeDao: FakePendingAckDao
    private lateinit var fakeDataSource: FakeAckSyncDataSource
    private lateinit var fakeConnectivityMonitor: FakeConnectivityMonitor
    private val retryPolicy = ExponentialBackoffAckRetryPolicy(
        baseDelayMs = 1_000L,
        multiplier = 2.0,
        maxDelayMs = 10_000L,
        maxRetries = 5
    )
    private val recoveryPolicy = DefaultAckRecoveryPolicy(staleThresholdMs = 300_000L)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        fakeDao = FakePendingAckDao()
        fakeDataSource = FakeAckSyncDataSource()
        fakeConnectivityMonitor = FakeConnectivityMonitor(online = true)
    }

    private fun createEngine(): AckSyncEngine {
        return AckSyncEngine(
            pendingAckDao = fakeDao,
            ackSyncDataSource = fakeDataSource,
            ackRetryPolicy = retryPolicy,
            ackRecoveryPolicy = recoveryPolicy,
            connectivityMonitor = fakeConnectivityMonitor,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `reconcilePendingAcks transitions PENDING to IN_FLIGHT then COMPLETED on success`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity(
                alertId = "ALT-001",
                acknowledgedAt = now,
                retryCount = 0,
                lastAttemptAt = null,
                status = AckSyncStatus.PENDING
            )
        )
        fakeDataSource.shouldSucceed = true

        val engine = createEngine()
        val processed = engine.reconcilePendingAcks()

        assertEquals(1, processed)
        val record = fakeDao.getPendingAckById("ALT-001")
        assertNotNull(record)
        assertEquals(AckSyncStatus.COMPLETED, record!!.status)
        assertNotNull(record.lastAttemptAt)
        assertEquals(0, record.retryCount)
    }

    @Test
    fun `recoverInterruptedSyncs converts stale IN_FLIGHT to FAILED`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity(
                alertId = "ALT-002",
                acknowledgedAt = now.minusSeconds(600),
                retryCount = 1,
                lastAttemptAt = now.minusSeconds(400), // Stale
                status = AckSyncStatus.IN_FLIGHT
            )
        )

        val engine = createEngine()
        val recovered = engine.recoverInterruptedSyncs()

        assertEquals(1, recovered)
        val record = fakeDao.getPendingAckById("ALT-002")
        assertNotNull(record)
        assertEquals(AckSyncStatus.FAILED, record!!.status)
        assertEquals(1, record.retryCount) // Unchanged
    }

    @Test
    fun `recoverInterruptedSyncs leaves fresh IN_FLIGHT unchanged`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity(
                alertId = "ALT-003",
                acknowledgedAt = now.minusSeconds(600),
                retryCount = 1,
                lastAttemptAt = now.minusSeconds(100), // Fresh
                status = AckSyncStatus.IN_FLIGHT
            )
        )

        val engine = createEngine()
        val recovered = engine.recoverInterruptedSyncs()

        assertEquals(0, recovered)
        val record = fakeDao.getPendingAckById("ALT-003")
        assertEquals(AckSyncStatus.IN_FLIGHT, record!!.status)
    }

    @Test
    fun `concurrent reconcile calls do not duplicate same alert synchronization`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity(
                alertId = "ALT-004",
                acknowledgedAt = now,
                retryCount = 0,
                lastAttemptAt = null,
                status = AckSyncStatus.PENDING
            )
        )
        fakeDataSource.shouldSucceed = true
        fakeDataSource.delayMs = 1000L // Ensure they overlap

        val engine = createEngine()
        
        // Launch two concurrent reconciliations
        val deferred1 = async { engine.reconcilePendingAcks() }
        val deferred2 = async { engine.reconcilePendingAcks() }

        val results = awaitAll(deferred1, deferred2)
        
        // Only one should process the alert
        val totalAttempted = results.sum()
        assertEquals(1, totalAttempted)
        assertEquals(1, fakeDataSource.synchronizeCallCount)
        assertEquals(AckSyncStatus.COMPLETED, fakeDao.getPendingAckById("ALT-004")?.status)
    }

    @Test
    fun `different alerts can synchronize concurrently`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity("ALT-005", now, 0, null, AckSyncStatus.PENDING)
        )
        fakeDao.insertOrIgnore(
            PendingAckEntity("ALT-006", now, 0, null, AckSyncStatus.PENDING)
        )
        fakeDataSource.shouldSucceed = true

        val engine = createEngine()
        val processed = engine.reconcilePendingAcks()

        assertEquals(2, processed)
        assertEquals(2, fakeDataSource.synchronizeCallCount)
    }

    @Test
    fun `reconcilePendingAcks skips reconciliation when device is offline`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity("ALT-007", now, 0, null, AckSyncStatus.PENDING)
        )
        fakeConnectivityMonitor.online = false

        val engine = createEngine()
        val processed = engine.reconcilePendingAcks()

        assertEquals(0, processed)
        val record = fakeDao.getPendingAckById("ALT-007")
        assertNotNull(record)
        assertEquals(AckSyncStatus.PENDING, record!!.status)
        assertEquals(0, record.retryCount)
    }

    @Test
    fun `exception thrown by transport does not crash engine and marks record FAILED`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity("ALT-008", now, 0, null, AckSyncStatus.PENDING)
        )
        fakeDataSource.throwException = true

        val engine = createEngine()
        val processed = engine.reconcilePendingAcks()

        assertEquals(1, processed)
        val record = fakeDao.getPendingAckById("ALT-008")
        assertNotNull(record)
        assertEquals(AckSyncStatus.FAILED, record!!.status)
        assertEquals(1, record.retryCount)
    }

    @Test
    fun `one failing record does not prevent other records from synchronizing`() = runTest(testDispatcher) {
        val now = Instant.now()
        fakeDao.insertOrIgnore(
            PendingAckEntity("ALT-009", now, 0, null, AckSyncStatus.PENDING)
        )
        fakeDao.insertOrIgnore(
            PendingAckEntity("ALT-010", now, 0, null, AckSyncStatus.PENDING)
        )

        // Custom transport: fail ALT-009, succeed ALT-010
        val mixedDataSource = object : AckSyncDataSource {
            override suspend fun synchronizeAck(pendingAck: PendingAckEntity): Result<Unit> {
                return if (pendingAck.alertId == "ALT-009") {
                    Result.failure(RuntimeException("Network error"))
                } else {
                    Result.success(Unit)
                }
            }
        }

        val engine = AckSyncEngine(
            pendingAckDao = fakeDao,
            ackSyncDataSource = mixedDataSource,
            ackRetryPolicy = retryPolicy,
            ackRecoveryPolicy = recoveryPolicy,
            connectivityMonitor = fakeConnectivityMonitor,
            dispatcher = testDispatcher
        )

        val processed = engine.reconcilePendingAcks()

        assertEquals(2, processed)
        assertEquals(AckSyncStatus.FAILED, fakeDao.getPendingAckById("ALT-009")?.status)
        assertEquals(AckSyncStatus.COMPLETED, fakeDao.getPendingAckById("ALT-010")?.status)
    }

    // --- Test Doubles ---

    private class FakeConnectivityMonitor(var online: Boolean) : NetworkConnectivityMonitor {
        override fun isOnline(): Boolean = online
    }

    private class FakeAckSyncDataSource : AckSyncDataSource {
        var shouldSucceed: Boolean = true
        var throwException: Boolean = false
        var synchronizeCallCount: Int = 0
        var delayMs: Long = 0L

        override suspend fun synchronizeAck(pendingAck: PendingAckEntity): Result<Unit> {
            synchronizeCallCount++
            if (delayMs > 0) {
                delay(delayMs)
            }
            if (throwException) {
                throw RuntimeException("Unexpected socket crash")
            }
            return if (shouldSucceed) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Simulated network timeout"))
            }
        }
    }

    private class FakePendingAckDao : PendingAckDao {
        private val storage = mutableMapOf<String, PendingAckEntity>()

        override fun observePendingAcks(): Flow<List<PendingAckEntity>> {
            return flowOf(storage.values.toList())
        }

        override fun getPendingAcks(): List<PendingAckEntity> {
            return storage.values.toList()
        }

        override fun getEligiblePendingAcks(): List<PendingAckEntity> {
            return storage.values.filter { it.status != AckSyncStatus.COMPLETED }
        }

        override fun getPendingAckById(alertId: String): PendingAckEntity? {
            return storage[alertId]
        }

        override fun observeAckSyncStatus(alertId: String): Flow<AckSyncStatus?> {
            return flowOf(storage[alertId]?.status)
        }

        override fun insertOrIgnore(entity: PendingAckEntity): Long {
            return if (storage.containsKey(entity.alertId)) {
                -1L
            } else {
                storage[entity.alertId] = entity
                1L
            }
        }

        override fun update(entity: PendingAckEntity) {
            storage[entity.alertId] = entity
        }

        override fun delete(alertId: String) {
            storage.remove(alertId)
        }
    }
}
