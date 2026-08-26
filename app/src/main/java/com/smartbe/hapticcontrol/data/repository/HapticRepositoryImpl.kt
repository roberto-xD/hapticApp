package com.smartbe.hapticcontrol.data.repository

import com.smartbe.hapticcontrol.data.bluetooth.BroadcastAdvertiser
import com.smartbe.hapticcontrol.data.bluetooth.RfPayloadBuilder
import com.smartbe.hapticcontrol.domain.model.HapticCommand
import com.smartbe.hapticcontrol.domain.repository.HapticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticRepositoryImpl @Inject constructor(
    private val payloadBuilder: RfPayloadBuilder,
    private val advertiser: BroadcastAdvertiser
) : HapticRepository {

    override val isBroadcasting: StateFlow<Boolean> = advertiser.isBroadcasting

    private val _lastCommand = MutableStateFlow<HapticCommand?>(null)
    override val lastCommand: StateFlow<HapticCommand?> = _lastCommand.asStateFlow()

    override suspend fun send(command: HapticCommand): Result<Unit> {
        val payload = payloadBuilder.build(command).getOrElse { error ->
            return Result.failure(error)
        }

        return advertiser.broadcast(payload).onSuccess {
            _lastCommand.value = command
        }
    }

    override suspend fun stop(): Result<Unit> = send(HapticCommand.Off)
}
