package com.smartbe.hapticcontrol.data.repository

import android.util.Log
import com.smartbe.hapticcontrol.data.bluetooth.BroadcastAdvertiser
import com.smartbe.hapticcontrol.data.bluetooth.RfPayloadBuilder
import com.smartbe.hapticcontrol.domain.model.HapticCommand
import com.smartbe.hapticcontrol.domain.model.VibrationProgram
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticRepositoryImpl @Inject constructor(
    private val payloadBuilder: RfPayloadBuilder,
    private val advertiser: BroadcastAdvertiser
) : HapticRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var programJob: Job? = null

    override val isBroadcasting: StateFlow<Boolean> = advertiser.isBroadcasting

    private val _activeProgram = MutableStateFlow<VibrationProgram?>(null)
    override val activeProgram: StateFlow<VibrationProgram?> = _activeProgram.asStateFlow()

    override fun run(program: VibrationProgram) {
        programJob?.cancel()
        _activeProgram.value = program

        programJob = scope.launch {
            when (program) {
                is VibrationProgram.Steady -> runSteady(program)
                is VibrationProgram.Rhythm -> runRhythm(program)
            }
        }
    }

    /**
     * Un unico anuncio sostenido. No hay que reemitir: mientras el anuncio
     * siga al aire el receptor lo esta viendo en cada evento de advertising.
     */
    private suspend fun runSteady(program: VibrationProgram.Steady) {
        val payload = payloadBuilder
            .build(HapticCommand.Intensity(program.level))
            .getOrElse { error ->
                Log.e(TAG, "No se pudo construir la trama", error)
                return
            }
        advertiser.emit(payload, durationMs = null)
    }

    /**
     * Ciclo de pasos. Cada cambio de nivel obliga a parar y rearrancar el
     * anuncio porque AdvertiseData es inmutable una vez emitido.
     *
     * El coste de ese stop/start es de decenas de milisegundos, asi que los
     * pasos por debajo de ~100 ms no van a sonar limpios.
     */
    private suspend fun runRhythm(program: VibrationProgram.Rhythm) {
        while (currentCoroutineContext().isActive) {
            for (step in program.steps) {
                if (!currentCoroutineContext().isActive) return

                val command =
                    if (step.level == 0) HapticCommand.Off
                    else HapticCommand.Intensity(step.level)

                val payload = payloadBuilder.build(command).getOrElse { error ->
                    Log.e(TAG, "No se pudo construir la trama del paso", error)
                    return
                }

                advertiser.emit(payload, durationMs = step.holdMs)
            }
        }
    }

    override suspend fun stop() {
        programJob?.cancel()
        programJob = null
        _activeProgram.value = null

        // Apagado explicito. El watchdog cortaria solo en un par de segundos,
        // pero esperar a que expire se siente como un mando que no responde.
        sendOnce(HapticCommand.Off)
    }

    override suspend fun sendOnce(command: HapticCommand): Result<Unit> {
        val payload = payloadBuilder.build(command).getOrElse { error ->
            return Result.failure(error)
        }
        return advertiser.emit(payload, durationMs = BroadcastAdvertiser.BURST_DURATION_MS)
    }

    companion object {
        private const val TAG = "HapticRepository"
    }
}
