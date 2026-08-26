package com.smartbe.hapticcontrol.probe

import android.util.Log
import com.example.nirjon.bledemo4_advertising.util.BLEUtil

/**
 * Sonda diferencial sobre libble24.so
 *
 * En vez de adivinar el algoritmo, lo interrogamos: variamos una entrada a la vez
 * y observamos que bytes de salida cambian. Eso revela la estructura de la trama
 * (que bytes son cabecera fija, cuales dependen del comando, cuales son CRC) y si
 * hay whitening aplicado encima.
 *
 * Ejecutar UNA vez desde onCreate y pegar el logcat completo (tag "RF_PROBE").
 */
object RfPayloadProbe {

    private const val TAG = "RF_PROBE"

    /** Prefijo por defecto de la app original: ASCII "wbMSE" */
    private val DEFAULT_ADDR = byteArrayOf(0x77, 0x62, 0x4D, 0x53, 0x45)

    fun run() {
        Log.i(TAG, "===== INICIO SONDA =====")

        // 1. Comandos reales, prefijo por defecto.
        //    0x00 = apagar, 0x51..0x55 = modos, 0x11..0x19 y 0x20 = intensidades.
        Log.i(TAG, "--- 1. comandos reales ---")
        val comandosReales = intArrayOf(
            0x00,
            0x51, 0x52, 0x53, 0x54, 0x55,
            0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,
            0x20
        )
        for (cmd in comandosReales) {
            dump("cmd=${hex(cmd)}", DEFAULT_ADDR, byteArrayOf(cmd.toByte()))
        }

        // 2. Barrido de un solo bit. Si el algoritmo es lineal (CRC + XOR de whitening),
        //    cada bit del comando debe afectar siempre a los mismos bits de salida.
        Log.i(TAG, "--- 2. barrido de bit unico ---")
        for (bit in 0..7) {
            val v = (1 shl bit)
            dump("bit$bit=${hex(v)}", DEFAULT_ADDR, byteArrayOf(v.toByte()))
        }

        // 3. Variacion del prefijo. Confirma en que posicion se incrusta la direccion
        //    y si participa en el CRC.
        Log.i(TAG, "--- 3. variacion de prefijo ---")
        dump("addr ceros    ", byteArrayOf(0, 0, 0, 0, 0), byteArrayOf(0x51))
        dump("addr FFs      ", byteArrayOf(-1, -1, -1, -1, -1), byteArrayOf(0x51))
        dump("addr ult byte+1", byteArrayOf(0x77, 0x62, 0x4D, 0x53, 0x46), byteArrayOf(0x51))
        dump("addr 1er byte+1", byteArrayOf(0x78, 0x62, 0x4D, 0x53, 0x45), byteArrayOf(0x51))

        // 4. Determinismo. Si dos llamadas identicas difieren, hay estado interno
        //    (contador de secuencia o canal rotativo) y cambia toda la estrategia.
        Log.i(TAG, "--- 4. determinismo ---")
        repeat(3) { dump("repeticion $it", DEFAULT_ADDR, byteArrayOf(0x51)) }

        // 5. Longitudes distintas de comando, por si el protocolo admite payload multibyte.
        Log.i(TAG, "--- 5. payload multibyte ---")
        dumpVar("data 2 bytes", DEFAULT_ADDR, byteArrayOf(0x51, 0x00))
        dumpVar("data 3 bytes", DEFAULT_ADDR, byteArrayOf(0x51, 0x00, 0x00))

        Log.i(TAG, "===== FIN SONDA =====")
    }

    private fun dump(etiqueta: String, addr: ByteArray, data: ByteArray) {
        val out = ByteArray(addr.size + data.size + 5)
        try {
            BLEUtil.get_rf_payload(addr, addr.size, data, data.size, out)
            Log.i(TAG, "$etiqueta -> ${out.toHex()}")
        } catch (e: Throwable) {
            Log.e(TAG, "$etiqueta -> FALLO: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Igual que dump pero tolera que el nativo escriba mas de lo previsto. */
    private fun dumpVar(etiqueta: String, addr: ByteArray, data: ByteArray) {
        val esperado = addr.size + data.size + 5
        val out = ByteArray(esperado + 16) // margen por si desborda
        try {
            BLEUtil.get_rf_payload(addr, addr.size, data, data.size, out)
            Log.i(TAG, "$etiqueta (esperado $esperado) -> ${out.toHex()}")
        } catch (e: Throwable) {
            Log.e(TAG, "$etiqueta -> FALLO: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun hex(v: Int) = "0x%02X".format(v and 0xFF)

    private fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }
}
