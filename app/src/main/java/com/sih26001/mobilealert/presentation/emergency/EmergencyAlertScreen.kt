package com.sih26001.mobilealert.presentation.emergency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.sih26001.mobilealert.core.ui.theme.NormalGreen600
import com.sih26001.mobilealert.core.ui.theme.NormalGreen700
import com.sih26001.mobilealert.core.ui.theme.SafeBlue600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.SafeBlue50
import com.sih26001.mobilealert.core.ui.theme.Slate100
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate300
import com.sih26001.mobilealert.core.ui.theme.Slate400
import com.sih26001.mobilealert.core.ui.theme.Slate500
import com.sih26001.mobilealert.core.ui.theme.Slate600
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate800
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.SafePlaceDestination

@Composable
fun EmergencyAlertScreen(
    viewModel: EmergencyAlertViewModel,
    onNavigateToRoute: (alertId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alert by viewModel.alert.collectAsState()
    val isSirenActive by viewModel.isSirenActive.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val isAckPending by viewModel.isAckPending.collectAsState()
    val isAcknowledged by viewModel.isAcknowledged.collectAsState()
    val ackError by viewModel.ackError.collectAsState()

    // High impact dark emergency background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
    ) {
        if (alert == null) {
            CircularProgressIndicator(
                color = AlertRed600,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            EmergencyContent(
                alert = alert!!,
                isSirenActive = isSirenActive,
                destination = destination,
                isAckPending = isAckPending,
                isAcknowledged = isAcknowledged,
                ackError = ackError,
                onSilenceSiren = viewModel::silenceSiren,
                onAcknowledge = viewModel::acknowledgeAlert,
                onViewRoute = { onNavigateToRoute(alert!!.alertId) }
            )
        }
    }
}

@Composable
private fun EmergencyContent(
    alert: Alert,
    isSirenActive: Boolean,
    destination: SafePlaceDestination,
    isAckPending: Boolean,
    isAcknowledged: Boolean,
    ackError: String?,
    onSilenceSiren: () -> Unit,
    onAcknowledge: () -> Unit,
    onViewRoute: () -> Unit
) {
    val isCritical = alert.severity == AlertSeverity.CRITICAL

    // Pulsing transition for active siren badge
    val infiniteTransition = rememberInfiniteTransition(label = "sirenPulse")
    val sirenAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sirenAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // --- 1. ALERT SEVERITY HEADER ---
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AlertRed600,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isCritical) {
                        "🚨 " + stringResource(id = R.string.alert_critical_title)
                    } else {
                        "🚨 " + stringResource(id = R.string.alert_high_title)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- 2. HAZARD TYPE & DOMINANT EVACUATION BANNER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, AlertRed600, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = AlertRed900.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.hazard_landslide_detected),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AlertRed100
                    ),
                    textAlign = TextAlign.Center
                )

                // Dominant "EVACUATE NOW"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AlertRed600,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.action_evacuate_now),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Instruction & Location
                val areaName = alert.location?.name ?: stringResource(id = R.string.default_location)
                Text(
                    text = stringResource(id = R.string.hazard_detected_near, areaName),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.hazard_move_away_desc),
                    style = MaterialTheme.typography.bodySmall.copy(color = AlertRed100),
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- 3. RECOMMENDED SAFE PLACE DESTINATION CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, SafeBlue600, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🛡️ " + stringResource(id = R.string.label_safe_place),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SafeBlue600
                        )
                    )

                    if (destination.isDemo) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SafeBlue700.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.label_demo_destination),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate300
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                // Distance and ETA metrics
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate700
                    ) {
                        Text(
                            text = "📍 ${destination.distance}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "•",
                        color = Slate500,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate700
                    ) {
                        Text(
                            text = "⏱️ ${destination.estimatedTime}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Slate200
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // VIEW SAFE ROUTE ACTION BUTTON
                Button(
                    onClick = onViewRoute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeBlue700)
                ) {
                    Text(
                        text = stringResource(id = R.string.action_view_safe_route),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // --- 4. SIREN STATUS INDICATOR ---
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSirenActive) {
                AlertRed600.copy(alpha = sirenAlpha)
            } else {
                Slate800
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isSirenActive) {
                        "🔊 " + stringResource(id = R.string.siren_active)
                    } else {
                        "🔇 " + stringResource(id = R.string.siren_silenced)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                )
            }
        }

        // --- 5. OPERATIONAL CONTROLS: SILENCE VS ACKNOWLEDGE ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate700, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.operational_controls),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                )

                // SILENCE SIREN BUTTON
                OutlinedButton(
                    onClick = onSilenceSiren,
                    enabled = isSirenActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSirenActive) AlertRed600 else Slate600
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSirenActive) AlertRed100 else Slate500
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.action_silence_siren),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (!isSirenActive && !isAcknowledged) {
                    Text(
                        text = stringResource(id = R.string.action_siren_silenced_status),
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = Slate700)

                // I'M SAFE / ACKNOWLEDGE BUTTON
                if (!isAcknowledged) {
                    Button(
                        onClick = onAcknowledge,
                        enabled = !isAckPending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = if (isAckPending) {
                                stringResource(id = R.string.action_acknowledging)
                            } else {
                                stringResource(id = R.string.action_im_safe_ack)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NormalGreen700.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NormalGreen600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✓ " + stringResource(id = R.string.action_acknowledged),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            if (isAckPending) {
                                Text(
                                    text = stringResource(id = R.string.sync_pending_notice),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate300),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (ackError != null) {
                    Text(
                        text = ackError,
                        color = AlertRed600,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
