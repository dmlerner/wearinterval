package com.wearinterval.wearos.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

/** Minimal complication service to test system recognition */
class SimpleComplicationService : ComplicationDataSourceService() {

  override fun onComplicationRequest(
    request: ComplicationRequest,
    callback: ComplicationRequestListener
  ) {
    val complicationData =
      ShortTextComplicationData.Builder(
          text = PlainComplicationText.Builder("WI").build(),
          contentDescription = PlainComplicationText.Builder("WearInterval").build()
        )
        .build()

    callback.onComplicationData(complicationData)
  }

  override fun getPreviewData(type: ComplicationType): ComplicationData {
    return ShortTextComplicationData.Builder(
        text = PlainComplicationText.Builder("WI").build(),
        contentDescription = PlainComplicationText.Builder("WearInterval Preview").build()
      )
      .build()
  }
}
