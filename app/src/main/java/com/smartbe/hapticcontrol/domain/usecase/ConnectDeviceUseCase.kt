package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

/**
 * Use Case para conectar a un dispositivo BLE
 * Encapsula la lógica de conexión desde el dominio
 */
class ConnectDeviceUseCase @Inject constructor(
    private val repository: BleRepository
) {
    /**
     * Conecta a un dispositivo BLE específico
     *
     * @param device Dispositivo al que se desea conectar
     * @return Result<Unit> con el resultado de la operación
     *         - Success si la conexión se inició correctamente
     *         - Failure si hubo un error
     *
     * @throws SecurityException si no hay permisos de Bluetooth
     * @throws IllegalStateException si el adaptador BLE no está disponible
     */
    suspend operator fun invoke(device: BleDeviceModel): Result<Unit> {
        return try {
            // Validar que el dispositivo tenga datos válidos
            if (device.macAddress.isBlank()) {
                return Result.failure(IllegalArgumentException("Device address cannot be empty"))
            }

            if (device.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Device name cannot be empty"))
            }

            // Delegar la conexión al repositorio
            repository.connect(device)
        } catch (e: SecurityException) {
            // Error de permisos
            Result.failure(SecurityException("Bluetooth permissions not granted: ${e.message}"))
        } catch (e: IllegalStateException) {
            // Error de estado del adaptador
            Result.failure(IllegalStateException("Bluetooth adapter error: ${e.message}"))
        } catch (e: Exception) {
            // Cualquier otro error
            Result.failure(Exception("Failed to connect to device: ${e.message}"))
        }
    }
}