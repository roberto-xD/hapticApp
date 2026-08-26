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

| Acción | Byte |
|---|---|
| Apagar | `0x00` |
| Patrón 1–5 | `0x51` … `0x55` |
| Intensidad 1–9 | `0x11` … `0x19` |
| Intensidad 10 | `0x20` |

El nivel 10 rompe la secuencia lineal: es `0x20`, no `0x1A`. Codificación tipo
BCD, no un contador.

Los patrones **no son frecuencias en Hz**. Son índices; el ritmo lo decide el
firmware. No existe parámetro continuo, por eso la UI no tiene slider de
frecuencia.

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
