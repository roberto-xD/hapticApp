package com.smartbe.hapticcontrol.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartbe.hapticcontrol.data.local.dao.DeviceDao
import com.smartbe.hapticcontrol.data.local.entity.DeviceEntity

/**
 * Base de datos principal de la aplicación
 */
@Database(
    entities = [DeviceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    companion object {
        const val DATABASE_NAME = "haptic_control_db"
    }
}