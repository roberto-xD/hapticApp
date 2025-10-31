package com.smartbe.hapticcontrol.presentation.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.domain.model.DeviceMode
import com.smartbe.hapticcontrol.domain.usecase.SetFrequencyUseCase
import com.smartbe.hapticcontrol.domain.usecase.SetIntensityUseCase
import com.smartbe.hapticcontrol.domain.usecase.SetPatternUseCase
import com.smartbe.hapticcontrol.domain.usecase.SetPowerStateUseCase
import com.smartbe.hapticcontrol.domain.usecase.connection.DisconnectDeviceUseCase
import com.smartbe.hapticcontrol.domain.usecase.connection.ObserveConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de control del dispositivo
 */
@HiltViewModel
class ControlViewModel @Inject constructor(
    private val setPowerStateUseCase: SetPowerStateUseCase,
    private val setIntensityUseCase: SetIntensityUseCase,
    private val setFrequencyUseCase: SetFrequencyUseCase,
    private val setPatternUseCase: SetPatternUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    init {
        observeConnectionState()
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
     * Enciende o apaga el dispositivo
     */
    fun setPowerState(isOn: Boolean) {
        if (!uiState.value.canSendCommands) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCommand = true, error = null) }

            setPowerStateUseCase(isOn)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentMode = currentState.currentMode.copy(isActive = isOn),
                            isSendingCommand = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isSendingCommand = false,
                            error = "Error al cambiar estado: ${exception.message}"
                        )
                    }
                }
        }
    }

    /**
     * Cambia la intensidad del dispositivo
     */
    fun setIntensity(level: Int) {
        if (!uiState.value.canSendCommands) return
        if (level !in 1..10) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCommand = true, error = null) }

            setIntensityUseCase(level)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentMode = currentState.currentMode.copy(
                                intensity = DeviceMode.Intensity(level)
                            ),
                            isSendingCommand = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isSendingCommand = false,
                            error = "Error al cambiar intensidad: ${exception.message}"
                        )
                    }
                }
        }
    }

    /**
     * Cambia la frecuencia del dispositivo
     */
    fun setFrequency(frequency: DeviceMode.Frequency) {
        if (!uiState.value.canSendCommands) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCommand = true, error = null) }

            setFrequencyUseCase(frequency)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentMode = currentState.currentMode.copy(frequency = frequency),
                            isSendingCommand = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isSendingCommand = false,
                            error = "Error al cambiar frecuencia: ${exception.message}"
                        )
                    }
                }
        }
    }

    /**
     * Cambia el patrón de vibración
     */
    fun setPattern(pattern: DeviceMode.Pattern) {
        if (!uiState.value.canSendCommands) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCommand = true, error = null) }

            setPatternUseCase(pattern)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentMode = currentState.currentMode.copy(pattern = pattern),
                            isSendingCommand = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isSendingCommand = false,
                            error = "Error al cambiar patrón: ${exception.message}"
                        )
                    }
                }
        }
    }

    /**
     * Incrementa la intensidad
     */
    fun increaseIntensity() {
        val currentLevel = uiState.value.currentIntensity
        if (currentLevel < DeviceMode.Intensity.MAX) {
            setIntensity(currentLevel + 1)
        }
    }

    /**
     * Decrementa la intensidad
     */
    fun decreaseIntensity() {
        val currentLevel = uiState.value.currentIntensity
        if (currentLevel > DeviceMode.Intensity.MIN) {
            setIntensity(currentLevel - 1)
        }
    }

    /**
     * Desconecta del dispositivo actual
     */
    fun disconnect() {
        viewModelScope.launch {
            disconnectDeviceUseCase()
        }
    }

    /**
     * Limpia el error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}