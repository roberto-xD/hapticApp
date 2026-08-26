package com.smartbe.hapticcontrol.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Error de arranque del anuncio, con el codigo crudo de AdvertiseCallback. */
class AdvertiseException(val errorCode: Int) : Exception(describe(errorCode)) {
    companion object {
        fun describe(code: Int): String = when (code) {
            AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
                "Los datos superan 31 bytes"
            AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                "No hay instancias de advertising libres"
            AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED ->
                "Ya habia un anuncio activo"
            AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR ->
                "Error interno del stack Bluetooth"
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                "Este telefono no soporta rol periferico"
            else -> "Error desconocido ($code)"
        }
    }
}

/**
 * Emite rafagas de advertising BLE.
 *
 * Reemplaza el Timer + AdvertiseCallback sueltos del codigo original por una
 * suspend function: el callback se envuelve en suspendCancellableCoroutine, asi
 * que un fallo de arranque se propaga como excepcion en vez de perderse en un
 * log, y cancelar la corrutina detiene el anuncio de verdad.
 *
 * El Mutex serializa las rafagas. Sin el, dos toques rapidos en la UI
 * provocarian ADVERTISE_FAILED_ALREADY_STARTED, que es exactamente el motivo
 * de que la app original hiciera stop antes de cada start.
 */
@Singleton
class BroadcastAdvertiser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mutex = Mutex()
    private var activeCallback: AdvertiseCallback? = null

    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val advertiser: BluetoothLeAdvertiser?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter
            ?.takeIf { it.isEnabled }
            ?.bluetoothLeAdvertiser

    /**
     * Emite [payload] durante [durationMs] y despues corta.
     *
     * El dispositivo memoriza el estado, asi que una rafaga corta basta: no hay
     * que mantener el anuncio mientras dure la vibracion.
     */
    @SuppressLint("MissingPermission")
    suspend fun broadcast(
        payload: ByteArray,
        durationMs: Long = BURST_DURATION_MS
    ): Result<Unit> = mutex.withLock {
        val adv = advertiser
            ?: return@withLock Result.failure(
                IllegalStateException("Bluetooth apagado o sin soporte de advertising")
            )

        stopInternal(adv)

        try {
            awaitStart(adv, payload)
            _isBroadcasting.value = true
            delay(durationMs)
            Result.success(Unit)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Throwable) {
            Log.e(TAG, "Fallo al emitir", error)
            Result.failure(error)
        } finally {
            // NonCancellable: si cancelan la corrutina, el anuncio TIENE que
            // pararse igual. Dejarlo al aire bloquea la siguiente rafaga.
            withContext(NonCancellable) {
                stopInternal(adv)
                _isBroadcasting.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitStart(
        adv: BluetoothLeAdvertiser,
        payload: ByteArray
    ): Unit = suspendCancellableCoroutine { cont ->

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onStartFailure(errorCode: Int) {
                if (cont.isActive) cont.resumeWithException(AdvertiseException(errorCode))
            }
        }
        activeCallback = callback

        cont.invokeOnCancellation {
            runCatching { adv.stopAdvertising(callback) }
        }

        runCatching {
            adv.startAdvertising(buildSettings(), buildData(payload), callback)
        }.onFailure { error ->
            if (cont.isActive) cont.resumeWithException(error)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopInternal(adv: BluetoothLeAdvertiser) {
        activeCallback?.let { callback ->
            runCatching { adv.stopAdvertising(callback) }
            activeCallback = null
        }
    }

    /**
     * Equivale a createAdvSettings2() de la app original.
     *
     * setConnectable(true) parece absurdo en un anuncio que nadie va a conectar,
     * pero cambia el tipo de PDU a ADV_IND y con ello la longitud de trama que
     * el receptor espera. No tocarlo sin validar contra hardware real.
     */
    private fun buildSettings(): AdvertiseSettings =
        AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(ADVERTISE_TIMEOUT_MS)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

    /**
     * Presupuesto de los 31 bytes del anuncio:
     *   Flags (automatico, por ser conectable) ... 3
     *   ServiceUuid ae8f (Android lo comprime a 16 bits) ... 4
     *   ManufacturerData: 2 id + 11 payload + 2 cabecera ... 15
     *   Total ... 22
     */
    private fun buildData(payload: ByteArray): AdvertiseData =
        AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addManufacturerData(MANUFACTURER_ID, payload)
            .build()

    companion object {
        private const val TAG = "BroadcastAdvertiser"

        /** 0x00FF, el que usa la app original. */
        const val MANUFACTURER_ID = 255

        val SERVICE_UUID: UUID = UUID.fromString("0000ae8f-0000-1000-8000-00805f9b34fb")

        /** Cuanto mantenemos la rafaga. El original usa 1000 ms. */
        const val BURST_DURATION_MS = 1000L

        /** Corte de seguridad del propio stack, por si la corrutina muere. */
        const val ADVERTISE_TIMEOUT_MS = 3000
    }
}
