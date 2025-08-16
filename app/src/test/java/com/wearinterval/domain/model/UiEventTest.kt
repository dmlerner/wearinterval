package com.wearinterval.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UiEventTest {

    @Test
    fun `Timer events are properly sealed`() {
        val playPause = UiEvent.Timer.PlayPause
        val stop = UiEvent.Timer.Stop
        val dismissAlarm = UiEvent.Timer.DismissAlarm

        assertThat(playPause).isInstanceOf(UiEvent.Timer::class.java)
        assertThat(stop).isInstanceOf(UiEvent.Timer::class.java)
        assertThat(dismissAlarm).isInstanceOf(UiEvent.Timer::class.java)

        assertThat(playPause).isInstanceOf(UiEvent::class.java)
        assertThat(stop).isInstanceOf(UiEvent::class.java)
        assertThat(dismissAlarm).isInstanceOf(UiEvent::class.java)
    }

    @Test
    fun `Configuration events carry proper data`() {
        val updateLaps = UiEvent.Configuration.UpdateLaps(10)
        val updateWork = UiEvent.Configuration.UpdateWorkDuration(45.seconds)
        val updateRest = UiEvent.Configuration.UpdateRestDuration(15.seconds)
        val resetDefaults = UiEvent.Configuration.ResetToDefaults

        assertThat(updateLaps.laps).isEqualTo(10)
        assertThat(updateWork.duration).isEqualTo(45.seconds)
        assertThat(updateRest.duration).isEqualTo(15.seconds)
        assertThat(resetDefaults).isInstanceOf(UiEvent.Configuration::class.java)
    }

    @Test
    fun `Configuration SelectConfiguration event contains configuration`() {
        val config = TimerConfiguration(
            laps = 5,
            workDuration = 30.seconds,
            restDuration = 10.seconds,
        )

        val selectEvent = UiEvent.Configuration.SelectConfiguration(config)

        assertThat(selectEvent.configuration).isEqualTo(config)
        assertThat(selectEvent).isInstanceOf(UiEvent.Configuration::class.java)
    }

    @Test
    fun `Settings events carry boolean states`() {
        val toggleVibration = UiEvent.Settings.ToggleVibration(true)
        val toggleSound = UiEvent.Settings.ToggleSound(false)
        val toggleFlash = UiEvent.Settings.ToggleFlash(true)
        val toggleAutoMode = UiEvent.Settings.ToggleAutoMode(false)

        assertThat(toggleVibration.enabled).isTrue()
        assertThat(toggleSound.enabled).isFalse()
        assertThat(toggleFlash.enabled).isTrue()
        assertThat(toggleAutoMode.enabled).isFalse()
    }

    @Test
    fun `Navigation events are object instances`() {
        val navigateToConfig = UiEvent.Navigation.NavigateToConfig
        val navigateToHistory = UiEvent.Navigation.NavigateToHistory
        val navigateToSettings = UiEvent.Navigation.NavigateToSettings
        val navigateBack = UiEvent.Navigation.NavigateBack

        // Test that they're singletons (same reference)
        assertThat(UiEvent.Navigation.NavigateToConfig).isSameInstanceAs(navigateToConfig)
        assertThat(UiEvent.Navigation.NavigateToHistory).isSameInstanceAs(navigateToHistory)
        assertThat(UiEvent.Navigation.NavigateToSettings).isSameInstanceAs(navigateToSettings)
        assertThat(UiEvent.Navigation.NavigateBack).isSameInstanceAs(navigateBack)
    }

    @Test
    fun `History events handle configuration data`() {
        val config = TimerConfiguration(
            laps = 3,
            workDuration = 2.minutes,
            restDuration = 30.seconds,
        )

        val selectItem = UiEvent.History.SelectHistoryItem(config)
        val deleteItem = UiEvent.History.DeleteHistoryItem("test-id")
        val clearHistory = UiEvent.History.ClearHistory

        assertThat(selectItem.configuration).isEqualTo(config)
        assertThat(deleteItem.configurationId).isEqualTo("test-id")
        assertThat(clearHistory).isInstanceOf(UiEvent.History::class.java)
    }

    @Test
    fun `System events handle different data types`() {
        val refreshData = UiEvent.System.RefreshData
        val handlePermission = UiEvent.System.HandlePermissionResult
        val testError = RuntimeException("Test error")
        val handleError = UiEvent.System.HandleError(testError)

        assertThat(refreshData).isInstanceOf(UiEvent.System::class.java)
        assertThat(handlePermission).isInstanceOf(UiEvent.System::class.java)
        assertThat(handleError.throwable).isEqualTo(testError)
    }

    @Test
    fun `Events can be compared for equality`() {
        val updateLaps1 = UiEvent.Configuration.UpdateLaps(5)
        val updateLaps2 = UiEvent.Configuration.UpdateLaps(5)
        val updateLaps3 = UiEvent.Configuration.UpdateLaps(10)

        assertThat(updateLaps1).isEqualTo(updateLaps2)
        assertThat(updateLaps1).isNotEqualTo(updateLaps3)

        val playPause1 = UiEvent.Timer.PlayPause
        val playPause2 = UiEvent.Timer.PlayPause
        val stop = UiEvent.Timer.Stop

        assertThat(playPause1).isEqualTo(playPause2)
        assertThat(playPause1).isNotEqualTo(stop)
    }

    @Test
    fun `Events have proper hierarchy structure`() {
        val allEvents = listOf(
            UiEvent.Timer.PlayPause,
            UiEvent.Configuration.UpdateLaps(1),
            UiEvent.Settings.ToggleVibration(true),
            UiEvent.Navigation.NavigateBack,
            UiEvent.History.ClearHistory,
            UiEvent.System.RefreshData,
        )

        allEvents.forEach { event ->
            assertThat(event).isInstanceOf(UiEvent::class.java)
        }
    }

    @Test
    fun `Event data classes can be copied with changes`() {
        val original = UiEvent.Configuration.UpdateLaps(5)
        val modified = original.copy(laps = 10)

        assertThat(original.laps).isEqualTo(5)
        assertThat(modified.laps).isEqualTo(10)
    }

    @Test
    fun `Events can be used in when expressions`() {
        val events = listOf<UiEvent>(
            UiEvent.Timer.PlayPause,
            UiEvent.Configuration.UpdateLaps(5),
            UiEvent.Settings.ToggleVibration(true),
            UiEvent.Navigation.NavigateBack,
        )

        events.forEach { event ->
            val result = when (event) {
                is UiEvent.Timer -> "timer"
                is UiEvent.Configuration -> "config"
                is UiEvent.Settings -> "settings"
                is UiEvent.Navigation -> "navigation"
                is UiEvent.History -> "history"
                is UiEvent.System -> "system"
            }

            assertThat(result).isNotEmpty()
        }
    }
}
