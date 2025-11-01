package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

class SendCommandUseCase @Inject constructor(
    private val repository: BleRepository
) {
    suspend operator fun invoke(
        commandType: Byte,
        data: ByteArray = byteArrayOf()
    ): Result<Unit> {
        return repository.sendCommand(commandType, data)
    }
}