package com.smartbe.hapticcontrol.probe

import android.util.Log
import com.example.nirjon.bledemo4_advertising.util.BLEUtil

/**
 * Recupera el comando en claro a partir de una trama capturada del aire.
 *
 * get_rf_payload aplica whitening, asi que el byte de comando no es legible en
 * la captura. Pero la funcion es determinista y el espacio de busqueda de un
 * comando de un byte es de 256 posibilidades: basta generar las 256 tramas y
 * ver cual coincide.
 *
 * Uso: pegar las tramas capturadas en CAPTURAS y llamar a run().
 */
object CommandRecovery {

    private const val TAG = "CMD_RECOVERY"

    private val PREFIX = byteArrayOf(0x77, 0x62, 0x4D, 0x53, 0x45)

    /**
     * Tramas del snoop log: 11 bytes de manufacturer data por modo.
     * El nombre es el del modo en la app original.
     */
    private val CAPTURAS: List<Pair<String, String>> = listOf(
        // "Continua baja"  to "6D B6 43 CE 97 FE 42 7C B4 19 3E",
        // "Pulso lento"    to "...",
    )

    fun run() {
        if (CAPTURAS.isEmpty()) {
            Log.w(TAG, "No hay capturas. Pega las tramas en CAPTURAS primero.")
            return
        }

        Log.i(TAG, "===== RECUPERACION DE COMANDOS =====")
        for ((nombre, hex) in CAPTURAS) {
            val objetivo = hex.parseHex()
            when (objetivo.size) {
                11 -> buscarUnByte(nombre, objetivo)
                15 -> buscarCincoBytes(nombre, objetivo)
                else -> Log.w(TAG, "$nombre: longitud inesperada ${objetivo.size}")
            }
        }
        Log.i(TAG, "===== FIN =====")
    }

    /** Trama de 11 bytes => payload de 1 byte. 256 candidatos. */
    private fun buscarUnByte(nombre: String, objetivo: ByteArray) {
        for (valor in 0..0xFF) {
            val candidato = construir(byteArrayOf(valor.toByte()))
            if (candidato != null && candidato.contentEquals(objetivo)) {
                Log.i(TAG, "$nombre -> comando 0x%02X".format(valor))
                return
            }
        }
        Log.w(TAG, "$nombre -> sin coincidencia de 1 byte. ¿Prefijo distinto?")
    }

    /**
     * Trama de 15 bytes => payload de 5 bytes, familia 0xAA.
     * Fijamos el 0xAA y barremos los cuatro restantes por capas: primero el
     * subtipo, y solo dentro del subtipo que encaje, los tres parametros.
     */
    private fun buscarCincoBytes(nombre: String, objetivo: ByteArray) {
        for (subtipo in 0..0xFF) {
            for (a in 0..0xFF) {
                for (b in 0..0xFF) {
                    for (c in 0..0xFF) {
                        val data = byteArrayOf(
                            0xAA.toByte(), subtipo.toByte(),
                            a.toByte(), b.toByte(), c.toByte()
                        )
                        val candidato = construir(data)
                        if (candidato != null && candidato.contentEquals(objetivo)) {
                            Log.i(
                                TAG,
                                "$nombre -> AA %02X %02X %02X %02X".format(subtipo, a, b, c)
                            )
                            return
                        }
                    }
                }
            }
        }
        Log.w(TAG, "$nombre -> sin coincidencia de 5 bytes")
    }

    private fun construir(data: ByteArray): ByteArray? {
        val out = ByteArray(PREFIX.size + data.size + 5)
        return runCatching {
            BLEUtil.get_rf_payload(PREFIX, PREFIX.size, data, data.size, out)
            out
        }.getOrNull()
    }

    private fun String.parseHex(): ByteArray =
        trim().split(Regex("[\\s:,]+"))
            .filter { it.isNotEmpty() }
            .map { it.toInt(16).toByte() }
            .toByteArray()
}
