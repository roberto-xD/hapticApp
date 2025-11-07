package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.BleRepository
import javax.inject.Inject

/**
 * Use Case para habilitar/deshabilitar modo visitante
 */
class SetVisitorModeUseCase @Inject constructor(
    private val repository: BleRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setVisitorMode(enabled)
    }
}