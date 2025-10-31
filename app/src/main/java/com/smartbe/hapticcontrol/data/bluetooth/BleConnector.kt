package com.smartbe.hapticcontrol.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.smartbe.hapticcontrol.domain.model.BleDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Gestiona la conexión y comunicación con dispositivos BLE
 */
class BleConnector @Inject constructor(
    private val context: Context
) {
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BleDevice? = null

    private val _connectionState = MutableStateFlow(BleConstants.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState

    companion object {
        private const val TAG = "BleConnector"
    }

    /**
     * Verifica si tiene permisos de conexión
     */
    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Conecta a un dispositivo BLE
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(device: BleDevice): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (!hasConnectPermission()) {
            continuation.resume(Result.failure(Exception("Missing BLUETOOTH_CONNECT permission")))
            return@suspendCancellableCoroutine
        }

        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.macAddress)
        if (bluetoothDevice == null) {
            continuation.resume(Result.failure(Exception("Device not found")))
            return@suspendCancellableCoroutine
        }

        _connectionState.value = BleConstants.STATE_CONNECTING

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Connected to GATT server")
                        _connectionState.value = BleConstants.STATE_CONNECTED
                        connectedDevice = device

                        // Descubrir servicios
                        gatt?.discoverServices()

                        if (continuation.context.isActive) {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Disconnected from GATT server")
                        _connectionState.value = BleConstants.STATE_DISCONNECTED
                        connectedDevice = null

                        if (continuation.context.isActive) {
                            continuation.resume(Result.failure(Exception("Connection failed")))
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered")
                } else {
                    Log.w(TAG, "Service discovery failed with status: $status")
                }
            }
        }

        try {
            bluetoothGatt = bluetoothDevice.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: Exception) {
            _connectionState.value = BleConstants.STATE_DISCONNECTED
            continuation.resume(Result.failure(e))
        }

        continuation.invokeOnCancellation {
            disconnect()
        }
    }

    /**
     * Desconecta del dispositivo actual
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            connectedDevice = null
            _connectionState.value = BleConstants.STATE_DISCONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    /**
     * Escribe datos en la característica de escritura
     */
    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(data: ByteArray): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (bluetoothGatt == null) {
            continuation.resume(Result.failure(Exception("Not connected")))
            return@suspendCancellableCoroutine
        }

        val service = bluetoothGatt?.getService(BleConstants.SERVICE_UUID)
        if (service == null) {
            continuation.resume(Result.failure(Exception("Service not found")))
            return@suspendCancellableCoroutine
        }

        val characteristic = service.getCharacteristic(BleConstants.WRITE_CHARACTERISTIC_UUID)
        if (characteristic == null) {
            continuation.resume(Result.failure(Exception("Write characteristic not found")))
            return@suspendCancellableCoroutine
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Characteristic written successfully")
                    if (continuation.context.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                } else {
                    Log.e(TAG, "Failed to write characteristic, status: $status")
                    if (continuation.context.isActive) {
                        continuation.resume(Result.failure(Exception("Write failed with status: $status")))
                    }
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                if (result == BluetoothGatt.GATT_SUCCESS) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception("Write operation failed")))
                }
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                @Suppress("DEPRECATION")
                val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false

                if (!success) {
                    continuation.resume(Result.failure(Exception("Write operation failed")))
                }
            }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Habilita notificaciones en la característica de notificación
     */
    @SuppressLint("MissingPermission")
    suspend fun enableNotifications(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (bluetoothGatt == null) {
            continuation.resume(Result.failure(Exception("Not connected")))
            return@suspendCancellableCoroutine
        }

        val service = bluetoothGatt?.getService(BleConstants.SERVICE_UUID)
        if (service == null) {
            continuation.resume(Result.failure(Exception("Service not found")))
            return@suspendCancellableCoroutine
        }

        val characteristic = service.getCharacteristic(BleConstants.NOTIFY_CHARACTERISTIC_UUID)
        if (characteristic == null) {
            continuation.resume(Result.failure(Exception("Notify characteristic not found")))
            return@suspendCancellableCoroutine
        }

        try {
            // Habilitar notificaciones localmente
            val success = bluetoothGatt?.setCharacteristicNotification(characteristic, true) ?: false

            if (!success) {
                continuation.resume(Result.failure(Exception("Failed to enable notifications locally")))
                return@suspendCancellableCoroutine
            }

            // Escribir descriptor para habilitar notificaciones en el dispositivo
            val descriptor = characteristic.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (descriptor == null) {
                continuation.resume(Result.failure(Exception("Descriptor not found")))
                return@suspendCancellableCoroutine
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeDescriptor(descriptor)
            }

            continuation.resume(Result.success(Unit))
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Observa las notificaciones del dispositivo
     */
    @SuppressLint("MissingPermission")
    fun observeNotifications(): Flow<ByteArray> = callbackFlow {
        if (bluetoothGatt == null) {
            close()
            return@callbackFlow
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (characteristic.uuid == BleConstants.NOTIFY_CHARACTERISTIC_UUID) {
                    trySend(value)
                }
            }

            @Deprecated("Deprecated for Android 13+")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                if (characteristic?.uuid == BleConstants.NOTIFY_CHARACTERISTIC_UUID) {
                    @Suppress("DEPRECATION")
                    characteristic.value?.let { trySend(it) }
                }
            }
        }

        awaitClose {
            // Cleanup si es necesario
        }
    }

    /**
     * Obtiene el dispositivo actualmente conectado
     */
    fun getConnectedDevice(): BleDevice? = connectedDevice

    /**
     * Verifica si está conectado
     */
    fun isConnected(): Boolean {
        return _connectionState.value == BleConstants.STATE_CONNECTED
    }
}