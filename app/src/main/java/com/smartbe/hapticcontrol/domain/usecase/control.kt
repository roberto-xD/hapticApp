package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.DeviceCommand
import com.smartbe.hapticcontrol.domain.model.DeviceMode
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

/**
 * Caso de uso para enviar comandos al dispositivo conectado
 */
class SendCommandUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Envía un comando al dispositivo BLE conectado
     *
     * @param command Comando a enviar
     * @return Result indicando éxito o fallo
     */
    suspend operator fun invoke(command: DeviceCommand): Result<Unit> {
        return bleRepository.sendCommand(command)
    }
}

class SetIntensityUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Cambia la intensidad del dispositivo conectado
     *
     * @param level Nivel de intensidad (1-10)
     * @return Result indicando éxito o fallo
     */
    suspend operator fun invoke(level: Int): Result<Unit> {
        require(level in 1..10) { "Intensity level must be between 1 and 10" }

        val command = DeviceCommand.SetIntensity(level)
        return bleRepository.sendCommand(command)
    }
}

class SetFrequencyUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Cambia la frecuencia del dispositivo conectado
     *
     * @param frequency Frecuencia a establecer (LOW, MEDIUM, HIGH)
     * @return Result indicando éxito o fallo
     */
    suspend operator fun invoke(frequency: DeviceMode.Frequency): Result<Unit> {
        val command = DeviceCommand.SetFrequency(frequency)
        return bleRepository.sendCommand(command)
    }
}

class SetPatternUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Cambia el patrón de vibración del dispositivo conectado
     *
     * @param pattern Patrón a establecer (CONTINUOUS, WAVE, PULSE)
     * @return Result indicando éxito o fallo
     */
    suspend operator fun invoke(pattern: DeviceMode.Pattern): Result<Unit> {
        val command = DeviceCommand.SetPattern(pattern)
        return bleRepository.sendCommand(command)
    }
}

class SetPowerStateUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Enciende o apaga el dispositivo conectado
     *
     * @param isOn true para encender, false para apagar
     * @return Result indicando éxito o fallo
     */
    suspend operator fun invoke(isOn: Boolean): Result<Unit> {
        val command = if (isOn) {
            DeviceCommand.PowerOn
        } else {
            DeviceCommand.PowerOff
        }

        return bleRepository.sendCommand(command)
    }
}

