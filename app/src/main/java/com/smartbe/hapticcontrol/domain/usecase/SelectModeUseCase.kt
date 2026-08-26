package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.HapticMode
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import javax.inject.Inject

class SelectModeUseCase @Inject constructor(
    private val repository: HapticRepository
) {
    operator fun invoke(mode: HapticMode) = repository.select(mode)
}
