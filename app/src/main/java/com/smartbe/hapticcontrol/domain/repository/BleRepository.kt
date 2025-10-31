package com.smartbe.hapticcontrol.domain.repository

import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.model.DeviceCommand
import com.smartbe.hapticcontrol.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones BLE (Bluetooth Low Energy)
 * Maneja escaneo, conexión y comunicación con dispositivos
 */
interface BleRepository {

    /**
     * Flow que emite el estado actual de la conexión
     */
    val connectionState: Flow<ConnectionState>

    /**
     * Flow que emite los resultados del escaneo
     */
    val scanResults: Flow<ScanResult>

    /**
     * Flow que emite notificaciones recibidas del dispositivo
     */
    val notifications: Flow<ByteArray>

    /**
     * Inicia el escaneo de dispositivos BLE cercanos
     *
     * @param timeoutMillis Tiempo máximo de escaneo en milisegundos (default: 5000ms)
     */
    suspend fun startScan(timeoutMillis: Long = 5000L)

    /**
     * Detiene el escaneo activo
     */
    suspend fun stopScan()

    /**
     * Conecta con un dispositivo BLE específico
     *
     * @param device Dispositivo al que conectarse
     * @return Result indicando éxito o fallo con mensaje de error
     */
    suspend fun connect(device: BleDevice): Result<Unit>

    /**
     * Desconecta del dispositivo actual
     */
    suspend fun disconnect()

    /**
     * Envía un comando al dispositivo conectado
     *
     * @param command Comando a enviar
     * @return Result indicando éxito o fallo
     */
    suspend fun sendCommand(command: DeviceCommand): Result<Unit>

    /**
     * Habilita las notificaciones del dispositivo
     *
     * @return Result indicando éxito o fallo
     */
    suspend fun enableNotifications(): Result<Unit>

    /**
     * Deshabilita las notificaciones del dispositivo
     */
    suspend fun disableNotifications()

    /**
     * Obtiene el dispositivo actualmente conectado
     *
     * @return Dispositivo conectado o null si no hay conexión
     */
    suspend fun getConnectedDevice(): BleDevice?

    /**
     * Verifica si Bluetooth está habilitado
     *
     * @return true si Bluetooth está habilitado
     */
    suspend fun isBluetoothEnabled(): Boolean

    /**
     * Verifica si el dispositivo soporta BLE
     *
     * @return true si BLE está soportado
     */
    suspend fun isBleSupported(): Boolean
}