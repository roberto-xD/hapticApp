package com.smartbe.hapticcontrol.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.usecase.DeleteDeviceUseCase
import com.smartbe.hapticcontrol.domain.usecase.GetSavedDevicesUseCase
import com.smartbe.hapticcontrol.domain.usecase.SetFavoriteDeviceUseCase
import com.smartbe.hapticcontrol.domain.usecase.UpdateDeviceNicknameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de configuración
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSavedDevicesUseCase: GetSavedDevicesUseCase,
    private val setFavoriteDeviceUseCase: SetFavoriteDeviceUseCase,
    private val updateDeviceNicknameUseCase: UpdateDeviceNicknameUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSavedDevices()
    }

    /**
     * Observa los dispositivos guardados
     */
    private fun observeSavedDevices() {
        viewModelScope.launch {
            getSavedDevicesUseCase.invoke().let{ devices ->
                val favorite = devices.firstOrNull { it.isFavorite }
                _uiState.update {
                    it.copy(
                        savedDevices = devices,
                        favoriteDevice = favorite,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Marca un dispositivo como favorito
     */
    fun setFavoriteDevice(device: BleDeviceModel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                setFavoriteDeviceUseCase(device.macAddress)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al marcar favorito: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Muestra el diálogo para editar apodo
     */
    fun showNicknameDialog(device: BleDeviceModel) {
        _uiState.update {
            it.copy(
                showNicknameDialog = true,
                deviceToEdit = device,
                editNickname = device.nickname
            )
        }
    }

    /**
     * Oculta el diálogo de apodo
     */
    fun hideNicknameDialog() {
        _uiState.update {
            it.copy(
                showNicknameDialog = false,
                deviceToEdit = null,
                editNickname = ""
            )
        }
    }

    /**
     * Actualiza el texto del apodo en edición
     */
    fun updateEditNickname(nickname: String) {
        _uiState.update { it.copy(editNickname = nickname) }
    }

    /**
     * Guarda el apodo editado
     */
    fun saveNickname() {
        val device = uiState.value.deviceToEdit ?: return
        val nickname = uiState.value.editNickname.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                updateDeviceNicknameUseCase(device.macAddress, nickname)
                hideNicknameDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al guardar apodo: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Muestra el diálogo de confirmación de eliminación
     */
    fun showDeleteDialog(device: BleDeviceModel) {
        _uiState.update {
            it.copy(
                showDeleteDialog = true,
                deviceToDelete = device
            )
        }
    }

    /**
     * Oculta el diálogo de eliminación
     */
    fun hideDeleteDialog() {
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                deviceToDelete = null
            )
        }
    }

    /**
     * Confirma y elimina un dispositivo
     */
    fun confirmDeleteDevice() {
        val device = uiState.value.deviceToDelete ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                deleteDeviceUseCase(device.macAddress)
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al eliminar dispositivo: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Limpia el error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}