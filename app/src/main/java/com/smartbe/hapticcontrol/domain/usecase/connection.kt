package com.smartbe.hapticcontrol.domain.usecase.connection

import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import com.smartbe.hapticcontrol.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para conectar a un dispositivo BLE
 * Maneja la conexión y actualiza el registro local del dispositivo
 */
class ConnectDeviceUseCase @Inject constructor(
    private val bleRepository: BleRepository,
    private val deviceRepository: DeviceRepository
) {
    /**
     * Conecta al dispositivo especificado y guarda/actualiza su información
     *
     * @param device Dispositivo al que conectarse
     * @return Result indicando éxito o fallo
     */
    suspend operator fun invoke(device: BleDevice): Result<Unit> {
        return bleRepository.connect(device).onSuccess {
            // Actualizar timestamp de última conexión
            val timestamp = System.currentTimeMillis()

            // Guardar o actualizar el dispositivo en la base de datos local
            val updatedDevice = device.copy(lastConnectionDate = timestamp)
            deviceRepository.saveDevice(updatedDevice)
            deviceRepository.updateLastConnection(device.macAddress, timestamp)
        }
    }
}

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

class ObserveConnectionStateUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Obtiene un Flow con el estado actual de la conexión
     *
     * @return Flow que emite ConnectionState
     */
    operator fun invoke(): Flow<ConnectionState> {
        return bleRepository.connectionState
    }
}

class AutoConnectUseCase @Inject constructor(
    private val bleRepository: BleRepository,
    private val deviceRepository: DeviceRepository
) {
    /**
     * Intenta conectar automáticamente al dispositivo marcado como favorito
     *
     * @return Result indicando si se conectó exitosamente
     */
    suspend operator fun invoke(): Result<Unit> {
        val favoriteDevice = deviceRepository.getFavoriteDevice()
            ?: return Result.failure(Exception("No favorite device found"))

        // Verificar que Bluetooth esté habilitado
        if (!bleRepository.isBluetoothEnabled()) {
            return Result.failure(Exception("Bluetooth is disabled"))
        }

        return bleRepository.connect(favoriteDevice).onSuccess {
            // Actualizar timestamp de última conexión
            val timestamp = System.currentTimeMillis()
            deviceRepository.updateLastConnection(favoriteDevice.macAddress, timestamp)
        }
    }
}