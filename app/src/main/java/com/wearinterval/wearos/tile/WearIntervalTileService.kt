package com.wearinterval.wearos.tile

import android.content.Intent
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.wearinterval.MainActivity
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.repository.TileData
import com.wearinterval.domain.repository.WearOsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

/**
 * Wear OS Tile Service for WearInterval timer application.
 *
 * Provides a 2x2 grid of recent timer configurations that matches the recent history config page
 * for DRY consistency.
 */
@AndroidEntryPoint
class WearIntervalTileService : TileService() {

  @Inject lateinit var wearOsRepository: WearOsRepository

  private val serviceScope = CoroutineScope(Dispatchers.Main)

  override fun onTileRequest(
    requestParams: RequestBuilders.TileRequest
  ): ListenableFuture<TileBuilders.Tile> {
    return serviceScope.future {
      try {
        val tileData = wearOsRepository.getTileData()

        val freshnessIntervalMs =
          when (tileData.timerState.phase) {
            TimerPhase.Running,
            TimerPhase.Resting -> 1000L // Update every second when active
            else -> 30000L // Update every 30 seconds when stopped/paused
          }

        TileBuilders.Tile.Builder()
          .setResourcesVersion("1")
          .setTimeline(
            TimelineBuilders.Timeline.Builder()
              .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                  .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                      .setRoot(createTileLayout(tileData))
                      .build(),
                  )
                  .build(),
              )
              .build(),
          )
          .setFreshnessIntervalMillis(freshnessIntervalMs)
          .build()
      } catch (e: Exception) {
        createErrorTile()
      }
    }
  }

  private fun createTileLayout(tileData: TileData): LayoutElementBuilders.LayoutElement {
    return when (tileData.timerState.phase) {
      TimerPhase.Stopped -> createConfigurationGrid(tileData.recentConfigurations)
      else -> createRunningTile(tileData)
    }
  }

  private fun createConfigurationGrid(
    configurations: List<TimerConfiguration>
  ): LayoutElementBuilders.LayoutElement {
    return if (configurations.isEmpty()) {
      createEmptyTile()
    } else {
      LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .setModifiers(
          ModifiersBuilders.Modifiers.Builder()
            .setPadding(
              ModifiersBuilders.Padding.Builder().setAll(DimensionBuilders.dp(4f)).build()
            )
            .build()
        )
        .addContent(createGrid(configurations))
        .build()
    }
  }

  private fun createGrid(
    configurations: List<TimerConfiguration>
  ): LayoutElementBuilders.LayoutElement {
    val columns = 2
    val rows = maxOf(1, (configurations.size + columns - 1) / columns)

    val columnBuilder =
      LayoutElementBuilders.Column.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())

    repeat(rows) { rowIndex ->
      val rowBuilder =
        LayoutElementBuilders.Row.Builder()
          .setWidth(DimensionBuilders.expand())
          .setHeight(DimensionBuilders.wrap())

      repeat(columns) { colIndex ->
        val itemIndex = rowIndex * columns + colIndex
        if (itemIndex < configurations.size) {
          rowBuilder.addContent(createGridItem(configurations[itemIndex]))
        } else {
          rowBuilder.addContent(createEmptyGridItem())
        }
      }

      columnBuilder.addContent(rowBuilder.build())
    }

    return columnBuilder.build()
  }

  private fun createGridItem(
    configuration: TimerConfiguration
  ): LayoutElementBuilders.LayoutElement {
    val intent =
      Intent(this, MainActivity::class.java).apply {
        putExtra("config_id", configuration.id)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }

    return LayoutElementBuilders.Box.Builder()
      .setWidth(DimensionBuilders.dp(62f))
      .setHeight(DimensionBuilders.dp(48f))
      .setModifiers(
        ModifiersBuilders.Modifiers.Builder()
          .setBackground(
            ModifiersBuilders.Background.Builder()
              .setColor(ColorBuilders.argb(-2960686)) // Dark gray
              .build()
          )
          .setClickable(
            ModifiersBuilders.Clickable.Builder()
              .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                  .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                      .setClassName(MainActivity::class.java.name)
                      .setPackageName(packageName)
                      .build()
                  )
                  .build()
              )
              .build()
          )
          .setPadding(ModifiersBuilders.Padding.Builder().setAll(DimensionBuilders.dp(4f)).build())
          .build()
      )
      .addContent(
        LayoutElementBuilders.Text.Builder()
          .setText(configuration.displayString())
          .setFontStyle(
            LayoutElementBuilders.FontStyle.Builder()
              .setSize(DimensionBuilders.sp(12f))
              .setColor(ColorBuilders.argb(-1)) // White
              .build()
          )
          .setMaxLines(2)
          .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
          .build()
      )
      .build()
  }

  private fun createEmptyGridItem(): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Box.Builder()
      .setWidth(DimensionBuilders.dp(62f))
      .setHeight(DimensionBuilders.dp(48f))
      .build()
  }

  private fun createEmptyTile(): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Box.Builder()
      .setWidth(DimensionBuilders.expand())
      .setHeight(DimensionBuilders.expand())
      .addContent(
        LayoutElementBuilders.Text.Builder()
          .setText("No recent sets")
          .setFontStyle(
            LayoutElementBuilders.FontStyle.Builder()
              .setSize(DimensionBuilders.sp(14f))
              .setColor(ColorBuilders.argb(-10066330)) // Medium gray
              .build()
          )
          .build()
      )
      .build()
  }

  private fun createRunningTile(tileData: TileData): LayoutElementBuilders.LayoutElement {
    val timerState = tileData.timerState
    val progressText = "${timerState.timeRemaining.inWholeSeconds}s"
    val lapText =
      if (timerState.totalLaps == 999) "${timerState.currentLap}"
      else "${timerState.currentLap}/${timerState.totalLaps}"

    return LayoutElementBuilders.Box.Builder()
      .setWidth(DimensionBuilders.expand())
      .setHeight(DimensionBuilders.expand())
      .addContent(
        LayoutElementBuilders.Column.Builder()
          .setWidth(DimensionBuilders.expand())
          .setHeight(DimensionBuilders.wrap())
          .addContent(
            LayoutElementBuilders.Text.Builder()
              .setText(progressText)
              .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                  .setSize(DimensionBuilders.sp(18f))
                  .setColor(ColorBuilders.argb(-1)) // White
                  .build()
              )
              .build()
          )
          .addContent(
            LayoutElementBuilders.Text.Builder()
              .setText(lapText)
              .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                  .setSize(DimensionBuilders.sp(12f))
                  .setColor(ColorBuilders.argb(-7829368)) // Light gray
                  .build()
              )
              .build()
          )
          .build()
      )
      .setModifiers(
        ModifiersBuilders.Modifiers.Builder()
          .setClickable(
            ModifiersBuilders.Clickable.Builder()
              .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                  .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                      .setClassName(MainActivity::class.java.name)
                      .setPackageName(packageName)
                      .build()
                  )
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()
  }

  private fun createErrorTile(): TileBuilders.Tile {
    return TileBuilders.Tile.Builder()
      .setResourcesVersion("1")
      .setTimeline(
        TimelineBuilders.Timeline.Builder()
          .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
              .setLayout(
                LayoutElementBuilders.Layout.Builder()
                  .setRoot(
                    LayoutElementBuilders.Box.Builder()
                      .setWidth(DimensionBuilders.expand())
                      .setHeight(DimensionBuilders.expand())
                      .addContent(
                        LayoutElementBuilders.Text.Builder()
                          .setText("Timer Error")
                          .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                              .setSize(DimensionBuilders.sp(14f))
                              .setColor(ColorBuilders.argb(-10066330)) // Medium gray
                              .build()
                          )
                          .build()
                      )
                      .build()
                  )
                  .build(),
              )
              .build(),
          )
          .build(),
      )
      .build()
  }

  override fun onResourcesRequest(
    requestParams: RequestBuilders.ResourcesRequest
  ): ListenableFuture<ResourceBuilders.Resources> {
    return Futures.immediateFuture(
      ResourceBuilders.Resources.Builder().setVersion("1").build(),
    )
  }
}
