package com.smartbe.hapticcontrol.domain.repository

import com.smartbe.hapticcontrol.domain.model.HapticMode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * El receptor tiene watchdog: obedece mientras recibe y corta a los pocos
 * segundos de silencio. Por eso esto no es "enviar un comando" sino "sostener
 * un modo".
 *
 * El enlace es unidireccional: que un modo quede seleccionado significa que lo
 * estamos emitiendo, no que el dispositivo lo haya recibido.
 */
interface HapticRepository {

    val isBroadcasting: StateFlow<Boolean>

    val activeMode: StateFlow<HapticMode?>

    /** True mientras el microfono gobierna la emision. */
    val isAudioReactive: StateFlow<Boolean>

    /**
     * Fallos de la fuente de audio, de uno en uno.
     *
     * No es StateFlow a proposito: el mismo mensaje repetido tiene que volver a
     * llegar a la UI, y un StateFlow lo descartaria por ser igual al anterior.
     */
    val audioErrors: SharedFlow<String>

    /** Arranca un modo y lo sostiene hasta stop() u otro select(). */
    fun select(mode: HapticMode)

    /**
     * Cede el control al microfono. El modo activo pasa a cambiar solo entre
     * las tres continuas segun el volumen.
     *
     * Requiere RECORD_AUDIO concedido; si falta, baja isAudioReactive y
     * publica el motivo en audioErrors.
     */
    fun startAudioReactive()

    /** Corta la emision y manda el apagado explicito. */
    suspend fun stop()
}
