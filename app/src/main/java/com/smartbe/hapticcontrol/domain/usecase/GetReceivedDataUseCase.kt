package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetReceivedDataUseCase @Inject constructor(
    private val repository: BleRepository
) {
    operator fun invoke(): StateFlow<ByteArray?> {
        return repository.receivedData
    }
}