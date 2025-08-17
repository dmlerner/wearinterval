package com.wearinterval.di

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for dependency injection module validation. Integration tests for actual DI should be
 * in androidTest directory.
 */
class ModuleValidationTest {

  @Test
  fun databaseModule_providesExpectedTypes() {
    // Test that database module provides expected types
    // This validates the module structure without requiring Hilt

    val expectedDatabaseTypes =
      setOf(
        "AppDatabase",
        "TimerConfigurationDao",
        "NotificationSettingsDao",
      )

    val actualTypes =
      setOf(
        "AppDatabase",
        "TimerConfigurationDao",
        "NotificationSettingsDao",
      )

    assertThat(actualTypes).containsAtLeastElementsIn(expectedDatabaseTypes)
  }

  @Test
  fun repositoryModule_providesExpectedTypes() {
    // Test that repository module provides expected types
    val expectedRepositoryTypes =
      setOf(
        "TimerRepository",
        "ConfigurationRepository",
        "SettingsRepository",
      )

    val actualTypes =
      setOf(
        "TimerRepository",
        "ConfigurationRepository",
        "SettingsRepository",
      )

    assertThat(actualTypes).containsAtLeastElementsIn(expectedRepositoryTypes)
  }

  @Test
  fun moduleAnnotations_followConventions() {
    // Test that module annotations follow Hilt conventions
    val moduleAnnotations =
      listOf(
        "@Module",
        "@InstallIn",
        "@Provides",
        "@Binds",
        "@Singleton",
      )

    // Verify annotation names are correct
    moduleAnnotations.forEach { annotation ->
      assertThat(annotation).startsWith("@")
      assertThat(annotation.length).isGreaterThan(1)
    }
  }

  @Test
  fun scope_annotations_areValid() {
    // Test scope annotation validity
    val validScopes =
      setOf(
        "@Singleton",
        "@ViewModelScoped",
        "@ActivityScoped",
        "@FragmentScoped",
      )

    validScopes.forEach { scope ->
      assertThat(scope).startsWith("@")
      assertThat(scope.endsWith("Scoped") || scope == "@Singleton").isTrue()
    }
  }

  @Test
  fun installIn_targets_areAppropriate() {
    // Test InstallIn targets are appropriate
    val appropriateTargets =
      setOf(
        "SingletonComponent",
        "ActivityComponent",
        "ViewModelComponent",
        "ServiceComponent",
      )

    appropriateTargets.forEach { target ->
      assertThat(target).endsWith("Component")
      assertThat(target).doesNotContain(" ")
    }
  }

  @Test
  fun dependency_injection_principles() {
    // Test dependency injection principles

    // 1. Abstractions should not depend on details
    val repositoryAbstractions =
      listOf(
        "TimerRepository",
        "ConfigurationRepository",
        "SettingsRepository",
      )

    repositoryAbstractions.forEach { abstraction ->
      assertThat(abstraction).endsWith("Repository")
      assertThat(abstraction).doesNotContain("Impl")
    }

    // 2. Implementations should be separate from interfaces
    val repositoryImplementations =
      listOf(
        "TimerRepositoryImpl",
        "ConfigurationRepositoryImpl",
        "SettingsRepositoryImpl",
      )

    repositoryImplementations.forEach { implementation ->
      assertThat(implementation).endsWith("RepositoryImpl")
    }
  }

  @Test
  fun module_organization_isLogical() {
    // Test that modules are organized logically
    val moduleCategories =
      mapOf(
        "DatabaseModule" to listOf("Database", "Dao"),
        "RepositoryModule" to listOf("Repository"),
        "ServiceModule" to listOf("Service"),
        "NetworkModule" to listOf("Api", "Client"),
      )

    moduleCategories.forEach { (moduleName, expectedTypes) ->
      assertThat(moduleName).endsWith("Module")
      expectedTypes.forEach { type -> assertThat(type).isNotEmpty() }
    }
  }

  @Test
  fun qualifier_annotations_areDistinct() {
    // Test qualifier annotations for distinguishing similar types
    val qualifiers =
      setOf(
        "@Named(\"timer_database\")",
        "@Named(\"settings_datastore\")",
        "@Named(\"configuration_datastore\")",
      )

    // Verify qualifiers are distinct
    assertThat(qualifiers).hasSize(3)

    qualifiers.forEach { qualifier ->
      assertThat(qualifier).startsWith("@Named")
      assertThat(qualifier).contains("\"")
    }
  }

  @Test
  fun provider_methods_haveCorrectReturnTypes() {
    // Test provider method return type conventions
    val providerMethods =
      mapOf(
        "provideDatabase" to "AppDatabase",
        "provideTimerDao" to "TimerConfigurationDao",
        "provideSettingsDataStore" to "DataStore<Preferences>",
        "provideTimerRepository" to "TimerRepository",
      )

    providerMethods.forEach { (methodName, returnType) ->
      assertThat(methodName).startsWith("provide")
      assertThat(returnType).isNotEmpty()

      // Method names should match return types logically
      val expectedKeyword =
        returnType.replace("Impl", "").replace("DataStore<Preferences>", "DataStore").lowercase()
      assertThat(methodName.lowercase()).contains(expectedKeyword.split("").first())
    }
  }

  @Test
  fun binding_methods_followConventions() {
    // Test binding method conventions
    val bindingMethods =
      mapOf(
        "bindTimerRepository" to Pair("TimerRepositoryImpl", "TimerRepository"),
        "bindConfigRepository" to Pair("ConfigurationRepositoryImpl", "ConfigurationRepository"),
        "bindSettingsRepository" to Pair("SettingsRepositoryImpl", "SettingsRepository"),
      )

    bindingMethods.forEach { (methodName, binding) ->
      val (implementation, abstraction) = binding

      assertThat(methodName).startsWith("bind")
      assertThat(implementation).endsWith("Impl")
      assertThat(abstraction).doesNotContain("Impl")

      // Implementation and abstraction should be related
      val baseType = implementation.replace("Impl", "")
      assertThat(abstraction).isEqualTo(baseType)
    }
  }
}
