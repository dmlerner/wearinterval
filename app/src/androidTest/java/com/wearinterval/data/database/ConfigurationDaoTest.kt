package com.wearinterval.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigurationDaoTest {

  private lateinit var database: AppDatabase
  private lateinit var dao: ConfigurationDao

  @Before
  fun setup() {
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          AppDatabase::class.java,
        )
        .allowMainThreadQueries()
        .build()

    dao = database.configurationDao()
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun insertAndRetrieveConfiguration() = runTest {
    // Given
    val config =
      TimerConfigurationEntity(
        id = "test-id",
        laps = 10,
        workDurationSeconds = 60,
        restDurationSeconds = 30,
        lastUsed = System.currentTimeMillis(),
      )

    // When
    dao.insertConfiguration(config)
    val retrieved = dao.getRecentConfigurations(1)

    // Then
    assertThat(retrieved).hasSize(1)
    assertThat(retrieved.first()).isEqualTo(config)
  }

  @Test
  fun getRecentConfigurationsOrdersByLastUsed() = runTest {
    // Given
    val config1 = TimerConfigurationEntity("id1", 5, 45, 15, 1000L)
    val config2 = TimerConfigurationEntity("id2", 8, 60, 20, 2000L)
    val config3 = TimerConfigurationEntity("id3", 3, 30, 10, 1500L)

    // When
    dao.insertConfiguration(config1)
    dao.insertConfiguration(config2)
    dao.insertConfiguration(config3)
    val retrieved = dao.getRecentConfigurations(3)

    // Then
    assertThat(retrieved).hasSize(3)
    assertThat(retrieved[0]).isEqualTo(config2) // Most recent (2000L)
    assertThat(retrieved[1]).isEqualTo(config3) // Middle (1500L)
    assertThat(retrieved[2]).isEqualTo(config1) // Oldest (1000L)
  }

  @Test
  fun getRecentConfigurationsRespectsLimit() = runTest {
    // Given
    val configs = (1..5).map { i -> TimerConfigurationEntity("id$i", i, 60, 30, i * 1000L) }

    // When
    configs.forEach { dao.insertConfiguration(it) }
    val retrieved = dao.getRecentConfigurations(3)

    // Then
    assertThat(retrieved).hasSize(3)
    // Should return the 3 most recent (highest lastUsed values)
    assertThat(retrieved.map { it.id }).containsExactly("id5", "id4", "id3").inOrder()
  }

  @Test
  fun getRecentConfigurationsFlowEmitsChanges() = runTest {
    // Given
    val config = TimerConfigurationEntity("flow-test", 1, 60, 0, 1000L)

    // When/Then
    dao.getRecentConfigurationsFlow(5).test {
      // Initially empty
      assertThat(awaitItem()).isEmpty()

      // Insert config
      dao.insertConfiguration(config)
      val items = awaitItem()
      assertThat(items).hasSize(1)
      assertThat(items.first()).isEqualTo(config)
    }
  }

  @Test
  fun insertWithSameIdReplacesExisting() = runTest {
    // Given
    val original = TimerConfigurationEntity("same-id", 5, 60, 30, 1000L)
    val updated = TimerConfigurationEntity("same-id", 10, 90, 45, 2000L)

    // When
    dao.insertConfiguration(original)
    dao.insertConfiguration(updated)
    val retrieved = dao.getRecentConfigurations(5)

    // Then
    assertThat(retrieved).hasSize(1)
    assertThat(retrieved.first()).isEqualTo(updated)
  }

  @Test
  fun deleteConfigurationRemovesFromDatabase() = runTest {
    // Given
    val config = TimerConfigurationEntity("delete-me", 1, 60, 0, 1000L)
    dao.insertConfiguration(config)

    // When
    dao.deleteConfiguration("delete-me")
    val retrieved = dao.getRecentConfigurations(5)

    // Then
    assertThat(retrieved).isEmpty()
  }

  @Test
  fun updateLastUsedModifiesTimestamp() = runTest {
    // Given
    val config = TimerConfigurationEntity("update-test", 1, 60, 0, 1000L)
    dao.insertConfiguration(config)

    // When
    dao.updateLastUsed("update-test", 5000L)
    val retrieved = dao.getConfigurationById("update-test")

    // Then
    assertThat(retrieved).isNotNull()
    assertThat(retrieved!!.lastUsed).isEqualTo(5000L)
    assertThat(retrieved.id).isEqualTo("update-test")
    assertThat(retrieved.laps).isEqualTo(1)
  }

  @Test
  fun getConfigurationCountReturnsCorrectCount() = runTest {
    // Given
    assertThat(dao.getConfigurationCount()).isEqualTo(0)

    // When
    dao.insertConfiguration(TimerConfigurationEntity("1", 1, 60, 0, 1000L))
    dao.insertConfiguration(TimerConfigurationEntity("2", 2, 60, 0, 2000L))

    // Then
    assertThat(dao.getConfigurationCount()).isEqualTo(2)
  }

  @Test
  fun cleanupOldConfigurationsKeepsOnlyMostRecent() = runTest {
    // Given
    val configs = (1..10).map { i -> TimerConfigurationEntity("id$i", i, 60, 30, i * 1000L) }
    configs.forEach { dao.insertConfiguration(it) }

    // When
    dao.cleanupOldConfigurations(keepCount = 4)
    val remaining = dao.getRecentConfigurations(10)

    // Then
    assertThat(remaining).hasSize(4)
    assertThat(remaining.map { it.id }).containsExactly("id10", "id9", "id8", "id7").inOrder()
  }

  @Test
  fun getConfigurationByIdReturnsCorrectItem() = runTest {
    // Given
    val config1 = TimerConfigurationEntity("find-me", 5, 90, 30, 1000L)
    val config2 = TimerConfigurationEntity("other", 3, 60, 15, 2000L)
    dao.insertConfiguration(config1)
    dao.insertConfiguration(config2)

    // When
    val found = dao.getConfigurationById("find-me")
    val notFound = dao.getConfigurationById("non-existent")

    // Then
    assertThat(found).isEqualTo(config1)
    assertThat(notFound).isNull()
  }

  @Test
  fun findConfigurationByValuesReturnsCorrectMatch() = runTest {
    // Given
    val config1 = TimerConfigurationEntity("id1", 5, 60, 30, 1000L)
    val config2 = TimerConfigurationEntity("id2", 8, 90, 45, 2000L)
    dao.insertConfiguration(config1)
    dao.insertConfiguration(config2)

    // When
    val found = dao.findConfigurationByValues(5, 60, 30)
    val notFound = dao.findConfigurationByValues(10, 120, 60)

    // Then
    assertThat(found).isEqualTo(config1)
    assertThat(notFound).isNull()
  }

  @Test
  fun findConfigurationByValuesReturnsNullWhenNoMatch() = runTest {
    // Given
    val config = TimerConfigurationEntity("id1", 5, 60, 30, 1000L)
    dao.insertConfiguration(config)

    // When
    val found = dao.findConfigurationByValues(5, 90, 30) // Different work duration

    // Then
    assertThat(found).isNull()
  }

  @Test
  fun emptyDatabaseReturnsEmptyResults() = runTest {
    // When/Then
    assertThat(dao.getRecentConfigurations(5)).isEmpty()
    assertThat(dao.getConfigurationCount()).isEqualTo(0)
    assertThat(dao.getConfigurationById("any-id")).isNull()
    assertThat(dao.findConfigurationByValues(5, 60, 30)).isNull()
  }
}
