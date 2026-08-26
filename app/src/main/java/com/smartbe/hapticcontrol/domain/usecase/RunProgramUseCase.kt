package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.VibrationProgram
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import javax.inject.Inject

class RunProgramUseCase @Inject constructor(
    private val repository: HapticRepository
) {
    operator fun invoke(program: VibrationProgram) = repository.run(program)
}
