package com.smartbe.hapticcontrol.di

import android.content.Context
import androidx.room.Room
import com.smartbe.hapticcontrol.data.local.dao.BleDeviceDao
import com.smartbe.hapticcontrol.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para proveer dependencias de Room Database
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provee la instancia de la base de datos Room
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Para desarrollo, en producción usa migraciones
            .build()
    }

    /**
     * Provee el DAO de dispositivos
     */
    @Provides
    @Singleton
    fun provideDeviceDao(database: AppDatabase): BleDeviceDao {
        return database.deviceDao()
    }
}