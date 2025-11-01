package com.smartbe.hapticcontrol.domain.model

/**
 * Modelo de dominio que representa un dispositivo BLE
 *
 * @property macAddress Dirección MAC única del dispositivo
 * @property name Nombre BLE del dispositivo (KGL, WB, LG-)
 * @property nickname Apodo personalizado del usuario (vacío por defecto)
 * @property rssi Intensidad de señal recibida (Received Signal Strength Indicator)
 * @property lastConnectionDate Timestamp de última conexión exitosa
 * @property isFavorite Marca si es el dispositivo favorito para reconexión automática
 */
data class BleDeviceModel(
    val macAddress: String,
    val name: String,
    val nickname: String = "",
    val rssi: Int = 0,
    val lastConnectionDate: Long = 0L,
    val isFavorite: Boolean = false
) {
    /**
     * Nombre a mostrar: prioriza nickname, si está vacío usa name
     */
    val displayName: String
        get() = nickname.ifEmpty { name }

    /**
     * Indica si el dispositivo tiene buena señal (RSSI > -70 dBm)
     */
    val hasGoodSignal: Boolean
        get() = rssi > -70

    /**
     * Verifica si el nombre del dispositivo es válido según los filtros
     */
    fun isValidDevice(): Boolean {
        return name.contains("KGL") ||
                name.contains("WB") ||
                name.startsWith("LG-")
    }

    companion object {
        /**
         * Filtros de nombres de dispositivos soportados
         * Preparado para agregar más filtros en el futuro
         */
        val DEVICE_NAME_FILTERS = listOf("KGL", "WB", "LG-")
    }
}