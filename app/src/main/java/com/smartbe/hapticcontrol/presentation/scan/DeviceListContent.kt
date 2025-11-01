package com.smartbe.hapticcontrol.presentation.scan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.ui.theme.HapticControlTheme

/**
 * Componente que muestra la lista de dispositivos BLE escaneados
 */
@Composable
fun DeviceListContent(
    devices: List<BleDeviceModel>,
    isScanning: Boolean,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            // Mostrar mensaje de escaneo si está buscando y no hay dispositivos
            isScanning && devices.isEmpty() -> {
                ScanningEmptyState()
            }
            // Mostrar mensaje si no hay dispositivos y no está escaneando
            devices.isEmpty() && !isScanning -> {
                EmptyState()
            }
            // Mostrar lista de dispositivos
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header con contador
                    item {
                        DeviceListHeader(
                            deviceCount = devices.size,
                            isScanning = isScanning
                        )
                    }

                    // Lista de dispositivos
                    items(
                        items = devices,
                        key = { device -> device.macAddress }
                    ) { device ->
                        DeviceCard(
                            device = device,
                            onClick = { onDeviceClick(device.macAddress) }
                        )
                    }

                    // Espaciado inferior para el FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Indicador de escaneo flotante
        AnimatedVisibility(
            visible = isScanning && devices.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ScanningIndicator()
        }
    }
}

// ========== HEADER ==========

@Composable
private fun DeviceListHeader(
    deviceCount: Int,
    isScanning: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dispositivos encontrados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isScanning) {
                        "Buscando dispositivos..."
                    } else {
                        "$deviceCount dispositivo${if (deviceCount != 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Divider()
    }
}

// ========== DEVICE CARD ==========

@Composable
private fun DeviceCard(
    device: BleDeviceModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de Bluetooth
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del dispositivo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Indicador de señal
                SignalStrengthIndicator(rssi = device.rssi)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Icono de navegación
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Conectar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ========== SIGNAL STRENGTH INDICATOR ==========

@Composable
private fun SignalStrengthIndicator(
    rssi: Int,
    modifier: Modifier = Modifier
) {
    val (strength, color, icon) = getSignalInfo(rssi)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = strength,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "($rssi dBm)",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Obtiene información sobre la calidad de señal
 */
private fun getSignalInfo(rssi: Int): Triple<String, Color, ImageVector> {
    return when {
        rssi >= -50 -> Triple(
            "Excelente",
            Color(0xFF4CAF50), // Verde
            Icons.Default.Warning
        )
        rssi >= -70 -> Triple(
            "Buena",
            Color(0xFF2196F3), // Azul
            Icons.Default.Warning
        )
        rssi >= -85 -> Triple(
            "Regular",
            Color(0xFFFF9800), // Naranja
            Icons.Default.Warning
        )
        else -> Triple(
            "Débil",
            Color(0xFFF44336), // Rojo
            Icons.Default.Warning
        )
    }
}

// ========== EMPTY STATES ==========

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No se encontraron dispositivos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Presiona el botón para buscar dispositivos BLE cercanos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Asegúrate de que:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            BulletPoint("El dispositivo está encendido")
            BulletPoint("Está cerca de tu teléfono")
            BulletPoint("Bluetooth está habilitado")
            BulletPoint("El dispositivo está en modo emparejamiento")
        }
    }
}

@Composable
private fun ScanningEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Buscando dispositivos...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Esto puede tomar unos segundos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ========== SCANNING INDICATOR ==========

@Composable
private fun ScanningIndicator() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Escaneando...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ========== PREVIEWS ==========

@Preview(showBackground = true)
@Composable
private fun DeviceCardPreview() {
    HapticControlTheme {
        DeviceCard(
            device = BleDeviceModel(
                name = "KGL Device 01",
                macAddress = "AA:BB:CC:DD:EE:FF",
                rssi = -65
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceListContentPreview() {
    HapticControlTheme {
        DeviceListContent(
            devices = listOf(
                BleDeviceModel("KGL Device 01", "AA:BB:CC:DD:EE:FF", rssi = -45),
                BleDeviceModel("WB Sensor", "11:22:33:44:55:66",rssi = -70),
                BleDeviceModel("LG-Controller", "99:88:77:66:55:44", rssi = -85),
                BleDeviceModel("KGL Device 02", "AA:BB:CC:DD:EE:11", rssi = -92)
            ),
            isScanning = false,
            onDeviceClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    HapticControlTheme {
        EmptyState()
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanningEmptyStatePreview() {
    HapticControlTheme {
        ScanningEmptyState()
    }
}

@Preview(showBackground = true)
@Composable
private fun SignalStrengthIndicatorPreview() {
    HapticControlTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SignalStrengthIndicator(rssi = -45)
            SignalStrengthIndicator(rssi = -65)
            SignalStrengthIndicator(rssi = -80)
            SignalStrengthIndicator(rssi = -95)
        }
    }
}