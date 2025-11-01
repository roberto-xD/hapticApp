package com.smartbe.hapticcontrol.domain.repository

import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio para gestionar operaciones Bluetooth Low Energy
 * Define el contrato para todas las operaciones BLE de la aplicación
 */
interface BleRepository {

    // ========== ESTADOS OBSERVABLES ==========

    /**
     * Lista de dispositivos encontrados durante el escaneo
     */
    val scannedDevices: StateFlow<List<BleDeviceModel>>

    /**
     * Estado actual de la conexión
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Dispositivo actualmente conectado (null si no hay conexión)
     */
    val connectedDevice: StateFlow<BleDeviceModel?>

    /**
     * Datos recibidos del dispositivo conectado
     */
    val receivedData: StateFlow<ByteArray?>

    /**
     * Estado de advertising (transmitiendo como beacon)
     */
    val isAdvertising: StateFlow<Boolean>

    /**
     * Error durante advertising (null si no hay error)
     */
    val advertisingError: StateFlow<String?>

    // ========== FUNCIONES DE ESCANEO ==========

    /**
     * Inicia el escaneo de dispositivos BLE cercanos
     * Filtra automáticamente por nombres permitidos (KGL, WB, LG-)
     * Se detiene automáticamente después del timeout configurado
     */
    suspend fun startScan()

    /**
     * Detiene el escaneo de dispositivos BLE
     */
    suspend fun stopScan()

    // ========== FUNCIONES DE CONEXIÓN ==========

    /**
     * Conecta con un dispositivo BLE específico
     *
     * @param device Dispositivo al que conectarse
     * @return Result con éxito o error
     */
    suspend fun connect(device: BleDeviceModel): Result<Unit>

    /**
     * Desconecta del dispositivo actual
     * Limpia las características y cierra la conexión GATT
     */
    suspend fun disconnect()

    // ========== FUNCIONES DE ENVÍO DE DATOS ==========

    /**
     * Envía datos raw al dispositivo conectado
     *
     * @param data ByteArray con los datos a enviar
     * @return Result con éxito o error
     */
    suspend fun sendData(data: ByteArray): Result<Unit>

    /**
     * Envía un comando con formato específico (TAG + tipo + data + checksum)
     *
     * @param commandType Tipo de comando (byte)
     * @param data Datos adicionales del comando (vacío por defecto)
     * @return Result con éxito o error
     */
    suspend fun sendCommand(commandType: Byte, data: ByteArray = byteArrayOf()): Result<Unit>

    /**
     * Envía comando de parada al dispositivo
     * Equivalente a sendCommand(0x00)
     *
     * @return Result con éxito o error
     */
    suspend fun sendStopCommand(): Result<Unit>

    /**
     * Envía comando de modo con frecuencia e intensidad
     *
     * @param mode Modo de operación (byte)
     * @param frequency Frecuencia (0-100)
     * @param intensity Intensidad (0-100)
     * @return Result con éxito o error
     */
    suspend fun sendModeCommand(
        mode: Byte,
        frequency: Int,
        intensity: Int
    ): Result<Unit>

    // ========== FUNCIONES DE ADVERTISING (iBeacon) ==========

    /**
     * Inicia broadcasting BLE con datos personalizados
     * El dispositivo actúa como un beacon transmitiendo información
     *
     * @param data ByteArray con los datos a transmitir
     * @param durationMillis Duración del advertising en milisegundos (default: 5000)
     */
    suspend fun startAdvertising(data: ByteArray, durationMillis: Long = 5000)

    /**
     * Inicia broadcasting como iBeacon
     * Transmite en formato compatible con iBeacon de Apple
     *
     * @param uuid UUID del beacon (formato: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
     * @param major Identificador major (0-65535)
     * @param minor Identificador minor (0-65535)
     */
    suspend fun startIBeaconAdvertising(uuid: String, major: Int, minor: Int)

    /**
     * Detiene el broadcasting BLE
     */
    suspend fun stopAdvertising()

    // ========== FUNCIONES DE PERSISTENCIA ==========

    /**
     * Guarda un dispositivo en la base de datos local
     * Actualiza el timestamp de última conexión
     *
     * @param device Dispositivo a guardar
     */
    suspend fun saveDevice(device: BleDeviceModel)

    /**
     * Obtiene el último dispositivo conectado desde la base de datos
     * Útil para reconexión automática
     *
     * @return BleDeviceModel o null si no hay dispositivos guardados
     */
    suspend fun getLastConnectedDevice(): BleDeviceModel?

    /**
     * Obtiene todos los dispositivos guardados como Flow
     * Permite observar cambios en la lista de dispositivos guardados
     *
     * @return Flow que emite lista de dispositivos guardados
     */
    suspend fun getSavedDevices(): List<BleDeviceModel>

    /**
     * Elimina un dispositivo de la base de datos
     *
     * @param address Dirección MAC del dispositivo a eliminar
     */
    suspend fun deleteDevice(address: String)

    // ========== LIMPIEZA ==========

    /**
     * Libera recursos y cierra conexiones
     * Debe llamarse cuando ya no se necesite el repositorio
     */
    fun release()
}