package com.wearinterval.wearos.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.wearinterval.MainActivity
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
    Log.d(TAG, "=== IsolatedComplicationService onCreate() ===")

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

      Log.d(TAG, "Hilt EntryPoint injection successful")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize Hilt dependencies", e)
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
    Log.d(TAG, "=== Complication REQUEST: ${request.complicationType} ===")

    if (!::timerRepository.isInitialized) {
      Log.w(TAG, "Timer repository not initialized - using fallback")
      callback.onComplicationData(createFallbackData())
      return
    }

    // Get timer data asynchronously using Hilt-injected repositories
    serviceScope.launch {
      try {
        val timerState = timerRepository.timerState.first()
        val config = configRepository.currentConfiguration.first()

        Log.d(
          TAG,
          "Timer state: phase=${timerState.phase}, timeRemaining=${timerState.timeRemaining.inWholeSeconds}s, lap=${timerState.currentLap}/${timerState.totalLaps}"
        )
        Log.d(
          TAG,
          "Config: work=${config.workDuration.inWholeSeconds}s, rest=${config.restDuration.inWholeSeconds}s"
        )

        val complicationData =
          when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createShortTextComplication(timerState)
            ComplicationType.LONG_TEXT -> createLongTextComplication(timerState, config)
            ComplicationType.RANGED_VALUE -> createRangedValueComplication(timerState)
            else -> createShortTextComplication(timerState)
          }

        Log.d(TAG, "SUCCESS: Returning real timer complication data via Hilt")
        callback.onComplicationData(complicationData)
      } catch (e: Exception) {
        Log.e(TAG, "ERROR fetching timer data", e)
        callback.onComplicationData(createFallbackData())
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
    val timeText = formatTimeRemaining(timerState.timeRemaining.inWholeSeconds)
    val statusText =
      when (timerState.phase) {
        is com.wearinterval.domain.model.TimerPhase.Running -> "Work"
        is com.wearinterval.domain.model.TimerPhase.Resting -> "Rest"
        is com.wearinterval.domain.model.TimerPhase.Paused -> "Paused"
        is com.wearinterval.domain.model.TimerPhase.AlarmActive -> "Done"
        else -> "Ready"
      }

    return ShortTextComplicationData.Builder(
        text = PlainComplicationText.Builder(timeText).build(),
        contentDescription = PlainComplicationText.Builder("Timer: $timeText $statusText").build()
      )
      .setTitle(PlainComplicationText.Builder(statusText).build())
      .setTapAction(createTapAction())
      .build()
  }

  private fun createLongTextComplication(
    timerState: com.wearinterval.domain.model.TimerState,
    config: com.wearinterval.domain.model.TimerConfiguration
  ): ComplicationData {
    val timeText = formatTimeRemaining(timerState.timeRemaining.inWholeSeconds)
    val lapText = timerState.displayCurrentLap
    val statusText =
      when (timerState.phase) {
        is com.wearinterval.domain.model.TimerPhase.Running -> "Work"
        is com.wearinterval.domain.model.TimerPhase.Resting -> "Rest"
        else -> "Ready"
      }

    val fullText = "$timeText • $statusText • $lapText"

    return LongTextComplicationData.Builder(
        text = PlainComplicationText.Builder(fullText).build(),
        contentDescription = PlainComplicationText.Builder("WearInterval: $fullText").build()
      )
      .setTitle(PlainComplicationText.Builder("Timer").build())
      .setTapAction(createTapAction())
      .build()
  }

  private fun createRangedValueComplication(
    timerState: com.wearinterval.domain.model.TimerState
  ): ComplicationData {
    val progressPercent = (timerState.progressPercentage * 100).toInt().coerceIn(0, 100)
    val timeText = formatTimeRemaining(timerState.timeRemaining.inWholeSeconds)
    val statusText =
      when (timerState.phase) {
        is com.wearinterval.domain.model.TimerPhase.Running -> "Work"
        is com.wearinterval.domain.model.TimerPhase.Resting -> "Rest"
        else -> "Ready"
      }

    return RangedValueComplicationData.Builder(
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

  private fun createFallbackData(): ComplicationData {
    return ShortTextComplicationData.Builder(
        text = PlainComplicationText.Builder("WI").build(),
        contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
      )
      .setTapAction(createTapAction())
      .build()
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
      else ->
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("Timer").build(),
            contentDescription = PlainComplicationText.Builder("WearInterval Timer").build()
          )
          .build()
    }
  }
}
