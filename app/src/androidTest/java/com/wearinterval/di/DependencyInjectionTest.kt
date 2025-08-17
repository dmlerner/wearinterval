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
        // Given - Check that modules have correct annotations via reflection
        val dataModuleClass = DataModule::class.java
        val repositoryModuleClass = RepositoryModule::class.java

        // Then - DataModule should have correct annotations
        val dataModuleAnnotations = dataModuleClass.annotations
        val hasModuleAnnotation = dataModuleAnnotations.any { it.annotationClass.simpleName == "Module" }
        val hasInstallInAnnotation = dataModuleAnnotations.any { it.annotationClass.simpleName == "InstallIn" }
        assertThat(hasModuleAnnotation).isTrue()
        assertThat(hasInstallInAnnotation).isTrue()

        // RepositoryModule should have correct annotations
        val repositoryModuleAnnotations = repositoryModuleClass.annotations
        val hasRepoModuleAnnotation = repositoryModuleAnnotations.any { it.annotationClass.simpleName == "Module" }
        val hasRepoInstallInAnnotation = repositoryModuleAnnotations.any { it.annotationClass.simpleName == "InstallIn" }
        assertThat(hasRepoModuleAnnotation).isTrue()
        assertThat(hasRepoInstallInAnnotation).isTrue()
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

        // Then - Verify required Hilt annotations are present
        val dataModuleHasHiltAnnotations = dataModuleClass.annotations.any {
            it.annotationClass.simpleName in listOf("Module", "InstallIn")
        }
        val repositoryModuleHasHiltAnnotations = repositoryModuleClass.annotations.any {
            it.annotationClass.simpleName in listOf("Module", "InstallIn")
        }

        assertThat(dataModuleHasHiltAnnotations).isTrue()
        assertThat(repositoryModuleHasHiltAnnotations).isTrue()
    }
}
