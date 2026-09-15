package com.sih26001.mobilealert.presentation.route

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sih26001.mobilealert.R
import com.sih26001.mobilealert.core.ui.theme.AlertRed50
import com.sih26001.mobilealert.core.ui.theme.AlertRed600
import com.sih26001.mobilealert.core.ui.theme.AlertRed900
import com.sih26001.mobilealert.core.ui.theme.NormalGreen600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue50
import com.sih26001.mobilealert.core.ui.theme.SafeBlue600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.Slate100
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate500
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate800
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.domain.repository.AlertRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    alertId: String,
    alertRepository: AlertRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alertFlow = alertRepository.getAlertById(alertId)
    val alert by alertFlow.collectAsState(initial = null)

    // Authoritative road disruption detection
    val closedRoads = alert?.affectedAssets?.filter { it.type.equals("road", ignoreCase = true) } ?: emptyList()
    val hasLocationCoords = alert?.location?.latitude != null && alert?.location?.longitude != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.route_title),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Topological Map Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(1.5.dp, Slate200, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Minimalist Route Canvas: Communicating YOU -> SAFE LOCATION
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val startX = width * 0.25f
                        val startY = height * 0.80f

                        val endX = width * 0.75f
                        val endY = height * 0.20f

                        // Draw background grid lines (subtle disaster GIS grid)
                        val gridSpacing = 40.dp.toPx()
                        var x = 0f
                        while (x < width) {
                            drawLine(
                                color = Slate100,
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1f
                            )
                            x += gridSpacing
                        }
                        var y = 0f
                        while (y < height) {
                            drawLine(
                                color = Slate100,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                            y += gridSpacing
                        }

                        // Evacuation Path line
                        val path = Path().apply {
                            moveTo(startX, startY)
                            cubicTo(
                                startX + (width * 0.2f), startY - (height * 0.3f),
                                endX - (width * 0.2f), endY + (height * 0.3f),
                                endX, endY
                            )
                        }

                        // Glow path
                        drawPath(
                            path = path,
                            color = SafeBlue600.copy(alpha = 0.25f),
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Main path
                        drawPath(
                            path = path,
                            color = SafeBlue700,
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                            )
                        )

                        // If closed road exists, draw disruption icon / zone
                        if (closedRoads.isNotEmpty()) {
                            val hazardX = width * 0.48f
                            val hazardY = height * 0.55f
                            drawCircle(
                                color = AlertRed600.copy(alpha = 0.15f),
                                radius = 26.dp.toPx(),
                                center = Offset(hazardX, hazardY)
                            )
                            drawCircle(
                                color = AlertRed600,
                                radius = 6.dp.toPx(),
                                center = Offset(hazardX, hazardY)
                            )
                        }

                        // YOU marker (Green)
                        drawCircle(
                            color = NormalGreen600.copy(alpha = 0.25f),
                            radius = 16.dp.toPx(),
                            center = Offset(startX, startY)
                        )
                        drawCircle(
                            color = NormalGreen600,
                            radius = 8.dp.toPx(),
                            center = Offset(startX, startY)
                        )

                        // SAFE LOCATION marker (Blue)
                        drawCircle(
                            color = SafeBlue600.copy(alpha = 0.25f),
                            radius = 20.dp.toPx(),
                            center = Offset(endX, endY)
                        )
                        drawCircle(
                            color = SafeBlue700,
                            radius = 10.dp.toPx(),
                            center = Offset(endX, endY)
                        )
                    }

                    // Floating Origin & Destination Labels
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NormalGreen600,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Text(
                            text = stringResource(id = R.string.route_origin),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 24.dp, top = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SafeBlue700,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Text(
                            text = stringResource(id = R.string.route_destination),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = SafeBlue700
                            )
                        )
                    }
                }
            }

            // Road disruption banner if verified in authoritative data
            if (closedRoads.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AlertRed600.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = AlertRed50),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚠️ " + stringResource(id = R.string.route_road_closed_warning),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AlertRed900
                            )
                        )
                        closedRoads.forEach { road ->
                            Text(
                                text = "• ${road.identifier} reported unstable / disrupted",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = AlertRed900
                                )
                            )
                        }
                    }
                }
            }

            // Primary Action: START NAVIGATION
            Button(
                onClick = {
                    if (hasLocationCoords) {
                        val lat = alert!!.location!!.latitude
                        val lng = alert!!.location!!.longitude
                        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng"))
                            context.startActivity(browserIntent)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeBlue700)
            ) {
                Text(
                    text = stringResource(id = R.string.action_start_navigation),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
