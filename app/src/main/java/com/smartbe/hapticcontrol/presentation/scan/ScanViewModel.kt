package com.smartbe.hapticcontrol.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.model.ScanResult
import com.smartbe.hapticcontrol.domain.usecase.connection.ConnectDeviceUseCase
import com.smartbe.hapticcontrol.domain.usecase.connection.ObserveConnectionStateUseCase
import com.smartbe.hapticcontrol.domain.usecase.scan.ObserveScanResultsUseCase
import com.smartbe.hapticcontrol.domain.usecase.scan.StartScanUseCase
import com.smartbe.hapticcontrol.domain.usecase.scan.StopScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de escaneo de dispositivos BLE
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val startScanUseCase: StartScanUseCase,
    private val stopScanUseCase: StopScanUseCase,
    private val observeScanResultsUseCase: ObserveScanResultsUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val scannedDevices = mutableMapOf<String, BleDevice>()

    init {
        observeScanResults()
        observeConnectionState()
    }

    /**
     * Observa los resultados del escaneo
     */
    private fun observeScanResults() {
        viewModelScope.launch {
            observeScanResultsUseCase()
                .collect { result ->
                    when (result) {
                        is ScanResult.Scanning -> {
                            _uiState.update { it.copy(isScanning = true, error = null) }
                        }
                        is ScanResult.DeviceFound -> {
                            // Evitar duplicados
                            if (!scannedDevices.containsKey(result.device.macAddress)) {
                                scannedDevices[result.device.macAddress] = result.device
                                _uiState.update {
                                    it.copy(scannedDevices = scannedDevices.values.toList())
                                }
                            }
                        }
                        is ScanResult.ScanComplete -> {
                            _uiState.update {
                                it.copy(
                                    isScanning = false,
                                    scannedDevices = result.devices
                                )
                            }
                        }
                        is ScanResult.ScanError -> {
                            _uiState.update {
                                it.copy(
                                    isScanning = false,
                                    error = result.error,
                                    errorMessage = result.message
                                )
                            }
                        }
                        is ScanResult.ScanStopped -> {
                            _uiState.update { it.copy(isScanning = false) }
                        }
                    }
                }
        }
    }

    /**
     * Observa el estado de conexión
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            observeConnectionStateUseCase()
                .collect { state ->
                    _uiState.update { it.copy(connectionState = state) }
                }
        }
    }

    /**
     * Inicia el escaneo de dispositivos
     */
    fun startScan() {
        viewModelScope.launch {
            scannedDevices.clear()
            _uiState.update { it.copy(scannedDevices = emptyList(), error = null) }
            startScanUseCase(timeoutMillis = 10000L)
        }
    }

    /**
     * Detiene el escaneo
     */
    fun stopScan() {
        viewModelScope.launch {
            stopScanUseCase()
        }
    }

    /**
     * Conecta a un dispositivo
     */
    fun connectToDevice(device: BleDevice) {
        viewModelScope.launch {
            stopScan() // Detener escaneo antes de conectar

            connectDeviceUseCase(device)
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Error al conectar: ${exception.message}")
                    }
                }
        }
    }

    /**
     * Actualiza el estado de permisos
     */
    fun updatePermissionsGranted(granted: Boolean) {
        _uiState.update { it.copy(hasPermissions = granted) }
    }

    /**
     * Actualiza el estado de Bluetooth
     */
    fun updateBluetoothEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isBluetoothEnabled = enabled) }
    }

    /**
     * Limpia el error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            stopScanUseCase()
        }
    }
}