package com.example.camply.shared.navigation.view

import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.camply.shared.navigation.controller.NavigationController
import com.example.camply.shared.navigation.model.NavigationModel
import com.example.camply.shared.navigation.model.Screen

/**
 * Displays the shared bottom navigation bar for Camply.
 *
 * @property navigationModel the model containing navigation state and items
 * @property navigationController the controller handling navigation requests
 */
class CamplyBottomNavBar(
    private val navigationModel: NavigationModel,
    private val navigationController: NavigationController
) {

    /**
     * Displays the bottom navigation bar for the specified user role.
     *
     * @param role the role used to determine which navigation items appear
     */
    @Composable
    fun Display(role: String, hasUnreadNotifications: Boolean = false) {

        val items = navigationModel.getNavBarItems(role)
        val currentScreen = navigationModel.getCurrentScreen()

        /*
         * AI Assistance:
         * Provider: OpenAI
         * Tool: ChatGPT
         * Date: 2026-10-29
         * Purpose: Assisted with implementing the Jetpack Compose
         * NavigationBar and NavigationBarItem components.
         *
         * Reference: Google Android Developers, Navigation bar
         * https://developer.android.com/develop/ui/compose/components/navigation-bar
         *
         * Implementation reviewed against the official Android documentation
         * and adapted for Camply's MVC navigation structure.
         */
        NavigationBar {
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentScreen == item.id,
                    onClick = {
                        navigationController.onNavItemClick(item.id)
                    },
                    icon = {
                        BadgedBox(badge = {
                            if (item.id == Screen.NOTIFICATIONS && hasUnreadNotifications) {
                                Badge(modifier = Modifier.semantics { contentDescription = "Unread notifications" })
                            }
                        }) {
                            Icon(
                            painter = painterResource(id = item.iconId),
                            contentDescription = item.label
                            )
                        }
                    },
                    label = {
                        Text(item.label)
                    },
                    enabled = item.id != Screen.BACK
                )
            }
        }
    }
}
