# Protocolo de difusión 2.4 GHz

Reconstruido por ingeniería inversa de la app original (`com.wb.lsr` /
`com.wbkj.lovespouse`). Este documento es la referencia; el código lo implementa.

## Arquitectura del enlace

El dispositivo **no es un periférico BLE**. Es un receptor de radio clase
nRF24L01 que escucha permanentemente en una dirección fija. El teléfono actúa de
emisor. De ahí las tres propiedades observadas en pruebas físicas:

- Ningún escáner BLE los ve: nunca anuncian, solo escuchan.
- No hay emparejamiento: cualquier emisor en rango los controla.
- No hay comunicación de vuelta: el enlace es estrictamente unidireccional.

## Trama

`BLEUtil.get_rf_payload(addr[5], 5, data[1], 1, out[11])` produce 11 bytes:

```
[cabecera 2] [dirección 5] [payload 1] [CRC-24 3]
```

| Elemento | Valor |
|---|---|
| Dirección por defecto | `77 62 4D 53 45` — ASCII `wbMSE` |
| Dirección alternativa | `DeviceBean.BroadcastPrefix`, sólo si `IsPrivateProtocol == 1` |
| Algoritmo | `libble24.so` (nativo, no reimplementado) |

Esos 11 bytes se incrustan como manufacturer data de un anuncio BLE real.

## Anuncio

| Parámetro | Valor |
|---|---|
| Service UUID | `0000ae8f-0000-1000-8000-00805f9b34fb` |
| Manufacturer ID | `255` (`0xFF`) |
| Modo | `ADVERTISE_MODE_LOW_LATENCY` |
| Conectable | `true` |
| Potencia | `ADVERTISE_TX_POWER_HIGH` |
| Timeout del stack | 3000 ms |
| Duración de la ráfaga | 1000 ms |

`setConnectable(true)` parece contradictorio en un anuncio que nadie va a
conectar, pero cambia el tipo de PDU a `ADV_IND` y con ello la longitud de trama
que el receptor espera. No modificar sin validar contra hardware.

## Comandos

**Tabla definitiva, validada uno a uno contra la app original:**

| Acción | Byte |
|---|---|
| Apagar | `0x00` |
| Continua baja / media / alta | `0x01` `0x02` `0x03` |
| Ritmos 1–6 | `0x04` … `0x09` |

Eso es todo el vocabulario. Un byte, diez valores. No hay eje de intensidad
separado del de modo: siempre fue uno solo. Los ritmos los ejecuta el firmware.

Descartado por el camino, para que nadie lo reintente:

- `0x51`–`0x55` (de `sendQdOrSy`): el aparato los ignora, incluso encadenados
  sobre un motor ya en marcha. Pertenecen a otra línea de producto del APK.
- Familia `0xAA` (`getBleCommand55`, `getBleCommand3`): existe en el APK pero no
  en este modelo. Las 43 tramas capturadas miden 11 bytes sin excepción, o sea
  payload de un solo byte.
- `0x11`–`0x19` / `0x20` (de `sendZl`): es la rama **GATT**, no la de difusión.

## Limitaciones que no se pueden resolver desde la app

1. **Sin direccionamiento.** Lo único que discrimina destinatarios es el prefijo,
   que es por modelo. Dos unidades del mismo modelo en rango obedecen siempre las
   dos.
2. **Sin confirmación.** Un `Result.success()` significa que el anuncio salió al
   aire, no que el dispositivo lo recibió.

## Pendiente de validar

`app/src/debug/.../RfPayloadProbe.kt` interroga la librería nativa variando una
entrada a la vez. Ejecutarlo una vez y revisar el logcat con el tag `RF_PROBE`.

El bloque 4 (determinismo) es el crítico: si tres llamadas idénticas devuelven
resultados distintos hay estado interno en el nativo —contador de secuencia o
rotación de canal— y la estrategia de emisión cambia por completo.

## Inversión del whitening (confirmado por captura HCI)

`get_rf_payload` aplica whitening, así que el comando no es legible en el aire.
Pero el whitening BLE es un XOR con un keystream que depende solo de la posición
dentro del paquete, no del contenido. Con un par claro/cifrado conocido se
recupera la clave de esa posición y se descifra cualquier captura.

Trama de 11 bytes observada:

```
6D B6 43 CE 97 FE 42 7C | XX | CC CC
└──── constante ────────┘   │   └ CRC
                            └ comando ⊕ 0xE5
```

Los ocho primeros bytes son idénticos en todas las emisiones con el mismo
prefijo. La posición 8 lleva el comando cifrado; las dos últimas varían con él.

Clave obtenida anclando en nuestra propia emisión de `0x51`, que produjo `0xB4`:

    K = 0x51 XOR 0xB4 = 0xE5

Tabla decodificada de una rampa de intensidad capturada de la app original:

| Aire | Claro | | Aire | Claro |
|---|---|---|---|---|
| `E5` | `0x00` (apagado) | | `E0` | `0x05` |
| `E4` | `0x01` | | `E3` | `0x06` |
| `E7` | `0x02` | | `E2` | `0x07` |
| `E6` | `0x03` | | `ED` | `0x08` |
| `E1` | `0x04` | | `EC` | `0x09` |

