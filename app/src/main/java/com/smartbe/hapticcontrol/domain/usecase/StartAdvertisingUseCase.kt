package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

class StartAdvertisingUseCase @Inject constructor(
    private val repository: BleRepository
) {
    suspend operator fun invoke(data: ByteArray, durationMillis: Long = 5000) {
        repository.startAdvertising(data, durationMillis)
    }
}
