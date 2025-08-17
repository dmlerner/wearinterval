package com.wearinterval.ui.screen.config

import com.wearinterval.util.Constants

data class ConfigUiState(
    val laps: Int = 2,
    val workMinutes: Int = 0,
    val workSeconds: Int = 3,
    val restMinutes: Int = 0,
    val restSeconds: Int = 3,
) {
    val totalWorkTimeText: String
        get() = if (workMinutes > 0) {
            "$workMinutes:${workSeconds.toString().padStart(Constants.UI.STRING_PADDING_WIDTH, '0')}"
        } else {
            "${workSeconds}s"
        }

    val totalRestTimeText: String
        get() = if (restMinutes > 0) {
            "$restMinutes:${restSeconds.toString().padStart(Constants.UI.STRING_PADDING_WIDTH, '0')}"
        } else if (restSeconds > 0) {
            "${restSeconds}s"
        } else {
            "None"
        }
}

sealed class ConfigEvent {
    data class SetLaps(val laps: Int) : ConfigEvent()
    data class SetWorkDuration(val duration: kotlin.time.Duration) : ConfigEvent()
    data class SetRestDuration(val duration: kotlin.time.Duration) : ConfigEvent()
    object Reset : ConfigEvent()
    object ResetLaps : ConfigEvent()
    object ResetWork : ConfigEvent()
    object ResetRest : ConfigEvent()
    object SetLapsToInfinite : ConfigEvent()
    object SetWorkToLong : ConfigEvent()
    object SetRestToLong : ConfigEvent()
}
