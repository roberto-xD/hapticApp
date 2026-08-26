package com.smartbe.hapticcontrol.di

import com.smartbe.hapticcontrol.data.repository.HapticRepositoryImpl
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RfPayloadBuilder y BroadcastAdvertiser ya llevan @Inject constructor y
 * @Singleton, asi que Hilt los construye solo. Aqui basta con enlazar la
 * interfaz del repositorio a su implementacion.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {

    @Binds
    @Singleton
    abstract fun bindHapticRepository(impl: HapticRepositoryImpl): HapticRepository
}
