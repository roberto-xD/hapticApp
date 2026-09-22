package com.smartbe.hapticcontrol.presentation.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.data.bluetooth.RfPayloadBuilder
import com.smartbe.hapticcontrol.domain.model.HapticMode
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import com.smartbe.hapticcontrol.domain.usecase.SelectModeUseCase
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
    private val selectMode: SelectModeUseCase,
    private val stopHaptic: StopHapticUseCase,
    private val payloadBuilder: RfPayloadBuilder,
    private val repository: HapticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ControlUiState(nativeAvailable = payloadBuilder.nativeAvailable)
    )
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    init {
        repository.isBroadcasting
            .onEach { value -> _uiState.value = _uiState.value.copy(isBroadcasting = value) }
            .launchIn(viewModelScope)

        repository.isAudioReactive
            .onEach { value -> _uiState.value = _uiState.value.copy(isAudioReactive = value) }
            .launchIn(viewModelScope)

        repository.activeMode
            .onEach { value -> _uiState.value = _uiState.value.copy(activeMode = value) }
            .launchIn(viewModelScope)

        if (!payloadBuilder.nativeAvailable) {
            _uiState.value = _uiState.value.copy(
                error = "libble24.so no se pudo cargar. Revisa jniLibs/ y que la clase " +
                        "BLEUtil conserve su paquete original."
            )
        }
    }

    fun select(mode: HapticMode) {
        if (!payloadBuilder.nativeAvailable) return
        selectMode(mode)
    }

    fun toggleAudioReactive() {
        if (!payloadBuilder.nativeAvailable) return
        if (_uiState.value.isAudioReactive) {
            powerOff()
        } else {
            repository.startAudioReactive()
        }
    }

    fun powerOff() {
        viewModelScope.launch { stopHaptic() }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
