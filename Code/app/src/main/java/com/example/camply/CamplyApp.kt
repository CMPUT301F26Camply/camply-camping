package com.example.camply

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.CampsiteBrowseModel
import com.example.camply.campsite.model.CampsiteDetailModel
import com.example.camply.campsite.model.DateRange
import com.example.camply.campsite.model.FirestoreCampsiteRepository
import com.example.camply.campsite.view.CampsiteBrowseView
import com.example.camply.campsite.view.CampsiteDetailView
import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.view.EntrantLotteryView
import com.example.camply.notification.controller.NotificationsController
import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.repository.FirestoreNotificationRepository
import com.example.camply.notification.repository.FirestoreRecipientNameRepository
import com.example.camply.notification.view.NotificationsView
import com.example.camply.reservations.controller.ActiveReservationsController
import com.example.camply.reservations.controller.ReservationBookingController
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.reservations.model.ReservationBookingModel
import com.example.camply.reservations.repository.FirestoreReservationRepository
import com.example.camply.reservations.service.BookingService
import com.example.camply.reservations.view.ActiveReservationsView
import com.example.camply.reservations.view.ReservationBookingView
import com.example.camply.lottery.controller.CampsiteLotteryController
import com.example.camply.lottery.controller.EntrantLotteryController
import com.example.camply.lottery.model.CampsiteLotteryModel
import com.example.camply.lottery.repository.FirestoreLotteryRepository
import com.example.camply.lottery.repository.FirestoreWaitingListRepository
import com.example.camply.lottery.view.CampsiteLotteryView
import com.example.camply.shared.navigation.controller.NavigationController
import com.example.camply.shared.navigation.model.NavigationModel
import com.example.camply.shared.navigation.model.Screen
import com.example.camply.shared.navigation.view.CamplyBottomNavBar
import com.example.camply.shared.view.ErrorView
import com.example.camply.shared.view.LoadingView
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.user.controller.UserController
import com.example.camply.user.model.UserModel
import com.example.camply.user.view.UserProfileView
import com.example.camply.user.view.UserSignUpView

/**
 * Displays the Camply application and manages its startup screens
 * Shows the loading screen, signup screen, main application, or
 * error screen depending on the current user state.
 */
