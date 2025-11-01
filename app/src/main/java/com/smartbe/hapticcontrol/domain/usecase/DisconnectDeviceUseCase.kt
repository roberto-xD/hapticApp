package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

class DisconnectDeviceUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Desconecta del dispositivo BLE actualmente conectado
     */
    suspend operator fun invoke() {
        bleRepository.disconnect()
    }
}