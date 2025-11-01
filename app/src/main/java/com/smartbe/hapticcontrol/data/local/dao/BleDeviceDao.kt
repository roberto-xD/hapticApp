package com.smartbe.hapticcontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartbe.hapticcontrol.data.local.entity.BleDeviceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos de dispositivos BLE
 */
@Dao
interface BleDeviceDao {

    /**
     * Obtiene todos los dispositivos como Flow
     */
    @Query("SELECT * FROM devices ORDER BY last_connection_date DESC")
    fun observeAllDevices(): Flow<List<BleDeviceEntity>>

    /**
     * Obtiene el dispositivo favorito como Flow
     */
    @Query("SELECT * FROM devices WHERE is_favorite = 1 LIMIT 1")
    fun observeFavoriteDevice(): Flow<BleDeviceEntity?>

    /**
     * Obtiene todos los dispositivos
     */
    @Query("SELECT * FROM devices ORDER BY last_connection_date DESC")
    suspend fun getAllDevices(): List<BleDeviceEntity>

    /**
     * Obtiene un dispositivo por su MAC address
     */
    @Query("SELECT * FROM devices WHERE mac_address = :macAddress")
    suspend fun getDeviceByMac(macAddress: String): BleDeviceEntity?

    /**
     * Obtiene el dispositivo favorito
     */
    @Query("SELECT * FROM devices WHERE is_favorite = 1 LIMIT 1")
    suspend fun getFavoriteDevice(): BleDeviceEntity?

    /**
     * Obtiene los dispositivos más recientes limitados
     */
    @Query("SELECT * FROM devices ORDER BY last_connection_date DESC LIMIT :limit")
    suspend fun getRecentDevices(limit: Int): List<BleDeviceEntity>

    /**
     * Inserta o actualiza un dispositivo
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: BleDeviceEntity)

    /**
     * Actualiza un dispositivo existente
     */
    @Update
    suspend fun updateDevice(device: BleDeviceEntity)

    /**
     * Desmarca todos los dispositivos como no favoritos
     */
    @Query("UPDATE devices SET is_favorite = 0")
    suspend fun clearAllFavorites()

    /**
     * Marca un dispositivo como favorito por su MAC
     */
    @Query("UPDATE devices SET is_favorite = 1 WHERE mac_address = :macAddress")
    suspend fun setFavorite(macAddress: String)

    /**
     * Actualiza el apodo de un dispositivo
     */
    @Query("UPDATE devices SET nickname = :nickname WHERE mac_address = :macAddress")
    suspend fun updateNickname(macAddress: String, nickname: String)

    /**
     * Actualiza la última fecha de conexión
     */
    @Query("UPDATE devices SET last_connection_date = :timestamp WHERE mac_address = :macAddress")
    suspend fun updateLastConnection(macAddress: String, timestamp: Long)

    /**
     * Actualiza el RSSI de un dispositivo
     */
    @Query("UPDATE devices SET rssi = :rssi WHERE mac_address = :macAddress")
    suspend fun updateRssi(macAddress: String, rssi: Int)

    /**
     * Elimina un dispositivo por su MAC
     */
    @Query("DELETE FROM devices WHERE mac_address = :macAddress")
    suspend fun deleteDevice(macAddress: String)

    /**
     * Elimina todos los dispositivos
     */
    @Query("DELETE FROM devices")
    suspend fun deleteAllDevices()

    /**
     * Cuenta cuántos dispositivos hay guardados
     */
    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getDeviceCount(): Int
}