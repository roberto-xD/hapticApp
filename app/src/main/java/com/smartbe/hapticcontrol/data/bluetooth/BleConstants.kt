package com.smartbe.hapticcontrol.data.bluetooth

import java.util.UUID

/**
 * Constantes para operaciones BLE
 */
object BleConstants {

    /**
     * UUIDs del servicio y características
     * (Valores del código original)
     */
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")

    /**
     * Descriptor para habilitar notificaciones
     */
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /**
     * Timeouts en milisegundos
     */
    const val SCAN_TIMEOUT = 5000L
    const val CONNECTION_TIMEOUT = 10000L
    const val OPERATION_TIMEOUT = 5000L

    /**
     * Límites de escaneo
     */
    const val MAX_SCAN_DEVICES = 20

    /**
     * Filtros de nombres de dispositivos
     * Extensible para agregar más filtros en el futuro
     */
    val DEVICE_NAME_FILTERS = listOf("KGL", "WB", "LG-")

    /**
     * Verifica si un nombre de dispositivo es válido
     */
    fun isValidDeviceName(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        return DEVICE_NAME_FILTERS.any { filter ->
            name.contains(filter, ignoreCase = true) || name.startsWith(filter, ignoreCase = true)
        }
    }

    /**
     * Estados de conexión GATT
     */
    const val STATE_DISCONNECTED = 0
    const val STATE_CONNECTING = 1
    const val STATE_CONNECTED = 2

    /**
     * Códigos de resultado GATT
     */
    const val GATT_SUCCESS = 0
    const val GATT_FAILURE = 133
}