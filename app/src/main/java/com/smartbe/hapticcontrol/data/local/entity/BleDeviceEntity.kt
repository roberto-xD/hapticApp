package com.smartbe.hapticcontrol.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para persistir dispositivos BLE
 */
@Entity(tableName = "devices")
data class BleDeviceEntity(
    @PrimaryKey
    @ColumnInfo(name = "mac_address")
    val macAddress: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "nickname")
    val nickname: String = "",

    @ColumnInfo(name = "rssi")
    val rssi: Int = 0,

    @ColumnInfo(name = "last_connection_date")
    val lastConnectionDate: Long = 0L,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)