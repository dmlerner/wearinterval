package com.wearinterval.wearos.tile

import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import com.wearinterval.ui.component.GridLayoutUtils
import com.wearinterval.util.Constants

/**
 * Utility functions for Wear OS Tile styling that maintain consistency with the main UI. This
 * reduces code duplication between tile and Compose implementations.
 */
object TileStyleUtils {

  /** Create a standardized grid item with consistent styling. */
  fun createStandardGridItem(
    text: String,
    clickAction: ModifiersBuilders.Clickable
  ): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Box.Builder()
      .setWidth(DimensionBuilders.dp(Constants.Dimensions.GRID_ITEM_WIDTH.toFloat()))
      .setHeight(DimensionBuilders.dp(Constants.Dimensions.GRID_ITEM_HEIGHT.toFloat()))
      .setModifiers(
        ModifiersBuilders.Modifiers.Builder()
          .setBackground(createStandardBackground())
          .setClickable(clickAction)
          .build()
      )
      .addContent(createStandardText(text))
      .build()
  }

  /** Create a standardized empty grid item. */
  fun createStandardEmptyItem(): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Box.Builder()
      .setWidth(DimensionBuilders.dp(Constants.Dimensions.GRID_ITEM_WIDTH.toFloat()))
      .setHeight(DimensionBuilders.dp(Constants.Dimensions.GRID_ITEM_HEIGHT.toFloat()))
      .build()
  }

  /** Create spacing element for grid layout. */
  fun createHorizontalSpacer(): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Spacer.Builder()
      .setWidth(DimensionBuilders.dp(Constants.Dimensions.GRID_ITEM_SPACING.toFloat()))
      .build()
  }

  /** Create spacing element for grid layout. */
  fun createVerticalSpacer(): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Spacer.Builder()
      .setHeight(DimensionBuilders.dp(Constants.Dimensions.GRID_ITEM_SPACING.toFloat()))
      .build()
  }

  /** Create a grid using shared layout logic. */
  fun createGrid(
    items: List<String>,
    clickActionProvider: (Int) -> ModifiersBuilders.Clickable
  ): LayoutElementBuilders.LayoutElement {
    val dimensions =
      GridLayoutUtils.calculateGridDimensions(items.size, Constants.Dimensions.GRID_COLUMNS)

    val columnBuilder =
      LayoutElementBuilders.Column.Builder()
        .setWidth(DimensionBuilders.wrap())
        .setHeight(DimensionBuilders.wrap())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

    repeat(dimensions.rows) { rowIndex ->
      val rowBuilder =
        LayoutElementBuilders.Row.Builder()
          .setWidth(DimensionBuilders.wrap())
          .setHeight(DimensionBuilders.wrap())
          .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

      repeat(Constants.Dimensions.GRID_COLUMNS) { colIndex ->
        val itemIndex =
          GridLayoutUtils.calculateItemIndex(rowIndex, colIndex, Constants.Dimensions.GRID_COLUMNS)

        if (
          GridLayoutUtils.shouldDisplayItem(
            rowIndex,
            colIndex,
            Constants.Dimensions.GRID_COLUMNS,
            items.size
          )
        ) {
          rowBuilder.addContent(
            createStandardGridItem(items[itemIndex], clickActionProvider(itemIndex))
          )
        } else {
          rowBuilder.addContent(createStandardEmptyItem())
        }

        // Add spacing between columns (except after last column)
        if (colIndex < Constants.Dimensions.GRID_COLUMNS - 1) {
          rowBuilder.addContent(createHorizontalSpacer())
        }
      }

      columnBuilder.addContent(rowBuilder.build())

      // Add spacing between rows (except after last row)
      if (rowIndex < dimensions.rows - 1) {
        columnBuilder.addContent(createVerticalSpacer())
      }
    }

    return columnBuilder.build()
  }

  private fun createStandardBackground(): ModifiersBuilders.Background {
    return ModifiersBuilders.Background.Builder()
      .setColor(ColorBuilders.argb(Constants.Colors.Tile.HISTORY_ITEM_BACKGROUND_ARGB))
      .setCorner(
        ModifiersBuilders.Corner.Builder()
          .setRadius(DimensionBuilders.dp(Constants.Dimensions.GRID_CORNER_RADIUS.toFloat()))
          .build()
      )
      .build()
  }

  private fun createStandardText(text: String): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Text.Builder()
      .setText(text)
      .setFontStyle(
        LayoutElementBuilders.FontStyle.Builder()
          .setSize(DimensionBuilders.sp(Constants.Dimensions.GRID_TEXT_SIZE_SP.toFloat()))
          .setColor(ColorBuilders.argb(Constants.Colors.Tile.HISTORY_ITEM_TEXT_ARGB))
          .build()
      )
      .setMaxLines(2)
      .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
      .build()
  }

  /** Create standardized empty state text. */
  fun createEmptyStateText(text: String): LayoutElementBuilders.LayoutElement {
    return LayoutElementBuilders.Text.Builder()
      .setText(text)
      .setFontStyle(
        LayoutElementBuilders.FontStyle.Builder()
          .setSize(DimensionBuilders.sp(Constants.Dimensions.GRID_TEXT_SIZE_SP.toFloat()))
          .setColor(ColorBuilders.argb(Constants.Colors.Tile.DIVIDER_COLOR_ARGB))
          .build()
      )
      .build()
  }
}
