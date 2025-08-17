package com.wearinterval.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationSettingsTest {

    @Test
    fun `hasAnyNotification returns true when vibration is enabled`() {
        val settings = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = false,
            flashEnabled = false,
            autoMode = true,
        )

        assertThat(settings.hasAnyNotification()).isTrue()
    }

    @Test
    fun `hasAnyNotification returns true when sound is enabled`() {
        val settings = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = true,
        )

        assertThat(settings.hasAnyNotification()).isTrue()
    }

    @Test
    fun `hasAnyNotification returns true when flash is enabled`() {
        val settings = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = false,
            flashEnabled = true,
            autoMode = true,
        )

        assertThat(settings.hasAnyNotification()).isTrue()
    }

    @Test
    fun `hasAnyNotification returns false when all notifications disabled`() {
        val settings = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = false,
            flashEnabled = false,
            autoMode = true,
        )

        assertThat(settings.hasAnyNotification()).isFalse()
    }

    @Test
    fun `hasAnyNotification returns true when multiple notifications enabled`() {
        val settings = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = false,
        )

        assertThat(settings.hasAnyNotification()).isTrue()
    }

    @Test
    fun `withVibration updates vibration setting only`() {
        val original = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = true,
            flashEnabled = true,
            autoMode = false,
        )

        val updated = original.withVibration(true)

        assertThat(updated.vibrationEnabled).isTrue()
        assertThat(updated.soundEnabled).isEqualTo(original.soundEnabled)
        assertThat(updated.flashEnabled).isEqualTo(original.flashEnabled)
        assertThat(updated.autoMode).isEqualTo(original.autoMode)
    }

    @Test
    fun `withSound updates sound setting only`() {
        val original = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = true,
        )

        val updated = original.withSound(false)

        assertThat(updated.soundEnabled).isFalse()
        assertThat(updated.vibrationEnabled).isEqualTo(original.vibrationEnabled)
        assertThat(updated.flashEnabled).isEqualTo(original.flashEnabled)
        assertThat(updated.autoMode).isEqualTo(original.autoMode)
    }

    @Test
    fun `withFlash updates flash setting only`() {
        val original = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = false,
            flashEnabled = false,
            autoMode = false,
        )

        val updated = original.withFlash(true)

        assertThat(updated.flashEnabled).isTrue()
        assertThat(updated.vibrationEnabled).isEqualTo(original.vibrationEnabled)
        assertThat(updated.soundEnabled).isEqualTo(original.soundEnabled)
        assertThat(updated.autoMode).isEqualTo(original.autoMode)
    }

    @Test
    fun `withAutoMode updates auto mode setting only`() {
        val original = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = true,
            autoMode = true,
        )

        val updated = original.withAutoMode(false)

        assertThat(updated.autoMode).isFalse()
        assertThat(updated.vibrationEnabled).isEqualTo(original.vibrationEnabled)
        assertThat(updated.soundEnabled).isEqualTo(original.soundEnabled)
        assertThat(updated.flashEnabled).isEqualTo(original.flashEnabled)
    }

    @Test
    fun `DEFAULT has vibration and sound enabled with auto mode, flash disabled`() {
        val settings = NotificationSettings.DEFAULT

        assertThat(settings.vibrationEnabled).isTrue()
        assertThat(settings.soundEnabled).isTrue()
        assertThat(settings.flashEnabled).isFalse()
        assertThat(settings.autoMode).isTrue()
    }

    @Test
    fun `QUIET has only flash enabled with auto mode`() {
        val settings = NotificationSettings.QUIET

        assertThat(settings.vibrationEnabled).isFalse()
        assertThat(settings.soundEnabled).isFalse()
        assertThat(settings.flashEnabled).isTrue()
        assertThat(settings.autoMode).isTrue()
    }

    @Test
    fun `FULL_ALERTS has all notifications enabled with manual mode`() {
        val settings = NotificationSettings.FULL_ALERTS

        assertThat(settings.vibrationEnabled).isTrue()
        assertThat(settings.soundEnabled).isTrue()
        assertThat(settings.flashEnabled).isTrue()
        assertThat(settings.autoMode).isFalse()
    }

    @Test
    fun `VIBRATION_ONLY has only vibration enabled with auto mode`() {
        val settings = NotificationSettings.VIBRATION_ONLY

        assertThat(settings.vibrationEnabled).isTrue()
        assertThat(settings.soundEnabled).isFalse()
        assertThat(settings.flashEnabled).isFalse()
        assertThat(settings.autoMode).isTrue()
    }

    @Test
    fun `preset configurations have notifications when expected`() {
        assertThat(NotificationSettings.DEFAULT.hasAnyNotification()).isTrue()
        assertThat(NotificationSettings.QUIET.hasAnyNotification()).isTrue()
        assertThat(NotificationSettings.FULL_ALERTS.hasAnyNotification()).isTrue()
        assertThat(NotificationSettings.VIBRATION_ONLY.hasAnyNotification()).isTrue()
    }

    @Test
    fun `settings can be chained using with methods`() {
        val settings = NotificationSettings.DEFAULT
            .withVibration(false)
            .withSound(false)
            .withAutoMode(false)

        assertThat(settings.vibrationEnabled).isFalse()
        assertThat(settings.soundEnabled).isFalse()
        assertThat(settings.flashEnabled).isFalse() // unchanged from DEFAULT
        assertThat(settings.autoMode).isFalse()
    }

    @Test
    fun `settings equality works correctly`() {
        val settings1 = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = false,
            flashEnabled = true,
            autoMode = false,
        )

        val settings2 = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = false,
            flashEnabled = true,
            autoMode = false,
        )

        val settings3 = settings1.copy(autoMode = true)

        assertThat(settings1).isEqualTo(settings2)
        assertThat(settings1).isNotEqualTo(settings3)
    }

    @Test
    fun `constructor default values match DEFAULT preset`() {
        val defaultConstructed = NotificationSettings()
        val defaultPreset = NotificationSettings.DEFAULT

        assertThat(defaultConstructed).isEqualTo(defaultPreset)
    }
}
