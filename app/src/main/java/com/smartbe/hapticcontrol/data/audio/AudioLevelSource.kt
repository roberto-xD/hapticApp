package com.smartbe.hapticcontrol.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * El microfono no se pudo abrir.
 *
 * Se lanza en vez de terminar el flujo en silencio: un flujo que acaba sin
 * emitir es indistinguible de una cancelacion normal, y quien colecciona no
 * tiene forma de saber que el modo nunca arranco.
 */
class AudioCaptureException(message: String) : Exception(message)

/**
 * Convierte el sonido del microfono en un nivel 0..3.
 *
 * Solo tres escalones porque el aparato solo tiene tres intensidades continuas
 * (0x01-0x03). Los modos 0x04-0x09 son ritmos con tempo propio: si el volumen
 * los disparara, el firmware ejecutaria su patron ignorando la musica. El techo
 * no es nuestro, es del hardware.
 */
@Singleton
class AudioLevelSource @Inject constructor() {

    /**
     * Emite el nivel cuando cambia. Requiere permiso RECORD_AUDIO concedido
     * antes de coleccionar; si falta, lanza [AudioCaptureException].
     */
    @SuppressLint("MissingPermission")
    fun levels(): Flow<Int> = flow {
        val windowSamples = SAMPLE_RATE * WINDOW_MS / 1000
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            throw AudioCaptureException(
                "Este telefono no soporta captura a 16 kHz mono PCM16"
            )
        }

        val record = AudioRecord(
            audioSource(),
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, windowSamples * 4)
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw AudioCaptureException(
                "No se pudo abrir el microfono. Revisa el permiso o cierra la " +
                        "app que lo este usando."
            )
        }

        val buffer = ShortArray(windowSamples)

        // Ganancia adaptativa. Un RMS crudo se queda clavado en el minimo con
        // musica baja y en el maximo con musica alta; suelo y techo se
        // recalculan sobre una ventana movil para que el rango util siempre
        // ocupe los tres escalones.
        var floor = Float.MAX_VALUE
        var ceiling = 0f

        var level = 0
        var lastChange = 0L

        try {
            record.startRecording()
            emit(0)
            while (currentCoroutineContext().isActive) {
                val read = record.read(buffer, 0, windowSamples)
                if (read <= 0) continue

                val rms = rms(buffer, read)

                if (floor == Float.MAX_VALUE) {
                    floor = rms
                    ceiling = rms * 2
                }
                ceiling = if (rms > ceiling) rms else ceiling - (ceiling - rms) * ADAPT
                floor = if (rms < floor) rms else floor + (rms - floor) * ADAPT

                val objetivo = classify(rms, floor, ceiling)
                val ahora = System.currentTimeMillis()

                // Histeresis asimetrica: subir es inmediato, bajar espera y va
                // de uno en uno. Sin esto se oscila entre dos escalones, y cada
                // oscilacion cuesta un stop/start del anuncio. De paso imita la
                // envolvente de un compresor, que es como suena natural.
                val nuevo = when {
                    objetivo > level -> objetivo
                    objetivo < level && ahora - lastChange >= FALL_HOLD_MS -> level - 1
                    else -> level
                }

                if (nuevo != level) {
                    level = nuevo
                    lastChange = ahora
                    emit(level)
                }
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun classify(rms: Float, floor: Float, ceiling: Float): Int {
        if (rms < NOISE_GATE) return 0
        val rango = (ceiling - floor).coerceAtLeast(1f)
        val norm = ((rms - floor) / rango).coerceIn(0f, 1f)
        return when {
            norm < 0.20f -> 0
            norm < 0.50f -> 1
            norm < 0.80f -> 2
            else -> 3
        }
    }

    private fun rms(buffer: ShortArray, size: Int): Float {
        var suma = 0.0
        for (i in 0 until size) {
            val v = buffer[i].toDouble()
            suma += v * v
        }
        return sqrt(suma / size).toFloat()
    }

    /**
     * UNPROCESSED evita el control automatico de ganancia y la supresion de
     * ruido del sistema, que pelearian contra nuestra propia normalizacion.
     */
    private fun audioSource(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val WINDOW_MS = 80
        private const val ADAPT = 0.02f
        private const val NOISE_GATE = 300f
        private const val FALL_HOLD_MS = 250L
    }
}
