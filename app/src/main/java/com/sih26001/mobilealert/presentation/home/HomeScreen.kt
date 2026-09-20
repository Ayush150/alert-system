package com.sih26001.mobilealert.presentation.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sih26001.mobilealert.R
import com.sih26001.mobilealert.core.ui.theme.AlertRed100
import com.sih26001.mobilealert.core.ui.theme.AlertRed50
import com.sih26001.mobilealert.core.ui.theme.AlertRed600
import com.sih26001.mobilealert.core.ui.theme.AlertRed900
import com.sih26001.mobilealert.core.ui.theme.NormalGreen100
import com.sih26001.mobilealert.core.ui.theme.NormalGreen50
import com.sih26001.mobilealert.core.ui.theme.NormalGreen600
import com.sih26001.mobilealert.core.ui.theme.NormalGreen700
import com.sih26001.mobilealert.core.ui.theme.NormalGreen800
import com.sih26001.mobilealert.core.ui.theme.SafeBlue50
import com.sih26001.mobilealert.core.ui.theme.SafeBlue600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.Slate100
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate300
import com.sih26001.mobilealert.core.ui.theme.Slate400
import com.sih26001.mobilealert.core.ui.theme.Slate50
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
import com.sih26001.mobilealert.core.util.AppLanguage
import com.sih26001.mobilealert.core.util.LocaleManager
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import com.sih26001.mobilealert.domain.model.PriorityActionType
import com.sih26001.mobilealert.domain.model.RoleDashboardConfig
import com.sih26001.mobilealert.domain.model.RolePriority
import com.sih26001.mobilealert.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAlerts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAlertDetails: (alertId: String) -> Unit,
    onNavigateToSafePlace: (alertId: String) -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    onChangeRole: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentLang by LocaleManager.currentLanguage.collectAsState()
    val selectedRole = remember {
        DependencyContainer.rolePreferences.getSelectedRole() ?: UserRole.CITIZEN
    }
    val roleConfig = remember(selectedRole) {
        RoleDashboardConfig.forRole(selectedRole)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Slate900
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.role_prefix, selectedRole.getLocalizedName().uppercase()),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate700
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SafeBlue50,
                                modifier = Modifier.clickable { onChangeRole?.invoke() }
                            ) {
                                Text(
                                    text = "${selectedRole.emoji} " + stringResource(id = R.string.action_change_role),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SafeBlue700
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = selectedRole.getLocalizedDesc(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Language Switcher Chips: EN | HI | MR | AS + Settings Gear
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageChip(
                            label = "EN",
                            selected = currentLang == AppLanguage.ENGLISH,
                            onClick = { LocaleManager.setLanguage(AppLanguage.ENGLISH, context) }
                        )
                        LanguageChip(
                            label = "HI",
                            selected = currentLang == AppLanguage.HINDI,
                            onClick = { LocaleManager.setLanguage(AppLanguage.HINDI, context) }
                        )
                        LanguageChip(
                            label = "MR",
                            selected = currentLang == AppLanguage.MARATHI,
                            onClick = { LocaleManager.setLanguage(AppLanguage.MARATHI, context) }
                        )
                        LanguageChip(
                            label = "AS",
                            selected = currentLang == AppLanguage.ASSAMESE,
                            onClick = { LocaleManager.setLanguage(AppLanguage.ASSAMESE, context) }
                        )
                        if (onNavigateToSettings != null) {
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(text = "⚙️", fontSize = 18.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Current Area Header (Section 2)
            AreaHeader(monitoredArea = uiState.monitoredArea)

            // Connection / Offline Status Notice (Section 12)
            if (uiState.isOffline) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate300)
                ) {
                    Text(
                        text = "⚡ " + stringResource(id = R.string.offline_notice),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate700,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Compact Role Priority Section
            RolePrioritySection(
                config = roleConfig,
                uiState = uiState,
                onNavigateToAlerts = onNavigateToAlerts,
                onNavigateToSafePlace = onNavigateToSafePlace
            )

            // PRIMARY EMERGENCY CARD: NORMAL vs WARNING vs HIGH ALERT
            val primaryAlert = uiState.primaryAlert

            if (primaryAlert == null || primaryAlert.severity == AlertSeverity.NORMAL) {
                // NORMAL STATE (Section 2)
                NormalStateCard()
            } else if (primaryAlert.severity == AlertSeverity.HIGH) {
                // WARNING STATE (Section 3)
                WarningStateCard(
                    alert = primaryAlert,
                    onViewDetails = { onNavigateToAlertDetails(primaryAlert.alertId) },
                    onFindSafePlace = { onNavigateToSafePlace(primaryAlert.alertId) }
                )
            } else if (primaryAlert.severity == AlertSeverity.CRITICAL) {
                // HIGH ALERT STATE (Section 4)
                HighAlertStateCard(
                    alert = primaryAlert,
                    isAckPending = uiState.pendingAckIds.contains(primaryAlert.alertId),
                    onSilence = { viewModel.silenceAlert(primaryAlert.alertId) },
                    onAcknowledge = { viewModel.acknowledgeAlert(primaryAlert.alertId) },
                    onFindSafePlace = { onNavigateToSafePlace(primaryAlert.alertId) },
                    onViewDetails = { onNavigateToAlertDetails(primaryAlert.alertId) }
                )
            }

            // Last Updated Timestamp
            uiState.lastUpdatedText?.let { time ->
                Text(
                    text = stringResource(id = R.string.last_updated_prefix, time),
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate400),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Primary Navigation Buttons (Section 14)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToAlerts,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Slate300)
                ) {
                    Text(
                        text = stringResource(id = R.string.nav_alerts) + if (uiState.allActiveAlerts.isNotEmpty()) " (${uiState.allActiveAlerts.size})" else "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Slate300)
                ) {
                    Text(
                        text = stringResource(id = R.string.nav_history),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AreaHeader(monitoredArea: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.label_current_area),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "📍 $monitoredArea",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Slate100
            ) {
                Text(
                    text = stringResource(id = R.string.radar_sensor_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Slate600
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Normal Calm State (Section 2)
 * Strictly NO sensor data, risk scores, ML information, or technical metrics.
 */
@Composable
private fun NormalStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, NormalGreen600.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = NormalGreen50),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = NormalGreen100,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🛡️", fontSize = 28.sp)
                }
            }

            Text(
                text = stringResource(id = R.string.status_normal_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = NormalGreen800
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(id = R.string.status_normal_subtitle),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = NormalGreen700
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Warning State (Section 3)
 * Amber visual state, short description, max 3 factors, clear action.
 */
@Composable
private fun WarningStateCard(
    alert: Alert,
    onViewDetails: () -> Unit,
    onFindSafePlace: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, WarningAmber600, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = WarningAmber50),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = WarningAmber600
                ) {
                    Text(
                        text = "⚠️ " + stringResource(id = R.string.alert_warning_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.alert_warning_subtitle),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber900
                )
            )

            alert.location?.name?.let { loc ->
                Text(
                    text = "📍 $loc",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                )
            }

            HorizontalDivider(color = WarningAmber600.copy(alpha = 0.2f))

            // 1. WHAT HAPPENED?
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.question_what_happened),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                )
                val description = stringResource(
                    id = R.string.what_happened_desc,
                    alert.location?.name ?: stringResource(id = R.string.default_location)
                )
                Text(
                    text = description,
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

            // 3. WHAT SHOULD YOU DO?
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.question_what_to_do),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = WarningAmber900
                    )
                )
                val instruction = alert.recommendedAction?.takeIf { it.isNotBlank() }
                    ?: stringResource(id = R.string.warning_instruction_default)
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = WarningAmber900
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action: FIND SAFE PLACE / VIEW DETAILS
            Button(
                onClick = onFindSafePlace,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber700)
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
}

/**
 * High Alert State (Section 4)
 * Red emergency state, dominant EVACUATE NOW / FIND SAFE PLACE button, alarm actions.
 */
@Composable
private fun HighAlertStateCard(
    alert: Alert,
    isAckPending: Boolean,
    onSilence: () -> Unit,
    onAcknowledge: () -> Unit,
    onFindSafePlace: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.5.dp, AlertRed600, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = AlertRed50),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AlertRed600
                ) {
                    Text(
                        text = "🚨 " + stringResource(id = R.string.alert_high_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.alert_high_subtitle),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = AlertRed900
                )
            )

            alert.location?.name?.let { loc ->
                Text(
                    text = "📍 $loc",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                )
            }

            HorizontalDivider(color = AlertRed600.copy(alpha = 0.2f))

            // WHAT SHOULD YOU DO? Immediately exposed!
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.question_what_to_do),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = AlertRed900
                    )
                )
                val instruction = alert.recommendedAction?.takeIf { it.isNotBlank() }
                    ?: stringResource(id = R.string.high_alert_instruction_default)
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AlertRed900
                    )
                )
            }

            // PRIMARY DOMINANT ACTION: EVACUATE NOW / FIND SAFE PLACE
            val actionLabel = if (alert.requiresAck || alert.recommendedAction?.contains("evacuat", ignoreCase = true) == true) {
                stringResource(id = R.string.action_evacuate_now)
            } else {
                stringResource(id = R.string.action_find_safe_place)
            }

            Button(
                onClick = onFindSafePlace,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed600)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                )
            }

            // Silence vs Acknowledge actions (Section 10)
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
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            ),
                            maxLines = 1
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
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                    }
                }
            } else if (alert.status == AlertStatus.SILENCED) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.silence_notice),
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
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
                }
            } else if (alert.status == AlertStatus.ACKNOWLEDGED) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate200,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ " + stringResource(id = R.string.action_acknowledged),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        ),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Slate900 else Slate200,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else Slate700
            ),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}

