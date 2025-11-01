package com.smartbe.hapticcontrol.data.bluetooth

import android.util.Log

/**
 * Utilidades para comandos BLE
 * Equivalente a BleUtil.java del código original
 */
object BleCommandUtil {
    private const val TAG = "BleCommandUtil"

    // Tag de comando (equivalente a "77 62 4d 53 45" en el código original)
    private val COMMAND_TAG = byteArrayOf(0x77.toByte(), 0x62, 0x4d, 0x53, 0x45)

    /**
     * Genera comando BLE con formato específico
     * Formato: [TAG][tipo][data][checksum]
     *
     * @param commandType Tipo de comando
     * @param data Datos del comando
     * @return ByteArray con el comando completo
     */
    fun getBleCommand(commandType: Byte, data: ByteArray = byteArrayOf()): ByteArray {
        // Tamaño total: TAG + tipo + data + checksum
        val totalSize = COMMAND_TAG.size + 1 + data.size + 1
        val command = ByteArray(totalSize)

        var index = 0

        // 1. Agregar TAG
        COMMAND_TAG.forEach { command[index++] = it }

        // 2. Agregar tipo de comando
        command[index++] = commandType

        // 3. Agregar datos
        data.forEach { command[index++] = it }

        // 4. Calcular y agregar checksum (XOR de todos los bytes)
        command[index] = calculateChecksum(command, index)

        Log.d(TAG, "Command created: ${bytesToHexString(command)}")
        return command
    }

    /**
     * Crea comando de parada
     */
    fun createStopCommand(): ByteArray {
        return getBleCommand(0x00.toByte()) // 0x00 = comando de parada
    }

    /**
     * Crea comando de inicio con intensidad
     * @param intensity Intensidad (0-100)
     */
    fun createStartCommand(intensity: Int): ByteArray {
        val intensityByte = intensity.coerceIn(0, 100).toByte()
        return getBleCommand(0x01.toByte(), byteArrayOf(intensityByte))
    }

    /**
     * Crea comando con modo y parámetros
     * @param mode Modo de operación
     * @param frequency Frecuencia (0-100)
     * @param intensity Intensidad (0-100)
     */
    fun createModeCommand(mode: Byte, frequency: Int, intensity: Int): ByteArray {
        val freqByte = frequency.coerceIn(0, 100).toByte()
        val intensityByte = intensity.coerceIn(0, 100).toByte()
        return getBleCommand(mode, byteArrayOf(freqByte, intensityByte))
    }

    /**
     * Calcula checksum (XOR de todos los bytes)
     */
    private fun calculateChecksum(data: ByteArray, length: Int): Byte {
        var checksum: Byte = 0
        for (i in 0 until length) {
            checksum = (checksum.toInt() xor data[i].toInt()).toByte()
        }
        return checksum
    }

    /**
     * Convierte String hex a ByteArray
     * Ejemplo: "01 02 03" -> [0x01, 0x02, 0x03]
     */
    fun hexStringToBytes(hexString: String): ByteArray {
        val cleaned = hexString.replace(" ", "").replace("0x", "")
        return cleaned.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * Convierte ByteArray a String hex
     * Ejemplo: [0x01, 0x02, 0x03] -> "01 02 03"
     */
    fun bytesToHexString(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }

    /**
     * Convierte String a ByteArray
     */
    fun stringToBytes(str: String): ByteArray {
        return str.toByteArray(Charsets.UTF_8)
    }

    /**
     * Convierte Char a Byte
     */
    fun charToByte(char: Char): Byte {
        return char.code.toByte()
    }

    /**
     * Valida si un comando tiene el formato correcto
     */
    fun isValidCommand(command: ByteArray): Boolean {
        if (command.size < COMMAND_TAG.size + 2) return false

        // Verificar TAG
        for (i in COMMAND_TAG.indices) {
            if (command[i] != COMMAND_TAG[i]) return false
        }

        // Verificar checksum
        val checksumIndex = command.size - 1
        val calculatedChecksum = calculateChecksum(command, checksumIndex)

        return command[checksumIndex] == calculatedChecksum
    }

    /**
     * Crea datos de advertising para iBeacon
     */
    fun createIBeaconAdvertiseData(uuid: String, major: Int, minor: Int): ByteArray {
        val data = ByteArray(23)
        data[0] = 0x02 // iBeacon type
        data[1] = 0x15 // Length = 21 bytes

        // UUID (16 bytes)
        val uuidBytes = uuid.replace("-", "").chunked(2).map { it.toInt(16).toByte() }
        uuidBytes.forEachIndexed { index, byte ->
            if (index < 16) data[2 + index] = byte
        }

        // Major (2 bytes, big endian)
        data[18] = (major shr 8).toByte()
        data[19] = major.toByte()

        // Minor (2 bytes, big endian)
        data[20] = (minor shr 8).toByte()
        data[21] = minor.toByte()

        // TX Power
        data[22] = 0xC5.toByte()

        return data
    }
}