Las diferencias entre tramas son consistentes entre sí sin usar el ancla, lo que
valida la deducción de forma independiente.

**Corrección importante:** la tabla `0x11`–`0x19` / `0x20` que aparece en
`sendZl` corresponde a la rama **GATT**. La rama de difusión usa `0x01`–`0x0A`.

## Nota sobre la captura en Xiaomi

MIUI sobre Qualcomm no emite `0x2008` (LE Set Advertising Data). Envuelve los
datos en el comando propietario **`0xFD54`**:

```
01 54 FD 22 | 02 16 | 02 01 02 | 0E FF FF 00 <11 bytes> | 03 03 8F AE
             len=22   flags      manufacturer 0x00FF       UUID AE8F
```

Filtrar por `0x2008` en Wireshark no encuentra nada en estos teléfonos. Hay que
buscar `0xFD54`, o directamente el patrón `03 03 8F AE`.

Cada pulsación genera **una sola** trama `0xFD54`, no una cada 100 ms: los datos
del anuncio no cambian mientras se mantiene pulsado, así que el stack solo los
escribe una vez.

## Inversión del whitening (confirmado por captura HCI)

`get_rf_payload` aplica whitening, así que el comando no es legible en el aire.
Pero el whitening BLE es un XOR con un keystream que depende solo de la posición
dentro del paquete, no del contenido. Con un par claro/cifrado conocido se
recupera la clave de esa posición y se descifra cualquier captura.

Trama de 11 bytes observada:

```
6D B6 43 CE 97 FE 42 7C | XX | CC CC
└──── constante ────────┘   │   └ CRC
                            └ comando ⊕ 0xE5
```

Los ocho primeros bytes son idénticos en todas las emisiones con el mismo
prefijo. La posición 8 lleva el comando cifrado; las dos últimas varían con él.

Clave obtenida anclando en nuestra propia emisión de `0x51`, que produjo `0xB4`:

    K = 0x51 XOR 0xB4 = 0xE5

Tabla decodificada de una rampa de intensidad capturada de la app original:

| Aire | Claro | | Aire | Claro |
|---|---|---|---|---|
| `E5` | `0x00` (apagado) | | `E0` | `0x05` |
| `E4` | `0x01` | | `E3` | `0x06` |
| `E7` | `0x02` | | `E2` | `0x07` |
| `E6` | `0x03` | | `ED` | `0x08` |
| `E1` | `0x04` | | `EC` | `0x09` |

Las diferencias entre tramas son consistentes entre sí sin usar el ancla, lo que
valida la deducción de forma independiente.

**Corrección importante:** la tabla `0x11`–`0x19` / `0x20` que aparece en
`sendZl` corresponde a la rama **GATT**. La rama de difusión usa `0x01`–`0x0A`.

## Nota sobre la captura en Xiaomi

MIUI sobre Qualcomm no emite `0x2008` (LE Set Advertising Data). Envuelve los
datos en el comando propietario **`0xFD54`**:

```
01 54 FD 22 | 02 16 | 02 01 02 | 0E FF FF 00 <11 bytes> | 03 03 8F AE
             len=22   flags      manufacturer 0x00FF       UUID AE8F
```

Filtrar por `0x2008` en Wireshark no encuentra nada en estos teléfonos. Hay que
buscar `0xFD54`, o directamente el patrón `03 03 8F AE`.

Cada pulsación genera **una sola** trama `0xFD54`, no una cada 100 ms: los datos
del anuncio no cambian mientras se mantiene pulsado, así que el stack solo los
escribe una vez.

## Modo reactivo al sonido

El aparato no tiene ningún comando de audio. El modo reactivo es entrada del
teléfono alimentando los mismos nueve comandos, igual que los modos `music`,
`shake` y `finger` de la app original: todos son fuentes distintas apuntando al
mismo vocabulario de un byte.

**Techo real: tres niveles, no nueve.** Solo `0x01`–`0x03` son intensidades
continuas. Los `0x04`–`0x09` son ritmos con tempo propio; si el volumen los
disparara, el firmware ejecutaría su patrón ignorando la música.

Cadena de proceso en `AudioLevelSource`:

1. `AudioRecord` a 16 kHz mono, ventanas de 80 ms
2. RMS por ventana
3. Ganancia adaptativa: suelo y techo se recalculan sobre ventana móvil, porque
   un RMS crudo se queda clavado en el mínimo con música baja y en el máximo con
   música alta
4. Clasificación a 0–3 con puerta de ruido
5. Histéresis asimétrica: subir inmediato, bajar tras 250 ms y de uno en uno

La histéresis no es cosmética. Sin ella se oscila entre escalones adyacentes, y
cada oscilación cuesta un stop/start del anuncio, que ronda las decenas de
milisegundos. El techo práctico son 6–8 cambios por segundo.

Se usa `AudioSource.UNPROCESSED` para evitar el control automático de ganancia y
la supresión de ruido del sistema, que pelearían contra la normalización propia.

En el repositorio, `collectLatest` hace el trabajo: cada nivel nuevo cancela la
emisión anterior —suspendida sosteniendo el anuncio— y arranca la siguiente. Sin
temporizadores ni estado adicional.
