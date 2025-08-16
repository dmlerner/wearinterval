package com.wearinterval.domain.repository

import com.wearinterval.domain.model.NotificationSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val notificationSettings: StateFlow<NotificationSettings>
    suspend fun updateSettings(settings: NotificationSettings): Result<Unit>
}