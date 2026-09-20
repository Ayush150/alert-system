package com.sih26001.mobilealert.presentation.settings

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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sih26001.mobilealert.R
import com.sih26001.mobilealert.core.ui.theme.SafeBlue600
import com.sih26001.mobilealert.core.ui.theme.SafeBlue700
import com.sih26001.mobilealert.core.ui.theme.SafeBlue50
import com.sih26001.mobilealert.core.ui.theme.Slate50
import com.sih26001.mobilealert.core.ui.theme.Slate100
import com.sih26001.mobilealert.core.ui.theme.Slate200
import com.sih26001.mobilealert.core.ui.theme.Slate300
import com.sih26001.mobilealert.core.ui.theme.Slate400
import com.sih26001.mobilealert.core.ui.theme.Slate500
import com.sih26001.mobilealert.core.ui.theme.Slate600
import com.sih26001.mobilealert.core.ui.theme.Slate700
import com.sih26001.mobilealert.core.ui.theme.Slate800
import com.sih26001.mobilealert.core.ui.theme.Slate900
import com.sih26001.mobilealert.core.util.AppLanguage
import com.sih26001.mobilealert.core.util.LocaleManager
import com.sih26001.mobilealert.di.DependencyContainer
import com.sih26001.mobilealert.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onChangeRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by LocaleManager.currentLanguage.collectAsState()
    val selectedRole = remember {
        DependencyContainer.rolePreferences.getSelectedRole() ?: UserRole.CITIZEN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
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
            // --- 1. LANGUAGE SELECTION CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.language_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = stringResource(id = R.string.settings_language_desc),
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )

                    HorizontalDivider(color = Slate100)

                    AppLanguage.entries.forEach { lang ->
                        val isSelected = currentLang == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LocaleManager.setLanguage(lang, context)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { LocaleManager.setLanguage(lang, context) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = SafeBlue700,
                                        unselectedColor = Slate400
                                    )
                                )
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Slate900 else Slate700
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) SafeBlue50 else Slate100
                            ) {
                                Text(
                                    text = lang.code.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SafeBlue700 else Slate500
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. ACTIVE ROLE CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.current_role_label),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Slate100,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = selectedRole.emoji, fontSize = 24.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedRole.getLocalizedName(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Text(
                                text = selectedRole.getLocalizedDesc(),
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )
                        }
                    }

                    Button(
                        onClick = onChangeRole,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text(
                            text = stringResource(id = R.string.action_change_role),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // --- 3. ABOUT GEOAGENT CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Slate50),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.about_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    Text(
                        text = stringResource(id = R.string.about_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate600,
                            lineHeight = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "GeoAgent v1.0.0 • SIH26001 Authoritative Alert System",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
