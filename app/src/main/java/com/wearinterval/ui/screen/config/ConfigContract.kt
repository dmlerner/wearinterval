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
    object IncreaseLaps : ConfigEvent()
    object DecreaseLaps : ConfigEvent()
    object IncreaseWorkDuration : ConfigEvent()
    object DecreaseWorkDuration : ConfigEvent()
    object IncreaseRestDuration : ConfigEvent()
    object DecreaseRestDuration : ConfigEvent()
    object Reset : ConfigEvent()
}
