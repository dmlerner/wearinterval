package com.wearinterval.wearos.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.wearinterval.MainActivity
import com.wearinterval.R
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Working complication service with Hilt EntryPoint injection Demonstrates proper Hilt integration
 * for ComplicationDataSourceService
 */
class IsolatedComplicationService : ComplicationDataSourceService() {

  companion object {
    private const val TAG = "IsolatedComplication"
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface ComplicationServiceEntryPoint {
    fun getTimerRepository(): TimerRepository

    fun getConfigurationRepository(): ConfigurationRepository
  }

  private lateinit var timerRepository: TimerRepository
  private lateinit var configRepository: ConfigurationRepository
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private lateinit var updateRequester: ComplicationDataSourceUpdateRequester

  override fun onCreate() {
    super.onCreate()
    android.util.Log.e(TAG, "=== IsolatedComplicationService onCreate() ===")

    try {
      // Use Hilt EntryPoint to get dependencies
      val entryPoint =
        EntryPointAccessors.fromApplication(
          applicationContext,
          ComplicationServiceEntryPoint::class.java
        )

      timerRepository = entryPoint.getTimerRepository()
      configRepository = entryPoint.getConfigurationRepository()
      updateRequester =
        ComplicationDataSourceUpdateRequester.create(
          applicationContext,
          ComponentName(applicationContext, IsolatedComplicationService::class.java)
        )

      // Observe timer state changes and push updates to complications
      observeTimerStateChanges()

      android.util.Log.e(TAG, "Hilt EntryPoint injection successful")
    } catch (e: Exception) {
      android.util.Log.e(TAG, "Failed to initialize Hilt dependencies", e)
    }
  }

  override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
    super.onComplicationActivated(complicationInstanceId, type)
    Log.d(TAG, "=== Complication ACTIVATED: id=$complicationInstanceId, type=$type ===")
  }

  override fun onComplicationDeactivated(complicationInstanceId: Int) {
    super.onComplicationDeactivated(complicationInstanceId)
    Log.d(TAG, "=== Complication DEACTIVATED: id=$complicationInstanceId ===")
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    Log.d(TAG, "=== IsolatedComplicationService destroyed ===")
  }

