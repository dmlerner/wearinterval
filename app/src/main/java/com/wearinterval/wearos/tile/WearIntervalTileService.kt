package com.wearinterval.wearos.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.wearinterval.MainActivity
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.repository.TileData
import com.wearinterval.domain.repository.WearOsRepository
import com.wearinterval.util.Constants
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
          .setTileTimeline(
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
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .setModifiers(
          ModifiersBuilders.Modifiers.Builder()
            .setPadding(
              ModifiersBuilders.Padding.Builder()
                .setAll(DimensionBuilders.dp(Constants.Dimensions.GRID_PADDING.toFloat()))
                .build()
            )
            .build()
        )
        .addContent(
          TileStyleUtils.createGrid(configurations.map { it.displayString() }) { index ->
            createClickAction(configurations[index])
          }
        )
        .build()
    }
  }

  private fun createClickAction(configuration: TimerConfiguration): ModifiersBuilders.Clickable {
    return ModifiersBuilders.Clickable.Builder()
      .setOnClick(
        ActionBuilders.LaunchAction.Builder()
          .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
              .setClassName(MainActivity::class.java.name)
              .setPackageName(packageName)
              .addKeyToExtraMapping("config_id", ActionBuilders.stringExtra(configuration.id))
              .addKeyToExtraMapping("navigate_to", ActionBuilders.stringExtra("main"))
              .build()
          )
          .build()
      )
      .build()
  }

  private fun createEmptyTile(): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Box.Builder()
      .setWidth(DimensionBuilders.expand())
      .setHeight(DimensionBuilders.expand())
      .addContent(TileStyleUtils.createEmptyStateText("No recent sets"))
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
                  .setColor(ColorBuilders.argb(Constants.Colors.Tile.WHITE_ARGB))
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
                  .setColor(ColorBuilders.argb(Constants.Colors.Tile.HISTORY_ITEM_TEXT_ARGB))
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
                      .addKeyToExtraMapping("navigate_to", ActionBuilders.stringExtra("main"))
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
      .setTileTimeline(
        TimelineBuilders.Timeline.Builder()
          .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
              .setLayout(
                LayoutElementBuilders.Layout.Builder()
                  .setRoot(
                    LayoutElementBuilders.Box.Builder()
                      .setWidth(DimensionBuilders.expand())
                      .setHeight(DimensionBuilders.expand())
                      .addContent(TileStyleUtils.createEmptyStateText("Timer Error"))
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

  override fun onTileResourcesRequest(
    requestParams: RequestBuilders.ResourcesRequest
  ): ListenableFuture<ResourceBuilders.Resources> {
    return Futures.immediateFuture(
      ResourceBuilders.Resources.Builder().setVersion("1").build(),
    )
  }
}
