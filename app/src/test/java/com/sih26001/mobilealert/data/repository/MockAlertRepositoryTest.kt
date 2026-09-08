package com.sih26001.mobilealert.data.repository

import com.sih26001.mobilealert.data.mock.MockAlertData
import com.sih26001.mobilealert.domain.model.AlertStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockAlertRepositoryTest {

    private lateinit var classUnderTest: MockAlertRepository

    @Before
    fun setUp() {
        classUnderTest = MockAlertRepository()
    }

    @Test
    fun `initial state is empty`() = runBlocking {
        val activeAlerts = classUnderTest.getActiveAlerts().first()
        assertTrue(activeAlerts.isEmpty())
    }

    @Test
    fun `triggering alert adds it to active alerts`() = runBlocking {
        val alert = MockAlertData.normalAlert
        classUnderTest.triggerTestAlert(alert)

        val activeAlerts = classUnderTest.getActiveAlerts().first()
        assertEquals(1, activeAlerts.size)
        assertEquals(alert.alertId, activeAlerts[0].alertId)
    }

    @Test
    fun `triggering alert with same id overwrites existing alert`() = runBlocking {
        val alert = MockAlertData.normalAlert
        classUnderTest.triggerTestAlert(alert)
        
        // Trigger again with same ID but different status
        val modifiedAlert = alert.copy(status = AlertStatus.SILENCED)
        classUnderTest.triggerTestAlert(modifiedAlert)

        val activeAlerts = classUnderTest.getActiveAlerts().first()
        assertEquals(1, activeAlerts.size) // Deduplication working
        assertEquals(AlertStatus.SILENCED, activeAlerts[0].status)
    }

    @Test
    fun `clearAllAlerts empties the flow`() = runBlocking {
        classUnderTest.triggerTestAlert(MockAlertData.criticalAlert)
        classUnderTest.triggerTestAlert(MockAlertData.highAlert)

        assertEquals(2, classUnderTest.getActiveAlerts().first().size)

        classUnderTest.clearAllAlerts()
        assertTrue(classUnderTest.getActiveAlerts().first().isEmpty())
    }

    @Test
    fun `silencing an active alert changes its status`() = runBlocking {
        classUnderTest.triggerTestAlert(MockAlertData.highAlert)
        
        val result = classUnderTest.silenceAlert(MockAlertData.highAlert.alertId)
        assertTrue(result.isSuccess)

        val activeAlerts = classUnderTest.getActiveAlerts().first()
        assertEquals(AlertStatus.SILENCED, activeAlerts[0].status)
    }

    @Test
    fun `acknowledging an alert moves it to history`() = runBlocking {
        classUnderTest.triggerTestAlert(MockAlertData.highAlert)
        
        val result = classUnderTest.acknowledgeAlert(MockAlertData.highAlert.alertId)
        assertTrue(result.isSuccess)

        val activeAlerts = classUnderTest.getActiveAlerts().first()
        val historyAlerts = classUnderTest.getAlertHistory().first()

        // It should no longer be in active alerts
        assertTrue(activeAlerts.isEmpty())
        
        // It should be in history
        assertEquals(1, historyAlerts.size)
        assertEquals(AlertStatus.ACKNOWLEDGED, historyAlerts[0].status)
    }
}
