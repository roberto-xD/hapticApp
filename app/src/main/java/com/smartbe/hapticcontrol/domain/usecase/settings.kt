package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener los dispositivos guardados
 */
class GetSavedDevicesUseCase @Inject constructor(
    private val deviceRepository: BleRepository
) {
    /**
     * Obtiene un Flow con todos los dispositivos guardados
     *
     * @return Flow que emite la lista de dispositivos
     */
    suspend operator fun invoke(): List<BleDeviceModel> {
        return deviceRepository.getSavedDevices()
    }
}

class SetFavoriteDeviceUseCase @Inject constructor(
    private val deviceRepository: BleRepository
) {
    /**
     * Marca un dispositivo como favorito para reconexión automática
     * Desmarca cualquier dispositivo favorito anterior
     *
     * @param macAddress Dirección MAC del dispositivo a marcar como favorito
     */
    suspend operator fun invoke(macAddress: String) {
//        deviceRepository.setFavoriteDevice(macAddress)
    }
}

class UpdateDeviceNicknameUseCase @Inject constructor(
    private val deviceRepository: BleRepository
) {
    /**
     * Actualiza el apodo personalizado de un dispositivo
     *
     * @param macAddress Dirección MAC del dispositivo
     * @param nickname Nuevo apodo (puede estar vacío)
     */
    suspend operator fun invoke(macAddress: String, nickname: String) {
//        deviceRepository.updateNickname(macAddress, nickname)
    }
}

class DeleteDeviceUseCase @Inject constructor(
    private val deviceRepository: BleRepository
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