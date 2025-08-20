package com.wearinterval.data.database

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class TimerConfigurationEntityTest {

  @Test
  fun `toDomain converts entity to domain model correctly`() {
    // Given
    val entity =
      TimerConfigurationEntity(
        id = "test-id",
        laps = 10,
        workDurationSeconds = 90,
        restDurationSeconds = 30,
        lastUsed = 1234567890L,
      )

    // When
    val domainModel = entity.toDomain()

    // Then
    assertThat(domainModel.id).isEqualTo("test-id")
    assertThat(domainModel.laps).isEqualTo(10)
    assertThat(domainModel.workDuration).isEqualTo(90.seconds)
    assertThat(domainModel.restDuration).isEqualTo(30.seconds)
    assertThat(domainModel.lastUsed).isEqualTo(Instant.ofEpochMilli(1234567890L))
  }

  @Test
  fun `fromDomain converts domain model to entity correctly`() {
    // Given
    val domainModel =
      TimerConfiguration(
        id = "domain-id",
        laps = 5,
        workDuration = 2.minutes,
        restDuration = 45.seconds,
        lastUsed = Instant.ofEpochMilli(9876543210L),
      )

    // When
    val entity = TimerConfigurationEntity.fromDomain(domainModel)

    // Then
    assertThat(entity.id).isEqualTo("domain-id")
    assertThat(entity.laps).isEqualTo(5)
    assertThat(entity.workDurationSeconds).isEqualTo(120)
    assertThat(entity.restDurationSeconds).isEqualTo(45)
    assertThat(entity.lastUsed).isEqualTo(9876543210L)
  }

  @Test
  fun `roundtrip conversion preserves data integrity`() {
    // Given
    val original =
      TimerConfiguration(
        id = "roundtrip-test",
        laps = 15,
        workDuration = 3.minutes + 30.seconds,
        restDuration = 1.minutes + 15.seconds,
        lastUsed = Instant.ofEpochMilli(1111111111L),
      )

    // When
    val entity = TimerConfigurationEntity.fromDomain(original)
    val converted = entity.toDomain()

    // Then
    assertThat(converted).isEqualTo(original)
  }

  @Test
  fun `handles zero rest duration correctly`() {
    // Given
    val domainModel =
      TimerConfiguration(
        id = "zero-rest",
        laps = 1,
        workDuration = 60.seconds,
        restDuration = 0.seconds,
        lastUsed = Instant.ofEpochMilli(1000L),
      )

    // When
    val entity = TimerConfigurationEntity.fromDomain(domainModel)
    val converted = entity.toDomain()

    // Then
    assertThat(entity.restDurationSeconds).isEqualTo(0)
    assertThat(converted.restDuration).isEqualTo(0.seconds)
  }

  @Test
  fun `handles large duration values correctly`() {
    // Given
    val domainModel =
      TimerConfiguration(
        id = "large-durations",
        laps = 999,
        workDuration = 10.minutes,
        restDuration = 10.minutes,
        lastUsed = Instant.ofEpochMilli(Long.MAX_VALUE),
      )

    // When
    val entity = TimerConfigurationEntity.fromDomain(domainModel)
    val converted = entity.toDomain()

    // Then
    assertThat(converted).isEqualTo(domainModel)
  }

  @Test
  fun `entity constructor creates valid entity`() {
    // When
    val entity =
      TimerConfigurationEntity(
        id = "constructor-test",
        laps = 8,
        workDurationSeconds = 45,
        restDurationSeconds = 15,
        lastUsed = 2000L,
      )

    // Then
    assertThat(entity.id).isNotEmpty()
    assertThat(entity.laps).isGreaterThan(0)
    assertThat(entity.workDurationSeconds).isAtLeast(0)
    assertThat(entity.restDurationSeconds).isAtLeast(0)
    assertThat(entity.lastUsed).isAtLeast(0)
  }
}
