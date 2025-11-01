package com.smartbe.hapticcontrol.presentation.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartbe.hapticcontrol.domain.model.ConnectionState

/**
 * Pantalla de control del dispositivo BLE
 * Permite controlar intensidad, frecuencia y modos de operación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ControlViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val currentIntensity by viewModel.currentIntensity.collectAsState()
    val currentFrequency by viewModel.currentFrequency.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar mensajes de error
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Mostrar mensajes de éxito
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Control de Dispositivo",
                            style = MaterialTheme.typography.titleLarge
                        )
                        connectedDevice?.let { device ->
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (connectionState == ConnectionState.CONNECTED) {
                FloatingActionButton(
                    onClick = { viewModel.toggleDevice() },
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Detener" else "Iniciar"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (connectionState) {
                ConnectionState.DISCONNECTED -> {
                    DisconnectedContent(
                        onReconnect = {  viewModel.startScan() }
                    )
                }
                ConnectionState.CONNECTING -> {
                    ConnectingContent()
                }
                ConnectionState.CONNECTED -> {
                    ConnectedContent(
                        currentIntensity = currentIntensity,
                        currentFrequency = currentFrequency,
                        currentMode = currentMode,
                        isRunning = isRunning,
                        isAdvertising = isAdvertising,
                        isLoading = isLoading,
                        modeName = viewModel.getCurrentModeName(),
                        onIntensityChange = { viewModel.setIntensity(it) },
                        onIncreaseIntensity = { viewModel.increaseIntensity() },
                        onDecreaseIntensity = { viewModel.decreaseIntensity() },
                        onFrequencyChange = { viewModel.setFrequency(it) },
                        onIncreaseFrequency = { viewModel.increaseFrequency() },
                        onDecreaseFrequency = { viewModel.decreaseFrequency() },
                        onModeChange = { viewModel.setMode(it) },
                        onStartAdvertising = { viewModel.startIBeaconAdvertising() },
                        onDisconnect = { viewModel.disconnect() },
                        onReset = { viewModel.resetToDefaults() }
                    )
                }

                ConnectionState.SCANNING -> Unit
                ConnectionState.ERROR -> Unit
            }

            // Overlay de carga
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ========== ESTADOS DE CONEXIÓN ==========

@Composable
private fun DisconnectedContent(
    onReconnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Desconectado",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "La conexión con el dispositivo se ha perdido",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReconnect,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reconectar")
        }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Conectando...",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Estableciendo conexión con el dispositivo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConnectedContent(
    currentIntensity: Int,
    currentFrequency: Int,
    currentMode: Byte,
    isRunning: Boolean,
    isAdvertising: Boolean,
    isLoading: Boolean,
    modeName: String,
    onIntensityChange: (Int) -> Unit,
    onIncreaseIntensity: () -> Unit,
    onDecreaseIntensity: () -> Unit,
    onFrequencyChange: (Int) -> Unit,
    onIncreaseFrequency: () -> Unit,
    onDecreaseFrequency: () -> Unit,
    onModeChange: (Byte) -> Unit,
    onStartAdvertising: () -> Unit,
    onDisconnect: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Estado del dispositivo
        StatusCard(
            isRunning = isRunning,
            isAdvertising = isAdvertising,
            modeName = modeName
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control de intensidad
        ControlCard(
            title = "Intensidad",
            value = currentIntensity,
            onValueChange = onIntensityChange,
            onIncrease = onIncreaseIntensity,
            onDecrease = onDecreaseIntensity,
            enabled = !isLoading,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control de frecuencia
        ControlCard(
            title = "Frecuencia",
            value = currentFrequency,
            onValueChange = onFrequencyChange,
            onIncrease = onIncreaseFrequency,
            onDecrease = onDecreaseFrequency,
            enabled = !isLoading,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de modos
        ModeSelector(
            currentMode = currentMode,
            onModeChange = onModeChange,
            enabled = !isLoading && !isRunning
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Controles adicionales
        AdditionalControls(
            isAdvertising = isAdvertising,
            onStartAdvertising = onStartAdvertising,
            onReset = onReset,
            onDisconnect = onDisconnect,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
    }
}

// ========== COMPONENTES ==========

@Composable
private fun StatusCard(
    isRunning: Boolean,
    isAdvertising: Boolean,
    modeName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isRunning) "En funcionamiento" else "Detenido",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip(
                    label = "Modo",
                    value = modeName
                )

                if (isAdvertising) {
                    StatusChip(
                        label = "Broadcasting",
                        value = "Activo",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun ControlCard(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    enabled: Boolean,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "$value%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de progreso visual
            LinearProgressIndicator(
                progress = value / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = color.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botones +/-
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onDecrease,
                    enabled = enabled && value > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Disminuir")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("-10")
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    onClick = onIncrease,
                    enabled = enabled && value < 100,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+10")
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(
    currentMode: Byte,
    onModeChange: (Byte) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Modo de Operación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val modes = listOf(
                ControlViewModel.MODE_BASIC to "Básico",
                ControlViewModel.MODE_WAVE to "Ondas",
                ControlViewModel.MODE_PULSE to "Pulsos",
                ControlViewModel.MODE_RAMP to "Rampa",
                ControlViewModel.MODE_RANDOM to "Aleatorio"
            )

            modes.chunked(2).forEach { rowModes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowModes.forEach { (mode, name) ->
                        ModeButton(
                            name = name,
                            isSelected = currentMode == mode,
                            onClick = { onModeChange(mode) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Agregar espacio si solo hay un botón en la fila
                    if (rowModes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ModeButton(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Text(name)
    }
}

@Composable
private fun AdditionalControls(
    isAdvertising: Boolean,
    onStartAdvertising: () -> Unit,
    onReset: () -> Unit,
    onDisconnect: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Controles Adicionales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Broadcasting
            OutlinedButton(
                onClick = onStartAdvertising,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !isAdvertising
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isAdvertising) "Broadcasting Activo" else "Iniciar Broadcasting")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reset
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Text("Restablecer Valores")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Disconnect
            Button(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Desconectar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ControlScreenPreview() {
    MaterialTheme {
        // Preview del control card
        ControlCard(
            title = "Intensidad",
            value = 75,
            onValueChange = {},
            onIncrease = {},
            onDecrease = {},
            enabled = true,
            color = MaterialTheme.colorScheme.primary
        )
    }
}