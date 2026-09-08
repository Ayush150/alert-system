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

        InfoRow("Status:", alert.status.name, contentColor)

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

        // Actions
        if (alert.status == AlertStatus.ACTIVE) {
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
        } else if (alert.status == AlertStatus.SILENCED) {
            OutlinedButton(
                onClick = onAcknowledge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("ACKNOWLEDGE", style = MaterialTheme.typography.titleMedium, color = contentColor)
            }
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
