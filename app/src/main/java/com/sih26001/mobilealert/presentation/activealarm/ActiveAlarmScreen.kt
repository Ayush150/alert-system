package com.sih26001.mobilealert.presentation.activealarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sih26001.mobilealert.R
import com.sih26001.mobilealert.core.ui.theme.AlertRed100
import com.sih26001.mobilealert.core.ui.theme.AlertRed50
import com.sih26001.mobilealert.core.ui.theme.AlertRed600
import com.sih26001.mobilealert.core.ui.theme.AlertRed900
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.Slate100
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate300
import com.sih26001.mobilealert.core.ui.theme.Slate400
import com.sih26001.mobilealert.core.ui.theme.Slate500
import com.sih26001.mobilealert.core.ui.theme.Slate600
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate800
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.core.ui.theme.WarningAmber100
import com.sih26001.mobilealert.core.ui.theme.WarningAmber50
import com.sih26001.mobilealert.core.ui.theme.WarningAmber600
import com.sih26001.mobilealert.core.ui.theme.WarningAmber700
import com.sih26001.mobilealert.core.ui.theme.WarningAmber900
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveAlarmScreen(
    viewModel: ActiveAlarmViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSafePlace: (alertId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alert by viewModel.alert.collectAsState()
    val isAckPending by viewModel.isAckPending.collectAsState()
    val ackSyncStatus by viewModel.ackSyncStatus.collectAsState()
    val ackError by viewModel.ackError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.action_view_details),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge.copy(color = Slate900)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (alert == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AlertDetailsContent(
                    alert = alert!!,
                    isAckPending = isAckPending,
                    ackSyncStatus = ackSyncStatus,
                    ackError = ackError,
                    onSilence = viewModel::silenceAlert,
                    onAcknowledge = viewModel::acknowledgeAlert,
                    onFindSafePlace = { onNavigateToSafePlace(alert!!.alertId) }
                )
            }
        }
    }
}

@Composable
private fun AlertDetailsContent(
    alert: Alert,
    isAckPending: Boolean,
    ackSyncStatus: com.sih26001.mobilealert.data.local.AckSyncStatus?,
    ackError: String?,
    onSilence: () -> Unit,
    onAcknowledge: () -> Unit,
    onFindSafePlace: () -> Unit
) {
    val isCritical = alert.severity == AlertSeverity.CRITICAL
    val cardBg = if (isCritical) AlertRed50 else WarningAmber50
    val cardBorder = if (isCritical) AlertRed600 else WarningAmber600
    val headerColor = if (isCritical) AlertRed900 else WarningAmber900

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, cardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = cardBorder
                ) {
                    Text(
                        text = if (isCritical) "🚨 " + stringResource(id = R.string.alert_high_title) else "⚠️ " + stringResource(id = R.string.alert_warning_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = if (isCritical) stringResource(id = R.string.alert_high_subtitle) else stringResource(id = R.string.alert_warning_subtitle),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = headerColor
                    )
                )

                HorizontalDivider(color = cardBorder.copy(alpha = 0.25f))

                // 1. WHAT HAPPENED?
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.question_what_happened),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "Unusual ground conditions have been detected near ${alert.location?.name ?: "the monitored area"}.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate800)
                    )
                }

                // 2. WHY? (Max 3 drivers)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.question_why),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    )
                    val drivers = alert.topDrivers?.take(3)
                    if (!drivers.isNullOrEmpty()) {
                        drivers.forEach { factor ->
                            Text(
                                text = "• $factor",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Slate800)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.why_unavailable),
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                    }
                }

                // 3. WHERE?
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "WHERE?",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "📍 ${alert.location?.name ?: stringResource(id = R.string.data_unavailable)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    )
                }

                // 4. WHAT SHOULD I DO?
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.question_what_to_do),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = headerColor
                        )
                    )
                    val instruction = alert.recommendedAction?.takeIf { it.isNotBlank() }
                        ?: if (isCritical) stringResource(id = R.string.high_alert_instruction_default) else stringResource(id = R.string.warning_instruction_default)
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = headerColor
                        )
                    )
                }

                // 5. WHERE SHOULD I GO?
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.question_where_to_go),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                    )
                    Text(
                        text = "Nearest verified safe location or designated high ground relief center.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Slate800)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Primary Action Button: FIND SAFE PLACE
                Button(
                    onClick = onFindSafePlace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isCritical) AlertRed600 else SafeBlue700)
                ) {
                    Text(
                        text = stringResource(id = R.string.action_find_safe_place),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Silence vs Acknowledge Section (Section 10)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Operational Controls",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                )

                if (alert.status == AlertStatus.ACTIVE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSilence,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Slate400)
                        ) {
                            Text(
                                text = stringResource(id = R.string.action_silence),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            )
                        }

                        Button(
                            onClick = onAcknowledge,
                            enabled = !isAckPending,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                        ) {
                            Text(
                                text = if (isAckPending) stringResource(id = R.string.action_acknowledging) else stringResource(id = R.string.action_acknowledge),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                } else if (alert.status == AlertStatus.SILENCED) {
                    Text(
                        text = stringResource(id = R.string.silence_notice),
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                    Button(
                        onClick = onAcknowledge,
                        enabled = !isAckPending,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text(
                            text = if (isAckPending) stringResource(id = R.string.action_acknowledging) else stringResource(id = R.string.action_acknowledge),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                } else if (alert.status == AlertStatus.ACKNOWLEDGED) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "✓ " + stringResource(id = R.string.action_acknowledged),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            if (isAckPending) {
                                Text(
                                    text = stringResource(id = R.string.sync_pending_notice),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                )
                            }
                        }
                    }
                }

                if (ackError != null) {
                    Text(
                        text = ackError,
                        color = AlertRed600,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
