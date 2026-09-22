package com.smartbe.hapticcontrol.data.repository

import android.util.Log
import com.smartbe.hapticcontrol.data.audio.AudioLevelSource
import com.smartbe.hapticcontrol.data.bluetooth.BroadcastAdvertiser
import com.smartbe.hapticcontrol.data.bluetooth.RfPayloadBuilder
import com.smartbe.hapticcontrol.domain.model.HapticMode
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticRepositoryImpl @Inject constructor(
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
     */
    override fun startAudioReactive() {
        emissionJob?.cancel()
        _isAudioReactive.value = true

        emissionJob = scope.launch {
            audioLevelSource.levels()
                .distinctUntilChanged()
                .collectLatest { nivel ->
                    val mode = AUDIO_LEVELS.getOrNull(nivel - 1)
                    _activeMode.value = mode

                    val code = mode?.code ?: HapticMode.OFF_CODE
                    val etiqueta = mode?.displayName ?: "Audio silencio"

                    payloadBuilder.build(code, etiqueta).onSuccess { payload ->
                        advertiser.emit(payload, durationMs = null)
                    }
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
