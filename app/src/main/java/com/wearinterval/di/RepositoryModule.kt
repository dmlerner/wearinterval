package com.wearinterval.di

import com.wearinterval.data.repository.ConfigurationRepositoryImpl
import com.wearinterval.data.repository.SettingsRepositoryImpl
import com.wearinterval.data.repository.TimerRepositoryImpl
import com.wearinterval.data.repository.WearOsRepositoryImpl
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.domain.repository.WearOsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
    
    @Binds
    abstract fun bindConfigurationRepository(
        configurationRepositoryImpl: ConfigurationRepositoryImpl
    ): ConfigurationRepository
    
    @Binds
    abstract fun bindTimerRepository(
        timerRepositoryImpl: TimerRepositoryImpl
    ): TimerRepository
    
    @Binds
    abstract fun bindWearOsRepository(
        wearOsRepositoryImpl: WearOsRepositoryImpl
    ): WearOsRepository
}