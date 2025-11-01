package com.smartbe.hapticcontrol.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartbe.hapticcontrol.data.local.dao.BleDeviceDao
import com.smartbe.hapticcontrol.data.local.entity.BleDeviceEntity

/**
 * Base de datos principal de la aplicación
 */
@Database(
    entities = [BleDeviceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): BleDeviceDao

    companion object {
        const val DATABASE_NAME = "haptic_control_db"
    }
}