package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

/**
 * Use Case para transmitir comandos en modo visitante (broadcasting)
 * Envía comandos BLE a dispositivos cercanos sin conexión directa
 */
class BroadcastCommandUseCase @Inject constructor(
    private val repository: BleRepository
) {
    /**
     * Transmite un comando vía BLE advertising
     *
     * @param commandType Tipo de comando
     * @param data Datos adicionales
     * @param lowLatency Si true usa broadcasting rápido (1.5s), si false normal (5s)
     */
    suspend operator fun invoke(
        commandType: Byte,
        data: ByteArray = byteArrayOf(),
        lowLatency: Boolean = false
    ): Result<Unit> {
        return repository.broadcastCommand(commandType, data, lowLatency)
    }
}