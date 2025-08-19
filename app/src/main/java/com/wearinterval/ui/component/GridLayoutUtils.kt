package com.wearinterval.ui.component

/**
 * Utility functions for grid layout calculations. These are extracted from UI components to make
 * them unit testable.
 */
object GridLayoutUtils {

  /**
   * Calculate the number of rows needed for a grid layout.
   *
   * @param itemCount Total number of items to display
   * @param columns Number of columns in the grid
   * @return Number of rows needed, or 0 if no items
   */
  fun calculateGridRows(itemCount: Int, columns: Int): Int {
    if (itemCount <= 0 || columns <= 0) return 0
    return (itemCount + columns - 1) / columns
  }

  /**
   * Calculate the item index for a given grid position.
   *
   * @param rowIndex Zero-based row index
   * @param colIndex Zero-based column index
   * @param columns Number of columns in the grid
   * @return The item index, or -1 if invalid position
   */
  fun calculateItemIndex(rowIndex: Int, colIndex: Int, columns: Int): Int {
    if (rowIndex < 0 || colIndex < 0 || columns <= 0) return -1
    return rowIndex * columns + colIndex
  }

  /**
   * Check if a grid position should display an item or be empty.
   *
   * @param rowIndex Zero-based row index
   * @param colIndex Zero-based column index
   * @param columns Number of columns in the grid
   * @param totalItems Total number of items available
   * @return true if this position should show an item, false for empty placeholder
   */
  fun shouldDisplayItem(rowIndex: Int, colIndex: Int, columns: Int, totalItems: Int): Boolean {
    val itemIndex = calculateItemIndex(rowIndex, colIndex, columns)
    return itemIndex >= 0 && itemIndex < totalItems
  }

  /**
   * Calculate grid dimensions and validate input parameters.
   *
   * @param itemCount Total number of items
   * @param columns Number of columns
   * @return GridDimensions with validated parameters
   */
  fun calculateGridDimensions(itemCount: Int, columns: Int): GridDimensions {
    val validItemCount = maxOf(0, itemCount)
    val validColumns = maxOf(1, columns)
    val rows = calculateGridRows(validItemCount, validColumns)

    return GridDimensions(
      items = validItemCount,
      columns = validColumns,
      rows = rows,
      totalCells = rows * validColumns
    )
  }

  /**
   * Get all item positions for a grid layout.
   *
   * @param itemCount Total number of items
   * @param columns Number of columns
   * @return List of GridPosition objects for each item
   */
  fun getItemPositions(itemCount: Int, columns: Int): List<GridPosition> {
    val dimensions = calculateGridDimensions(itemCount, columns)
    val positions = mutableListOf<GridPosition>()

    for (row in 0 until dimensions.rows) {
      for (col in 0 until dimensions.columns) {
        val itemIndex = calculateItemIndex(row, col, columns)
        if (itemIndex < itemCount) {
          positions.add(GridPosition(row, col, itemIndex))
        }
      }
    }

    return positions
  }
}

/** Data class representing grid layout dimensions. */
data class GridDimensions(val items: Int, val columns: Int, val rows: Int, val totalCells: Int) {
  val isEmpty: Boolean
    get() = items == 0

  val hasEmptySlots: Boolean
    get() = totalCells > items

  val utilization: Float
    get() = if (totalCells > 0) items.toFloat() / totalCells else 0f
}

/** Data class representing a position in a grid. */
data class GridPosition(val row: Int, val column: Int, val itemIndex: Int) {
  val isValid: Boolean
    get() = row >= 0 && column >= 0 && itemIndex >= 0
}
