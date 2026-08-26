package com.smartbe.hapticcontrol.presentation.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.data.bluetooth.RfPayloadBuilder
import com.smartbe.hapticcontrol.domain.model.HapticCommand
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import com.smartbe.hapticcontrol.domain.usecase.SendHapticCommandUseCase
import com.smartbe.hapticcontrol.domain.usecase.StopHapticUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val sendCommand: SendHapticCommandUseCase,
    private val stopHaptic: StopHapticUseCase,
    private val payloadBuilder: RfPayloadBuilder,
    repository: HapticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ControlUiState(nativeAvailable = payloadBuilder.nativeAvailable)
    )
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    /**
     * Una sola emision en vuelo. Si el usuario toca otro modo mientras la
     * rafaga anterior sigue al aire, cancelamos la anterior: el ultimo toque
     * gana, que es lo que espera cualquiera manejando un mando.
     */
    private var pendingJob: Job? = null

    init {
        repository.isBroadcasting
            .onEach { broadcasting ->
                _uiState.value = _uiState.value.copy(isBroadcasting = broadcasting)
            }
            .launchIn(viewModelScope)

        if (!payloadBuilder.nativeAvailable) {
            _uiState.value = _uiState.value.copy(
                error = "libble24.so no se pudo cargar. Revisa que este en jniLibs/ " +
                        "y que la clase BLEUtil conserve su paquete original."
            )
        }
    }

    fun selectMode(index: Int) {
        dispatch(HapticCommand.Mode(index)) { state ->
            state.copy(activeMode = index)
        }
    }

    fun setIntensity(level: Int) {
        dispatch(HapticCommand.Intensity(level)) { state ->
            state.copy(intensity = level)
        }
    }

    fun powerOff() {
        dispatch(HapticCommand.Off) { state ->
            state.copy(activeMode = null)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun dispatch(
        command: HapticCommand,
        onSuccess: (ControlUiState) -> ControlUiState
    ) {
        if (!payloadBuilder.nativeAvailable) return

        pendingJob?.cancel()
        pendingJob = viewModelScope.launch {
            sendCommand(command)
                .onSuccess { _uiState.value = onSuccess(_uiState.value) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "No se pudo emitir el comando"
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // No apagamos el dispositivo aqui a proposito: el usuario puede cerrar
        // la app dejandolo encendido, igual que con el mando original.
    }
}
