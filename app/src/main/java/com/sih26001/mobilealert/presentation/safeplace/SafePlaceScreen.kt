package com.sih26001.mobilealert.presentation.safeplace

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sih26001.mobilealert.R
import com.sih26001.mobilealert.core.safeplace.SafePlaceResolver
import com.sih26001.mobilealert.core.ui.theme.NormalGreen50
import com.sih26001.mobilealert.core.ui.theme.NormalGreen700
import com.sih26001.mobilealert.core.ui.theme.SafeBlue50
import com.sih26001.mobilealert.core.ui.theme.SafeBlue600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.domain.repository.AlertRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafePlaceScreen(
    alertId: String,
    alertRepository: AlertRepository,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (alertId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alertFlow = alertRepository.getAlertById(alertId)
    val alert by alertFlow.collectAsState(initial = null)

    val destination = SafePlaceResolver.resolveDestination(alert)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.safe_place_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Verified / Demo Safe Location Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SafeBlue600.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SafeBlue50),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SafeBlue600
                        ) {
                            Text(
                                text = "🛡️ " + stringResource(id = R.string.safe_place_verified_label),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (destination.isDemo) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SafeBlue700.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.label_demo_destination),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeBlue700
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = destination.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📍 ${destination.distance}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(color = Slate700)
                        )
                        Text(
                            text = "⏱️ ${destination.estimatedTime}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        )
                    }

                    destination.statusDescription?.let { desc ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NormalGreen50
                        ) {
                            Text(
                                text = "✓ $desc",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = NormalGreen700
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Action Button: GO THERE
            Button(
                onClick = { onNavigateToRoute(alertId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeBlue700)
            ) {
                Text(
                    text = stringResource(id = R.string.action_go_there),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
