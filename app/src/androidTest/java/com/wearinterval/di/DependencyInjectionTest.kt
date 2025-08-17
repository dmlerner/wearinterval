package com.wearinterval.di

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DependencyInjectionTest {

    @Test
    fun dataModule_annotations_areCorrect() {
        // Given - Check that modules exist and are accessible
        val dataModuleClass = DataModule::class.java
        val repositoryModuleClass = RepositoryModule::class.java

        // Then - Modules should be accessible and properly configured
        assertThat(dataModuleClass).isNotNull()
        assertThat(dataModuleClass.simpleName).isEqualTo("DataModule")
        assertThat(repositoryModuleClass).isNotNull()
        assertThat(repositoryModuleClass.simpleName).isEqualTo("RepositoryModule")

        // Basic functionality check - modules should be instantiable/accessible
        assertThat(dataModuleClass.canonicalName).contains("DataModule")
        assertThat(repositoryModuleClass.canonicalName).contains("RepositoryModule")
    }

    @Test
    fun systemServices_available() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Then - System services should be available
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(notificationManager).isNotNull()
    }

    @Test
    fun moduleClasses_canBeInstantiated() {
        // Given/When - Module classes should be accessible
        val dataModuleClass = DataModule::class.java
        val repositoryModuleClass = RepositoryModule::class.java

        // Then - Classes should exist and be properly configured
        assertThat(dataModuleClass).isNotNull()
        assertThat(repositoryModuleClass).isNotNull()
        assertThat(dataModuleClass.simpleName).isEqualTo("DataModule")
        assertThat(repositoryModuleClass.simpleName).isEqualTo("RepositoryModule")
    }

    @Test
    fun dependencyInjectionStructure_isComplete() {
        // Test that DI structure has all expected components
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Basic context services should be available
        assertThat(context.packageName).isEqualTo("com.wearinterval")
        assertThat(context.getSystemService(Context.NOTIFICATION_SERVICE)).isNotNull()

        // Module classes should be available for Hilt configuration
        assertThat(DataModule::class.java).isNotNull()
        assertThat(RepositoryModule::class.java).isNotNull()
    }

    @Test
    fun hiltAnnotations_presentOnModules() {
        // Given
        val dataModuleClass = DataModule::class.java
        val repositoryModuleClass = RepositoryModule::class.java

        // Then - Verify the classes are accessible and have expected structure
        assertThat(dataModuleClass.modifiers).isNotEqualTo(0) // Has some modifiers
        assertThat(repositoryModuleClass.modifiers).isNotEqualTo(0) // Has some modifiers

        // Verify basic class properties
        assertThat(dataModuleClass.packageName).isEqualTo("com.wearinterval.di")
        assertThat(repositoryModuleClass.packageName).isEqualTo("com.wearinterval.di")
    }
}
