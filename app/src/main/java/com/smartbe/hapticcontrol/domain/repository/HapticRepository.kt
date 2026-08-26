package com.smartbe.hapticcontrol.domain.repository

import com.smartbe.hapticcontrol.domain.model.HapticCommand
import com.smartbe.hapticcontrol.domain.model.VibrationProgram
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de emision.
 *
 * El transporte es unidireccional por difusion y el receptor tiene watchdog:
 * obedece mientras recibe y corta a los pocos segundos de silencio. Por eso
 * esto no es una API de "enviar comando" sino de "mantener un programa".
 *
 * Un exito significa que la emision arranco, NO que el dispositivo la recibio.
 */
interface HapticRepository {

    val isBroadcasting: StateFlow<Boolean>

    /** Programa en curso, o null si esta detenido. */
    val activeProgram: StateFlow<VibrationProgram?>

    /** Arranca un programa y lo sostiene hasta stop() u otro run(). */
    fun run(program: VibrationProgram)

    /** Detiene la emision y manda el apagado explicito. */
    suspend fun stop()

    /** Emite un comando suelto. Util para diagnostico. */
    suspend fun sendOnce(command: HapticCommand): Result<Unit>
}
