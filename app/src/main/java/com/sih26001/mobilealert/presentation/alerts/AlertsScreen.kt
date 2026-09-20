package com.sih26001.mobilealert.presentation.alerts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.sih26001.mobilealert.core.ui.theme.Slate400
import com.sih26001.mobilealert.core.ui.theme.Slate500
import com.sih26001.mobilealert.core.ui.theme.Slate600
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate800
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.core.ui.theme.WarningAmber100
import com.sih26001.mobilealert.core.ui.theme.WarningAmber50
import com.sih26001.mobilealert.core.ui.theme.WarningAmber600
import com.sih26001.mobilealert.core.ui.theme.WarningAmber900
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAlertDetails: (alertId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.alerts_title),
                        style = MaterialTheme.typography.titleLarge.copy(
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.alerts.isEmpty() -> {
                    EmptyAlertsCard(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = uiState.alerts, key = { it.alertId }) { alert ->
                            AlertCard(
                                alert = alert,
                                onClick = { onNavigateToAlertDetails(alert.alertId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: Alert,
    onClick: () -> Unit
) {
    val isCritical = alert.severity == AlertSeverity.CRITICAL
    val cardBg = if (isCritical) AlertRed50 else WarningAmber50
    val cardBorder = if (isCritical) AlertRed600 else WarningAmber600
    val titleColor = if (isCritical) AlertRed900 else WarningAmber900

    val timeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm")
        .withZone(ZoneId.systemDefault())
    val formattedTime = timeFormatter.format(alert.issuedAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, cardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = cardBorder
                ) {
                    Text(
                        text = if (isCritical) stringResource(R.string.alert_high_title) else stringResource(R.string.alert_warning_title),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                )
            }

            val zoneName = alert.location?.name ?: stringResource(R.string.monitored_zone_default)
            Text(
                text = "📍 $zoneName",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            )

            val action = alert.recommendedAction?.takeIf { it.isNotBlank() }
                ?: if (isCritical) stringResource(id = R.string.high_alert_instruction_default) else stringResource(id = R.string.warning_instruction_default)
            Text(
                text = action,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = titleColor,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun EmptyAlertsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate100),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Slate200,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "✓", fontSize = 28.sp, color = Slate700)
                }
            }

            Text(
                text = stringResource(id = R.string.alerts_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(id = R.string.alerts_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate600),
                textAlign = TextAlign.Center
            )
        }
    }
}
