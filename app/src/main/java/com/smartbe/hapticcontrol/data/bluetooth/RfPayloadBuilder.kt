package com.smartbe.hapticcontrol.data.bluetooth

import android.util.Log
import com.example.nirjon.bledemo4_advertising.util.BLEUtil
import com.smartbe.hapticcontrol.domain.model.HapticCommand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Construye la trama RF cruda que espera el receptor 2.4 GHz.
 *
 * Estructura deducida de la firma nativa get_rf_payload(addr, 5, data, 1, out[11]):
 *
 *   [cabecera 2] [direccion 5] [payload 1] [CRC-24 3]  =  11 bytes
 *
 * El receptor NO es un periferico BLE: es un chip clase nRF24L01 escuchando en
 * la direccion "wbMSE". Por eso la trama se arma a mano y despues se incrusta
 * como manufacturer data de un anuncio BLE real.
 *
 * El algoritmo vive en libble24.so. Aqui solo lo envolvemos.
 */
@Singleton
class RfPayloadBuilder @Inject constructor() {

    /** Resultado de comprobar que la libreria nativa carga. */
    val nativeAvailable: Boolean by lazy {
        runCatching {
            val probe = ByteArray(DEFAULT_PREFIX.size + 1 + EXTRA_BYTES)
            BLEUtil.get_rf_payload(DEFAULT_PREFIX, DEFAULT_PREFIX.size, byteArrayOf(0), 1, probe)
            true
        }.getOrElse { error ->
            Log.e(TAG, "libble24.so no disponible: ${error.javaClass.simpleName}", error)
            false
        }
    }

    /**
     * @param command  comando de dominio
     * @param prefix   direccion de escucha. Solo cambia si el modelo declara
     *                 IsPrivateProtocol=1 con un BroadcastPrefix propio.
     */
    fun build(
        command: HapticCommand,
        prefix: ByteArray = DEFAULT_PREFIX
    ): Result<ByteArray> {
        val data = byteArrayOf(command.code)
        val out = ByteArray(prefix.size + data.size + EXTRA_BYTES)

        return runCatching {
            BLEUtil.get_rf_payload(prefix, prefix.size, data, data.size, out)
            Log.d(TAG, "${command.label} -> ${out.toHex()}")
            out
        }.onFailure { error ->
            Log.e(TAG, "Fallo construyendo trama para ${command.label}", error)
        }
    }

    private fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }

    companion object {
        private const val TAG = "RfPayloadBuilder"

        /** ASCII "wbMSE". Prefijo por defecto de la app original. */
        val DEFAULT_PREFIX = byteArrayOf(0x77, 0x62, 0x4D, 0x53, 0x45)

        /** Cabecera (2) + CRC (3) que el nativo agrega por su cuenta. */
        const val EXTRA_BYTES = 5
    }
}
