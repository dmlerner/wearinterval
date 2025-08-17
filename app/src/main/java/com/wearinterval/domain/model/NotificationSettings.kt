package com.wearinterval.domain.model

data class NotificationSettings(
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val flashEnabled: Boolean = false,
    val autoMode: Boolean = true,
) {
    fun hasAnyNotification(): Boolean {
        return vibrationEnabled || soundEnabled || flashEnabled
    }

    fun withVibration(enabled: Boolean): NotificationSettings {
        return copy(vibrationEnabled = enabled)
    }

    fun withSound(enabled: Boolean): NotificationSettings {
        return copy(soundEnabled = enabled)
    }

    fun withFlash(enabled: Boolean): NotificationSettings {
        return copy(flashEnabled = enabled)
    }

    fun withAutoMode(enabled: Boolean): NotificationSettings {
        return copy(autoMode = enabled)
    }

    companion object {
        val DEFAULT = NotificationSettings()

        val QUIET = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = false,
            flashEnabled = true,
            autoMode = true,
        )

        val FULL_ALERTS = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = true,
            autoMode = false,
        )

        val VIBRATION_ONLY = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = false,
            flashEnabled = false,
            autoMode = true,
        )
    }
}
