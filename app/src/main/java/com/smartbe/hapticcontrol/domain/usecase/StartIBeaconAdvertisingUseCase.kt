package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

class StartIBeaconAdvertisingUseCase @Inject constructor(
    private val repository: BleRepository
) {
    suspend operator fun invoke(uuid: String, major: Int, minor: Int) {
        repository.startIBeaconAdvertising(uuid, major, minor)
    }
}