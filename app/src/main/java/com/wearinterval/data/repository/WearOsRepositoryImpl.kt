package com.wearinterval.data.repository

import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.repository.ComplicationData
import com.wearinterval.domain.repository.ComplicationType
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TileData
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.domain.repository.WearOsRepository
import com.wearinterval.util.TimeUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WearOsRepositoryImpl
@Inject
constructor(
  private val timerRepository: TimerRepository,
  private val configurationRepository: ConfigurationRepository,
) : WearOsRepository {

  override suspend fun updateWearOsComponents(): Result<Unit> {
    return try {
      // TODO: Implement tile and complication updates in Phase 5
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun getTileData(): TileData {
    return TileData(
      timerState = timerRepository.timerState.first(),
      recentConfigurations =
        configurationRepository.recentConfigurations
          .first()
          .take(4), // Match history screen limit for 2x2 grid
    )
  }

  override suspend fun getComplicationData(type: ComplicationType): ComplicationData {
    val timerState = timerRepository.timerState.first()

    return when (type) {
      is ComplicationType.ShortText -> {
        val text =
          when (timerState.phase) {
            TimerPhase.Stopped -> "Ready"
            TimerPhase.Running -> TimeUtils.formatTimeCompact(timerState.timeRemaining)
            TimerPhase.Resting -> "R:${TimeUtils.formatTimeCompact(timerState.timeRemaining)}"
            TimerPhase.Paused -> "Paused"
            TimerPhase.AlarmActive -> "Alarm"
          }
        val title =
          if (timerState.phase != TimerPhase.Stopped) {
            timerState.displayCurrentLap
          } else {
            null
          }

        ComplicationData.ShortText(text, title)
      }
      is ComplicationType.LongText -> {
        val text =
          when (timerState.phase) {
            TimerPhase.Stopped -> timerState.configuration.shortDisplayString()
            TimerPhase.Running ->
              "${TimeUtils.formatTimeCompact(timerState.timeRemaining)} - Lap ${timerState.displayCurrentLap}"
            TimerPhase.Resting ->
              "Rest: ${TimeUtils.formatTimeCompact(
                        timerState.timeRemaining,
                    )} - Lap ${timerState.displayCurrentLap}"
            TimerPhase.Paused -> "Paused - Lap ${timerState.displayCurrentLap}"
            TimerPhase.AlarmActive -> "Alarm - Tap to dismiss"
          }
        val title = if (timerState.phase == TimerPhase.Stopped) "Ready" else null

        ComplicationData.LongText(text, title)
      }
      is ComplicationType.RangedValue -> {
        val progress = timerState.progressPercentage
        val text = TimeUtils.formatTimeCompact(timerState.timeRemaining)
        val title = timerState.displayCurrentLap

        ComplicationData.RangedValue(
          value = progress,
          min = 0f,
          max = 1f,
          text = text,
          title = title,
        )
      }
      is ComplicationType.MonochromaticImage,
      is ComplicationType.SmallImage, -> {
        val iconRes =
          when (timerState.phase) {
            TimerPhase.Stopped,
            TimerPhase.Paused -> android.R.drawable.ic_media_play
            TimerPhase.Running -> android.R.drawable.ic_media_pause
            TimerPhase.Resting -> android.R.drawable.ic_media_next
            TimerPhase.AlarmActive -> android.R.drawable.ic_delete
          }
        val description =
          when (timerState.phase) {
            TimerPhase.Stopped -> "Start timer"
            TimerPhase.Paused -> "Resume timer"
            TimerPhase.Running -> "Pause timer"
            TimerPhase.Resting -> "Skip rest"
            TimerPhase.AlarmActive -> "Dismiss alarm"
          }

        ComplicationData.Image(iconRes, description)
      }
    }
  }
}
