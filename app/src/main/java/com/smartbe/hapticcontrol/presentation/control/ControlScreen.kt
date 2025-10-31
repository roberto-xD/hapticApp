package com.smartbe.hapticcontrol.presentation.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.model.DeviceMode

/**
 * Pantalla de control del dispositivo BLE
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ControlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Volver atrás si se desconecta
    LaunchedEffect(uiState.connectionState) {
        if (uiState.connectionState == ConnectionState.DISCONNECTED) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.deviceDisplayName) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!uiState.isConnected) {
            DisconnectedState(modifier = Modifier.padding(paddingValues))
        } else {
            ControlContent(
                uiState = uiState,
                onPowerToggle = { viewModel.setPowerState(it) },
                onIntensityChange = { viewModel.setIntensity(it) },
                onIntensityIncrease = { viewModel.increaseIntensity() },
                onIntensityDecrease = { viewModel.decreaseIntensity() },
                onFrequencyChange = { viewModel.setFrequency(it) },
                onPatternChange = { viewModel.setPattern(it) },
                onErrorDismiss = { viewModel.clearError() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ControlContent(
    uiState: ControlUiState,
    onPowerToggle: (Boolean) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onIntensityIncrease: () -> Unit,
    onIntensityDecrease: () -> Unit,
    onFrequencyChange: (DeviceMode.Frequency) -> Unit,
    onPatternChange: (DeviceMode.Pattern) -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error message
        if (uiState.error != null) {
            ErrorCard(
                message = uiState.error,
                onDismiss = onErrorDismiss
            )
        }

        // Power control
        PowerControlCard(
            isPowerOn = uiState.isPowerOn,
            isEnabled = uiState.canSendCommands,
            onToggle = onPowerToggle
        )

        // Intensity control
        IntensityControlCard(
            intensity = uiState.currentIntensity,
            isEnabled = uiState.canSendCommands && uiState.isPowerOn,
            onIntensityChange = onIntensityChange,
            onIncrease = onIntensityIncrease,
            onDecrease = onIntensityDecrease
        )

        // Frequency control
        FrequencyControlCard(
            currentFrequency = uiState.currentFrequency,
            isEnabled = uiState.canSendCommands && uiState.isPowerOn,
            onFrequencyChange = onFrequencyChange
        )

        // Pattern control
        PatternControlCard(
            currentPattern = uiState.currentPattern,
            isEnabled = uiState.canSendCommands && uiState.isPowerOn,
            onPatternChange = onPatternChange
        )
    }
}

@Composable
private fun PowerControlCard(
    isPowerOn: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Encendido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isPowerOn) "Dispositivo encendido" else "Dispositivo apagado",
                    style = MaterialTheme.typography.bodyMedium
                )

                Switch(
                    checked = isPowerOn,
                    onCheckedChange = onToggle,
                    enabled = isEnabled
                )
            }
        }
    }
}

@Composable
private fun IntensityControlCard(
    intensity: Int,
    isEnabled: Boolean,
    onIntensityChange: (Int) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Intensidad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Display current intensity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    intensity.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Slider
            Slider(
                value = intensity.toFloat(),
                onValueChange = { onIntensityChange(it.toInt()) },
                valueRange = DeviceMode.Intensity.MIN.toFloat()..DeviceMode.Intensity.MAX.toFloat(),
                steps = DeviceMode.Intensity.MAX - DeviceMode.Intensity.MIN - 1,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth()
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecrease,
                    enabled = isEnabled && intensity > DeviceMode.Intensity.MIN,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Disminuir")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disminuir")
                }

                OutlinedButton(
                    onClick = onIncrease,
                    enabled = isEnabled && intensity < DeviceMode.Intensity.MAX,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aumentar")
                }
            }
        }
    }
}

@Composable
private fun FrequencyControlCard(
    currentFrequency: DeviceMode.Frequency,
    isEnabled: Boolean,
    onFrequencyChange: (DeviceMode.Frequency) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Frecuencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeviceMode.Frequency.values().forEach { frequency ->
                    FilterChip(
                        selected = currentFrequency == frequency,
                        onClick = { onFrequencyChange(frequency) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(frequency.name)
                                Text(
                                    "${frequency.hz} Hz",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        enabled = isEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternControlCard(
    currentPattern: DeviceMode.Pattern,
    isEnabled: Boolean,
    onPatternChange: (DeviceMode.Pattern) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Patrón",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeviceMode.Pattern.values().forEach { pattern ->
                    FilterChip(
                        selected = currentPattern == pattern,
                        onClick = { onPatternChange(pattern) },
                        label = { Text(pattern.name) },
                        enabled = isEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun DisconnectedState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Desconectando...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}