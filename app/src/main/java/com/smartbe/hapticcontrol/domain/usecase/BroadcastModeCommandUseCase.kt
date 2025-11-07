package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

/**
 * Use Case para transmitir comandos de modo en modo visitante
 */
class BroadcastModeCommandUseCase @Inject constructor(
    private val repository: BleRepository
) {
    /**
     * Transmite comando de modo con frecuencia e intensidad
     */
    suspend operator fun invoke(
        mode: Byte,
        frequency: Int,
        intensity: Int,
        lowLatency: Boolean = false
    ): Result<Unit> {
        return repository.broadcastModeCommand(mode, frequency, intensity, lowLatency)
    }
}