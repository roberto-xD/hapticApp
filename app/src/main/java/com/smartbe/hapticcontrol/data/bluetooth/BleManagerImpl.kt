package com.smartbe.hapticcontrol.data.bluetooth

import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.model.BleError
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.model.DeviceCommand
import com.smartbe.hapticcontrol.domain.model.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del gestor BLE que coordina escaneo y conexión
 */
@Singleton
class BleManagerImpl @Inject constructor(
    private val bleScanner: BleScanner,
    private val bleConnector: BleConnector
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _scanResults = MutableSharedFlow<ScanResult>(replay = 0)
    val scanResults: SharedFlow<ScanResult> = _scanResults.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _notifications = MutableSharedFlow<ByteArray>(replay = 0)
    val notifications: SharedFlow<ByteArray> = _notifications.asSharedFlow()

    private var scanJob: Job? = null

    /**
     * Inicia el escaneo de dispositivos BLE
     */
    fun startScan(timeoutMillis: Long = BleConstants.SCAN_TIMEOUT) {
        // Cancelar escaneo anterior si existe
        stopScan()

        _scanResults.tryEmit(ScanResult.Scanning)
        _connectionState.value = ConnectionState.SCANNING

        scanJob = scope.launch {
            val devices = mutableListOf<BleDevice>()

            try {
                bleScanner.startScan()
                    .catch { exception ->
                        val error = when {
                            exception.message?.contains("not supported") == true ->
                                BleError.BLUETOOTH_NOT_SUPPORTED
                            exception.message?.contains("disabled") == true ->
                                BleError.BLUETOOTH_DISABLED
                            exception.message?.contains("permission") == true ->
                                BleError.BLUETOOTH_PERMISSION_DENIED
                            else -> BleError.SCAN_FAILED
                        }
                        _scanResults.emit(ScanResult.ScanError(error, exception.message))
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = { device ->
                                devices.add(device)
                                _scanResults.emit(ScanResult.DeviceFound(device))
                            },
                            onFailure = { exception ->
                                _scanResults.emit(
                                    ScanResult.ScanError(
                                        BleError.SCAN_FAILED,
                                        exception.message
                                    )
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                _scanResults.emit(ScanResult.ScanError(BleError.SCAN_FAILED, e.message))
            }

            // Timeout del escaneo
            delay(timeoutMillis)
            stopScan()

            _scanResults.emit(ScanResult.ScanComplete(devices))
            if (_connectionState.value == ConnectionState.SCANNING) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Detiene el escaneo actual
     */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        bleScanner.stopScan()
        bleScanner.clearScannedDevices()

        scope.launch {
            _scanResults.emit(ScanResult.ScanStopped)
        }
    }

    /**
     * Conecta a un dispositivo BLE
     */
    suspend fun connect(device: BleDevice): Result<Unit> {
        _connectionState.value = ConnectionState.CONNECTING

        return bleConnector.connect(device)
            .onSuccess {
                _connectionState.value = ConnectionState.CONNECTED

                // Habilitar notificaciones automáticamente
                enableNotifications()

                // Iniciar observación de notificaciones
                observeDeviceNotifications()
            }
            .onFailure {
                _connectionState.value = ConnectionState.ERROR
                delay(500) // Pequeño delay antes de volver a DISCONNECTED
                _connectionState.value = ConnectionState.DISCONNECTED
            }
    }

    /**
     * Desconecta del dispositivo actual
     */
    fun disconnect() {
        bleConnector.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Envía un comando al dispositivo conectado
     */
    suspend fun sendCommand(command: DeviceCommand): Result<Unit> {
        if (!bleConnector.isConnected()) {
            return Result.failure(Exception("Device not connected"))
        }

        return bleConnector.writeCharacteristic(command.hexCommand)
    }

    /**
     * Habilita notificaciones del dispositivo
     */
    suspend fun enableNotifications(): Result<Unit> {
        return bleConnector.enableNotifications()
    }

    /**
     * Observa las notificaciones del dispositivo y las reenvía
     */
    private fun observeDeviceNotifications() {
        scope.launch {
            bleConnector.observeNotifications()
                .catch { exception ->
                    // Manejar errores de notificación
                }
                .collect { data ->
                    _notifications.emit(data)
                }
        }
    }

    /**
     * Obtiene el dispositivo actualmente conectado
     */
    fun getConnectedDevice(): BleDevice? {
        return bleConnector.getConnectedDevice()
    }

    /**
     * Verifica si Bluetooth está habilitado
     */
    fun isBluetoothEnabled(): Boolean {
        return bleScanner.isBluetoothEnabled()
    }

    /**
     * Verifica si BLE está soportado
     */
    fun isBleSupported(): Boolean {
        return bleScanner.isBleSupported()
    }

    /**
     * Limpia recursos
     */
    fun cleanup() {
        stopScan()
        disconnect()
        scope.cancel()
    }
}