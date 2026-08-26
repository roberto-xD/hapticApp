package com.smartbe.hapticcontrol.domain.repository

import com.smartbe.hapticcontrol.domain.model.HapticCommand
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de emision de comandos.
 *
 * El transporte es unidireccional por difusion: el telefono anuncia, el
 * dispositivo escucha. No hay conexion, ni emparejamiento, ni confirmacion.
 * Un Result.success() significa "el anuncio salio al aire", NO "el dispositivo
 * lo recibio". No existe forma de saber lo segundo.
 */
interface HapticRepository {

    /** True mientras hay un anuncio al aire. */
    val isBroadcasting: StateFlow<Boolean>

    /** Ultimo comando emitido con exito, para reflejarlo en la UI. */
    val lastCommand: StateFlow<HapticCommand?>

    /** Emite un comando. */
    suspend fun send(command: HapticCommand): Result<Unit>

    /** Atajo para HapticCommand.Off. */
    suspend fun stop(): Result<Unit>
}
