package com.smartbe.hapticcontrol.presentation.control

import com.smartbe.hapticcontrol.domain.model.HapticMode

data class ControlUiState(
    val activeMode: HapticMode? = null,
    val isBroadcasting: Boolean = false,
    val nativeAvailable: Boolean = true,
    val error: String? = null
) {
    val isOn: Boolean get() = activeMode != null

    val continuous: List<HapticMode> get() = HapticMode.continuous

    val rhythmic: List<HapticMode> get() = HapticMode.rhythmic
}
