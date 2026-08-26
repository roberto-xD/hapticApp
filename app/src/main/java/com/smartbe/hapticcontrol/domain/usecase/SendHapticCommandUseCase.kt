package com.smartbe.hapticcontrol.domain.usecase

import com.smartbe.hapticcontrol.domain.model.HapticCommand
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import javax.inject.Inject

class SendHapticCommandUseCase @Inject constructor(
    private val repository: HapticRepository
) {
    suspend operator fun invoke(command: HapticCommand): Result<Unit> =
        repository.send(command)
}
