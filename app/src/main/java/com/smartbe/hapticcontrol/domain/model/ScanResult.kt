package com.smartbe.hapticcontrol.domain.model

/**
 * Representa el resultado de una operación de escaneo BLE
 */
sealed class ScanResult {

    /**
     * Escaneo en progreso
     */
    object Scanning : ScanResult()

    /**
     * Dispositivo encontrado durante el escaneo
     */
    data class DeviceFound(val device: BleDeviceModel) : ScanResult()

    /**
     * Escaneo completado exitosamente
     */
    data class ScanComplete(val devices: List<BleDeviceModel>) : ScanResult()

    /**
     * Error durante el escaneo
     */
    data class ScanError(
        val error: BleError,
        val message: String? = null
    ) : ScanResult()

    /**
     * Escaneo detenido por el usuario
     */
    object ScanStopped : ScanResult()
}

/**
 * Tipos de errores BLE posibles
 */
enum class BleError {
    BLUETOOTH_DISABLED,
    BLUETOOTH_NOT_SUPPORTED,
    LOCATION_PERMISSION_DENIED,
    BLUETOOTH_PERMISSION_DENIED,
    SCAN_FAILED,
    CONNECTION_FAILED,
    DEVICE_NOT_FOUND,
    WRITE_FAILED,
    READ_FAILED,
    NOTIFICATION_FAILED,
    UNKNOWN_ERROR;

    /**
     * Indica si el error es recuperable sin reiniciar la app
     */
    val isRecoverable: Boolean
        get() = when (this) {
            BLUETOOTH_DISABLED,
            LOCATION_PERMISSION_DENIED,
            BLUETOOTH_PERMISSION_DENIED,
            DEVICE_NOT_FOUND -> true
            else -> false
        }
}