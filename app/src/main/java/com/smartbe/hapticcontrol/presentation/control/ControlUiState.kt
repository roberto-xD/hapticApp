package com.smartbe.hapticcontrol.presentation.control

import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.model.DeviceMode

/**
 * Estado UI para la pantalla de control del dispositivo
 */
data class ControlUiState(
    val connectedDevice: BleDeviceModel? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val currentMode: DeviceMode.CurrentState = DeviceMode.CurrentState(),
    val isSendingCommand: Boolean = false,
    val error: String? = null,
    val lastNotification: ByteArray? = null
) {
    /**
     * Indica si el dispositivo está conectado
     */
    val isConnected: Boolean
        get() = connectionState == ConnectionState.CONNECTED && connectedDevice != null

    /**
     * Indica si se pueden enviar comandos
     */
    val canSendCommands: Boolean
        get() = isConnected && !isSendingCommand

    /**
     * Nombre a mostrar del dispositivo
     */
    val deviceDisplayName: String
        get() = connectedDevice?.displayName ?: "Sin dispositivo"

    /**
     * Intensidad actual
     */
    val currentIntensity: Int
        get() = currentMode.intensity.level

    /**
     * Frecuencia actual
     */
    val currentFrequency: DeviceMode.Frequency
        get() = currentMode.frequency

    /**
     * Patrón actual
     */
    val currentPattern: DeviceMode.Pattern
        get() = currentMode.pattern

    /**
     * Estado de encendido/apagado
     */
    val isPowerOn: Boolean
        get() = currentMode.isActive

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControlUiState

        if (connectedDevice != other.connectedDevice) return false
        if (connectionState != other.connectionState) return false
        if (currentMode != other.currentMode) return false
        if (isSendingCommand != other.isSendingCommand) return false
        if (error != other.error) return false
        if (lastNotification != null) {
            if (other.lastNotification == null) return false
            if (!lastNotification.contentEquals(other.lastNotification)) return false
        } else if (other.lastNotification != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = connectedDevice?.hashCode() ?: 0
        result = 31 * result + connectionState.hashCode()
        result = 31 * result + currentMode.hashCode()
        result = 31 * result + isSendingCommand.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (lastNotification?.contentHashCode() ?: 0)
        return result
    }
}