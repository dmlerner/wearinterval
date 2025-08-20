package com.wearinterval.wearos.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.wearinterval.MainActivity
import com.wearinterval.R
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.TimerRepository

/**
 * Feature-rich Wear OS Complication Service for WearInterval timer application. Supports all
 * complication types with dense, useful information and tap actions.
 */
class WearIntervalComplicationService : ComplicationDataSourceService() {

  private lateinit var timerRepository: TimerRepository

  companion object {
    private const val TAG = "WearComplication"
  }

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "=== WearIntervalComplicationService onCreate() ===")
    Log.d(TAG, "Package: ${packageName}")
    Log.d(TAG, "Service: ${this.javaClass.simpleName}")

    // Skip complex DI for now - just test service discovery
    Log.d(TAG, "Complication service created successfully - no DI for testing")
  }

  override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
    super.onComplicationActivated(complicationInstanceId, type)
    Log.d(TAG, "=== Complication ACTIVATED: id=$complicationInstanceId, type=$type ===")
  }

  override fun onComplicationDeactivated(complicationInstanceId: Int) {
    super.onComplicationDeactivated(complicationInstanceId)
    Log.d(TAG, "=== Complication DEACTIVATED: id=$complicationInstanceId ===")
  }

  override fun onComplicationRequest(
    request: ComplicationRequest,
    callback: ComplicationRequestListener
  ) {
    Log.d(TAG, "=== Complication REQUEST: ${request.complicationType} ===")

    try {
      // Simple fallback data to test service registration
      val fallbackData = createFallbackData()
      Log.d(TAG, "SUCCESS: Returning ${fallbackData.javaClass.simpleName}")
      callback.onComplicationData(fallbackData)
    } catch (e: Exception) {
      Log.e(TAG, "ERROR in complication request", e)
      callback.onComplicationData(createFallbackData())
    }
  }

  private fun createFallbackData(): ComplicationData {
    return ShortTextComplicationData.Builder(
        text = PlainComplicationText.Builder("FULL").build(),
        contentDescription = PlainComplicationText.Builder("Full Timer Service").build()
      )
      .build()
  }

  private fun createTapAction(): PendingIntent {
    val intent = Intent(this, MainActivity::class.java)
    return PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun createShortTextComplication(
    timerState: TimerState,
    tapAction: PendingIntent
  ): ComplicationData {
    val timeText = formatTimeRemaining(timerState)
    val statusText = formatTimerStatus(timerState)

    return ShortTextComplicationData.Builder(
        text = PlainComplicationText.Builder(timeText).build(),
        contentDescription = PlainComplicationText.Builder("Timer: $timeText, $statusText").build()
      )
      .setTitle(PlainComplicationText.Builder(statusText).build())
      .setTapAction(tapAction)
      .build()
  }

  private fun createLongTextComplication(
    timerState: TimerState,
    tapAction: PendingIntent
  ): ComplicationData {
    val timeText = formatTimeRemaining(timerState)
    val lapInfo = timerState.displayCurrentLap
    val phaseText = if (timerState.isResting) "Rest" else "Work"
    val fullText = "$timeText • $phaseText • Lap $lapInfo"

    return LongTextComplicationData.Builder(
        text = PlainComplicationText.Builder(fullText).build(),
        contentDescription = PlainComplicationText.Builder("WearInterval Timer: $fullText").build()
      )
      .setTitle(PlainComplicationText.Builder("WearInterval").build())
      .setTapAction(tapAction)
      .build()
  }

  private fun createRangedValueComplication(
    timerState: TimerState,
    tapAction: PendingIntent
  ): ComplicationData {
    val progress = calculateProgress(timerState)
    val timeText = formatTimeRemaining(timerState)
    val statusText = formatTimerStatus(timerState)

    return RangedValueComplicationData.Builder(
        value = progress.toFloat(),
        min = 0f,
        max = 100f,
        contentDescription = PlainComplicationText.Builder("Timer progress: ${progress}%").build()
      )
      .setText(PlainComplicationText.Builder(timeText).build())
      .setTitle(PlainComplicationText.Builder(statusText).build())
      .setTapAction(tapAction)
      .build()
  }

  private fun createSmallImageComplication(
    timerState: TimerState,
    tapAction: PendingIntent
  ): ComplicationData {
    val iconRes =
      when (timerState.phase) {
        is TimerPhase.Running,
        is TimerPhase.Resting -> R.drawable.ic_pause
        is TimerPhase.Paused -> R.drawable.ic_play_arrow
        else -> R.drawable.ic_timer
      }

    return SmallImageComplicationData.Builder(
        smallImage =
          SmallImage.Builder(
              image = Icon.createWithResource(this, iconRes),
              type = SmallImageType.ICON
            )
            .build(),
        contentDescription = PlainComplicationText.Builder(formatTimerStatus(timerState)).build()
      )
      .setTapAction(tapAction)
      .build()
  }

  private fun createMonochromaticImageComplication(
    timerState: TimerState,
    tapAction: PendingIntent
  ): ComplicationData {
    val iconRes =
      when (timerState.phase) {
        is TimerPhase.Running,
        is TimerPhase.Resting -> R.drawable.ic_pause
        is TimerPhase.Paused -> R.drawable.ic_play_arrow
        else -> R.drawable.ic_timer
      }

    return MonochromaticImageComplicationData.Builder(
        monochromaticImage =
          MonochromaticImage.Builder(image = Icon.createWithResource(this, iconRes)).build(),
        contentDescription = PlainComplicationText.Builder(formatTimerStatus(timerState)).build()
      )
      .setTapAction(tapAction)
      .build()
  }

  private fun formatTimeRemaining(timerState: TimerState): String {
    val totalSeconds = timerState.timeRemaining.inWholeSeconds
    return if (totalSeconds >= 60) {
      "${totalSeconds / 60}:${String.format("%02d", totalSeconds % 60)}"
    } else {
      "${totalSeconds}s"
    }
  }

  private fun formatTimerStatus(timerState: TimerState): String {
    return when (timerState.phase) {
      is TimerPhase.Stopped -> "Ready"
      is TimerPhase.Running -> "Work"
      is TimerPhase.Resting -> "Rest"
      is TimerPhase.Paused -> "Paused"
      is TimerPhase.AlarmActive -> "Done"
    }
  }

  private fun calculateProgress(timerState: TimerState): Int {
    return (timerState.progressPercentage * 100).toInt().coerceIn(0, 100)
  }

  override fun getPreviewData(type: ComplicationType): ComplicationData {
    val previewTapAction = createTapAction()

    return when (type) {
      ComplicationType.SHORT_TEXT -> {
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("45s").build(),
            contentDescription =
              PlainComplicationText.Builder("Timer Preview: 45 seconds remaining").build()
          )
          .setTitle(PlainComplicationText.Builder("Work").build())
          .setTapAction(previewTapAction)
          .build()
      }
      ComplicationType.LONG_TEXT -> {
        LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder("45s • Work • Lap 3/20").build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer Preview").build()
          )
          .setTitle(PlainComplicationText.Builder("WearInterval").build())
          .setTapAction(previewTapAction)
          .build()
      }
      ComplicationType.RANGED_VALUE -> {
        RangedValueComplicationData.Builder(
            value = 75f,
            min = 0f,
            max = 100f,
            contentDescription = PlainComplicationText.Builder("Timer progress: 75%").build()
          )
          .setText(PlainComplicationText.Builder("45s").build())
          .setTitle(PlainComplicationText.Builder("Work").build())
          .setTapAction(previewTapAction)
          .build()
      }
      ComplicationType.SMALL_IMAGE -> {
        SmallImageComplicationData.Builder(
            smallImage =
              SmallImage.Builder(
                  image = Icon.createWithResource(this, R.drawable.ic_timer),
                  type = SmallImageType.ICON
                )
                .build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(previewTapAction)
          .build()
      }
      ComplicationType.MONOCHROMATIC_IMAGE -> {
        MonochromaticImageComplicationData.Builder(
            monochromaticImage =
              MonochromaticImage.Builder(image = Icon.createWithResource(this, R.drawable.ic_timer))
                .build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(previewTapAction)
          .build()
      }
      else -> {
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("Timer").build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(previewTapAction)
          .build()
      }
    }
  }
}
