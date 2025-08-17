package com.wearinterval.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wearinterval.domain.model.TimerConfiguration
import kotlin.time.Duration.Companion.seconds

@Entity(tableName = "timer_configurations")
data class TimerConfigurationEntity(
  @PrimaryKey val id: String,
  val laps: Int,
  val workDurationSeconds: Long,
  val restDurationSeconds: Long,
  val lastUsed: Long,
) {
  fun toDomain(): TimerConfiguration {
    return TimerConfiguration(
      id = id,
      laps = laps,
      workDuration = workDurationSeconds.seconds,
      restDuration = restDurationSeconds.seconds,
      lastUsed = lastUsed,
    )
  }

  companion object {
    fun fromDomain(config: TimerConfiguration): TimerConfigurationEntity {
      return TimerConfigurationEntity(
        id = config.id,
        laps = config.laps,
        workDurationSeconds = config.workDuration.inWholeSeconds,
        restDurationSeconds = config.restDuration.inWholeSeconds,
        lastUsed = config.lastUsed,
      )
    }
  }
}
