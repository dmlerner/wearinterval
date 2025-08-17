package com.wearinterval

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationTest {

  @Test
  fun application_isWearIntervalApplication() {
    // Given
    val context = ApplicationProvider.getApplicationContext<WearIntervalApplication>()

    // Then
    assertThat(context).isInstanceOf(WearIntervalApplication::class.java)
    assertThat(context).isNotNull()
  }

  @Test
  fun application_hasCorrectPackageName() {
    // Given
    val context = ApplicationProvider.getApplicationContext<WearIntervalApplication>()

    // Then
    assertThat(context.packageName).isEqualTo("com.wearinterval")
  }

  @Test
  fun application_hiltAnnotationPresent() {
    // Given
    val appClass = WearIntervalApplication::class.java

    // Then - Verify Hilt annotation is present
    val hiltAnnotations =
      appClass.annotations.filter { it.annotationClass.simpleName == "HiltAndroidApp" }
    assertThat(hiltAnnotations).isNotEmpty()
  }

  @Test
  fun application_inheritsFromApplication() {
    // Given
    val appClass = WearIntervalApplication::class.java

    // Then
    assertThat(android.app.Application::class.java.isAssignableFrom(appClass)).isTrue()
  }

  @Test
  fun application_canBeInstantiated() {
    // Given/When
    val context = ApplicationProvider.getApplicationContext<WearIntervalApplication>()

    // Then
    assertThat(context).isNotNull()
    assertThat(context.applicationContext).isEqualTo(context)
  }

  @Test
  fun application_initializesSuccessfully() {
    // Given
    val context = ApplicationProvider.getApplicationContext<WearIntervalApplication>()

    // When/Then - Application should initialize without throwing
    assertThat(context.applicationInfo).isNotNull()
    assertThat(context.resources).isNotNull()
    assertThat(context.assets).isNotNull()
  }

  @Test
  fun application_hasCorrectApplicationInfo() {
    // Given
    val context = ApplicationProvider.getApplicationContext<WearIntervalApplication>()

    // Then
    val appInfo = context.applicationInfo
    assertThat(appInfo.packageName).isEqualTo("com.wearinterval")
    assertThat(appInfo.className).contains("WearIntervalApplication")
  }
}
