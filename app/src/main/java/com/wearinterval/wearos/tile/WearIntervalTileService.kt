package com.wearinterval.wearos.tile

import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.wearinterval.domain.repository.WearOsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import javax.inject.Inject

/**
 * Simple Wear OS Tile Service for WearInterval timer application.
 *
 * This is a basic implementation that provides a placeholder tile
 * while maintaining the architecture for future enhancement.
 */
@AndroidEntryPoint
class WearIntervalTileService : TileService() {

    @Inject
    lateinit var wearOsRepository: WearOsRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return serviceScope.future {
            try {
                val tileData = wearOsRepository.getTileData()

                // Create a simple text-based tile for now
                TileBuilders.Tile.Builder()
                    .setResourcesVersion("1")
                    .setTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineBuilders.TimelineEntry.Builder()
                                    .setLayout(
                                        LayoutElementBuilders.Layout.Builder()
                                            .setRoot(
                                                LayoutElementBuilders.Text.Builder()
                                                    .setText("WearInterval")
                                                    .build(),
                                            )
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .setFreshnessIntervalMillis(30000L) // 30 seconds
                    .build()
            } catch (e: Exception) {
                // Return basic error tile
                TileBuilders.Tile.Builder()
                    .setResourcesVersion("1")
                    .setTimeline(
                        TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                TimelineBuilders.TimelineEntry.Builder()
                                    .setLayout(
                                        LayoutElementBuilders.Layout.Builder()
                                            .setRoot(
                                                LayoutElementBuilders.Text.Builder()
                                                    .setText("Timer Error")
                                                    .build(),
                                            )
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .build()
            }
        }
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build(),
        )
    }
}