  override fun onComplicationRequest(
    request: ComplicationRequest,
    callback: ComplicationRequestListener
  ) {
    android.util.Log.e(TAG, "=== Complication REQUEST: ${request.complicationType} ===")

    if (!::timerRepository.isInitialized) {
      android.util.Log.e(TAG, "Timer repository not initialized - using fallback")
      callback.onComplicationData(createFallbackData(request.complicationType))
      return
    }

    // Get timer data asynchronously using Hilt-injected repositories
    serviceScope.launch {
      try {
        val timerState = timerRepository.timerState.first()
        val config = configRepository.currentConfiguration.first()

        android.util.Log.e(
          TAG,
          "Timer state: phase=${timerState.phase}, timeRemaining=${timerState.timeRemaining.inWholeSeconds}s, lap=${timerState.currentLap}/${timerState.totalLaps}"
        )
        android.util.Log.e(
          TAG,
          "Config: work=${config.workDuration.inWholeSeconds}s, rest=${config.restDuration.inWholeSeconds}s"
        )

        val complicationData =
          when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createShortTextComplication(timerState)
            ComplicationType.LONG_TEXT -> createLongTextComplication(timerState, config)
            ComplicationType.RANGED_VALUE -> createRangedValueComplication(timerState)
            ComplicationType.SMALL_IMAGE -> createSmallImageComplication()
            ComplicationType.MONOCHROMATIC_IMAGE -> createMonochromaticImageComplication()
            else -> createShortTextComplication(timerState)
          }

        android.util.Log.e(TAG, "SUCCESS: Returning ${complicationData.javaClass.simpleName}")
        callback.onComplicationData(complicationData)
      } catch (e: Exception) {
        android.util.Log.e(TAG, "ERROR fetching timer data", e)
        callback.onComplicationData(createFallbackData(request.complicationType))
      }
    }
  }

  private fun observeTimerStateChanges() {
    serviceScope.launch {
      try {
        timerRepository.timerState.collect { timerState ->
          Log.d(
            TAG,
            "Timer state changed: phase=${timerState.phase}, remaining=${timerState.timeRemaining.inWholeSeconds}s"
          )
          // Request complication updates when timer state changes
          updateRequester.requestUpdateAll()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error observing timer state changes", e)
      }
    }
  }

  private fun createShortTextComplication(
    timerState: com.wearinterval.domain.model.TimerState
  ): ComplicationData {
    return when (timerState.phase) {
      is com.wearinterval.domain.model.TimerPhase.Stopped -> {
        // When stopped, show just tiny timer icon
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("⏱").build(),
            contentDescription = PlainComplicationText.Builder("Timer ready").build()
          )
          .setTapAction(createTapAction())
          .build()
      }
      else -> {
        // When running, show time and status
        val timeText = formatTimeRemaining(timerState.timeRemaining.inWholeSeconds)
        val statusText =
          when (timerState.phase) {
            is com.wearinterval.domain.model.TimerPhase.Running -> "Work"
            is com.wearinterval.domain.model.TimerPhase.Resting -> "Rest"
            is com.wearinterval.domain.model.TimerPhase.Paused -> "Paused"
            is com.wearinterval.domain.model.TimerPhase.AlarmActive -> "Done"
            else -> "Ready"
          }

        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(timeText).build(),
            contentDescription =
              PlainComplicationText.Builder("Timer: $timeText $statusText").build()
          )
          .setTitle(PlainComplicationText.Builder(statusText).build())
          .setTapAction(createTapAction())
          .build()
      }
    }
  }

  private fun createLongTextComplication(
    timerState: com.wearinterval.domain.model.TimerState,
    config: com.wearinterval.domain.model.TimerConfiguration
  ): ComplicationData {
    return when (timerState.phase) {
      is com.wearinterval.domain.model.TimerPhase.Stopped -> {
        // When stopped, show minimal content
        LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder("WearInterval Timer").build(),
            contentDescription = PlainComplicationText.Builder("Timer ready").build()
          )
          .setTapAction(createTapAction())
          .build()
      }
      else -> {
        // When running, show full details
        val timeText = formatTimeRemaining(timerState.timeRemaining.inWholeSeconds)
        val lapText = timerState.displayCurrentLap
        val statusText =
          when (timerState.phase) {
            is com.wearinterval.domain.model.TimerPhase.Running -> "Work"
            is com.wearinterval.domain.model.TimerPhase.Resting -> "Rest"
            else -> "Ready"
          }

        val fullText = "$timeText • $statusText • $lapText"

        LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(fullText).build(),
            contentDescription = PlainComplicationText.Builder("WearInterval: $fullText").build()
          )
          .setTitle(PlainComplicationText.Builder("Timer").build())
          .setTapAction(createTapAction())
          .build()
      }
    }
  }

  private fun createRangedValueComplication(
    timerState: com.wearinterval.domain.model.TimerState
  ): ComplicationData {
    return when (timerState.phase) {
      is com.wearinterval.domain.model.TimerPhase.Stopped -> {
        // When stopped, show empty text but keep tap action
        RangedValueComplicationData.Builder(
            value = 0f,
            min = 0f,
            max = 100f,
            contentDescription = PlainComplicationText.Builder("Timer ready").build()
          )
          .setText(PlainComplicationText.Builder("").build()) // Empty string for invisible text
          .setTapAction(createTapAction())
          .build()
      }
      else -> {
        // When running, show progress and time
        val progressPercent = (timerState.progressPercentage * 100).toInt().coerceIn(0, 100)
        val timeText = formatTimeRemaining(timerState.timeRemaining.inWholeSeconds)
        val statusText =
          when (timerState.phase) {
            is com.wearinterval.domain.model.TimerPhase.Running -> "Work"
            is com.wearinterval.domain.model.TimerPhase.Resting -> "Rest"
            else -> "Ready"
          }

        RangedValueComplicationData.Builder(
            value = progressPercent.toFloat(),
            min = 0f,
            max = 100f,
            contentDescription =
              PlainComplicationText.Builder("Timer progress: $progressPercent%").build()
          )
          .setText(PlainComplicationText.Builder(timeText).build())
          .setTitle(PlainComplicationText.Builder(statusText).build())
          .setTapAction(createTapAction())
          .build()
      }
    }
  }

  private fun formatTimeRemaining(totalSeconds: Long): String {
    return if (totalSeconds >= 60) {
      "${totalSeconds / 60}:${String.format("%02d", totalSeconds % 60)}"
    } else {
      "${totalSeconds}s"
    }
  }

  private fun createTapAction(): PendingIntent {
    val intent =
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    return PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun createSmallImageComplication(): ComplicationData {
    return SmallImageComplicationData.Builder(
        smallImage =
          SmallImage.Builder(
              image = Icon.createWithResource(this, R.drawable.ic_timer),
              type = SmallImageType.ICON
            )
            .build(),
        contentDescription = PlainComplicationText.Builder("WearInterval Timer - Ready").build()
      )
      .setTapAction(createTapAction())
      .build()
  }

  private fun createMonochromaticImageComplication(): ComplicationData {
    return MonochromaticImageComplicationData.Builder(
        monochromaticImage =
          MonochromaticImage.Builder(image = Icon.createWithResource(this, R.drawable.ic_timer))
            .build(),
        contentDescription = PlainComplicationText.Builder("WearInterval Timer - Ready").build()
      )
      .setTapAction(createTapAction())
      .build()
  }

  private fun createFallbackData(type: ComplicationType): ComplicationData {
    return when (type) {
      ComplicationType.SHORT_TEXT ->
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("WI").build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(createTapAction())
          .build()
      ComplicationType.LONG_TEXT ->
        LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder("WearInterval Timer").build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(createTapAction())
          .build()
      ComplicationType.RANGED_VALUE ->
        // When stopped/fallback, show empty text but keep tap action
        RangedValueComplicationData.Builder(
            value = 0f,
            min = 0f,
            max = 100f,
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setText(PlainComplicationText.Builder("").build()) // Empty string for invisible text
          .setTapAction(createTapAction())
          .build()
      ComplicationType.SMALL_IMAGE ->
        SmallImageComplicationData.Builder(
            smallImage =
              SmallImage.Builder(
                  image = Icon.createWithResource(this, R.drawable.ic_timer),
                  type = SmallImageType.ICON
                )
                .build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(createTapAction())
          .build()
      ComplicationType.MONOCHROMATIC_IMAGE ->
        MonochromaticImageComplicationData.Builder(
            monochromaticImage =
              MonochromaticImage.Builder(image = Icon.createWithResource(this, R.drawable.ic_timer))
                .build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(createTapAction())
          .build()

      // Exhaustive when - compiler ensures all types are handled
      else ->
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("WI").build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(createTapAction())
          .build()
    }
  }

  override fun getPreviewData(type: ComplicationType): ComplicationData {
    return when (type) {
      ComplicationType.SHORT_TEXT ->
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("45s").build(),
            contentDescription = PlainComplicationText.Builder("Timer Preview").build()
          )
          .setTitle(PlainComplicationText.Builder("Work").build())
          .setTapAction(createTapAction())
          .build()
      ComplicationType.LONG_TEXT ->
        LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder("45s • Work • 3/10").build(),
            contentDescription = PlainComplicationText.Builder("Timer Preview").build()
          )
          .setTitle(PlainComplicationText.Builder("Timer").build())
          .setTapAction(createTapAction())
          .build()
      ComplicationType.RANGED_VALUE ->
        RangedValueComplicationData.Builder(
            value = 75f,
            min = 0f,
            max = 100f,
            contentDescription = PlainComplicationText.Builder("Timer progress: 75%").build()
          )
          .setText(PlainComplicationText.Builder("45s").build())
          .setTitle(PlainComplicationText.Builder("Work").build())
          .setTapAction(createTapAction())
          .build()
      ComplicationType.SMALL_IMAGE ->
        SmallImageComplicationData.Builder(
            smallImage =
              SmallImage.Builder(
                  image = Icon.createWithResource(this, R.drawable.ic_timer),
                  type = SmallImageType.ICON
                )
                .build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer Preview").build()
          )
          .setTapAction(createTapAction())
          .build()
      ComplicationType.MONOCHROMATIC_IMAGE ->
        MonochromaticImageComplicationData.Builder(
            monochromaticImage =
              MonochromaticImage.Builder(image = Icon.createWithResource(this, R.drawable.ic_timer))
                .build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer Preview").build()
          )
          .setTapAction(createTapAction())
          .build()
      else ->
        SmallImageComplicationData.Builder(
            smallImage =
              SmallImage.Builder(
                  image = Icon.createWithResource(this, R.drawable.ic_timer),
                  type = SmallImageType.ICON
                )
                .build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .setTapAction(createTapAction())
          .build()
    }
  }
}
