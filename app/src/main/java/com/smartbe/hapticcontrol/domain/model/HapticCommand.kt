package com.smartbe.hapticcontrol.domain.model

/**
 * Comandos que el firmware del dispositivo 2.4 GHz reconoce.
 *
 * Los valores salen de la ingenieria inversa de la app original
 * (com.wb.lsr.common.ble.BleCommandUtil):
 *
 *   - Apagado:      0x00
 *   - Modos 1..5:   0x51 .. 0x55   (sendQdOrSy)
 *   - Intensidad:   0x11 .. 0x19 para niveles 1..9, y 0x20 para el nivel 10 (sendZl)
 *
 * Ojo: los "modos" NO son frecuencias en Hz. Son indices de patron; el ritmo
 * concreto lo decide el firmware. No existe ningun parametro continuo que
 * exponer, por eso no hay slider de frecuencia.
 */
sealed interface HapticCommand {

    /** Byte unico que viaja dentro de la trama RF. */
    val code: Byte

    /** Etiqueta corta para logs. */
    val label: String

    data object Off : HapticCommand {
        override val code: Byte = 0x00
        override val label: String = "OFF"
    }

    /** Patron de vibracion, 1..5. */
    data class Mode(val index: Int) : HapticCommand {
        init { require(index in MODE_RANGE) { "Modo fuera de rango: $index" } }
        override val code: Byte = (0x50 + index).toByte()
        override val label: String = "MODO $index"
    }

    /** Nivel de intensidad, 1..10. */
    data class Intensity(val level: Int) : HapticCommand {
        init { require(level in INTENSITY_RANGE) { "Intensidad fuera de rango: $level" } }

        // El nivel 10 rompe la secuencia: la app original codifica "20" en hex,
        // no 0x1A. Es una codificacion tipo BCD, no un contador lineal.
        override val code: Byte = if (level == MAX_INTENSITY) 0x20 else (0x10 + level).toByte()
        override val label: String = "INT $level"
    }

    companion object {
        val MODE_RANGE = 1..5
        val INTENSITY_RANGE = 1..10
        const val MAX_INTENSITY = 10
    }
}
