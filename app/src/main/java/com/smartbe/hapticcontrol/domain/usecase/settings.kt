package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener los dispositivos guardados
 */
class GetSavedDevicesUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    /**
     * Obtiene un Flow con todos los dispositivos guardados
     *
     * @return Flow que emite la lista de dispositivos
     */
    operator fun invoke(): Flow<List<BleDevice>> {
        return deviceRepository.savedDevices
    }

    /**
     * Obtiene la lista de dispositivos de forma suspendida
     *
     * @return Lista de dispositivos guardados
     */
    suspend fun getList(): List<BleDevice> {
        return deviceRepository.getAllDevices()
    }
}

class SetFavoriteDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    /**
     * Marca un dispositivo como favorito para reconexión automática
     * Desmarca cualquier dispositivo favorito anterior
     *
     * @param macAddress Dirección MAC del dispositivo a marcar como favorito
     */
    suspend operator fun invoke(macAddress: String) {
        deviceRepository.setFavoriteDevice(macAddress)
    }
}

class UpdateDeviceNicknameUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    /**
     * Actualiza el apodo personalizado de un dispositivo
     *
     * @param macAddress Dirección MAC del dispositivo
     * @param nickname Nuevo apodo (puede estar vacío)
     */
    suspend operator fun invoke(macAddress: String, nickname: String) {
        deviceRepository.updateNickname(macAddress, nickname)
    }
}

class DeleteDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    /**
     * Elimina un dispositivo de la lista de dispositivos guardados
     *
     * @param macAddress Dirección MAC del dispositivo a eliminar
     */
    suspend operator fun invoke(macAddress: String) {
        deviceRepository.deleteDevice(macAddress)
    }
}