package com.sih26001.mobilealert

import com.sih26001.mobilealert.data.repository.AlertRepositoryImpl
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertState
import com.sih26001.mobilealert.domain.usecase.GetActiveAlertsUseCase
import com.sih26001.mobilealert.domain.usecase.GetAlertHistoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertDomainTest {

    @Test
    fun alertModel_preservesFieldsAndNullValuesSafely() {
        val alert = Alert(
            alertId = "ALT-12345",
            title = "High Risk Landslide Warning",
            description = null,
            severity = AlertSeverity.HIGH,
            state = AlertState.ACTIVE,
            locationName = "Sector 4 Slope",
            issuedAt = 1773000000L,
            receivedAt = 1773000010L,
            acknowledgedAt = null,
            expiresAt = null
        )

        assertEquals("ALT-12345", alert.alertId)
        assertEquals(AlertSeverity.HIGH, alert.severity)
        assertEquals(AlertState.ACTIVE, alert.state)
        assertNull(alert.description)
        assertNull(alert.acknowledgedAt)
        assertNull(alert.expiresAt)
        assertEquals(1773000000L, alert.issuedAt)
        assertEquals(1773000010L, alert.receivedAt)
    }

    @Test
    fun alertRepositoryImpl_returnsEmptyActiveAlertsInPhase1() = runBlocking {
        val repository = AlertRepositoryImpl()
        val useCase = GetActiveAlertsUseCase(repository)

        val activeAlerts = useCase().first()
        assertTrue(activeAlerts.isEmpty())
    }

    @Test
    fun alertRepositoryImpl_returnsEmptyAlertHistoryInPhase1() = runBlocking {
        val repository = AlertRepositoryImpl()
        val useCase = GetAlertHistoryUseCase(repository)

        val historyAlerts = useCase().first()
        assertTrue(historyAlerts.isEmpty())
    }

    @Test
    fun alertStates_containAllDocumentedStates() {
        val documentedStates = listOf(
            AlertState.RECEIVED,
            AlertState.DISPLAYED,
            AlertState.ACTIVE,
            AlertState.SILENCED,
            AlertState.ACKNOWLEDGED,
            AlertState.EXPIRED
        )
        assertEquals(6, AlertState.values().size)
        assertTrue(AlertState.values().toList().containsAll(documentedStates))
    }
}
