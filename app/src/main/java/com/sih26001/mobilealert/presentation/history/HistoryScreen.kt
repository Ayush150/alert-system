package com.sih26001.mobilealert.presentation.history

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sih26001.mobilealert.core.ui.theme.AlertRed600
import com.sih26001.mobilealert.core.ui.theme.NormalGreen700
import com.sih26001.mobilealert.core.ui.theme.Slate100
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate500
import com.sih26001.mobilealert.core.ui.theme.Slate600
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate800
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.core.ui.theme.WarningAmber600
import com.sih26001.mobilealert.domain.model.Alert
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.history_title),
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
                uiState.history.isEmpty() -> {
                    EmptyHistoryCard(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = uiState.history, key = { it.alertId }) { alert ->
                            HistoryItemCard(alert = alert)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimal History item (Section 13)
 * Strictly: Severity, Area, Date / Time, Status.
 * No charts, no analytics.
 */
@Composable
private fun HistoryItemCard(alert: Alert) {
    val isCritical = alert.severity == AlertSeverity.CRITICAL
    val badgeColor = if (isCritical) AlertRed600 else WarningAmber600
    val severityLabel = if (isCritical) "🚨 HIGH ALERT" else "⚠️ WARNING"

    val timeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm")
        .withZone(ZoneId.systemDefault())
    val formattedTime = timeFormatter.format(alert.issuedAt)

    val statusText = when (alert.status) {
        AlertStatus.ACKNOWLEDGED -> "Acknowledged"
        AlertStatus.EXPIRED -> "Expired"
        AlertStatus.SILENCED -> "Silenced"
        else -> alert.status.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = severityLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Slate100
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (alert.status == AlertStatus.ACKNOWLEDGED) NormalGreen700 else Slate600
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "📍 ${alert.location?.name ?: "Area Unspecified"}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            )

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard(modifier: Modifier = Modifier) {
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
                    Text(text = "—", fontSize = 28.sp, color = Slate700)
                }
            }

            Text(
                text = stringResource(id = R.string.history_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(id = R.string.history_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate600),
                textAlign = TextAlign.Center
            )
        }
    }
}
