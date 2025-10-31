package com.smartbe.hapticcontrol.data.repository

import com.smartbe.hapticcontrol.data.bluetooth.BleManagerImpl
import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.model.ConnectionState
import com.smartbe.hapticcontrol.domain.model.DeviceCommand
import com.smartbe.hapticcontrol.domain.model.ScanResult
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio BLE
 */
@Singleton
class BleRepositoryImpl @Inject constructor(
    private val bleManager: BleManagerImpl
) : BleRepository {

    override val connectionState: Flow<ConnectionState>
        get() = bleManager.connectionState

    override val scanResults: Flow<ScanResult>
        get() = bleManager.scanResults

    override val notifications: Flow<ByteArray>
        get() = bleManager.notifications

    override suspend fun startScan(timeoutMillis: Long) {
        bleManager.startScan(timeoutMillis)
    }

    override suspend fun stopScan() {
        bleManager.stopScan()
    }

    override suspend fun connect(device: BleDevice): Result<Unit> {
        return bleManager.connect(device)
    }

    override suspend fun disconnect() {
        bleManager.disconnect()
    }

    override suspend fun sendCommand(command: DeviceCommand): Result<Unit> {
        return bleManager.sendCommand(command)
    }

    override suspend fun enableNotifications(): Result<Unit> {
        return bleManager.enableNotifications()
    }

    override suspend fun disableNotifications() {
        // Por ahora no implementado, se maneja con disconnect
    }

    override suspend fun getConnectedDevice(): BleDevice? {
        return bleManager.getConnectedDevice()
    }

    override suspend fun isBluetoothEnabled(): Boolean {
        return bleManager.isBluetoothEnabled()
    }

    override suspend fun isBleSupported(): Boolean {
        return bleManager.isBleSupported()
    }
}