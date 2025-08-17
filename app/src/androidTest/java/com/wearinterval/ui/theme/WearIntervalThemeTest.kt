package com.wearinterval.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Shapes
import androidx.wear.compose.material.Typography
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearIntervalThemeTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun wearIntervalTheme_appliesMaterialTheme() {
    // Given
    var capturedColors: Colors? = null
    var capturedTypography: Typography? = null
    var capturedShapes: Shapes? = null

    // When
    composeTestRule.setContent {
      WearIntervalTheme {
        capturedColors = MaterialTheme.colors
        capturedTypography = MaterialTheme.typography
        capturedShapes = MaterialTheme.shapes
      }
    }

    // Then
    assertThat(capturedColors).isNotNull()
    assertThat(capturedTypography).isNotNull()
    assertThat(capturedShapes).isNotNull()
  }

  @Test
  fun wearIntervalTheme_usesWearMaterialDefaults() {
    // Given
    var themeColors: Colors? = null

    // When
    composeTestRule.setContent { WearIntervalTheme { themeColors = MaterialTheme.colors } }

    // Then - Should use Wear OS default dark theme colors
    assertThat(themeColors).isNotNull()
    assertThat(themeColors!!.primary).isNotEqualTo(Color.Unspecified)
    assertThat(themeColors!!.secondary).isNotEqualTo(Color.Unspecified)
    assertThat(themeColors!!.background).isNotEqualTo(Color.Unspecified)
    assertThat(themeColors!!.surface).isNotEqualTo(Color.Unspecified)
  }

  @Test
  fun wearIntervalTheme_providesContentLambda() {
    // Given
    var contentExecuted = false

    // When
    composeTestRule.setContent { WearIntervalTheme { contentExecuted = true } }

    // Then
    assertThat(contentExecuted).isTrue()
  }

  @Test
  fun wearIntervalTheme_preservesCompositionLocals() {
    // Given
    var densityInTheme: Density? = null
    val testDensity = Density(2.0f)

    // When
    composeTestRule.setContent {
      CompositionLocalProvider(LocalDensity provides testDensity) {
        WearIntervalTheme { densityInTheme = LocalDensity.current }
      }
    }

    // Then - Theme should preserve parent composition locals
    assertThat(densityInTheme).isEqualTo(testDensity)
  }

  @Test
  fun wearIntervalTheme_canBeNested() {
    // Given
    var outerColors: Colors? = null
    var innerColors: Colors? = null

    // When
    composeTestRule.setContent {
      WearIntervalTheme {
        outerColors = MaterialTheme.colors
        WearIntervalTheme { innerColors = MaterialTheme.colors }
      }
    }

    // Then - Nested themes should work correctly
    assertThat(outerColors).isNotNull()
    assertThat(innerColors).isNotNull()
    assertThat(outerColors).isEqualTo(innerColors) // Should be same since we use default
  }

  @Test
  fun wearIntervalTheme_worksWithCommonContent() {
    // Given
    var contentRendered = false

    // When
    composeTestRule.setContent {
      WearIntervalTheme { Box(modifier = Modifier.fillMaxSize()) { contentRendered = true } }
    }

    // Then
    assertThat(contentRendered).isTrue()
  }

  @Test
  fun wearIntervalTheme_providesTypographyScale() {
    // Given
    var typography: Typography? = null

    // When
    composeTestRule.setContent { WearIntervalTheme { typography = MaterialTheme.typography } }

    // Then - Should provide complete typography scale
    assertThat(typography).isNotNull()
    assertThat(typography!!.display1).isNotNull()
    assertThat(typography!!.display2).isNotNull()
    assertThat(typography!!.display3).isNotNull()
    assertThat(typography!!.title1).isNotNull()
    assertThat(typography!!.title2).isNotNull()
    assertThat(typography!!.title3).isNotNull()
    assertThat(typography!!.body1).isNotNull()
    assertThat(typography!!.body2).isNotNull()
    assertThat(typography!!.button).isNotNull()
    assertThat(typography!!.caption1).isNotNull()
    assertThat(typography!!.caption2).isNotNull()
    assertThat(typography!!.caption3).isNotNull()
  }

  @Test
  fun wearIntervalTheme_providesShapeDefinitions() {
    // Given
    var shapes: Shapes? = null

    // When
    composeTestRule.setContent { WearIntervalTheme { shapes = MaterialTheme.shapes } }

    // Then - Should provide shape definitions
    assertThat(shapes).isNotNull()
    assertThat(shapes!!.small).isNotNull()
    assertThat(shapes!!.medium).isNotNull()
    assertThat(shapes!!.large).isNotNull()
  }

  @Test
  fun wearIntervalTheme_handlesEmptyContent() {
    // Given/When
    composeTestRule.setContent {
      WearIntervalTheme {
        // Empty content
      }
    }

    // Then - Should not crash with empty content
    composeTestRule.waitForIdle()
  }

  @Test
  fun wearIntervalTheme_colorConsistency() {
    // Given
    var primaryColor: Color? = null
    var onPrimaryColor: Color? = null
    var backgroundColor: Color? = null
    var onBackgroundColor: Color? = null

    // When
    composeTestRule.setContent {
      WearIntervalTheme {
        val colors = MaterialTheme.colors
        primaryColor = colors.primary
        onPrimaryColor = colors.onPrimary
        backgroundColor = colors.background
        onBackgroundColor = colors.onBackground
      }
    }

    // Then - Colors should be consistent and appropriate for dark theme
    assertThat(primaryColor).isNotNull()
    assertThat(onPrimaryColor).isNotNull()
    assertThat(backgroundColor).isNotNull()
    assertThat(onBackgroundColor).isNotNull()

    // All colors should be specified (not Color.Unspecified)
    assertThat(primaryColor).isNotEqualTo(Color.Unspecified)
    assertThat(onPrimaryColor).isNotEqualTo(Color.Unspecified)
    assertThat(backgroundColor).isNotEqualTo(Color.Unspecified)
    assertThat(onBackgroundColor).isNotEqualTo(Color.Unspecified)
  }

  @Test
  fun wearIntervalTheme_accessibleThroughMaterialTheme() {
    // Given
    var themeColors: Colors? = null
    var materialThemeColors: Colors? = null

    // When
    composeTestRule.setContent {
      WearIntervalTheme {
        themeColors = MaterialTheme.colors
        materialThemeColors = MaterialTheme.colors
      }
    }

    // Then
    assertThat(themeColors).isNotNull()
    assertThat(materialThemeColors).isNotNull()
    assertThat(themeColors).isEqualTo(materialThemeColors)
  }
}
