package com.smartbe.hapticcontrol.presentation.control

import com.smartbe.hapticcontrol.domain.model.HapticCommand

/**
 * Estado de la pantalla de control.
 *
 * [activeMode] e [intensity] son lo que el usuario pidio, no lo que el
 * dispositivo confirma: el transporte es unidireccional y no hay telemetria.
 * La UI debe reflejar esa incertidumbre en vez de fingir que sabe el estado real.
 */
data class ControlUiState(
    val activeMode: Int? = null,
    val intensity: Int = 5,
    val isBroadcasting: Boolean = false,
    val nativeAvailable: Boolean = true,
    val error: String? = null
) {
    val isOn: Boolean get() = activeMode != null

    val canInteract: Boolean get() = nativeAvailable && !isBroadcasting

    val modes: IntRange get() = HapticCommand.MODE_RANGE

    val intensityRange: IntRange get() = HapticCommand.INTENSITY_RANGE
}
