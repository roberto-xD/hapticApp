package com.smartbe.hapticcontrol.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona el broadcasting BLE (iBeacon advertising)
 * Equivalente a BeaconManager.java del código original
 */
@Singleton
class BleAdvertiserManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?
) {
    private val tag = "BleAdvertiserManager"

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var timer: Timer? = null

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _advertisingError = MutableStateFlow<String?>(null)
    val advertisingError: StateFlow<String?> = _advertisingError.asStateFlow()

    init {
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    }

    /**
     * Callback para eventos de advertising
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(tag, "Advertising started successfully")
            _isAdvertising.value = true
            _advertisingError.value = null
        }

        override fun onStartFailure(errorCode: Int) {
            val error = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error: $errorCode"
            }
            Log.e(tag, "Advertising failed: $error")
            _isAdvertising.value = false
            _advertisingError.value = error
        }
    }

    /**
     * Inicia advertising con datos específicos
     * @param advertisingData Datos a transmitir
     * @param durationMillis Duración del advertising (default: 5000ms)
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertising(advertisingData: ByteArray, durationMillis: Long = 5000) {
        if (bluetoothAdapter?.isEnabled != true) {
            _advertisingError.value = "Bluetooth is disabled"
            return
        }

        if (bluetoothLeAdvertiser == null) {
            _advertisingError.value = "Advertising not supported on this device"
            return
        }

        stopAdvertising()

        try {
            val settings = createAdvSettings(durationMillis)
            val data = createAdvData(advertisingData)

            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)

            // Timer para detener automáticamente
            timer = Timer().apply {
                schedule(object : TimerTask() {
                    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                    override fun run() {
                        stopAdvertising()
                    }
                }, durationMillis)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error starting advertising", e)
            _advertisingError.value = e.message
        }
    }

    /**
     * Inicia advertising con modo de baja latencia (1.5 segundos)
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startLowLatencyAdvertising(advertisingData: ByteArray) {
        startAdvertising(advertisingData, 1500)
    }

    /**
     * Detiene el advertising
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        try {
            timer?.cancel()
            timer = null

            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            _isAdvertising.value = false
            Log.d(tag, "Advertising stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping advertising", e)
        }
    }

    /**
     * Crea configuración de advertising
     */
    private fun createAdvSettings(timeoutMillis: Long): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(timeoutMillis.toInt())
            .build()
    }

    /**
     * Crea datos de advertising en formato iBeacon
     */
    private fun createAdvData(advertisingData: ByteArray): AdvertiseData {
        return AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(0x004C, advertisingData) // Apple Company ID para iBeacon
            .build()
    }

    /**
     * Crea datos de iBeacon con UUID, Major y Minor
     */
    fun createIBeaconData(uuid: String, major: Int, minor: Int): ByteArray {
        // Formato iBeacon:
        // 02 15 [UUID 16 bytes] [Major 2 bytes] [Minor 2 bytes] [TX Power 1 byte]
        val data = ByteArray(23)
        data[0] = 0x02 // iBeacon type
        data[1] = 0x15 // Length

        // UUID (16 bytes)
        val uuidBytes = uuid.replace("-", "").chunked(2).map { it.toInt(16).toByte() }
        uuidBytes.forEachIndexed { index, byte ->
            data[2 + index] = byte
        }

        // Major (2 bytes, big endian)
        data[18] = (major shr 8).toByte()
        data[19] = major.toByte()

        // Minor (2 bytes, big endian)
        data[20] = (minor shr 8).toByte()
        data[21] = minor.toByte()

        // TX Power (calibrated at 1m)
        data[22] = 0xC5.toByte() // -59 dBm

        return data
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun release() {
        stopAdvertising()
        bluetoothLeAdvertiser = null
    }
}