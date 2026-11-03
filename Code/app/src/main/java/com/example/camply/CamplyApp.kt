package com.example.camply

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.camply.campsite.controller.CampsiteBrowseController
import com.example.camply.campsite.controller.CampsiteDetailController
import com.example.camply.campsite.model.CampsiteBrowseModel
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.FirestoreCampsiteRepository
import com.example.camply.campsite.view.CampsiteBrowseView
import com.example.camply.campsite.view.CampsiteDetailView
import com.example.camply.shared.navigation.controller.NavigationController
import com.example.camply.shared.navigation.model.NavigationModel
import com.example.camply.shared.navigation.view.CamplyBottomNavBar
import com.example.camply.shared.view.ErrorView
import com.example.camply.shared.view.LoadingView
import com.example.camply.user.controller.UserController
import com.example.camply.user.model.UserModel
import com.example.camply.user.view.UserSignUpView

/**
 * Displays the Camply application and manages its startup screens
 * Shows the loading screen, signup screen, main application, or
 * error screen depending on the current user state.
 */
@Composable
fun CamplyApp() {

    // Create and retain the user model, controller, and signup view.
    val userModel = remember { UserModel() }
    val userController = remember { UserController(userModel) }
    val userSignUpView = remember { UserSignUpView() }

    // Create and retain the navigation components.
    val navigationModel = remember { NavigationModel() }
    val navigationController = remember { NavigationController(navigationModel) }
    val bottomNavBar = remember { CamplyBottomNavBar(navigationModel, navigationController) }
    val loadingView = remember { LoadingView() }
    val errorView = remember { ErrorView() }

    // Create and retain the campsite discovery components (phase 2B).
    val campsiteRepository = remember { FirestoreCampsiteRepository() }
    val campsiteBrowseModel = remember { CampsiteBrowseModel() }
    val campsiteBrowseController = remember { CampsiteBrowseController(campsiteBrowseModel, campsiteRepository) }
    val campsiteBrowseView = remember { CampsiteBrowseView(campsiteBrowseModel, campsiteBrowseController) }
    val campsiteDetailModel = remember { CampsiteDetailModel() }
    val campsiteDetailController = remember { CampsiteDetailController(campsiteDetailModel, campsiteRepository) }
    val campsiteDetailView = remember { CampsiteDetailView(campsiteDetailModel, campsiteDetailController) }

    // Id of the campsite currently open from the browse screen (null = show the list).
    var selectedCampsiteId by remember { mutableStateOf<String?>(null) }

    // Check for an existing Firebase user or sign in anonymously
    LaunchedEffect(Unit) {
        userModel.initializeUser()
    }

    // Need to show the correct screen depending on the users startup state
    when (userModel.getStartupState()) {
        "loading" -> {
            loadingView.Display()
        }

        "signup" -> {
            userSignUpView.Display(userController)
        }

        "ready" -> {
            val currentUser = userModel.getCurrentUser()

            if (currentUser != null) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (navigationModel.getShowNavBar()) {
                            bottomNavBar.Display(currentUser.getRole())
                        }
                    }
                ) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (navigationModel.getCurrentScreen()) {
                            "browse" -> {
                                val openId = selectedCampsiteId
                                if (openId == null) {
                                    campsiteBrowseView.Display(onCampsiteClick = { selectedCampsiteId = it })
                                } else {
                                    BackHandler { selectedCampsiteId = null }
                                    campsiteDetailView.Display(
                                        campsiteId = openId,
                                        onBack = { selectedCampsiteId = null }
                                    )
                                }
                            }
                            else -> Text("Screen still needs to be implemented...")
                        }
                    }
                }
            }
        }

        "error" -> {
            errorView.Display()
        }
    }
}