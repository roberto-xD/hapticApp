package com.smartbe.hapticcontrol.data.local.mapper

import com.smartbe.hapticcontrol.data.local.entity.BleDeviceEntity
import com.smartbe.hapticcontrol.domain.model.BleDeviceModel

/**
 * Mappers para convertir entre Entity y Domain Model
 */

/**
 * Convierte DeviceEntity a BleDevice (domain model)
 */
fun BleDeviceEntity.toDomain(): BleDeviceModel {
    return BleDeviceModel(
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
fun BleDeviceModel.toEntity(): BleDeviceEntity {
    return BleDeviceEntity(
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
fun List<BleDeviceEntity>.toDomain(): List<BleDeviceModel> {
    return map { it.toDomain() }
}

/**
 * Convierte lista de BleDevice a lista de DeviceEntity
 */
fun List<BleDeviceModel>.toEntity(): List<BleDeviceEntity> {
    return map { it.toEntity() }
}