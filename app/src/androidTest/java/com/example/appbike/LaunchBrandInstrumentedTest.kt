package com.example.appbike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Rule
import org.junit.Test

class LaunchBrandInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun launchBrandUsesSharedLogoAndShowsSessionStatus() {
        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                LaunchBrandScreen()
            }
        }

        compose.onNodeWithContentDescription("APPBIKE").assertIsDisplayed()
        compose.onNodeWithText(LAUNCH_BRAND_STATUS_TEXT).assertIsDisplayed()
    }
}
