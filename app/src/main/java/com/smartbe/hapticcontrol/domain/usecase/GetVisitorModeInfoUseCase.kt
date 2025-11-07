package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.VisitorMode
import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use Case para obtener información del modo visitante
 */
class GetVisitorModeInfoUseCase @Inject constructor(
    private val repository: BleRepository
) {
    operator fun invoke(): StateFlow<VisitorMode> {
        return repository.visitorModeInfo
    }
}