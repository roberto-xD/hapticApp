package com.smartbe.hapticcontrol.di

import com.smartbe.hapticcontrol.data.bluetooth.BleManagerImpl
import com.smartbe.hapticcontrol.data.local.dao.DeviceDao
import com.smartbe.hapticcontrol.data.repository.BleRepositoryImpl
import com.smartbe.hapticcontrol.data.repository.DeviceRepositoryImpl
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import com.smartbe.hapticcontrol.domain.repository.DeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para proveer implementaciones de repositorios
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provee la implementación del repositorio BLE
     */
    @Provides
    @Singleton
    fun provideBleRepository(
        bleManager: BleManagerImpl
    ): BleRepository {
        return BleRepositoryImpl(bleManager)
    }

    /**
     * Provee la implementación del repositorio de dispositivos
     */
    @Provides
    @Singleton
    fun provideDeviceRepository(
        deviceDao: DeviceDao
    ): DeviceRepository {
        return DeviceRepositoryImpl(deviceDao)
    }
}