package com.wearinterval.domain.repository

import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerState

interface WearOsRepository {
    suspend fun updateWearOsComponents(): Result<Unit>
    suspend fun getTileData(): TileData
    suspend fun getComplicationData(type: ComplicationType): ComplicationData
}

sealed class ComplicationType {
    object ShortText : ComplicationType()
    object LongText : ComplicationType()
    object RangedValue : ComplicationType()
    object MonochromaticImage : ComplicationType()
    object SmallImage : ComplicationType()
}

data class TileData(
    val timerState: TimerState,
    val recentConfigurations: List<TimerConfiguration>
)

sealed class ComplicationData {
    data class ShortText(
        val text: String,
        val title: String?
    ) : ComplicationData()
    
    data class LongText(
        val text: String,
        val title: String?
    ) : ComplicationData()
    
    data class RangedValue(
        val value: Float,
        val min: Float,
        val max: Float,
        val text: String,
        val title: String?
    ) : ComplicationData()
    
    data class Image(
        val iconRes: Int,
        val contentDescription: String
    ) : ComplicationData()
}