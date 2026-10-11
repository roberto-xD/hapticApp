package com.smartbe.hapticcontrol.data.repository

import android.content.Context
import android.util.Log
import com.smartbe.hapticcontrol.data.audio.AudioCaptureException
import com.smartbe.hapticcontrol.data.audio.AudioLevelSource
import com.smartbe.hapticcontrol.data.audio.MicForegroundService
import com.smartbe.hapticcontrol.data.bluetooth.BroadcastAdvertiser
import com.smartbe.hapticcontrol.data.bluetooth.RfPayloadBuilder
import com.smartbe.hapticcontrol.domain.model.HapticMode
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadBuilder: RfPayloadBuilder,
    private val advertiser: BroadcastAdvertiser,
    private val audioLevelSource: AudioLevelSource
) : HapticRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var emissionJob: Job? = null

    override val isBroadcasting: StateFlow<Boolean> = advertiser.isBroadcasting

    private val _activeMode = MutableStateFlow<HapticMode?>(null)
    override val activeMode: StateFlow<HapticMode?> = _activeMode.asStateFlow()

    private val _isAudioReactive = MutableStateFlow(false)
    override val isAudioReactive: StateFlow<Boolean> = _isAudioReactive.asStateFlow()

    private val _audioErrors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val audioErrors: SharedFlow<String> = _audioErrors.asSharedFlow()

    init {
        // El servicio en primer plano sigue a isAudioReactive, no al job de
        // emision. Atarlo al job abre una carrera: el finally de una emision ya
        // cancelada puede ejecutarse despues de que otra haya arrancado el
        // servicio, y pararia el microfono de la sesion nueva. Con un solo
        // colector sobre la bandera, start y stop salen siempre en orden, y
        // ambos son idempotentes.
        scope.launch {
            _isAudioReactive.collect { activo ->
                if (activo) MicForegroundService.start(context)
                else MicForegroundService.stop(context)
            }
        }
    }

    override fun select(mode: HapticMode) {
        _isAudioReactive.value = false
        emissionJob?.cancel()
        _activeMode.value = mode

        emissionJob = scope.launch {
            val payload = payloadBuilder.build(mode.code, mode.displayName)
                .getOrElse { error ->
                    Log.e(TAG, "No se pudo construir la trama de ${mode.displayName}", error)
                    _activeMode.value = null
                    return@launch
                }
            // durationMs null: el anuncio se sostiene hasta que se cancele.
            advertiser.emit(payload, durationMs = null)
        }
    }

    /**
     * collectLatest hace justo lo que hace falta: cada nivel nuevo cancela la
     * emision anterior, que estaba suspendida sosteniendo el anuncio, y arranca
     * la siguiente. Sin temporizadores ni estado propio.
     *
     * El servicio en primer plano lo gobierna el colector del init: aqui basta
     * con levantar la bandera.
     */
    override fun startAudioReactive() {
        emissionJob?.cancel()
        _isAudioReactive.value = true

        emissionJob = scope.launch {
            try {
                audioLevelSource.levels().collectLatest { nivel ->
                    val mode = AUDIO_LEVELS.getOrNull(nivel - 1)
                    _activeMode.value = mode

                    if (mode == null) {
                        // Silencio: una rafaga de apagado y la radio se para.
                        // Sostener 0x00 indefinidamente no le dice nada al
                        // aparato y deja la antena encendida para nada.
                        emitOff()
                    } else {
                        payloadBuilder.build(mode.code, mode.displayName)
                            .onSuccess { payload ->
                                advertiser.emit(payload, durationMs = null)
                            }
                    }
                }
            } catch (error: AudioCaptureException) {
                // Sin esto el modo se quedaria encendido en la UI sin que pase
                // nada: el flujo habria terminado y nadie se habria enterado.
                Log.e(TAG, "Captura de audio imposible", error)
                _isAudioReactive.value = false
                _activeMode.value = null
                _audioErrors.tryEmit(
                    error.message ?: "No se pudo abrir el microfono"
                )
            }
        }
    }

    override suspend fun stop() {
        _isAudioReactive.value = false
        emissionJob?.cancel()
        emissionJob = null
        _activeMode.value = null

        // El watchdog cortaria solo en un par de segundos, pero esperar a que
        // expire se siente como un mando que no responde.
        emitOff()
    }

    /**
     * Rafaga de apagado explicito. Al ser de duracion fija, el advertiser para
     * la radio al terminar en vez de quedarse al aire.
     */
    private suspend fun emitOff() {
        payloadBuilder.build(HapticMode.OFF_CODE, "Apagado").onSuccess { payload ->
            advertiser.emit(payload, durationMs = BroadcastAdvertiser.BURST_DURATION_MS)
        }
    }

    companion object {
        private const val TAG = "HapticRepository"

        /**
         * Los tres escalones que el audio puede alcanzar. Los ritmos quedan
         * fuera a proposito: tienen tempo propio y no responderian al volumen.
         */
        private val AUDIO_LEVELS = listOf(
            HapticMode.CONTINUA_BAJA,
            HapticMode.CONTINUA_MEDIA,
            HapticMode.CONTINUA_ALTA
        )
    }
}
