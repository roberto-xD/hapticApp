package com.smartbe.hapticcontrol.domain.model

/**
 * Un programa de vibracion: lo que el usuario percibe como "modo".
 *
 * Hipotesis de trabajo: el firmware NO conoce patrones. Solo obedece
 * "vibra a nivel X" mientras esta recibiendo, y corta por watchdog cuando
 * deja de llegar señal. Por tanto el ritmo lo genera el telefono emitiendo
 * una secuencia temporizada de intensidades.
 *
 * Bajo esa hipotesis los 9 modos de la app original se descomponen en:
 *   - 3 continuos  -> Steady con nivel bajo / medio / alto
 *   - 6 ritmicos   -> Rhythm con distintas secuencias de pasos
 */
sealed interface VibrationProgram {

    val displayName: String

    /** Nivel fijo sostenido. */
    data class Steady(
        override val displayName: String,
        val level: Int
    ) : VibrationProgram {
        init { require(level in HapticCommand.INTENSITY_RANGE) }
    }

    /** Secuencia ciclica de niveles. Un nivel 0 significa silencio. */
    data class Rhythm(
        override val displayName: String,
        val steps: List<Step>
    ) : VibrationProgram {
        init { require(steps.isNotEmpty()) }
    }

    /**
     * @param level    0 = silencio, 1..10 = intensidad
     * @param holdMs   cuanto se sostiene este paso
     */
    data class Step(val level: Int, val holdMs: Long)

    companion object {
        /**
         * Catalogo provisional.
         *
         * Los tres continuos son fiables: son intensidades directas.
         * Los seis ritmicos son RECONSTRUCCION POR OIDO, no extraidos del APK.
         * Hay que ajustar tempos y niveles comparando con la app original, o
         * mejor, leyendo el fragment que dispara los modos.
         */
        val CATALOG: List<VibrationProgram> = listOf(
            Steady("Continua baja", 2),
            Steady("Continua media", 5),
            Steady("Continua alta", 10),

            Rhythm("Pulso lento", listOf(
                Step(10, 400), Step(0, 400)
            )),
            Rhythm("Pulso rapido", listOf(
                Step(10, 150), Step(0, 150)
            )),
            Rhythm("Onda", listOf(
                Step(2, 200), Step(5, 200), Step(8, 200),
                Step(10, 300), Step(8, 200), Step(5, 200)
            )),
            Rhythm("Escalera", listOf(
                Step(2, 500), Step(5, 500), Step(8, 500), Step(10, 800)
            )),
            Rhythm("Latido", listOf(
                Step(10, 130), Step(0, 130), Step(10, 130), Step(0, 700)
            )),
            Rhythm("Rafaga", listOf(
                Step(10, 90), Step(0, 90), Step(10, 90), Step(0, 90),
                Step(10, 90), Step(0, 600)
            ))
        )
    }
}
