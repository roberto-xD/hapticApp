package com.smartbe.hapticcontrol.data.local.mapper

import com.smartbe.hapticcontrol.data.local.entity.DeviceEntity
import com.smartbe.hapticcontrol.domain.model.BleDevice

/**
 * Mappers para convertir entre Entity y Domain Model
 */

/**
 * Convierte DeviceEntity a BleDevice (domain model)
 */
fun DeviceEntity.toDomain(): BleDevice {
    return BleDevice(
        macAddress = macAddress,
        name = name,
        nickname = nickname,
        rssi = rssi,
        lastConnectionDate = lastConnectionDate,
        isFavorite = isFavorite
    )
}

/**
 * Convierte BleDevice (domain model) a DeviceEntity
 */
fun BleDevice.toEntity(): DeviceEntity {
    return DeviceEntity(
        macAddress = macAddress,
        name = name,
        nickname = nickname,
        rssi = rssi,
        lastConnectionDate = lastConnectionDate,
        isFavorite = isFavorite
    )
}

/**
 * Convierte lista de DeviceEntity a lista de BleDevice
 */
fun List<DeviceEntity>.toDomain(): List<BleDevice> {
    return map { it.toDomain() }
}

/**
 * Convierte lista de BleDevice a lista de DeviceEntity
 */
fun List<BleDevice>.toEntity(): List<DeviceEntity> {
    return map { it.toEntity() }
}