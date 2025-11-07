package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use Case para obtener el estado del modo visitante
 */
class GetVisitorModeUseCase @Inject constructor(
    private val repository: BleRepository
) {
    operator fun invoke(): StateFlow<Boolean> {
        return repository.visitorModeEnabled
    }
}