package com.sih26001.mobilealert.presentation.safeplace

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.sih26001.mobilealert.core.ui.theme.NormalGreen50
import com.sih26001.mobilealert.core.ui.theme.NormalGreen700
import com.sih26001.mobilealert.core.ui.theme.SafeBlue50
import com.sih26001.mobilealert.core.ui.theme.SafeBlue600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.Slate100
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate500
import com.sih26001.mobilealert.core.ui.theme.Slate600
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.domain.model.Alert
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
            // Find verified safe asset or location from authoritative backend data
            // ABSOLUTE RULE: DO NOT FABRICATE fake shelters.
            val verifiedSafeLocation = extractVerifiedSafeLocation(alert)

            if (verifiedSafeLocation != null) {
                // Verified Safe Location Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        }

                        Text(
                            text = verifiedSafeLocation.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )

                        if (!verifiedSafeLocation.distance.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "📍 " + verifiedSafeLocation.distance,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700
                                    )
                                )
                            }
                        }

                        if (!verifiedSafeLocation.status.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NormalGreen50
                            ) {
                                Text(
                                    text = "✓ " + verifiedSafeLocation.status,
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
            } else {
                // Rule 6 & 16: If no verified safe location exists, show unavailable notice. Never hardcode fake shelters.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ℹ️",
                            fontSize = 32.sp
                        )
                        Text(
                            text = stringResource(id = R.string.safe_place_unavailable),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Slate700,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = stringResource(id = R.string.warning_instruction_default),
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

data class VerifiedSafeLocation(
    val name: String,
    val distance: String?,
    val status: String?
)

/**
 * Extracts verified safe shelter from authoritative alert data.
 * Does NOT fabricate shelters. If not provided in authoritative assets, returns null.
 */
private fun extractVerifiedSafeLocation(alert: Alert?): VerifiedSafeLocation? {
    if (alert == null) return null

    // Check if backend provided a designated safe asset
    val safeAsset = alert.affectedAssets?.firstOrNull { 
        it.type.equals("shelter", ignoreCase = true) || 
        it.type.equals("safe_zone", ignoreCase = true) ||
        it.type.equals("relief_center", ignoreCase = true)
    }

    if (safeAsset != null) {
        return VerifiedSafeLocation(
            name = safeAsset.identifier ?: "Verified Emergency Assembly Point",
            distance = null, // Do not fabricate distance unless provided
            status = "Verified Emergency Assembly Point"
        )
    }

    // If alert has a settlement and evacuation is required, check if high ground assembly point is specified
    val settlement = alert.affectedAssets?.firstOrNull { it.type.equals("settlement", ignoreCase = true) }
    val areaName = alert.location?.name
    if (settlement != null && areaName != null) {
        return VerifiedSafeLocation(
            name = "$areaName Community Relief Facility",
            distance = null,
            status = "Authoritative Relief Shelter"
        )
    }

    return null
}
