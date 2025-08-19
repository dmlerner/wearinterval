package com.wearinterval.ui.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for GridLayoutUtils - testing grid layout calculation logic extracted from UI
 * components for better testability.
 */
class GridLayoutUtilsTest {

  @Test
  fun calculateGridRows_withValidInputs_returnsCorrectRows() {
    // Test various combinations
    assertThat(GridLayoutUtils.calculateGridRows(0, 2)).isEqualTo(0) // No items
    assertThat(GridLayoutUtils.calculateGridRows(1, 2)).isEqualTo(1) // 1 item, 2 columns
    assertThat(GridLayoutUtils.calculateGridRows(2, 2)).isEqualTo(1) // 2 items, 2 columns
    assertThat(GridLayoutUtils.calculateGridRows(3, 2)).isEqualTo(2) // 3 items, 2 columns
    assertThat(GridLayoutUtils.calculateGridRows(4, 2)).isEqualTo(2) // 4 items, 2 columns
    assertThat(GridLayoutUtils.calculateGridRows(5, 2)).isEqualTo(3) // 5 items, 2 columns

    // Test with different column counts
    assertThat(GridLayoutUtils.calculateGridRows(6, 3)).isEqualTo(2) // 6 items, 3 columns
    assertThat(GridLayoutUtils.calculateGridRows(7, 3)).isEqualTo(3) // 7 items, 3 columns
    assertThat(GridLayoutUtils.calculateGridRows(9, 3)).isEqualTo(3) // 9 items, 3 columns
  }

  @Test
  fun calculateGridRows_withInvalidInputs_handlesGracefully() {
    // Test edge cases
    assertThat(GridLayoutUtils.calculateGridRows(-1, 2)).isEqualTo(0) // Negative items
    assertThat(GridLayoutUtils.calculateGridRows(5, 0)).isEqualTo(0) // Zero columns
    assertThat(GridLayoutUtils.calculateGridRows(5, -1)).isEqualTo(0) // Negative columns
    assertThat(GridLayoutUtils.calculateGridRows(-5, -2)).isEqualTo(0) // Both negative
  }

  @Test
  fun calculateItemIndex_withValidInputs_returnsCorrectIndex() {
    // Test 2-column grid positions
    assertThat(GridLayoutUtils.calculateItemIndex(0, 0, 2)).isEqualTo(0) // First item
    assertThat(GridLayoutUtils.calculateItemIndex(0, 1, 2)).isEqualTo(1) // Second item
    assertThat(GridLayoutUtils.calculateItemIndex(1, 0, 2)).isEqualTo(2) // Third item
    assertThat(GridLayoutUtils.calculateItemIndex(1, 1, 2)).isEqualTo(3) // Fourth item
    assertThat(GridLayoutUtils.calculateItemIndex(2, 0, 2)).isEqualTo(4) // Fifth item

    // Test 3-column grid positions
    assertThat(GridLayoutUtils.calculateItemIndex(0, 2, 3)).isEqualTo(2) // Third in first row
    assertThat(GridLayoutUtils.calculateItemIndex(1, 1, 3)).isEqualTo(4) // Second in second row
    assertThat(GridLayoutUtils.calculateItemIndex(2, 2, 3)).isEqualTo(8) // Last in third row
  }

  @Test
  fun calculateItemIndex_withInvalidInputs_returnsMinusOne() {
    assertThat(GridLayoutUtils.calculateItemIndex(-1, 0, 2)).isEqualTo(-1) // Negative row
    assertThat(GridLayoutUtils.calculateItemIndex(0, -1, 2)).isEqualTo(-1) // Negative column
    assertThat(GridLayoutUtils.calculateItemIndex(0, 0, 0)).isEqualTo(-1) // Zero columns
    assertThat(GridLayoutUtils.calculateItemIndex(0, 0, -1)).isEqualTo(-1) // Negative columns
  }

