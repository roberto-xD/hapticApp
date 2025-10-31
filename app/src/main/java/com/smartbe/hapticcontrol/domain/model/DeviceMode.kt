package com.smartbe.hapticcontrol.domain.model

/**
 * Representa los diferentes modos de operación del dispositivo
 */
sealed class DeviceMode {

    /**
     * Niveles de frecuencia del dispositivo
     */
    enum class Frequency(val hz: Int, val commandValue: Int) {
        LOW(60, 0x01),      // 60 Hz
        MEDIUM(90, 0x02),   // 90 Hz
        HIGH(120, 0x03);    // 120 Hz

        companion object {
            fun fromCommandValue(value: Int): Frequency? {
                return values().find { it.commandValue == value }
            }
        }
    }

    /**
     * Niveles de intensidad (1-10)
     */
    data class Intensity(val level: Int) {
        init {
            require(level in 1..10) { "Intensity level must be between 1 and 10" }
        }

        companion object {
            val MIN = 1
            val MAX = 10
            val DEFAULT = 5
        }
    }

    /**
     * Patrones de vibración disponibles
     */
    enum class Pattern(val patternId: Int) {
        CONTINUOUS(0x01),    // Vibración continua
        WAVE(0x02),          // Patrón de onda
        PULSE(0x03);         // Patrón de pulsos

        companion object {
            fun fromPatternId(id: Int): Pattern? {
                return values().find { it.patternId == id }
            }
        }
    }

    /**
     * Estado actual completo del dispositivo
     */
    data class CurrentState(
        val frequency: Frequency = Frequency.MEDIUM,
        val intensity: Intensity = Intensity(Intensity.DEFAULT),
        val pattern: Pattern = Pattern.CONTINUOUS,
        val isActive: Boolean = false
    )
}