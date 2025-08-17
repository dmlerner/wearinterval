package com.wearinterval.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigurationDao {

  @Query("SELECT * FROM timer_configurations ORDER BY lastUsed DESC LIMIT :limit")
  suspend fun getRecentConfigurations(limit: Int): List<TimerConfigurationEntity>

  @Query("SELECT * FROM timer_configurations ORDER BY lastUsed DESC LIMIT :limit")
  fun getRecentConfigurationsFlow(limit: Int): Flow<List<TimerConfigurationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConfiguration(config: TimerConfigurationEntity)

  @Query("DELETE FROM timer_configurations WHERE id = :id")
  suspend fun deleteConfiguration(id: String)

  @Query("UPDATE timer_configurations SET lastUsed = :timestamp WHERE id = :id")
  suspend fun updateLastUsed(id: String, timestamp: Long)

  @Query("SELECT COUNT(*) FROM timer_configurations") suspend fun getConfigurationCount(): Int

  @Query(
    "DELETE FROM timer_configurations WHERE id NOT IN (SELECT id FROM timer_configurations ORDER BY lastUsed DESC LIMIT :keepCount)"
  )
  suspend fun cleanupOldConfigurations(keepCount: Int)

  @Query("SELECT * FROM timer_configurations WHERE id = :id")
  suspend fun getConfigurationById(id: String): TimerConfigurationEntity?

  @Query(
    "SELECT * FROM timer_configurations WHERE laps = :laps AND " +
      "workDurationSeconds = :workDurationSeconds AND restDurationSeconds = :restDurationSeconds LIMIT 1",
  )
  suspend fun findConfigurationByValues(
    laps: Int,
    workDurationSeconds: Long,
    restDurationSeconds: Long
  ): TimerConfigurationEntity?
}