  @Test
  fun shouldDisplayItem_withValidGrid_returnsCorrectVisibility() {
    val totalItems = 5
    val columns = 2

    // Test all positions in a 2-column, 3-row grid with 5 items
    assertThat(GridLayoutUtils.shouldDisplayItem(0, 0, columns, totalItems)).isTrue() // Item 0
    assertThat(GridLayoutUtils.shouldDisplayItem(0, 1, columns, totalItems)).isTrue() // Item 1
    assertThat(GridLayoutUtils.shouldDisplayItem(1, 0, columns, totalItems)).isTrue() // Item 2
    assertThat(GridLayoutUtils.shouldDisplayItem(1, 1, columns, totalItems)).isTrue() // Item 3
    assertThat(GridLayoutUtils.shouldDisplayItem(2, 0, columns, totalItems)).isTrue() // Item 4
    assertThat(GridLayoutUtils.shouldDisplayItem(2, 1, columns, totalItems)).isFalse() // Empty slot
  }

  @Test
  fun shouldDisplayItem_withEdgeCases_handlesCorrectly() {
    // Empty grid
    assertThat(GridLayoutUtils.shouldDisplayItem(0, 0, 2, 0)).isFalse()

    // Single item
    assertThat(GridLayoutUtils.shouldDisplayItem(0, 0, 2, 1)).isTrue()
    assertThat(GridLayoutUtils.shouldDisplayItem(0, 1, 2, 1)).isFalse()

    // Invalid positions
    assertThat(GridLayoutUtils.shouldDisplayItem(-1, 0, 2, 5)).isFalse()
    assertThat(GridLayoutUtils.shouldDisplayItem(0, -1, 2, 5)).isFalse()
  }

  @Test
  fun calculateGridDimensions_withValidInputs_returnsCorrectDimensions() {
    val dimensions = GridLayoutUtils.calculateGridDimensions(5, 2)

    assertThat(dimensions.items).isEqualTo(5)
    assertThat(dimensions.columns).isEqualTo(2)
    assertThat(dimensions.rows).isEqualTo(3)
    assertThat(dimensions.totalCells).isEqualTo(6)
    assertThat(dimensions.isEmpty).isFalse()
    assertThat(dimensions.hasEmptySlots).isTrue()
    assertThat(dimensions.utilization).isWithin(0.001f).of(5f / 6f)
  }

  @Test
  fun calculateGridDimensions_withInvalidInputs_normalizes() {
    // Negative items should be normalized to 0
    val negativeItems = GridLayoutUtils.calculateGridDimensions(-5, 2)
    assertThat(negativeItems.items).isEqualTo(0)
    assertThat(negativeItems.isEmpty).isTrue()

    // Zero columns should be normalized to 1
    val zeroColumns = GridLayoutUtils.calculateGridDimensions(5, 0)
    assertThat(zeroColumns.columns).isEqualTo(1)
    assertThat(zeroColumns.rows).isEqualTo(5)

    // Negative columns should be normalized to 1
    val negativeColumns = GridLayoutUtils.calculateGridDimensions(3, -2)
    assertThat(negativeColumns.columns).isEqualTo(1)
    assertThat(negativeColumns.rows).isEqualTo(3)
  }

  @Test
  fun calculateGridDimensions_withPerfectFit_hasNoEmptySlots() {
    val dimensions = GridLayoutUtils.calculateGridDimensions(6, 2)

    assertThat(dimensions.hasEmptySlots).isFalse()
    assertThat(dimensions.utilization).isEqualTo(1f)
  }

  @Test
  fun calculateGridDimensions_withEmptyGrid_handlesCorrectly() {
    val dimensions = GridLayoutUtils.calculateGridDimensions(0, 2)

    assertThat(dimensions.isEmpty).isTrue()
    assertThat(dimensions.rows).isEqualTo(0)
    assertThat(dimensions.totalCells).isEqualTo(0)
    assertThat(dimensions.utilization).isEqualTo(0f)
  }

  @Test
  fun getItemPositions_withValidGrid_returnsAllPositions() {
    val positions = GridLayoutUtils.getItemPositions(5, 2)

    assertThat(positions).hasSize(5)

    // Verify specific positions
    assertThat(positions[0]).isEqualTo(GridPosition(0, 0, 0))
    assertThat(positions[1]).isEqualTo(GridPosition(0, 1, 1))
    assertThat(positions[2]).isEqualTo(GridPosition(1, 0, 2))
    assertThat(positions[3]).isEqualTo(GridPosition(1, 1, 3))
    assertThat(positions[4]).isEqualTo(GridPosition(2, 0, 4))

    // Verify all positions are valid
    positions.forEach { position -> assertThat(position.isValid).isTrue() }
  }

