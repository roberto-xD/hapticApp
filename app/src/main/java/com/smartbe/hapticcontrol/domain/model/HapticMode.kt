package com.smartbe.hapticcontrol.domain.model

/**
 * El vocabulario completo del protocolo.
 *
 * Confirmado en hardware contra la app original: nueve modos, un byte cada uno,
 * mas el apagado. El firmware ejecuta los ritmos por su cuenta; el telefono solo
 * tiene que sostener el anuncio.
 *
 * Origen: captura HCI (envoltorio Qualcomm 0xFD54) de los nueve modos clasicos
 * pulsados uno a uno, decodificada invirtiendo el whitening con K = 0xE5.
 */
enum class HapticMode(
    val code: Byte,
    val displayName: String,
    val isRhythmic: Boolean
) {
    CONTINUA_BAJA(0x01, "Continua baja", false),
    CONTINUA_MEDIA(0x02, "Continua media", false),
    CONTINUA_ALTA(0x03, "Continua alta", false),
    RITMO_1(0x04, "Ritmo 1", true),
    RITMO_2(0x05, "Ritmo 2", true),
    RITMO_3(0x06, "Ritmo 3", true),
    RITMO_4(0x07, "Ritmo 4", true),
    RITMO_5(0x08, "Ritmo 5", true),
    RITMO_6(0x09, "Ritmo 6", true);

    companion object {
        /** Apagado explicito. No es un modo, por eso vive aparte. */
        const val OFF_CODE: Byte = 0x00

        val continuous: List<HapticMode> get() = entries.filter { !it.isRhythmic }
        val rhythmic: List<HapticMode> get() = entries.filter { it.isRhythmic }
    }
}
