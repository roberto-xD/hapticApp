package com.smartbe.hapticcontrol.presentation.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.data.bluetooth.RfPayloadBuilder
import com.smartbe.hapticcontrol.domain.model.VibrationProgram
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import com.smartbe.hapticcontrol.domain.usecase.RunProgramUseCase
import com.smartbe.hapticcontrol.domain.usecase.StopHapticUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val runProgram: RunProgramUseCase,
    private val stopHaptic: StopHapticUseCase,
    private val payloadBuilder: RfPayloadBuilder,
    repository: HapticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ControlUiState(nativeAvailable = payloadBuilder.nativeAvailable)
    )
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    init {
        repository.isBroadcasting
            .onEach { value -> _uiState.value = _uiState.value.copy(isBroadcasting = value) }
            .launchIn(viewModelScope)

        repository.activeProgram
            .onEach { value -> _uiState.value = _uiState.value.copy(activeProgram = value) }
            .launchIn(viewModelScope)

        if (!payloadBuilder.nativeAvailable) {
            _uiState.value = _uiState.value.copy(
                error = "libble24.so no se pudo cargar. Revisa jniLibs/ y que la clase " +
                        "BLEUtil conserve su paquete original."
            )
        }
    }

    fun select(program: VibrationProgram) {
        if (!payloadBuilder.nativeAvailable) return
        runProgram(program)
    }

    fun powerOff() {
        viewModelScope.launch { stopHaptic() }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
