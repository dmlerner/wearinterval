package com.wearinterval.di

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import android.os.Vibrator
import androidx.room.Room
import com.wearinterval.data.database.AppDatabase
import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.util.SystemTimeProvider
import com.wearinterval.util.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

  @Provides
  @Singleton
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
      )
      .build()
  }

  @Provides
  fun provideConfigurationDao(database: AppDatabase): ConfigurationDao {
    return database.configurationDao()
  }

  @Provides
  @Singleton
  fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
    return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  @Provides
  @Singleton
  fun provideVibrator(@ApplicationContext context: Context): Vibrator {
    return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
  }

  @Provides
  @Singleton
  fun providePowerManager(@ApplicationContext context: Context): PowerManager {
    return context.getSystemService(Context.POWER_SERVICE) as PowerManager
  }

  @Provides
  @Singleton
  fun provideTimeProvider(): TimeProvider {
    return SystemTimeProvider()
  }
}
