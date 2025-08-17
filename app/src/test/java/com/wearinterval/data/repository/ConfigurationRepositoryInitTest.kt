package com.wearinterval.data.repository

import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.TimerConfiguration
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ConfigurationRepositoryInitTest {

    private val dataStoreManager = mockk<DataStoreManager>(relaxed = true)
    private val configurationDao = mockk<ConfigurationDao>()

    @Test
    fun `init should persist DEFAULT configuration when DataStore is empty on first launch`() = runTest {
        // Given DataStore returns null (first launch)
        val configFlow = MutableStateFlow<TimerConfiguration?>(null)
        every { dataStoreManager.currentConfiguration } returns configFlow
        every { configurationDao.getRecentConfigurationsFlow(any()) } returns flowOf(emptyList())

        // When repository is created
        val repository = ConfigurationRepositoryImpl(dataStoreManager, configurationDao)

        // Give the init coroutine time to run
        delay(100)

        // Then DEFAULT configuration should be persisted
        coVerify { dataStoreManager.updateCurrentConfiguration(TimerConfiguration.DEFAULT) }

        // And currentConfiguration should return DEFAULT
        assertEquals(TimerConfiguration.DEFAULT, repository.currentConfiguration.value)
    }

    @Test
    fun `init should not persist configuration when DataStore already has saved config`() = runTest {
        // Given DataStore returns an existing configuration
        val existingConfig = TimerConfiguration(
            laps = 10,
            workDuration = 30.seconds,
            restDuration = 10.seconds,
        )
        every { dataStoreManager.currentConfiguration } returns flowOf(existingConfig)
        every { configurationDao.getRecentConfigurationsFlow(any()) } returns flowOf(emptyList())

        // When repository is created
        val repository = ConfigurationRepositoryImpl(dataStoreManager, configurationDao)

        // Give the init coroutine time to run
        delay(100)

        // Then no configuration should be persisted (not called)
        coVerify(exactly = 0) { dataStoreManager.updateCurrentConfiguration(any()) }

        // And currentConfiguration should return the existing config
        assertEquals(existingConfig, repository.currentConfiguration.value)
    }

    @Test
    fun `currentConfiguration should always return valid config even when DataStore is null`() = runTest {
        // Given DataStore returns null
        every { dataStoreManager.currentConfiguration } returns flowOf(null)
        every { configurationDao.getRecentConfigurationsFlow(any()) } returns flowOf(emptyList())

        // When repository is created and currentConfiguration is accessed
        val repository = ConfigurationRepositoryImpl(dataStoreManager, configurationDao)
        val config = repository.currentConfiguration.first()

        // Then DEFAULT configuration should be returned
        assertEquals(TimerConfiguration.DEFAULT, config)
    }
}
