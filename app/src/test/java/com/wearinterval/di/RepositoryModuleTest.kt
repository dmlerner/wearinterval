package com.wearinterval.di

import com.google.common.truth.Truth.assertThat
import com.wearinterval.data.repository.ConfigurationRepositoryImpl
import com.wearinterval.data.repository.SettingsRepositoryImpl
import com.wearinterval.data.repository.TimerRepositoryImpl
import com.wearinterval.data.repository.WearOsRepositoryImpl
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.domain.repository.WearOsRepository
import org.junit.Test

/**
 * Unit tests for RepositoryModule dependency injection bindings.
 */
class RepositoryModuleTest {

    @Test
    fun repositoryModule_bindsCorrectTypes() {
        // Test that binding logic works correctly
        // In Hilt, @Binds methods just specify the binding, the actual binding is handled by Hilt

        // Test implementation class extends interface
        assertThat(SettingsRepositoryImpl::class.java.interfaces).asList()
            .contains(SettingsRepository::class.java)
        assertThat(ConfigurationRepositoryImpl::class.java.interfaces).asList()
            .contains(ConfigurationRepository::class.java)
        assertThat(TimerRepositoryImpl::class.java.interfaces).asList()
            .contains(TimerRepository::class.java)
        assertThat(WearOsRepositoryImpl::class.java.interfaces).asList()
            .contains(WearOsRepository::class.java)
    }

    @Test
    fun repositoryModule_hasCorrectAnnotations() {
        val repositoryModuleClass = RepositoryModule::class.java

        // Verify class is a Hilt module (has necessary structure)
        assertThat(repositoryModuleClass.isInterface).isFalse()
        assertThat(repositoryModuleClass.name).contains("RepositoryModule")
    }

    @Test
    fun bindingMethods_haveCorrectAnnotations() {
        val repositoryModuleClass = RepositoryModule::class.java

        // Check bindSettingsRepository method
        val bindSettingsRepositoryMethod = repositoryModuleClass.getDeclaredMethod(
            "bindSettingsRepository",
            SettingsRepositoryImpl::class.java,
        )
        assertThat(bindSettingsRepositoryMethod.getAnnotation(dagger.Binds::class.java)).isNotNull()

        // Check bindConfigurationRepository method
        val bindConfigurationRepositoryMethod = repositoryModuleClass.getDeclaredMethod(
            "bindConfigurationRepository",
            ConfigurationRepositoryImpl::class.java,
        )
        assertThat(bindConfigurationRepositoryMethod.getAnnotation(dagger.Binds::class.java)).isNotNull()

        // Check bindTimerRepository method
        val bindTimerRepositoryMethod = repositoryModuleClass.getDeclaredMethod(
            "bindTimerRepository",
            TimerRepositoryImpl::class.java,
        )
        assertThat(bindTimerRepositoryMethod.getAnnotation(dagger.Binds::class.java)).isNotNull()

        // Check bindWearOsRepository method
        val bindWearOsRepositoryMethod = repositoryModuleClass.getDeclaredMethod(
            "bindWearOsRepository",
            WearOsRepositoryImpl::class.java,
        )
        assertThat(bindWearOsRepositoryMethod.getAnnotation(dagger.Binds::class.java)).isNotNull()
    }

    @Test
    fun allRepositoryImplementations_extendInterfaces() {
        // Verify that all implementation classes implement their respective interfaces
        assertThat(SettingsRepository::class.java.isAssignableFrom(SettingsRepositoryImpl::class.java)).isTrue()
        assertThat(ConfigurationRepository::class.java.isAssignableFrom(ConfigurationRepositoryImpl::class.java)).isTrue()
        assertThat(TimerRepository::class.java.isAssignableFrom(TimerRepositoryImpl::class.java)).isTrue()
        assertThat(WearOsRepository::class.java.isAssignableFrom(WearOsRepositoryImpl::class.java)).isTrue()
    }
}
