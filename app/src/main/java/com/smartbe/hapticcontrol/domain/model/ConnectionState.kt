package com.smartbe.hapticcontrol.domain.model

/**
 * Estados posibles de la conexión BLE
 */
enum class ConnectionState {
    /**
     * Dispositivo desconectado, sin actividad BLE
     */
    DISCONNECTED,

    /**
     * Escaneando dispositivos BLE cercanos
     */
    SCANNING,

    /**
     * Intentando establecer conexión con un dispositivo
     */
    CONNECTING,

    /**
     * Conectado exitosamente y listo para comunicación
     */
    CONNECTED,

    /**
     * Error en el proceso de conexión o comunicación
     */
    ERROR;

    /**
     * Indica si el estado permite realizar operaciones BLE
     */
    val canPerformOperations: Boolean
        get() = this == CONNECTED

    /**
     * Indica si el estado está en transición (proceso activo)
     */
    val isTransitioning: Boolean
        get() = this == SCANNING || this == CONNECTING
}