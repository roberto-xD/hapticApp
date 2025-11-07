package com.smartbe.hapticcontrol.domain.model

/**
 * Modo visitante - Broadcasting BLE
 * Permite controlar dispositivos físicos sin conexión directa
 * El móvil actúa como beacon/controlador transmitiendo comandos
 */
data class VisitorMode(
    val isEnabled: Boolean = false,
    val isBroadcasting: Boolean = false,
    val broadcastingDuration: Long = 5000L, // Duración del broadcasting en ms
    val lastCommandSent: ByteArray? = null,
    val broadcastCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisitorMode

        if (isEnabled != other.isEnabled) return false
        if (isBroadcasting != other.isBroadcasting) return false
        if (broadcastingDuration != other.broadcastingDuration) return false
        if (lastCommandSent != null) {
            if (other.lastCommandSent == null) return false
            if (!lastCommandSent.contentEquals(other.lastCommandSent)) return false
        } else if (other.lastCommandSent != null) return false
        if (broadcastCount != other.broadcastCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isEnabled.hashCode()
        result = 31 * result + isBroadcasting.hashCode()
        result = 31 * result + broadcastingDuration.hashCode()
        result = 31 * result + (lastCommandSent?.contentHashCode() ?: 0)
        result = 31 * result + broadcastCount
        return result
    }
}

/**
 * Estados del modo visitante
 */
enum class VisitorModeState {
    DISABLED,           // Modo visitante desactivado
    ENABLED_IDLE,       // Habilitado pero sin transmitir
    BROADCASTING_LOW,   // Transmitiendo en modo baja latencia (1.5s)
    BROADCASTING_HIGH   // Transmitiendo en modo normal (5s)
}

/**
 * Comandos para modo visitante (broadcasting)
 */
object VisitorCommands {
    // Comandos básicos
    const val CMD_STOP: Byte = 0x00
    const val CMD_START: Byte = 0x01

    // Comandos de modo
    const val CMD_MODE_BASIC: Byte = 0x01
    const val CMD_MODE_WAVE: Byte = 0x02
    const val CMD_MODE_PULSE: Byte = 0x03
    const val CMD_MODE_RAMP: Byte = 0x04
    const val CMD_MODE_RANDOM: Byte = 0x05

    // Identificadores para broadcasting
    const val BROADCAST_TAG_PREFIX = "VIS"
    const val BROADCAST_MANUFACTURER_ID = 0x004C // Apple Company ID para iBeacon
}