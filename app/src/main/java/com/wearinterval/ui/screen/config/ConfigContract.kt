package com.wearinterval.ui.screen.config

data class ConfigUiState(
    val laps: Int = 1,
    val workMinutes: Int = 1,
    val workSeconds: Int = 0,
    val restMinutes: Int = 0,
    val restSeconds: Int = 0,
) {
    val totalWorkTimeText: String
        get() = if (workMinutes > 0) {
            "$workMinutes:${workSeconds.toString().padStart(2, '0')}"
        } else {
            "${workSeconds}s"
        }

    val totalRestTimeText: String
        get() = if (restMinutes > 0) {
            "$restMinutes:${restSeconds.toString().padStart(2, '0')}"
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
