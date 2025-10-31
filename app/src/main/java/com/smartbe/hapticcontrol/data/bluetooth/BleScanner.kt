package com.smartbe.hapticcontrol.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.smartbe.hapticcontrol.domain.model.BleDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Gestiona el escaneo de dispositivos BLE
 */
class BleScanner @Inject constructor(
    private val context: Context
) {
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private var isScanning = false
    private val scannedDevices = mutableMapOf<String, BleDevice>()

    /**
     * Verifica si el dispositivo soporta BLE
     */
    fun isBleSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    /**
     * Verifica si Bluetooth está habilitado
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Verifica si los permisos necesarios están otorgados
     */
    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Inicia el escaneo de dispositivos BLE
     */
    @SuppressLint("MissingPermission")
    fun startScan(): Flow<Result<BleDevice>> = callbackFlow {
        if (!isBleSupported()) {
            trySend(Result.failure(Exception("BLE not supported")))
            close()
            return@callbackFlow
        }

        if (!isBluetoothEnabled()) {
            trySend(Result.failure(Exception("Bluetooth is disabled")))
            close()
            return@callbackFlow
        }

        if (!hasRequiredPermissions()) {
            trySend(Result.failure(Exception("Missing Bluetooth permissions")))
            close()
            return@callbackFlow
        }

        scannedDevices.clear()
        isScanning = true

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                handleScanResult(result)?.let { device ->
                    trySend(Result.success(device))
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                super.onBatchScanResults(results)
                results.forEach { result ->
                    handleScanResult(result)?.let { device ->
                        trySend(Result.success(device))
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                isScanning = false
                trySend(Result.failure(Exception("Scan failed with error code: $errorCode")))
                close()
            }
        }

        try {
            bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
        } catch (e: Exception) {
            isScanning = false
            trySend(Result.failure(e))
            close()
        }

        awaitClose {
            try {
                if (isScanning) {
                    bluetoothLeScanner?.stopScan(scanCallback)
                    isScanning = false
                }
            } catch (e: Exception) {
                // Ignorar errores al detener
            }
        }
    }

    /**
     * Detiene el escaneo actual
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            try {
                bluetoothLeScanner?.stopScan(object : ScanCallback() {})
                isScanning = false
            } catch (e: Exception) {
                // Ignorar errores
            }
        }
    }

    /**
     * Procesa un resultado de escaneo y lo convierte a BleDevice
     */
    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult): BleDevice? {
        val device = result.device
        val macAddress = device.address
        val name = device.name ?: return null

        // Filtrar dispositivos no válidos
        if (!BleConstants.isValidDeviceName(name)) {
            return null
        }

        // Evitar duplicados
        if (scannedDevices.containsKey(macAddress)) {
            return null
        }

        // Limitar número de dispositivos escaneados
        if (scannedDevices.size >= BleConstants.MAX_SCAN_DEVICES) {
            return null
        }

        val bleDevice = BleDevice(
            macAddress = macAddress,
            name = name,
            rssi = result.rssi
        )

        scannedDevices[macAddress] = bleDevice
        return bleDevice
    }

    /**
     * Limpia la lista de dispositivos escaneados
     */
    fun clearScannedDevices() {
        scannedDevices.clear()
    }

    /**
     * Indica si actualmente está escaneando
     */
    fun isCurrentlyScanning(): Boolean = isScanning
}