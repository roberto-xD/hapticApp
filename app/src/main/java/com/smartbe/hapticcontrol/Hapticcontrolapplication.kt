package com.smartbe.hapticcontrol

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application principal de la aplicación
 * Anotada con @HiltAndroidApp para inicializar Hilt
 */
@HiltAndroidApp
class HapticControlApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializaciones adicionales si son necesarias
    }
}