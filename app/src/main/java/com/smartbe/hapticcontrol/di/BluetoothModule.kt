package com.smartbe.hapticcontrol.di

import android.content.Context
import com.smartbe.hapticcontrol.data.bluetooth.BleConnector
import com.smartbe.hapticcontrol.data.bluetooth.BleManagerImpl
import com.smartbe.hapticcontrol.data.bluetooth.BleScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para proveer dependencias de Bluetooth
 */
@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    /**
     * Provee el escáner BLE
     */
    @Provides
    @Singleton
    fun provideBleScanner(
        @ApplicationContext context: Context
    ): BleScanner {
        return BleScanner(context)
    }

    /**
     * Provee el conector BLE
     */
    @Provides
    @Singleton
    fun provideBleConnector(
        @ApplicationContext context: Context
    ): BleConnector {
        return BleConnector(context)
    }

    /**
     * Provee el gestor BLE principal
     */
    @Provides
    @Singleton
    fun provideBleManager(
        bleScanner: BleScanner,
        bleConnector: BleConnector
    ): BleManagerImpl {
        return BleManagerImpl(bleScanner, bleConnector)
    }
}