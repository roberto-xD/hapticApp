package com.smartbe.hapticcontrol.presentation.settings

import com.smartbe.hapticcontrol.domain.model.BleDevice

/**
 * Estado UI para la pantalla de configuración
 */
data class SettingsUiState(
    val savedDevices: List<BleDevice> = emptyList(),
    val favoriteDevice: BleDevice? = null,
    val isLoading: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deviceToDelete: BleDevice? = null,
    val showNicknameDialog: Boolean = false,
    val deviceToEdit: BleDevice? = null,
    val editNickname: String = "",
    val error: String? = null
) {
    /**
     * Indica si hay dispositivos guardados
     */
    val hasDevices: Boolean
        get() = savedDevices.isNotEmpty()

    /**
     * Indica si debe mostrar un mensaje de error
     */
    val shouldShowError: Boolean
        get() = error != null

    /**
     * Obtiene el número de dispositivos guardados
     */
    val deviceCount: Int
        get() = savedDevices.size
}