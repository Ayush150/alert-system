package com.sih26001.mobilealert.presentation.roleselection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sih26001.mobilealert.core.ui.theme.SafeBlue600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.SafeBlue50
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
import com.sih26001.mobilealert.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    viewModel: RoleSelectionViewModel,
    onRoleConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate away once confirmed
    LaunchedEffect(uiState.isConfirmed) {
        if (uiState.isConfirmed) {
            onRoleConfirmed()
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // --- SIXTH SENSE Branding ---
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Slate900
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate500,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.3.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- Title: Who are you? ---
            Text(
                text = stringResource(id = R.string.role_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.role_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate500
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Role Cards ---
            UserRole.entries.forEach { role ->
                val isSelected = uiState.selectedRole == role

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) SafeBlue600 else Slate200,
                    animationSpec = tween(200),
                    label = "borderColor"
                )
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) SafeBlue50 else Color.White,
                    animationSpec = tween(200),
                    label = "containerColor"
                )
                val elevation by animateDpAsState(
                    targetValue = if (isSelected) 4.dp else 0.dp,
                    animationSpec = tween(200),
                    label = "elevation"
                )

                Card(
                    onClick = { viewModel.selectRole(role) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji avatar
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) SafeBlue600.copy(alpha = 0.15f) else Slate100,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = role.emoji, fontSize = 22.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = role.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SafeBlue700 else Slate800
                                )
                            )
                            Text(
                                text = role.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSelected) Slate700 else Slate500
                                ),
                                maxLines = 2
                            )
                        }

                        // Selection indicator
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) SafeBlue600 else Color.Transparent,
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (isSelected) SafeBlue600 else Slate300
                            ),
                            modifier = Modifier.size(22.dp)
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Continue Button ---
            Button(
                onClick = { viewModel.confirmSelection() },
                enabled = uiState.selectedRole != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate900,
                    disabledContainerColor = Slate200
                )
            ) {
                Text(
                    text = stringResource(id = R.string.role_continue),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.selectedRole != null) Color.White else Slate400,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Disclaimer ---
            Text(
                text = stringResource(id = R.string.role_disclaimer),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate400,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
