package com.wearinterval.domain.repository

import com.wearinterval.domain.model.TimerState
import kotlinx.coroutines.flow.StateFlow

interface TimerRepository {
  val timerState: StateFlow<TimerState>
  val isServiceBound: StateFlow<Boolean>

  suspend fun startTimer(): Result<Unit>

  suspend fun pauseTimer(): Result<Unit>

  suspend fun resumeTimer(): Result<Unit>

  suspend fun stopTimer(): Result<Unit>

  suspend fun dismissAlarm(): Result<Unit>
}
