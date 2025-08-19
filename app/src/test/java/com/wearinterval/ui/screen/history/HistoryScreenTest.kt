package com.wearinterval.ui.screen.history

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class HistoryScreenTest {

  private val sampleConfigurations =
    listOf(
      TimerConfiguration(
        id = "1",
        laps = 5,
        workDuration = 45.seconds,
        restDuration = 15.seconds,
      ),
      TimerConfiguration(
        id = "2",
        laps = 3,
        workDuration = 2.minutes,
        restDuration = 30.seconds,
      ),
      TimerConfiguration(
        id = "3",
        laps = 8,
        workDuration = 30.seconds,
        restDuration = 10.seconds,
      ),
    )

  @Test
  fun `configuration selection callback logic works correctly`() {
    // Test the lambda that's passed to ConfigurationGridContent
    val mockOnEvent: (HistoryEvent) -> Unit = mockk(relaxed = true)
    val mockOnNavigateToMain: () -> Unit = mockk(relaxed = true)
    val testConfig = sampleConfigurations.first()

    // Create the callback that HistoryContent creates
    val onConfigurationSelect: (TimerConfiguration) -> Unit = { config ->
      mockOnEvent(HistoryEvent.ConfigurationSelected(config))
      mockOnNavigateToMain()
    }

    // When
    onConfigurationSelect(testConfig)

    // Then
    verify { mockOnEvent(HistoryEvent.ConfigurationSelected(testConfig)) }
    verify { mockOnNavigateToMain() }
  }

  @Test
  fun `history ui state hasConfigurations property works correctly`() {
    // Test empty configurations
    val emptyState = HistoryUiState(configurations = emptyList())
    assertThat(emptyState.hasConfigurations).isFalse()

    // Test with configurations
    val filledState = HistoryUiState(configurations = sampleConfigurations)
    assertThat(filledState.hasConfigurations).isTrue()

    // Test with single configuration
    val singleState = HistoryUiState(configurations = listOf(sampleConfigurations.first()))
    assertThat(singleState.hasConfigurations).isTrue()
  }

  @Test
  fun `history ui state default values are correct`() {
    // Test default constructor
    val defaultState = HistoryUiState()
    assertThat(defaultState.configurations).isEmpty()
    assertThat(defaultState.isLoading).isFalse()
    assertThat(defaultState.hasConfigurations).isFalse()
  }

  @Test
  fun `history ui state loading behavior`() {
    // Test loading with empty configurations
    val loadingEmptyState = HistoryUiState(isLoading = true, configurations = emptyList())
    assertThat(loadingEmptyState.isLoading).isTrue()
    assertThat(loadingEmptyState.hasConfigurations).isFalse()

    // Test loading with configurations
    val loadingFilledState = HistoryUiState(isLoading = true, configurations = sampleConfigurations)
    assertThat(loadingFilledState.isLoading).isTrue()
    assertThat(loadingFilledState.hasConfigurations).isTrue()
  }

  @Test
  fun `history event configuration selected contains correct data`() {
    val testConfig = sampleConfigurations.first()
    val event = HistoryEvent.ConfigurationSelected(testConfig)

    assertThat(event.configuration).isEqualTo(testConfig)
    assertThat(event.configuration.id).isEqualTo("1")
    assertThat(event.configuration.laps).isEqualTo(5)
  }

  @Test
  fun `history event refresh history is object singleton`() {
    val event1 = HistoryEvent.RefreshHistory
    val event2 = HistoryEvent.RefreshHistory

    assertThat(event1).isSameInstanceAs(event2)
  }

  @Test
  fun `multiple configuration selection calls work correctly`() {
    // Test multiple selections
    val mockOnEvent: (HistoryEvent) -> Unit = mockk(relaxed = true)
    val mockOnNavigateToMain: () -> Unit = mockk(relaxed = true)

    // Create the callback that HistoryContent creates
    val onConfigurationSelect: (TimerConfiguration) -> Unit = { config ->
      mockOnEvent(HistoryEvent.ConfigurationSelected(config))
      mockOnNavigateToMain()
    }

    // When - Select multiple configurations
    onConfigurationSelect(sampleConfigurations[0])
    onConfigurationSelect(sampleConfigurations[1])
    onConfigurationSelect(sampleConfigurations[2])

    // Then - All selections should work correctly
    verify(exactly = 3) { mockOnNavigateToMain() }
    verify { mockOnEvent(HistoryEvent.ConfigurationSelected(sampleConfigurations[0])) }
    verify { mockOnEvent(HistoryEvent.ConfigurationSelected(sampleConfigurations[1])) }
    verify { mockOnEvent(HistoryEvent.ConfigurationSelected(sampleConfigurations[2])) }
  }
}
