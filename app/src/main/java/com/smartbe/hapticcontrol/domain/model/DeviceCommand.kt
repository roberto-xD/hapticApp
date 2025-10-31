package com.smartbe.hapticcontrol.domain.model

/**
 * Representa los comandos que se pueden enviar al dispositivo BLE
 * Los comandos siguen el formato hex: 55AA + tipo + datos + checksum
 */
sealed class DeviceCommand(val hexCommand: ByteArray) {

    /**
     * Comando para encender el dispositivo
     * Formato: 55AA0301010105
     */
    object PowerOn : DeviceCommand(
        byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x01, 0x01, 0x01, 0x05)
    )

    /**
     * Comando para apagar el dispositivo
     * Formato: 55AA0301010006
     */
    object PowerOff : DeviceCommand(
        byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x01, 0x01, 0x00, 0x06)
    )

    /**
     * Comando para cambiar la intensidad
     * Formato: 55AA03020105XX (donde XX es el nivel 01-0A)
     */
    data class SetIntensity(val level: Int) : DeviceCommand(
        buildIntensityCommand(level)
    ) {
        init {
            require(level in 1..10) { "Intensity level must be between 1 and 10" }
        }

        companion object {
            private fun buildIntensityCommand(level: Int): ByteArray {
                val checksum = (0x55 + 0xAA + 0x03 + 0x02 + 0x01 + 0x05 + level) and 0xFF
                return byteArrayOf(
                    0x55.toByte(),
                    0xAA.toByte(),
                    0x03,
                    0x02,
                    0x01,
                    0x05,
                    level.toByte(),
                    checksum.toByte()
                )
            }
        }
    }

    /**
     * Comando para cambiar la frecuencia
     * Formato: 55AA0303010YY (donde YY es el valor de frecuencia)
     */
    data class SetFrequency(val frequency: DeviceMode.Frequency) : DeviceCommand(
        buildFrequencyCommand(frequency.commandValue)
    ) {
        companion object {
            private fun buildFrequencyCommand(value: Int): ByteArray {
                val checksum = (0x55 + 0xAA + 0x03 + 0x03 + 0x01 + 0x05 + value) and 0xFF
                return byteArrayOf(
                    0x55.toByte(),
                    0xAA.toByte(),
                    0x03,
                    0x03,
                    0x01,
                    0x05,
                    value.toByte(),
                    checksum.toByte()
                )
            }
        }
    }

    /**
     * Comando para cambiar el patrón de vibración
     * Formato: 55AA0304010ZZ (donde ZZ es el ID del patrón)
     */
    data class SetPattern(val pattern: DeviceMode.Pattern) : DeviceCommand(
        buildPatternCommand(pattern.patternId)
    ) {
        companion object {
            private fun buildPatternCommand(patternId: Int): ByteArray {
                val checksum = (0x55 + 0xAA + 0x03 + 0x04 + 0x01 + 0x05 + patternId) and 0xFF
                return byteArrayOf(
                    0x55.toByte(),
                    0xAA.toByte(),
                    0x03,
                    0x04,
                    0x01,
                    0x05,
                    patternId.toByte(),
                    checksum.toByte()
                )
            }
        }
    }

    /**
     * Convierte el comando a string hexadecimal para logging
     */
    fun toHexString(): String {
        return hexCommand.joinToString("") { byte ->
            "%02X".format(byte)
        }
    }

    companion object {
        /**
         * Header común de todos los comandos
         */
        const val COMMAND_HEADER = 0x55AA
    }
}