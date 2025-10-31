package com.smartbe.hapticcontrol.presentation.scan

import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.model.BleError
import com.smartbe.hapticcontrol.domain.model.ConnectionState

/**
 * Estado UI para la pantalla de escaneo
 */
data class ScanUiState(
    val isScanning: Boolean = false,
    val scannedDevices: List<BleDevice> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val error: BleError? = null,
    val errorMessage: String? = null,
    val isBluetoothEnabled: Boolean = true,
    val hasPermissions: Boolean = false
) {
    /**
     * Indica si se puede iniciar un escaneo
     */
    val canStartScan: Boolean
        get() = !isScanning &&
                isBluetoothEnabled &&
                hasPermissions &&
                connectionState != ConnectionState.CONNECTING

    /**
     * Indica si hay dispositivos encontrados
     */
    val hasDevices: Boolean
        get() = scannedDevices.isNotEmpty()

    /**
     * Indica si debe mostrar un mensaje de error
     */
    val shouldShowError: Boolean
        get() = error != null || errorMessage != null
}