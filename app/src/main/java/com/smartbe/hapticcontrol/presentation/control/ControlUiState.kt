package com.smartbe.hapticcontrol.presentation.control

import com.smartbe.hapticcontrol.domain.model.VibrationProgram

data class ControlUiState(
    val programs: List<VibrationProgram> = VibrationProgram.CATALOG,
    val activeProgram: VibrationProgram? = null,
    val isBroadcasting: Boolean = false,
    val nativeAvailable: Boolean = true,
    val error: String? = null
) {
    val isOn: Boolean get() = activeProgram != null

    val steady: List<VibrationProgram> get() = programs.filterIsInstance<VibrationProgram.Steady>()

    val rhythms: List<VibrationProgram> get() = programs.filterIsInstance<VibrationProgram.Rhythm>()
}
