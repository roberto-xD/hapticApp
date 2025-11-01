package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

class StartScanUseCase @Inject constructor(
    private val bleRepository: BleRepository
){
    suspend operator fun invoke(){
        bleRepository.startScan()
    }
}