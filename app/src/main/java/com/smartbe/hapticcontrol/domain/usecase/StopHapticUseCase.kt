package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import javax.inject.Inject

class StopHapticUseCase @Inject constructor(
    private val repository: HapticRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.stop()
}
