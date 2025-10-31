package com.smartbe.hapticcontrol.domain.repository

import com.smartbe.hapticcontrol.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar dispositivos guardados localmente
 * Maneja la persistencia de dispositivos emparejados e histórico
 */
interface DeviceRepository {

    /**
     * Flow que emite la lista completa de dispositivos guardados
     */
    val savedDevices: Flow<List<BleDevice>>

    /**
     * Flow que emite el dispositivo favorito (para reconexión automática)
     */
    val favoriteDevice: Flow<BleDevice?>

    /**
     * Guarda un nuevo dispositivo o actualiza uno existente
     *
     * @param device Dispositivo a guardar
     */
    suspend fun saveDevice(device: BleDevice)

    /**
     * Obtiene un dispositivo por su dirección MAC
     *
     * @param macAddress Dirección MAC del dispositivo
     * @return Dispositivo encontrado o null
     */
    suspend fun getDeviceByMac(macAddress: String): BleDevice?

    /**
     * Obtiene todos los dispositivos guardados
     *
     * @return Lista de dispositivos
     */
    suspend fun getAllDevices(): List<BleDevice>

    /**
     * Obtiene el dispositivo marcado como favorito
     *
     * @return Dispositivo favorito o null
     */
    suspend fun getFavoriteDevice(): BleDevice?

    /**
     * Marca un dispositivo como favorito (solo puede haber uno)
     * Desmarca cualquier otro dispositivo favorito anterior
     *
     * @param macAddress Dirección MAC del dispositivo
     */
    suspend fun setFavoriteDevice(macAddress: String)

    /**
     * Actualiza el apodo de un dispositivo
     *
     * @param macAddress Dirección MAC del dispositivo
     * @param nickname Nuevo apodo
     */
    suspend fun updateNickname(macAddress: String, nickname: String)

    /**
     * Actualiza la fecha de última conexión de un dispositivo
     *
     * @param macAddress Dirección MAC del dispositivo
     * @param timestamp Timestamp de la conexión
     */
    suspend fun updateLastConnection(macAddress: String, timestamp: Long)

    /**
     * Elimina un dispositivo guardado
     *
     * @param macAddress Dirección MAC del dispositivo
     */
    suspend fun deleteDevice(macAddress: String)

    /**
     * Elimina todos los dispositivos guardados
     */
    suspend fun deleteAllDevices()

    /**
     * Obtiene los dispositivos ordenados por última conexión
     *
     * @param limit Límite de dispositivos a devolver
     * @return Lista de dispositivos recientes
     */
    suspend fun getRecentDevices(limit: Int = 5): List<BleDevice>
}