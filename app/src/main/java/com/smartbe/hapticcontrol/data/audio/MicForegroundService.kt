package com.smartbe.hapticcontrol.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.smartbe.hapticcontrol.R

/**
 * Servicio en primer plano cuyo unico cometido es que el sistema no corte el
 * microfono.
 *
 * Desde Android 9 la captura se silencia en cuanto la app deja de estar
 * visible, y lo hace de la peor manera posible: AudioRecord sigue devolviendo
 * buffers, solo que llenos de ceros. El modo reactivo se moria al minimizar sin
 * dar ningun aviso.
 *
 * No graba nada. La captura sigue viviendo en AudioLevelSource; el servicio
 * solo mantiene el proceso en un estado que conserva el acceso al micro.
 */
class MicForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )
        // START_NOT_STICKY: si el sistema mata el proceso, el modo reactivo no
        // debe resucitar por su cuenta. Lo enciende el usuario o nadie.
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Modo reactivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene el microfono abierto mientras el modo " +
                        "reactivo esta encendido"
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reactivo al sonido")
            .setContentText("Escuchando el microfono")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val TAG = "MicForegroundService"
        private const val CHANNEL_ID = "mic_reactive"
        private const val NOTIFICATION_ID = 1

        /**
         * Solo se puede llamar con la app visible: desde Android 12 arrancar un
         * servicio en primer plano desde background lanza
         * ForegroundServiceStartNotAllowedException. El modo reactivo siempre
         * nace de un toque en la UI, asi que la condicion se cumple; el
         * runCatching esta para que un caso raro degrade a "el micro se corta
         * al minimizar" en vez de tumbar la app.
         */
        fun start(context: Context) {
            runCatching {
                context.startForegroundService(
                    Intent(context, MicForegroundService::class.java)
                )
            }.onFailure { error ->
                Log.e(TAG, "No se pudo arrancar el servicio de microfono", error)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, MicForegroundService::class.java))
            }
        }
    }
}
