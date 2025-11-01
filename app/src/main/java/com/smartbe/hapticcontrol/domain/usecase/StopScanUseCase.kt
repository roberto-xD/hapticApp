package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

class StopScanUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Detiene el escaneo activo de dispositivos BLE
     */
    suspend operator fun invoke() {
        bleRepository.stopScan()
    }
}