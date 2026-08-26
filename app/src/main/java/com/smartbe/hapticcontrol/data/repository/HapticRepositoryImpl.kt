package com.smartbe.hapticcontrol.data.repository

import android.util.Log
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticRepositoryImpl @Inject constructor(
    private val payloadBuilder: RfPayloadBuilder,
    private val advertiser: BroadcastAdvertiser
) : HapticRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var emissionJob: Job? = null

    override val isBroadcasting: StateFlow<Boolean> = advertiser.isBroadcasting

    private val _activeMode = MutableStateFlow<HapticMode?>(null)
    override val activeMode: StateFlow<HapticMode?> = _activeMode.asStateFlow()

    override fun select(mode: HapticMode) {
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

    override suspend fun stop() {
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
    }
}
