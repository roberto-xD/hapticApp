package com.smartbe.hapticcontrol.presentation.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.usecase.DisconnectDeviceUseCase
import com.smartbe.hapticcontrol.domain.usecase.GetConnectedDeviceUseCase
import com.smartbe.hapticcontrol.domain.usecase.GetConnectionStateUseCase
import com.smartbe.hapticcontrol.domain.usecase.GetReceivedDataUseCase
import com.smartbe.hapticcontrol.domain.usecase.SendCommandUseCase
import com.smartbe.hapticcontrol.domain.usecase.StartAdvertisingUseCase
import com.smartbe.hapticcontrol.domain.usecase.StartIBeaconAdvertisingUseCase
import com.smartbe.hapticcontrol.domain.usecase.StartScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de control del dispositivo BLE
 * Maneja el estado de conexión, comandos y modos de operación
 */
@HiltViewModel
class ControlViewModel @Inject constructor(
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val getConnectionStateUseCase: GetConnectionStateUseCase,
    private val getConnectedDeviceUseCase: GetConnectedDeviceUseCase,
    private val getReceivedDataUseCase: GetReceivedDataUseCase,
    private val sendCommandUseCase: SendCommandUseCase,
    private val startAdvertisingUseCase: StartAdvertisingUseCase,
    private val startIBeaconAdvertisingUseCase: StartIBeaconAdvertisingUseCase,
    private val startScanUseCase: StartScanUseCase,
) : ViewModel() {

    // ========== ESTADOS DEL DISPOSITIVO ==========

    /**
     * Estado de conexión actual
     */
    val connectionState: StateFlow<ConnectionState> = getConnectionStateUseCase()

    /**
     * Dispositivo conectado actualmente
     */
    val connectedDevice: StateFlow<BleDeviceModel?> = getConnectedDeviceUseCase()

    /**
     * Datos recibidos del dispositivo
     */
    val receivedData: StateFlow<ByteArray?> = getReceivedDataUseCase()

    // ========== ESTADOS DE CONTROL ==========

    /**
     * Intensidad actual (0-100)
     */
    private val _currentIntensity = MutableStateFlow(0)
    val currentIntensity: StateFlow<Int> = _currentIntensity.asStateFlow()

    /**
     * Frecuencia actual (0-100)
     */
    private val _currentFrequency = MutableStateFlow(50)
    val currentFrequency: StateFlow<Int> = _currentFrequency.asStateFlow()

    /**
     * Modo de operación actual
     * 0x01 = Modo básico
     * 0x02 = Modo ondas
     * 0x03 = Modo pulsos
     * etc.
     */
    private val _currentMode = MutableStateFlow<Byte>(0x01)
    val currentMode: StateFlow<Byte> = _currentMode.asStateFlow()

    /**
     * Estado de ejecución (si el dispositivo está operando)
     */
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /**
     * Estado de advertising (transmitiendo como beacon)
     */
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    // ========== ESTADOS DE UI ==========

    /**
     * Mensaje de error para mostrar al usuario
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Mensaje de éxito para mostrar al usuario
     */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * Estado de carga para operaciones asíncronas
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ========== MODOS PREDEFINIDOS ==========

    companion object {
        const val MODE_BASIC: Byte = 0x01
        const val MODE_WAVE: Byte = 0x02
        const val MODE_PULSE: Byte = 0x03
        const val MODE_RAMP: Byte = 0x04
        const val MODE_RANDOM: Byte = 0x05

        const val COMMAND_STOP: Byte = 0x00
        const val COMMAND_START: Byte = 0x01
    }

    // ========== FUNCIONES DE CONEXIÓN ==========

    fun startScan(){
        viewModelScope.launch {
            startScanUseCase()
        }
    }
    /**
     * Desconecta del dispositivo actual
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Detener dispositivo antes de desconectar
                if (_isRunning.value) {
                    stopDevice()
                }

                disconnectDeviceUseCase()
                _successMessage.value = "Desconectado correctamente"

                // Resetear estados
                resetControlStates()

            } catch (e: Exception) {
                _errorMessage.value = "Error al desconectar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== FUNCIONES DE CONTROL DE INTENSIDAD ==========

    /**
     * Establece la intensidad del dispositivo
     * @param intensity Valor entre 0 y 100
     */
    fun setIntensity(intensity: Int) {
        val clampedIntensity = intensity.coerceIn(0, 100)
        _currentIntensity.value = clampedIntensity

        // Solo enviar comando si el dispositivo está ejecutando
        if (_isRunning.value) {
            sendModeCommand()
        }
    }

    /**
     * Incrementa la intensidad en 10 unidades
     */
    fun increaseIntensity() {
        val newIntensity = (_currentIntensity.value + 10).coerceAtMost(100)
        setIntensity(newIntensity)
    }

    /**
     * Decrementa la intensidad en 10 unidades
     */
    fun decreaseIntensity() {
        val newIntensity = (_currentIntensity.value - 10).coerceAtLeast(0)
        setIntensity(newIntensity)
    }

    // ========== FUNCIONES DE CONTROL DE FRECUENCIA ==========

    /**
     * Establece la frecuencia del dispositivo
     * @param frequency Valor entre 0 y 100
     */
    fun setFrequency(frequency: Int) {
        val clampedFrequency = frequency.coerceIn(0, 100)
        _currentFrequency.value = clampedFrequency

        // Solo enviar comando si el dispositivo está ejecutando
        if (_isRunning.value) {
            sendModeCommand()
        }
    }

    /**
     * Incrementa la frecuencia en 10 unidades
     */
    fun increaseFrequency() {
        val newFrequency = (_currentFrequency.value + 10).coerceAtMost(100)
        setFrequency(newFrequency)
    }

    /**
     * Decrementa la frecuencia en 10 unidades
     */
    fun decreaseFrequency() {
        val newFrequency = (_currentFrequency.value - 10).coerceAtLeast(0)
        setFrequency(newFrequency)
    }

    // ========== FUNCIONES DE MODOS ==========

    /**
     * Establece el modo de operación
     * @param mode Byte representando el modo (usar constantes MODE_*)
     */
    fun setMode(mode: Byte) {
        _currentMode.value = mode

        // Solo enviar comando si el dispositivo está ejecutando
        if (_isRunning.value) {
            sendModeCommand()
        }
    }

    /**
     * Cambia al modo básico
     */
    fun setBasicMode() {
        setMode(MODE_BASIC)
    }

    /**
     * Cambia al modo ondas
     */
    fun setWaveMode() {
        setMode(MODE_WAVE)
    }

    /**
     * Cambia al modo pulsos
     */
    fun setPulseMode() {
        setMode(MODE_PULSE)
    }

    /**
     * Cambia al modo rampa
     */
    fun setRampMode() {
        setMode(MODE_RAMP)
    }

    /**
     * Cambia al modo aleatorio
     */
    fun setRandomMode() {
        setMode(MODE_RANDOM)
    }

    // ========== FUNCIONES DE ENVÍO DE COMANDOS ==========

    /**
     * Envía un comando personalizado al dispositivo
     * @param commandType Tipo de comando
     * @param data Datos adicionales (opcional)
     */
    fun sendCommand(commandType: Byte, data: ByteArray = byteArrayOf()) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                sendCommandUseCase(commandType, data).onFailure { error ->
                    _errorMessage.value = "Error enviando comando: ${error.message}"
                }.onSuccess {
                    _successMessage.value = "Comando enviado correctamente"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Envía comando de modo con frecuencia e intensidad actuales
     */
    private fun sendModeCommand() {
        viewModelScope.launch {
            try {
                sendCommandUseCase(
                    _currentMode.value,
                    byteArrayOf(
                        _currentFrequency.value.toByte(),
                        _currentIntensity.value.toByte()
                    )
                ).onFailure { error ->
                    _errorMessage.value = "Error enviando modo: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al enviar modo: ${e.message}"
            }
        }
    }

    /**
     * Inicia la operación del dispositivo con parámetros actuales
     */
    fun startDevice() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                sendCommandUseCase(
                    _currentMode.value,
                    byteArrayOf(
                        _currentFrequency.value.toByte(),
                        _currentIntensity.value.toByte()
                    )
                ).onFailure { error ->
                    _errorMessage.value = "Error al iniciar: ${error.message}"
                }.onSuccess {
                    _isRunning.value = true
                    _successMessage.value = "Dispositivo iniciado"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Detiene la operación del dispositivo
     */
    fun stopDevice() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                sendCommandUseCase(COMMAND_STOP).onFailure { error ->
                    _errorMessage.value = "Error al detener: ${error.message}"
                }.onSuccess {
                    _isRunning.value = false
                    _successMessage.value = "Dispositivo detenido"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error al detener: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alterna entre iniciar y detener el dispositivo
     */
    fun toggleDevice() {
        if (_isRunning.value) {
            stopDevice()
        } else {
            startDevice()
        }
    }

    // ========== FUNCIONES DE ADVERTISING (iBeacon) ==========

    /**
     * Inicia advertising como iBeacon con parámetros específicos
     * @param uuid UUID del beacon (formato estándar)
     * @param major Identificador major (0-65535)
     * @param minor Identificador minor (0-65535)
     */
    fun startIBeaconAdvertising(
        uuid: String = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
        major: Int = 100,
        minor: Int = 1
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                startIBeaconAdvertisingUseCase(uuid, major, minor)
                _isAdvertising.value = true
                _successMessage.value = "Advertising iniciado"

            } catch (e: Exception) {
                _errorMessage.value = "Error al iniciar advertising: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Inicia advertising con datos personalizados
     * @param data ByteArray con los datos a transmitir
     * @param durationMillis Duración del advertising
     */
    fun startCustomAdvertising(data: ByteArray, durationMillis: Long = 5000) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                startAdvertisingUseCase(data, durationMillis)
                _isAdvertising.value = true
                _successMessage.value = "Broadcasting iniciado"

            } catch (e: Exception) {
                _errorMessage.value = "Error al iniciar broadcasting: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========== FUNCIONES DE RESETEO ==========

    /**
     * Resetea todos los estados de control a valores por defecto
     */
    private fun resetControlStates() {
        _currentIntensity.value = 0
        _currentFrequency.value = 50
        _currentMode.value = MODE_BASIC
        _isRunning.value = false
        _isAdvertising.value = false
    }

    /**
     * Resetea a valores por defecto
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            // Detener si está ejecutando
            if (_isRunning.value) {
                stopDevice()
            }

            resetControlStates()
            _successMessage.value = "Valores reseteados"
        }
    }

    // ========== FUNCIONES DE UI ==========

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Limpia el mensaje de éxito
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * Limpia todos los mensajes
     */
    fun clearMessages() {
        clearError()
        clearSuccess()
    }

    // ========== UTILIDADES ==========

    /**
     * Obtiene el nombre del modo actual
     */
    fun getCurrentModeName(): String {
        return when (_currentMode.value) {
            MODE_BASIC -> "Básico"
            MODE_WAVE -> "Ondas"
            MODE_PULSE -> "Pulsos"
            MODE_RAMP -> "Rampa"
            MODE_RANDOM -> "Aleatorio"
            else -> "Desconocido"
        }
    }

    /**
     * Verifica si está conectado
     */
    fun isConnected(): Boolean {
        return connectionState.value == ConnectionState.CONNECTED
    }

    /**
     * Verifica si está conectando
     */
    fun isConnecting(): Boolean {
        return connectionState.value == ConnectionState.CONNECTING
    }

    /**
     * Verifica si está desconectado
     */
    fun isDisconnected(): Boolean {
        return connectionState.value == ConnectionState.DISCONNECTED
    }

    // ========== CLEANUP ==========

    override fun onCleared() {
        super.onCleared()
        // El repositorio se limpia automáticamente por el scope
    }
}