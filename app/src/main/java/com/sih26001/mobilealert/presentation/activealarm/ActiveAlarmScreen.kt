package com.sih26001.mobilealert.presentation.activealarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveAlarmScreen(
    viewModel: ActiveAlarmViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alert by viewModel.alert.collectAsState()
    val isAckPending by viewModel.isAckPending.collectAsState()
    val ackSyncStatus by viewModel.ackSyncStatus.collectAsState()
    val ackError by viewModel.ackError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (alert == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AlarmContent(
                    alert = alert!!,
                    isAckPending = isAckPending,
                    ackSyncStatus = ackSyncStatus,
                    ackError = ackError,
                    onSilence = viewModel::silenceAlert,
                    onAcknowledge = viewModel::acknowledgeAlert
                )
            }
        }
    }
}

@Composable
private fun AlarmContent(
    alert: Alert,
    isAckPending: Boolean,
    ackSyncStatus: com.sih26001.mobilealert.data.local.AckSyncStatus?,
    ackError: String?,
    onSilence: () -> Unit,
    onAcknowledge: () -> Unit
) {
    val backgroundColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        AlertSeverity.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
        AlertSeverity.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, shape = MaterialTheme.shapes.medium)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = alert.severity.name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )

        Text(
            text = alert.eventType,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
            textAlign = TextAlign.Center
        )

        HorizontalDivider(color = contentColor.copy(alpha = 0.2f))

        val riskText = alert.riskScore?.let { "${(it * 100).toInt()}/100" } ?: "Unavailable"
        InfoRow("Risk Score:", riskText, contentColor)

        val locText = alert.location?.let { "${it.latitude}, ${it.longitude}" } ?: "Unknown"
        InfoRow("Location:", locText, contentColor)

        val statusDisplay = when {
            ackSyncStatus == com.sih26001.mobilealert.data.local.AckSyncStatus.COMPLETED -> "ACKNOWLEDGED • SYNCED"
            ackSyncStatus == com.sih26001.mobilealert.data.local.AckSyncStatus.FAILED -> "ACKNOWLEDGED • SYNC PENDING (RETRYING)"
            ackSyncStatus == com.sih26001.mobilealert.data.local.AckSyncStatus.IN_FLIGHT -> "ACKNOWLEDGED • SYNCING..."
            ackSyncStatus == com.sih26001.mobilealert.data.local.AckSyncStatus.PENDING -> "ACKNOWLEDGED • SYNC PENDING"
            isAckPending || alert.status == AlertStatus.ACKNOWLEDGED -> "ACKNOWLEDGED • SYNC PENDING" // fallback for null or transit states
            alert.status == AlertStatus.SILENCED -> "SILENCED (UNACKNOWLEDGED)"
            else -> alert.status.name
        }
        InfoRow("Status:", statusDisplay, contentColor)

        if (alert.acknowledgedAt != null) {
            InfoRow("Acknowledged At:", alert.acknowledgedAt.toString(), contentColor)
        }

        if (!alert.recommendedAction.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recommended Action:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = alert.recommendedAction,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions distinguishing SILENCE ALARM vs ACKNOWLEDGE
        when (alert.status) {
            AlertStatus.ACTIVE -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSilence,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("SILENCE ALARM", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = onAcknowledge,
                        enabled = !isAckPending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            if (isAckPending) "ACKNOWLEDGING..." else "ACKNOWLEDGE",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                    }
                }
            }
            AlertStatus.SILENCED -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Alarm is silenced locally. Operational acknowledgement is required.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    OutlinedButton(
                        onClick = onAcknowledge,
                        enabled = !isAckPending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            if (isAckPending) "ACKNOWLEDGING..." else "ACKNOWLEDGE",
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                    }
                }
            }
            AlertStatus.ACKNOWLEDGED -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isAckPending) "ACKNOWLEDGED • SYNC PENDING" else "OPERATIONALLY ACKNOWLEDGED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (isAckPending) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Acknowledgement recorded locally. Will sync with authoritative server once connected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            else -> { /* EXPIRED, DISPLAYED, RECEIVED */ }
        }

        if (ackError != null) {
            Text(
                text = ackError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
