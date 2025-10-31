package com.smartbe.hapticcontrol.domain.usecase.scan

import com.smartbe.hapticcontrol.domain.model.ScanResult
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para iniciar el escaneo de dispositivos BLE
 */
class StartScanUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Inicia el escaneo de dispositivos BLE cercanos
     *
     * @param timeoutMillis Duración máxima del escaneo en milisegundos
     */
    suspend operator fun invoke(timeoutMillis: Long = 5000L) {
        bleRepository.startScan(timeoutMillis)
    }
}

class StopScanUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Detiene el escaneo activo de dispositivos BLE
     */
    suspend operator fun invoke() {
        bleRepository.stopScan()
    }
}

class ObserveScanResultsUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Obtiene un Flow con los resultados del escaneo en tiempo real
     *
     * @return Flow que emite ScanResult
     */
    operator fun invoke(): Flow<ScanResult> {
        return bleRepository.scanResults
    }
}