@Composable
fun CamplyApp() {

    // Create and retain the user model, controller, and user views.
    val userModel = remember { UserModel() }
    val userController = remember { UserController(userModel) }
    val userSignUpView = remember { UserSignUpView() }
    val userProfileView = remember { UserProfileView() }

    // Create and retain the navigation components.
    val navigationModel = remember { NavigationModel() }
    val navigationController = remember { NavigationController(navigationModel) }
    val bottomNavBar = remember { CamplyBottomNavBar(navigationModel, navigationController) }
    val loadingView = remember { LoadingView() }
    val errorView = remember { ErrorView() }

    // Create and retain the campsite discovery components (phase 2B).
    val campsiteRepository = remember { FirestoreCampsiteRepository() }
    val waitingListRepository = remember { FirestoreWaitingListRepository() }
    val campsiteBrowseModel = remember { CampsiteBrowseModel() }
    val campsiteBrowseController = remember { CampsiteBrowseController(campsiteBrowseModel, campsiteRepository) }
    val campsiteBrowseView = remember { CampsiteBrowseView(campsiteBrowseModel, campsiteBrowseController) }
    val campsiteDetailModel = remember { CampsiteDetailModel() }
    val campsiteDetailController = remember { CampsiteDetailController(campsiteDetailModel, campsiteRepository) }
    val campsiteDetailView = remember { CampsiteDetailView(campsiteDetailModel, campsiteDetailController) }

    // ID of the campsite currently open from the browse screen (null = show the list).
    var selectedCampsiteId by remember { mutableStateOf<String?>(null) }

    // Entrant lottery management
    val entrantLotteryModel = remember { EntrantLotteryModel() }
    val entrantLotteryView = remember(entrantLotteryModel) { EntrantLotteryView(entrantLotteryModel) }
    val entrantLotteryRepository = remember { FirestoreLotteryRepository() }
    val entrantLotteryController = remember {
        EntrantLotteryController(
            entrantLotteryModel,
            entrantLotteryRepository
        )
    }


    // Entrant reservation booking
    val reservationBookingModel = remember { ReservationBookingModel() }
    val reservationRepository = remember { FirestoreReservationRepository() }
    val bookingService = remember { BookingService() }
    val reservationBookingController = remember { ReservationBookingController(bookingService, reservationRepository, reservationBookingModel) }
    val reservationBookingView = remember { ReservationBookingView(reservationBookingModel, reservationBookingController) }
    var selectedBookingCampsite by remember { mutableStateOf<Campsite?>(null) }

    // Entrant active reservations
    val activeReservationsModel = remember { ActiveReservationsModel() }
    val activeReservationsController = remember {
        ActiveReservationsController(activeReservationsModel, reservationRepository, campsiteRepository)
    }
    val activeReservationsView = remember {
        ActiveReservationsView(activeReservationsModel, activeReservationsController)
    }

    // Shared notifications
    val notificationsModel = remember { NotificationsModel() }
    val notificationsController = remember {
        NotificationsController(
            notificationsModel,
            FirestoreNotificationRepository(),
            FirestoreRecipientNameRepository()
        )
    }
    val notificationsView = remember { NotificationsView(notificationsModel, notificationsController) }

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
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    userSignUpView.Display(userController)
                }
            }
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
                            Screen.BROWSE -> {
                                val openId = selectedCampsiteId
                                if (openId == null) {
                                    campsiteBrowseView.Display(onCampsiteClick = { selectedCampsiteId = it })
                                } else {
                                    BackHandler { selectedCampsiteId = null }
                                    campsiteDetailView.Display(
                                        campsiteId = openId,
                                        onBack = { selectedCampsiteId = null },
                                        extraContent = { campsite ->
                                            if (currentUser.getRole() == "entrant") {
                                                val lotteryModel = remember(campsite.id, currentUser.getUid()) {
                                                    CampsiteLotteryModel(waitingListRepository, campsite.id, currentUser.getUid())
                                                }
                                                val lotteryView = remember(lotteryModel) {
                                                    CampsiteLotteryView(lotteryModel, CampsiteLotteryController(lotteryModel))
                                                }
                                                lotteryView.Display()
                                            }
                                        }
                                    )
                                }
                            }

                            Screen.LOTTERIES -> {
                                LaunchedEffect(currentUser.getUid()) {
                                    entrantLotteryController.loadLotteries(currentUser.getUid())
                                }

                                val campsite = selectedBookingCampsite

                                if (campsite == null) {
                                    // TEMPORARY: Booking screen preview. Will remove after proper lottery integration.
                                    Column {
                                        PrimaryButton(
                                            text = "Preview Booking Screen",
                                            onClick = {
                                                selectedBookingCampsite = Campsite(
                                                    id = "preview",
                                                    name = "Preview Campsite",
                                                    availability = listOf(
                                                        DateRange(
                                                            "2027-06-01",
                                                            "2027-06-10"
                                                        )
                                                    ),
                                                    rules = "Quiet hours are from 10 PM to 7 AM."
                                                )
                                            }
                                        )

                                        entrantLotteryView.Display()
                                    }
                                } else {
                                    BackHandler { selectedBookingCampsite = null }

                                    reservationBookingView.Display(
                                        campsite = campsite,
                                        onBack = { selectedBookingCampsite = null }
                                    )
                                }
                            }

                            Screen.RESERVATIONS -> activeReservationsView.Display(currentUser.getUid())

                            Screen.PROFILE -> userProfileView.Display(currentUser, userController)

                            Screen.NOTIFICATIONS -> notificationsView.Display(currentUser.getUid(), currentUser.getRole())

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
