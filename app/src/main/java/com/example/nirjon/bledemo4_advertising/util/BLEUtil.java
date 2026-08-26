package com.example.nirjon.bledemo4_advertising.util;

/**
 * Puente JNI hacia libble24.so
 *
 * NO RENOMBRAR NI MOVER DE PAQUETE.
 * El simbolo exportado por la libreria nativa es:
 *   Java_com_example_nirjon_bledemo4_1advertising_util_BLEUtil_get_1rf_1payload
 * que se deriva literalmente de <paquete>.<clase>.<metodo>.
 * Cualquier cambio provoca UnsatisfiedLinkError en tiempo de ejecucion.
 *
 * Colocar los .so en: app/src/main/jniLibs/<abi>/libble24.so
 */
public class BLEUtil {

    static {
        System.loadLibrary("ble24");
    }

    /**
     * Construye la trama RF cruda que el dispositivo 2.4 GHz espera recibir.
     *
     * @param addr    direccion/prefijo, normalmente "wbMSE" = {0x77,0x62,0x4D,0x53,0x45}
     * @param addrLen longitud de addr (5)
     * @param data    bytes de comando (1)
     * @param dataLen longitud de data (1)
     * @param out     buffer de salida, debe medir addrLen + dataLen + 5
     */
    public static native void get_rf_payload(
            byte[] addr, int addrLen,
            byte[] data, int dataLen,
            byte[] out
    );
}