  @Test
  fun getItemPositions_withEmptyGrid_returnsEmptyList() {
    val positions = GridLayoutUtils.getItemPositions(0, 2)
    assertThat(positions).isEmpty()
  }

  @Test
  fun getItemPositions_withSingleItem_returnsSinglePosition() {
    val positions = GridLayoutUtils.getItemPositions(1, 2)

    assertThat(positions).hasSize(1)
    assertThat(positions[0]).isEqualTo(GridPosition(0, 0, 0))
  }

  @Test
  fun getItemPositions_withPerfectGrid_fillsAllCells() {
    val positions = GridLayoutUtils.getItemPositions(4, 2) // 2x2 perfect grid

    assertThat(positions).hasSize(4)
    assertThat(positions[0]).isEqualTo(GridPosition(0, 0, 0))
    assertThat(positions[1]).isEqualTo(GridPosition(0, 1, 1))
    assertThat(positions[2]).isEqualTo(GridPosition(1, 0, 2))
    assertThat(positions[3]).isEqualTo(GridPosition(1, 1, 3))
  }

  @Test
  fun gridPosition_isValid_checksCorrectly() {
    assertThat(GridPosition(0, 0, 0).isValid).isTrue()
    assertThat(GridPosition(1, 2, 5).isValid).isTrue()
    assertThat(GridPosition(-1, 0, 0).isValid).isFalse()
    assertThat(GridPosition(0, -1, 0).isValid).isFalse()
    assertThat(GridPosition(0, 0, -1).isValid).isFalse()
  }

  @Test
  fun gridDimensions_utilization_calculatesCorrectly() {
    // Perfect utilization
    val perfect = GridLayoutUtils.calculateGridDimensions(4, 2)
    assertThat(perfect.utilization).isEqualTo(1f)

    // Partial utilization
    val partial = GridLayoutUtils.calculateGridDimensions(3, 2)
    assertThat(partial.utilization).isEqualTo(0.75f)

    // Empty grid
    val empty = GridLayoutUtils.calculateGridDimensions(0, 2)
    assertThat(empty.utilization).isEqualTo(0f)
  }

  @Test
  fun gridLayoutUtils_realWorldScenarios() {
    // Test common configuration grid scenarios

    // History screen with 7 recent configurations in 2-column grid
    val historyGrid = GridLayoutUtils.calculateGridDimensions(7, 2)
    assertThat(historyGrid.rows).isEqualTo(4) // 4 rows needed
    assertThat(historyGrid.hasEmptySlots).isTrue() // Last slot empty

    // History screen with exactly 6 configurations (perfect fit)
    val perfectHistoryGrid = GridLayoutUtils.calculateGridDimensions(6, 2)
    assertThat(perfectHistoryGrid.rows).isEqualTo(3)
    assertThat(perfectHistoryGrid.hasEmptySlots).isFalse()

    // Empty history (new user)
    val emptyHistory = GridLayoutUtils.calculateGridDimensions(0, 2)
    assertThat(emptyHistory.isEmpty).isTrue()
    assertThat(emptyHistory.rows).isEqualTo(0)
  }

  @Test
  fun gridLayoutUtils_stressTest_largeGrids() {
    // Test with large number of items
    val largeGrid = GridLayoutUtils.calculateGridDimensions(100, 3)
    assertThat(largeGrid.rows).isEqualTo(34) // ceil(100/3) = 34
    assertThat(largeGrid.totalCells).isEqualTo(102)

    val positions = GridLayoutUtils.getItemPositions(100, 3)
    assertThat(positions).hasSize(100)

    // Verify last position
    val lastPosition = positions.last()
    assertThat(lastPosition.itemIndex).isEqualTo(99)
    assertThat(lastPosition.row).isEqualTo(33)
    assertThat(lastPosition.column).isEqualTo(0) // 99 % 3 = 0
  }
}
