package com.smartbe.hapticcontrol.data.repository

import com.smartbe.hapticcontrol.data.local.dao.DeviceDao
import com.smartbe.hapticcontrol.data.local.mapper.toDomain
import com.smartbe.hapticcontrol.data.local.mapper.toEntity
import com.smartbe.hapticcontrol.domain.model.BleDevice
import com.smartbe.hapticcontrol.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de dispositivos
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override val savedDevices: Flow<List<BleDevice>>
        get() = deviceDao.observeAllDevices().map { entities ->
            entities.toDomain()
        }

    override val favoriteDevice: Flow<BleDevice?>
        get() = deviceDao.observeFavoriteDevice().map { entity ->
            entity?.toDomain()
        }

    override suspend fun saveDevice(device: BleDevice) {
        deviceDao.insertDevice(device.toEntity())
    }

    override suspend fun getDeviceByMac(macAddress: String): BleDevice? {
        return deviceDao.getDeviceByMac(macAddress)?.toDomain()
    }

    override suspend fun getAllDevices(): List<BleDevice> {
        return deviceDao.getAllDevices().toDomain()
    }

    override suspend fun getFavoriteDevice(): BleDevice? {
        return deviceDao.getFavoriteDevice()?.toDomain()
    }

    override suspend fun setFavoriteDevice(macAddress: String) {
        // Primero desmarcar todos los favoritos
        deviceDao.clearAllFavorites()
        // Luego marcar el nuevo favorito
        deviceDao.setFavorite(macAddress)
    }

    override suspend fun updateNickname(macAddress: String, nickname: String) {
        deviceDao.updateNickname(macAddress, nickname)
    }

    override suspend fun updateLastConnection(macAddress: String, timestamp: Long) {
        deviceDao.updateLastConnection(macAddress, timestamp)
    }

    override suspend fun deleteDevice(macAddress: String) {
        deviceDao.deleteDevice(macAddress)
    }

    override suspend fun deleteAllDevices() {
        deviceDao.deleteAllDevices()
    }

    override suspend fun getRecentDevices(limit: Int): List<BleDevice> {
        return deviceDao.getRecentDevices(limit).toDomain()
    }
}