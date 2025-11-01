package com.smartbe.hapticcontrol.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.smartbe.hapticcontrol.data.bluetooth.BleAdvertiserManager
import com.smartbe.hapticcontrol.data.bluetooth.BleConnector
import com.smartbe.hapticcontrol.data.bluetooth.BleManagerImpl
import com.smartbe.hapticcontrol.data.bluetooth.BleScanner
import com.smartbe.hapticcontrol.data.local.dao.BleDeviceDao
import com.smartbe.hapticcontrol.data.repository.BleRepositoryImpl
import com.smartbe.hapticcontrol.domain.repository.BleRepository
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

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter
    }

    // NUEVO: Proveer BleAdvertiserManager
    @Provides
    @Singleton
    fun provideBleAdvertiserManager(
        bluetoothAdapter: BluetoothAdapter?
    ): BleAdvertiserManager {
        return BleAdvertiserManager(bluetoothAdapter)
    }

    @Provides
    @Singleton
    fun provideBleRepository(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter?,
        deviceDao: BleDeviceDao,
        advertiserManager: BleAdvertiserManager // NUEVO
    ): BleRepository {
        return BleRepositoryImpl(context, bluetoothAdapter, deviceDao, advertiserManager)
    }
}