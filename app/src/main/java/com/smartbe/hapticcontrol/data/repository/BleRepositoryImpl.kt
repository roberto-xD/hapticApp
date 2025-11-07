package com.smartbe.hapticcontrol.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.smartbe.hapticcontrol.data.bluetooth.BleAdvertiserManager
import com.smartbe.hapticcontrol.data.bluetooth.BleCommandUtil
import com.smartbe.hapticcontrol.data.local.dao.BleDeviceDao
import com.smartbe.hapticcontrol.data.local.entity.BleDeviceEntity
import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.model.VisitorCommands
import com.smartbe.hapticcontrol.domain.model.VisitorMode
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val deviceDao: BleDeviceDao,
    private val advertiserManager: BleAdvertiserManager
) : BleRepository {

    private val tag = "BleRepositoryImpl"
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        // UUIDs del servicio BLE
        val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val NOTIFY_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
        val WRITE_UUID: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Filtros de nombres de dispositivos
        val DEVICE_NAME_FILTERS = listOf("KGL", "WB", "LG-")

        // Configuración de timeouts (en milisegundos)
        const val SCAN_TIMEOUT = 10000L
        const val CONNECTION_TIMEOUT = 10000L
        const val MAX_RECONNECT_ATTEMPTS = 3
    }

    // Variables para gestión de GATT
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false
    private var reconnectAttempts = 0
    private var shouldReconnect = false
    private var lastConnectedAddress: String? = null

    // StateFlows para observar estados
    private val _scannedDevices = MutableStateFlow<List<BleDeviceModel>>(emptyList())
    override val scannedDevices: StateFlow<List<BleDeviceModel>> = _scannedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BleDeviceModel?>(null)
    override val connectedDevice: StateFlow<BleDeviceModel?> = _connectedDevice.asStateFlow()

    private val _receivedData = MutableStateFlow<ByteArray?>(null)
    override val receivedData: StateFlow<ByteArray?> = _receivedData.asStateFlow()

    // Estado de advertising
    override val isAdvertising: StateFlow<Boolean> = advertiserManager.isAdvertising
    override val advertisingError: StateFlow<String?> = advertiserManager.advertisingError

    // ========== MODO VISITANTE (BROADCASTING) ==========

    private val _visitorModeEnabled = MutableStateFlow(false)
    override val visitorModeEnabled: StateFlow<Boolean> = _visitorModeEnabled.asStateFlow()

    private val _isBroadcasting = MutableStateFlow(false)
    override val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val _visitorModeInfo = MutableStateFlow(VisitorMode())
    override val visitorModeInfo: StateFlow<VisitorMode> = _visitorModeInfo.asStateFlow()

    private var broadcastCount = 0

    // ========== SCAN CALLBACK ==========

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                Log.i("tag","on scan result: ${scanResult.device.name}")
                val device = scanResult.device
                val deviceName = device.name ?: "Unknown"

                // Filtrar por nombres permitidos
                val matchesFilter = DEVICE_NAME_FILTERS.any { filter ->
                    deviceName.contains(filter, ignoreCase = true)
                }

                if (matchesFilter) {
                    val bleDevice = BleDeviceModel(
                        name = deviceName,
                        macAddress = device.address,
                        rssi = scanResult.rssi
                    )

                    // Agregar o actualizar en la lista
                    val currentList = _scannedDevices.value.toMutableList()
                    val existingIndex = currentList.indexOfFirst { it.macAddress == bleDevice.macAddress }

                    if (existingIndex != -1) {
                        currentList[existingIndex] = bleDevice
                    } else {
                        currentList.add(bleDevice)
                    }

                    _scannedDevices.value = currentList
                    Log.d(tag, "Device found: $deviceName (${device.address}) RSSI: ${scanResult.rssi}")
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            val error = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error: $errorCode"
            }
            Log.e(tag, "Scan failed: $error")
            isScanning = false
        }
    }

    // ========== GATT CALLBACK ==========

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(tag, "Connected to GATT server")
                    _connectionState.value = ConnectionState.CONNECTED
                    reconnectAttempts = 0

                    // Descubrir servicios
                    scope.launch {
                        delay(600) // Delay recomendado antes de descubrir servicios
                        gatt?.discoverServices()
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(tag, "Disconnected from GATT server")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                    writeCharacteristic = null
                    notifyCharacteristic = null

                    // Intentar reconexión si está habilitada
                    if (shouldReconnect && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        reconnectAttempts++
                        Log.d(tag, "Attempting reconnection $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")

                        scope.launch {
                            delay(2000) // Esperar antes de reconectar
                            lastConnectedAddress?.let { address ->
                                val device = bluetoothAdapter?.getRemoteDevice(address)
                                device?.let {
                                    _connectionState.value = ConnectionState.CONNECTING
                                    bluetoothGatt = it.connectGatt(context, false,
                                        this as BluetoothGattCallback?
                                    )
                                }
                            }
                        }
                    } else {
                        shouldReconnect = false
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                    }
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d(tag, "Connecting to GATT server")
                    _connectionState.value = ConnectionState.CONNECTING
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Services discovered")

                // Buscar el servicio principal
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    // Obtener característica de escritura
                    writeCharacteristic = service.getCharacteristic(WRITE_UUID)
                    if (writeCharacteristic != null) {
                        Log.d(tag, "Write characteristic found")
                    } else {
                        Log.e(tag, "Write characteristic not found")
                    }

                    // Obtener y habilitar característica de notificación
                    notifyCharacteristic = service.getCharacteristic(NOTIFY_UUID)
                    if (notifyCharacteristic != null) {
                        Log.d(tag, "Notify characteristic found")
                        enableNotifications(gatt, notifyCharacteristic!!)
                    } else {
                        Log.e(tag, "Notify characteristic not found")
                    }
                } else {
                    Log.e(tag, "Service not found: $SERVICE_UUID")
                }
            } else {
                Log.e(tag, "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                _receivedData.value = data
                Log.d(tag, "Data received: ${BleCommandUtil.bytesToHexString(data)}")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Characteristic write successful")
            } else {
                Log.e(tag, "Characteristic write failed with status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.value?.let { data ->
                    Log.d(tag, "Characteristic read: ${BleCommandUtil.bytesToHexString(data)}")
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Descriptor write successful")
            } else {
                Log.e(tag, "Descriptor write failed with status: $status")
            }
        }
    }

    // ========== FUNCIONES DE ESCANEO ==========

    @SuppressLint("MissingPermission")
    override suspend fun startScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(tag, "Bluetooth is not enabled")
            return
        }

        if (isScanning) {
            Log.w(tag, "Scan already in progress")
            return
        }

        withContext(Dispatchers.Main) {
            _scannedDevices.value = emptyList()

            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                Log.e(tag, "BLE scanner not available")
                return@withContext
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            try {
                scanner.startScan(null, settings, scanCallback)
                isScanning = true
                Log.d(tag, "Scan started")

                // Auto-detener después del timeout
                scope.launch {
                    delay(SCAN_TIMEOUT)
                    if (isScanning) {
                        stopScan()
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error starting scan", e)
                isScanning = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() {
        if (!isScanning) return

        withContext(Dispatchers.Main) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                isScanning = false
                Log.d(tag, "Scan stopped")
            } catch (e: Exception) {
                Log.e(tag, "Error stopping scan", e)
            }
        }
    }

    // ========== FUNCIONES DE CONEXIÓN ==========

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: BleDeviceModel): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Detener escaneo si está activo
                if (isScanning) {
                    stopScan()
                }

                // Desconectar dispositivo anterior si existe
                if (bluetoothGatt != null) {
                    disconnect()
                    delay(500)
                }

                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.macAddress)
                    ?: return@withContext Result.failure(Exception("Device not found"))

                _connectionState.value = ConnectionState.CONNECTING
                lastConnectedAddress = device.macAddress
                shouldReconnect = true
                reconnectAttempts = 0

                bluetoothGatt = bluetoothDevice.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )

                _connectedDevice.value = device

                // Guardar dispositivo en la base de datos
                saveDevice(device)

                Log.d(tag, "Connecting to ${device.name} (${device.macAddress})")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Error connecting to device", e)
                _connectionState.value = ConnectionState.DISCONNECTED
                Result.failure(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            shouldReconnect = false
            reconnectAttempts = 0

            try {
                bluetoothGatt?.let { gatt ->
                    // Deshabilitar notificaciones antes de desconectar
                    notifyCharacteristic?.let { characteristic ->
                        gatt.setCharacteristicNotification(characteristic, false)

                        val descriptor = characteristic.getDescriptor(CCCD_UUID)
                        descriptor?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(it, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
                            } else {
                                @Suppress("DEPRECATION")
                                it.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                                @Suppress("DEPRECATION")
                                gatt.writeDescriptor(it)
                            }
                        }
                    }

                    delay(200)
                    gatt.disconnect()
                    delay(200)
                    gatt.close()
                }

                bluetoothGatt = null
                writeCharacteristic = null
                notifyCharacteristic = null
                _connectionState.value = ConnectionState.DISCONNECTED
                _connectedDevice.value = null
                lastConnectedAddress = null

                Log.d(tag, "Disconnected successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error during disconnect", e)
            }
        }
    }

    // ========== FUNCIONES DE NOTIFICACIONES ==========

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            // Habilitar notificaciones localmente
            val success = gatt.setCharacteristicNotification(characteristic, true)
            if (!success) {
                Log.e(tag, "Failed to set characteristic notification")
                return
            }

            // Escribir descriptor CCCD para habilitar notificaciones en el dispositivo remoto
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
                Log.d(tag, "Notifications enabled")
            } else {
                Log.e(tag, "CCCD descriptor not found")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error enabling notifications", e)
        }
    }

    // ========== FUNCIONES DE ENVÍO DE DATOS ==========

    @SuppressLint("MissingPermission")
    override suspend fun sendData(data: ByteArray): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (_connectionState.value != ConnectionState.CONNECTED) {
                    return@withContext Result.failure(Exception("Device not connected"))
                }

                val characteristic = writeCharacteristic
                    ?: return@withContext Result.failure(Exception("Write characteristic not found"))

                val gatt = bluetoothGatt
                    ?: return@withContext Result.failure(Exception("GATT connection not available"))

                // Escribir datos según la versión de Android
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        characteristic,
                        data,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = data
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(characteristic)
                }

                if (success) {
                    Log.d(tag, "Data sent successfully: ${BleCommandUtil.bytesToHexString(data)}")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to write characteristic"))
                }
            } catch (e: Exception) {
                Log.e(tag, "Error sending data", e)
                Result.failure(e)
            }
        }
    }

    // ========== FUNCIONES DE COMANDOS (NUEVAS) ==========

    override suspend fun sendCommand(commandType: Byte, data: ByteArray): Result<Unit> {
        val command = BleCommandUtil.getBleCommand(commandType, data)
        return sendData(command)
    }

    override suspend fun sendStopCommand(): Result<Unit> {
        val command = BleCommandUtil.createStopCommand()
        return sendData(command)
    }

    override suspend fun sendModeCommand(
        mode: Byte,
        frequency: Int,
        intensity: Int
    ): Result<Unit> {
        val command = BleCommandUtil.createModeCommand(mode, frequency, intensity)
        return sendData(command)
    }

    // ========== FUNCIONES DE ADVERTISING (NUEVAS) ==========

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override suspend fun startAdvertising(data: ByteArray, durationMillis: Long) {
        advertiserManager.startAdvertising(data, durationMillis)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override suspend fun startIBeaconAdvertising(uuid: String, major: Int, minor: Int) {
        val iBeaconData = advertiserManager.createIBeaconData(uuid, major, minor)
        advertiserManager.startAdvertising(iBeaconData)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override suspend fun stopAdvertising() {
        advertiserManager.stopAdvertising()
    }

    // ========== FUNCIONES DE PERSISTENCIA ==========

    override suspend fun saveDevice(device: BleDeviceModel) {
        withContext(Dispatchers.IO) {
            try {
                val entity = BleDeviceEntity(
                    macAddress = device.macAddress,
                    name = device.name,
                    lastConnectionDate = System.currentTimeMillis()
                )
                deviceDao.insertDevice(entity)
                Log.d(tag, "Device saved: ${device.name}")
            } catch (e: Exception) {
                Log.e(tag, "Error saving device", e)
            }
        }
    }

    override suspend fun getLastConnectedDevice(): BleDeviceModel? {
        return withContext(Dispatchers.IO) {
            try{
                deviceDao.getRecentDevices(1).map {  entity ->
                    BleDeviceModel(
                        name = entity.name,
                        macAddress = entity.macAddress,
                        rssi = 0
                    )
                }.firstOrNull()
            }catch (e: Exception){
                Log.e(tag, "Error getting last connected device", e)
                null
            }
        }
    }

    override suspend fun getSavedDevices(): List<BleDeviceModel> {
        return deviceDao.getAllDevices().map { entity ->
            BleDeviceModel(
                name = entity.name,
                macAddress = entity.macAddress,
                rssi = 0
            )
        }
    }

    override suspend fun deleteDevice(address: String) {
        withContext(Dispatchers.IO) {
            try {
                deviceDao.deleteDevice(address)
                Log.d(tag, "Device deleted: $address")
            } catch (e: Exception) {
                Log.e(tag, "Error deleting device", e)
            }
        }
    }

    // ========== RECONEXIÓN AUTOMÁTICA ==========

    suspend fun attemptAutoReconnect() {
        getLastConnectedDevice()?.let { device ->
            Log.d(tag, "Attempting auto-reconnect to ${device.name}")
            connect(device)
        }
    }

    // ========== IMPLEMENTACIÓN MODO VISITANTE (BROADCASTING) ==========
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    override suspend fun setVisitorMode(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            _visitorModeEnabled.value = enabled
            Log.d(tag, "Visitor mode (broadcasting) ${if (enabled) "ENABLED" else "DISABLED"}")

            if (!enabled) {
                // Detener broadcasting si está activo
                stopBroadcast()
                broadcastCount = 0
                _visitorModeInfo.value = VisitorMode()
            } else {
                _visitorModeInfo.value = VisitorMode(isEnabled = true)
            }
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    override suspend fun broadcastCommand(
        commandType: Byte,
        data: ByteArray,
        lowLatency: Boolean
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_visitorModeEnabled.value) {
                    return@withContext Result.failure(
                        Exception("Visitor mode not enabled")
                    )
                }

                Log.d(tag, "Broadcasting command: type=0x${commandType.toString(16)}, " +
                        "data=${BleCommandUtil.bytesToHexString(data)}, " +
                        "lowLatency=$lowLatency")

                // Crear comando BLE con formato completo
                val bleCommand = BleCommandUtil.getBleCommand(commandType, data)

                // Crear datos de advertising con el comando
                val advertisingData = createVisitorAdvertisingData(bleCommand)

                // Determinar duración según latencia
                val duration = if (lowLatency) 1500L else 5000L

                // Iniciar advertising
                _isBroadcasting.value = true
                advertiserManager.startAdvertising(advertisingData, duration)

                // Actualizar info
                broadcastCount++
                _visitorModeInfo.value = _visitorModeInfo.value.copy(
                    isBroadcasting = true,
                    broadcastingDuration = duration,
                    lastCommandSent = bleCommand,
                    broadcastCount = broadcastCount
                )

                Log.d(tag, "Broadcasting started (duration: ${duration}ms, count: $broadcastCount)")

                // Auto-detener después de la duración
                launch {
                    delay(duration)
                    _isBroadcasting.value = false
                    _visitorModeInfo.value = _visitorModeInfo.value.copy(
                        isBroadcasting = false
                    )
                    Log.d(tag, "Broadcasting finished")
                }

                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(tag, "Error broadcasting command", e)
                _isBroadcasting.value = false
                _visitorModeInfo.value = _visitorModeInfo.value.copy(
                    isBroadcasting = false
                )
                Result.failure(e)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override suspend fun broadcastModeCommand(
        mode: Byte,
        frequency: Int,
        intensity: Int,
        lowLatency: Boolean
    ): Result<Unit> {
        val data = byteArrayOf(
            frequency.coerceIn(0, 100).toByte(),
            intensity.coerceIn(0, 100).toByte()
        )
        return broadcastCommand(mode, data, lowLatency)
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    override suspend fun stopBroadcast() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Stopping broadcast...")
                advertiserManager.stopAdvertising()
                _isBroadcasting.value = false
                _visitorModeInfo.value = _visitorModeInfo.value.copy(
                    isBroadcasting = false
                )
            } catch (e: Exception) {
                Log.e(tag, "Error stopping broadcast", e)
            }
        }
    }

    /**
     * Crea datos de advertising para modo visitante
     * Formato compatible con los dispositivos físicos
     */
    private fun createVisitorAdvertisingData(command: ByteArray): ByteArray {
        // Formato: [Prefix (3 bytes)] + [Command data] + [Padding si necesario]
        val prefix = VisitorCommands.BROADCAST_TAG_PREFIX.toByteArray()

        // Tamaño total recomendado para advertising: ~20-23 bytes
        val totalSize = 23
        val commandSize = command.size.coerceAtMost(totalSize - prefix.size)

        val advertisingData = ByteArray(totalSize)

        // Copiar prefix
        prefix.copyInto(advertisingData, 0, 0, prefix.size)

        // Copiar comando
        command.copyInto(
            advertisingData,
            prefix.size,
            0,
            commandSize
        )

        // Padding con ceros si es necesario
        for (i in (prefix.size + commandSize) until totalSize) {
            advertisingData[i] = 0x00
        }

        Log.d(tag, "Advertising data created: ${BleCommandUtil.bytesToHexString(advertisingData)}")
        return advertisingData
    }

    /**
     * Modifica release() para detener broadcasting
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun release() {
        scope.launch {
            stopScan()
            disconnect()
            stopBroadcast() // NUEVO: Detener broadcasting
            advertiserManager.release()
        }
    }
}