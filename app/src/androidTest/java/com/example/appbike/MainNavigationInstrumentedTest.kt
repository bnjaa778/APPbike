package com.example.appbike

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.appbike.ui.theme.APPbikeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainNavigationInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bottomBarShowsOnlyActiveLabelAndKeepsEveryAccessibleName() {
        var selected: AppScreen? = null
        val destinations = listOf(
            MainDestination("Inicio", AppScreen.HOME, Icons.Outlined.Home),
            MainDestination("Marketplace", AppScreen.MARKETPLACE, Icons.Outlined.Storefront),
            MainDestination("Mapa", AppScreen.ROUTES, Icons.Outlined.Map, emphasized = true),
            MainDestination("Chat", AppScreen.CHAT, Icons.Outlined.ChatBubbleOutline),
            MainDestination("Perfil", AppScreen.ACCOUNT, Icons.Outlined.PersonOutline)
        )

        compose.setContent {
            APPbikeTheme(dynamicColor = false) {
                AppBottomBar(
                    destinations = destinations,
                    currentScreen = AppScreen.ROUTES,
                    unreadMessages = 0,
                    onNavigate = { selected = it }
                )
            }
        }

        compose.onNodeWithText("Mapa").assertIsDisplayed()
        compose.onAllNodesWithText("Inicio").assertCountEquals(0)
        destinations.forEach { destination ->
            compose.onAllNodesWithContentDescription(destination.label).assertCountEquals(1)
        }
        compose.onNodeWithContentDescription("Chat").performClick()
        compose.runOnIdle { assertEquals(AppScreen.CHAT, selected) }
    }
}