/**
 * Compact "Priority for your role" section.
 * Renders role-specific priorities in a 2x2 grid using ONLY existing data (no fabricated numbers).
 */
@Composable
private fun RolePrioritySection(
    config: RoleDashboardConfig,
    uiState: HomeUiState,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSafePlace: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate50),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = config.role.getLocalizedName().uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = stringResource(id = R.string.role_priority_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = config.role.emoji, fontSize = 18.sp)
                    }
                }
            }

            // 2x2 grid of compact priority cards
            val priorities = config.priorities
            if (priorities.size >= 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityCard(
                        priority = priorities[0],
                        uiState = uiState,
                        onNavigateToAlerts = onNavigateToAlerts,
                        onNavigateToSafePlace = onNavigateToSafePlace,
                        modifier = Modifier.weight(1f)
                    )
                    PriorityCard(
                        priority = priorities[1],
                        uiState = uiState,
                        onNavigateToAlerts = onNavigateToAlerts,
                        onNavigateToSafePlace = onNavigateToSafePlace,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityCard(
                        priority = priorities[2],
                        uiState = uiState,
                        onNavigateToAlerts = onNavigateToAlerts,
                        onNavigateToSafePlace = onNavigateToSafePlace,
                        modifier = Modifier.weight(1f)
                    )
                    PriorityCard(
                        priority = priorities[3],
                        uiState = uiState,
                        onNavigateToAlerts = onNavigateToAlerts,
                        onNavigateToSafePlace = onNavigateToSafePlace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Compact individual priority card.
 * Adheres strictly to the NO FABRICATED DATA rule: displays only real alert counts,
 * real severity states, or placeholder status strings.
 */
@Composable
private fun PriorityCard(
    priority: RolePriority,
    uiState: HomeUiState,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSafePlace: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAlertId = uiState.primaryAlert?.alertId
        ?: uiState.allActiveAlerts.firstOrNull()?.alertId

    val isClickable = priority.isAvailable && when (priority.actionType) {
        PriorityActionType.ACTIVE_ALERTS -> true
        PriorityActionType.SAFE_PLACES -> activeAlertId != null
        PriorityActionType.CURRENT_RISK,
        PriorityActionType.UNAVAILABLE -> false
    }

    // Real-data status badge text mapped to string resources - STRICTLY NO FABRICATED DATA
    val realStatusText: String = when (priority.actionType) {
        PriorityActionType.ACTIVE_ALERTS -> {
            if (priority.title.contains("Critical", ignoreCase = true) || priority.title.contains("High", ignoreCase = true)) {
                val criticalOrHighCount = uiState.allActiveAlerts.count {
                    it.severity == AlertSeverity.HIGH || it.severity == AlertSeverity.CRITICAL
                }
                if (criticalOrHighCount > 0) {
                    stringResource(id = R.string.status_active_count, criticalOrHighCount)
                } else {
                    stringResource(id = R.string.status_none_active)
                }
            } else {
                if (uiState.allActiveAlerts.isNotEmpty()) {
                    stringResource(id = R.string.status_active_count, uiState.allActiveAlerts.size)
                } else {
                    stringResource(id = R.string.status_none_active)
                }
            }
        }
        PriorityActionType.SAFE_PLACES -> {
            if (activeAlertId != null) {
                stringResource(id = R.string.status_available)
            } else {
                stringResource(id = R.string.status_standby)
            }
        }
        PriorityActionType.CURRENT_RISK -> {
            when (uiState.primaryAlert?.severity) {
                AlertSeverity.CRITICAL -> stringResource(id = R.string.status_critical)
                AlertSeverity.HIGH -> stringResource(id = R.string.status_warning)
                AlertSeverity.NORMAL, null -> stringResource(id = R.string.status_normal)
            }
        }
        PriorityActionType.UNAVAILABLE -> {
            priority.placeholderStatus?.let {
                if (it.contains("incident", ignoreCase = true)) {
                    stringResource(id = R.string.status_no_incidents)
                } else {
                    stringResource(id = R.string.data_unavailable)
                }
            } ?: stringResource(id = R.string.data_unavailable)
        }
    }

    val cardBg = if (priority.isAvailable) Color.White else Slate100.copy(alpha = 0.6f)
    val borderColor = if (priority.isAvailable) Slate200 else Slate200.copy(alpha = 0.5f)

    Card(
        modifier = modifier
            .height(82.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .then(
                if (isClickable) {
                    Modifier.clickable {
                        when (priority.actionType) {
                            PriorityActionType.ACTIVE_ALERTS -> onNavigateToAlerts()
                            PriorityActionType.SAFE_PLACES -> {
                                activeAlertId?.let { onNavigateToSafePlace(it) }
                            }
                            PriorityActionType.CURRENT_RISK,
                            PriorityActionType.UNAVAILABLE -> Unit
                        }
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = priority.iconEmoji, fontSize = 16.sp)
                if (isClickable) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                    )
                }
            }

            Column {
                Text(
                    text = priority.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (priority.isAvailable) Slate800 else Slate500
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = realStatusText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (priority.isAvailable) SafeBlue700 else Slate400
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
