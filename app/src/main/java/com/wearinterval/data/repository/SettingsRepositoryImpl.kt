package com.wearinterval.data.repository

import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Singleton
class SettingsRepositoryImpl
@Inject
constructor(
  private val dataStoreManager: DataStoreManager,
) : SettingsRepository {

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override val notificationSettings: StateFlow<NotificationSettings> =
    dataStoreManager.notificationSettings.stateIn(
      scope = repositoryScope,
      started = SharingStarted.Eagerly,
      initialValue = NotificationSettings.DEFAULT,
    )

  override suspend fun updateSettings(settings: NotificationSettings): Result<Unit> {
    return try {
      dataStoreManager.updateNotificationSettings(settings)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
