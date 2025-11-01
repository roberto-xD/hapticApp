package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.BleDeviceModel
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetConnectedDeviceUseCase @Inject constructor(
    private val repository: BleRepository
) {
    operator fun invoke(): StateFlow<BleDeviceModel?> {
        return repository.connectedDevice
    }
}