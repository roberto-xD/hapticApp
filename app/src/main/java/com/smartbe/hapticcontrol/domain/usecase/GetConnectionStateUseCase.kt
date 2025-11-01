package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use Case para obtener el estado actual de la conexión BLE
 * Proporciona un StateFlow que emite cambios en el estado de conexión
 */
class GetConnectionStateUseCase @Inject constructor(
    private val repository: BleRepository
) {
    /**
     * Obtiene el StateFlow del estado de conexión
     *
     * @return StateFlow<ConnectionState> que emite:
     *         - DISCONNECTED: Sin conexión
     *         - CONNECTING: Intentando conectar
     *         - CONNECTED: Conectado exitosamente
     */
    operator fun invoke(): StateFlow<ConnectionState> {
        return repository.connectionState
    }
}