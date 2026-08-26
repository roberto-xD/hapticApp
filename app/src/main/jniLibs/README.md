# jniLibs

Aquí va `libble24.so`, extraído del APK original. No está en el repositorio
porque es un binario de terceros.

```bash
unzip -o app-original.apk 'lib/*' -d extraido/
cp extraido/lib/arm64-v8a/libble24.so   app/src/main/jniLibs/arm64-v8a/
cp extraido/lib/armeabi-v7a/libble24.so app/src/main/jniLibs/armeabi-v7a/
```

Sin estos archivos la app compila y arranca, pero `RfPayloadBuilder.nativeAvailable`
devuelve `false` y la pantalla de control muestra el aviso correspondiente en vez
de emitir comandos a ciegas.
