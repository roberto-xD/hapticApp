package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

/**
 * Use Case para obtener el último dispositivo conectado
 * Útil para reconexión automática
 */
class GetLastConnectedDeviceUseCase @Inject constructor(
    private val repository: BleRepository
) {
    /**
     * Obtiene el último dispositivo al que se conectó la aplicación
     *
     * @return BleDeviceModel si existe un dispositivo guardado, null si no
     */
    suspend operator fun invoke(): BleDeviceModel? {
        return try {
            repository.getLastConnectedDevice()
        } catch (e: Exception) {
            // Si hay error, retornar null
            null
        }
    }
}