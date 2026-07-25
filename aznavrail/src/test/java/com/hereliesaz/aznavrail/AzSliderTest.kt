package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.model.AzSliderConfig
import com.hereliesaz.aznavrail.model.AzSliderOrientation
import com.hereliesaz.aznavrail.model.AzSliderVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzSliderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun azSlider_isFindableByItsDefaultDescription() {
        composeTestRule.setContent {
            Box(Modifier.width(40.dp).height(200.dp)) { AzSlider() }
        }
        // The track is drawn, not composed out of labelled parts, so its content description is the
        // only handle assistive tech (and this assertion) has on it.
        composeTestRule.onNodeWithContentDescription(AzSliderContentDescription).assertIsDisplayed()
    }

    @Test
    fun azSlider_takesTheCallersLabelWhenGivenOne() {
        composeTestRule.setContent {
            Box(Modifier.width(40.dp).height(200.dp)) { AzSlider(label = "Volume") }
        }
        composeTestRule.onNodeWithContentDescription("Volume").assertIsDisplayed()
    }

    @Test
    fun azSlider_rendersEveryVariantWithoutThrowing() {
        // One instrument in four shapes. A variant that fails to draw would otherwise only surface
        // on whichever screen happened to use it.
        composeTestRule.setContent {
            AzSliderVariant.entries.forEach { variant ->
                Box(Modifier.width(40.dp).height(60.dp)) {
                    AzSlider(
                        value = 0.4f,
                        config = AzSliderConfig(
                            variant = variant,
                            steps = 4,
                            orientation = AzSliderOrientation.VERTICAL,
                        ),
                        rangeValue = 0.2f..0.8f,
                        label = variant.name,
                    )
                }
            }
        }
        AzSliderVariant.entries.forEach { variant ->
            composeTestRule.onNodeWithContentDescription(variant.name).assertIsDisplayed()
        }
    }

    @Test
    fun railSliderItem_startsFoldedAsAnOrdinaryButtonAndUnfoldsOnTap() {
        // The point of a rail slider is that it arrives where the user is already looking: folded it
        // is indistinguishable from its neighbours, and tapping it grows the track in that same slot
        // rather than opening a panel somewhere else.
        composeTestRule.setContent {
            AzHostActivityLayout(navController = rememberNavController()) {
                azRailSlider(id = "vol", text = "Vol", value = 0.5f)
            }
        }

        composeTestRule.onNodeWithText("Vol").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vol").performClick()
        composeTestRule.waitForIdle()

        // Unfolded, the item is the track — addressed by the label it was declared with.
        composeTestRule.onNodeWithContentDescription("Vol").assertIsDisplayed()
    }
}
