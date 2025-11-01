package com.smartbe.hapticcontrol.presentation.scan

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    application: Application,
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val getScannedDevicesUseCase: GetScannedDevicesUseCase,
    private val getConnectionStateUseCase: GetConnectionStateUseCase,
    private val getLastConnectedDeviceUseCase: GetLastConnectedDeviceUseCase
) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val tag = "ScanViewModel"

    val scannedDevices: StateFlow<List<BleDeviceModel>> = getScannedDevicesUseCase()
    val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _lastConnectedDevice = MutableStateFlow<BleDeviceModel?>(null)
    val lastConnectedDevice: StateFlow<BleDeviceModel?> = _lastConnectedDevice.asStateFlow()

    private val _deviceCount = MutableStateFlow(0)
    val deviceCount: StateFlow<Int> = _deviceCount.asStateFlow()

    // NUEVO: StateFlow para permisos
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    companion object {
        private const val SCAN_DURATION = 10000L
    }

    init {
        viewModelScope.launch {
            scannedDevices.collect { devices ->
                _deviceCount.value = devices.size
            }
        }
        loadLastConnectedDevice()

        // IMPORTANTE: Verificar permisos al inicio SIN iniciar escaneo
        checkPermissions()
    }

    // ========== VERIFICACIÓN DE PERMISOS CORREGIDA ==========

    /**
     * CORREGIDO: Verifica permisos correctamente según la versión de Android
     */
    fun hasBluetoothPermissions(): Boolean {
        val permissionsToCheck = getRequiredPermissions()

        val allGranted = permissionsToCheck.all { permission ->
            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED

            // Log para debugging
            Log.d(tag, "Permission $permission: ${if (granted) "GRANTED" else "DENIED"}")

            granted
        }

        Log.d(tag, "All Bluetooth permissions granted: $allGranted")
        return allGranted
    }

    /**
     * NUEVO: Obtiene la lista de permisos necesarios según la versión de Android
     */
    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Android 11 y anteriores
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * NUEVO: Verifica y actualiza el estado de permisos
     */
    fun checkPermissions() {
        val hasPerms = hasBluetoothPermissions()
        _hasPermissions.value = hasPerms
        Log.d(tag, "Permissions check result: $hasPerms")
    }

    /**
     * Verifica si Bluetooth está habilitado
     */
    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val enabled = bluetoothAdapter?.isEnabled == true
        Log.d(tag, "Bluetooth enabled: $enabled")
        return enabled
    }

    /**
     * Verifica si el dispositivo soporta Bluetooth
     */
    fun isBluetoothSupported(): Boolean {
        val supported = BluetoothAdapter.getDefaultAdapter() != null
        Log.d(tag, "Bluetooth supported: $supported")
        return supported
    }

    /**
     * CORREGIDO: Verifica todos los requisitos
     */
    fun checkRequirements(): Pair<Boolean, String?> {
        return when {
            !isBluetoothSupported() -> {
                Log.e(tag, "Bluetooth not supported")
                false to "Este dispositivo no soporta Bluetooth"
            }
            !isBluetoothEnabled() -> {
                Log.e(tag, "Bluetooth not enabled")
                false to "Bluetooth no está habilitado"
            }
            !hasBluetoothPermissions() -> {
                Log.e(tag, "Bluetooth permissions not granted")
                false to "Faltan permisos de Bluetooth"
            }
            else -> {
                Log.d(tag, "All requirements met")
                true to null
            }
        }
    }

    // ========== FUNCIONES DE ESCANEO CORREGIDAS ==========

    /**
     * CORREGIDO: Inicia escaneo solo si se cumplen todos los requisitos
     */
    fun startScan() {
        viewModelScope.launch {
            try {
                Log.d(tag, "startScan() called")

                // IMPORTANTE: Verificar si ya está escaneando
                if (_isScanning.value) {
                    Log.w(tag, "Already scanning, ignoring request")
                    return@launch
                }

                // Verificar requisitos
                val (isReady, errorMsg) = checkRequirements()
                if (!isReady) {
                    Log.e(tag, "Cannot start scan: $errorMsg")
                    _errorMessage.value = errorMsg
                    return@launch
                }

                Log.d(tag, "Starting BLE scan...")
                _isScanning.value = true
                _errorMessage.value = null

                startScanUseCase()
                Log.d(tag, "Scan started successfully")

                // Auto-detener después de SCAN_DURATION
                launch {
                    delay(SCAN_DURATION)
                    if (_isScanning.value) {
                        Log.d(tag, "Auto-stopping scan after timeout")
                        stopScan()
                    }
                }

            } catch (e: SecurityException) {
                Log.e(tag, "SecurityException during scan", e)
                _errorMessage.value = "Error de permisos: ${e.message}"
                _isScanning.value = false
            } catch (e: Exception) {
                Log.e(tag, "Exception during scan", e)
                _errorMessage.value = "Error al iniciar escaneo: ${e.message}"
                _isScanning.value = false
            }
        }
    }

    /**
     * Detiene el escaneo
     */
    fun stopScan() {
        viewModelScope.launch {
            try {
                Log.d(tag, "stopScan() called")
                stopScanUseCase()
                _isScanning.value = false

                if (_deviceCount.value > 0) {
                    _successMessage.value = "Encontrados ${_deviceCount.value} dispositivos"
                }
                Log.d(tag, "Scan stopped successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error stopping scan", e)
                _errorMessage.value = "Error al detener escaneo: ${e.message}"
            }
        }
    }

    fun toggleScan() {
        if (_isScanning.value) {
            stopScan()
        } else {
            startScan()
        }
    }

    fun refreshScan() {
        viewModelScope.launch {
            if (_isScanning.value) {
                stopScan()
                delay(500)
            }
            startScan()
        }
    }

    // ========== FUNCIONES DE CONEXIÓN ==========

    fun connectToDevice(device: BleDeviceModel) {
        viewModelScope.launch {
            try {
                Log.d(tag, "connectToDevice() called for ${device.name}")

                val (isReady, errorMsg) = checkRequirements()
                if (!isReady) {
                    _errorMessage.value = errorMsg
                    return@launch
                }

                _isConnecting.value = true
                _errorMessage.value = null

                if (_isScanning.value) {
                    stopScan()
                    delay(300)
                }

                val result = connectDeviceUseCase(device)

                result.onSuccess {
                    Log.d(tag, "Connected successfully to ${device.name}")
                    _successMessage.value = "Conectado a ${device.name}"
                    _lastConnectedDevice.value = device
                }.onFailure { error ->
                    Log.e(tag, "Failed to connect to ${device.name}", error)
                    _errorMessage.value = "Error al conectar: ${error.message}"
                }

            } catch (e: SecurityException) {
                Log.e(tag, "SecurityException during connection", e)
                _errorMessage.value = "Error de permisos al conectar"
            } catch (e: Exception) {
                Log.e(tag, "Exception during connection", e)
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isConnecting.value = false
            }
        }
    }

    fun connectToDeviceByAddress(address: String) {
        val device = scannedDevices.value.find { it.macAddress == address }
        if (device != null) {
            connectToDevice(device)
        } else {
            _errorMessage.value = "Dispositivo no encontrado en la lista"
        }
    }

    // ========== RECONEXIÓN AUTOMÁTICA ==========

    private fun loadLastConnectedDevice() {
        viewModelScope.launch {
            try {
                val device = getLastConnectedDeviceUseCase()
                _lastConnectedDevice.value = device
                Log.d(tag, "Loaded last connected device: ${device?.name}")
            } catch (e: Exception) {
                Log.e(tag, "Error loading last connected device", e)
            }
        }
    }

    fun reconnectToLastDevice() {
        viewModelScope.launch {
            val device = _lastConnectedDevice.value
            if (device != null) {
                connectToDevice(device)
            } else {
                _errorMessage.value = "No hay dispositivo previo para reconectar"
            }
        }
    }

    // ========== UTILIDADES ==========

    fun getSignalStrength(rssi: Int): String {
        return when {
            rssi >= -50 -> "Excelente"
            rssi >= -70 -> "Buena"
            rssi >= -85 -> "Regular"
            else -> "Débil"
        }
    }

    fun hasDevices(): Boolean = _deviceCount.value > 0

    fun canStartScan(): Boolean {
        return !_isScanning.value &&
                !_isConnecting.value &&
                isBluetoothEnabled() &&
                hasBluetoothPermissions()
    }

    // ========== LIMPIEZA ==========

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    fun clearMessages() {
        clearError()
        clearSuccess()
    }

    // ========== LIFECYCLE ==========

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            if (_isScanning.value) {
                stopScan()
            }
        }
    }

    // CORREGIDO: Ya no inicia escaneo automáticamente
    fun onResume() {
        Log.d(tag, "onResume() called")
        loadLastConnectedDevice()
        checkPermissions() // Solo verifica permisos, no inicia escaneo

        if (!isBluetoothEnabled()) {
            _errorMessage.value = "Bluetooth está deshabilitado"
        }
    }

    fun onPause() {
        Log.d(tag, "onPause() called")
        if (_isScanning.value) {
            stopScan()
        }
    }
}