package com.example.camply

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.camply.lottery.controller.CampsiteLotteryController
import com.example.camply.lottery.controller.EntrantLotteryController
import com.example.camply.lottery.controller.LotteryBookingController
import com.example.camply.lottery.controller.LotteryOpportunityController
import com.example.camply.lottery.controller.LotteryCreateController
import com.example.camply.lottery.model.CampsiteLotteryModel
import com.example.camply.lottery.model.EntrantLotteryModel
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryBookingModel
import com.example.camply.lottery.model.LotteryOpportunityModel
import com.example.camply.lottery.model.LotteryCreateModel
import com.example.camply.lottery.repository.FirestoreLotteryEntryRepository
import com.example.camply.lottery.repository.FirestoreLotteryDrawRepository
import com.example.camply.lottery.repository.FirestoreLotteryRepository
import com.example.camply.lottery.repository.FirestoreWaitingListRepository
import com.example.camply.lottery.service.LotteryDrawService
import com.example.camply.lottery.view.CampsiteLotteryView
import com.example.camply.lottery.view.EntrantLotteryView
import com.example.camply.lottery.view.LotteryOpportunityView
import com.example.camply.lottery.view.LotteryCreateView
import com.example.camply.notification.controller.NotificationsController
import com.example.camply.notification.model.NotificationsModel
import com.example.camply.notification.repository.FirestoreNotificationRepository
import com.example.camply.notification.repository.FirestoreRecipientNameRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.notification.view.NotificationsView
import com.example.camply.reservations.controller.ActiveReservationsController
import com.example.camply.reservations.controller.OwnerReservationsController
import com.example.camply.reservations.controller.ReservationBookingController
import com.example.camply.reservations.model.ActiveReservationsModel
import com.example.camply.reservations.model.OwnerReservationsModel
import com.example.camply.reservations.model.ReservationBookingModel
import com.example.camply.reservations.repository.FirestoreReservationRepository
import com.example.camply.reservations.service.BookingService
import com.example.camply.reservations.view.ActiveReservationsView
import com.example.camply.reservations.view.OwnerReservationsView
import com.example.camply.reservations.view.ReservationBookingView
import com.example.camply.shared.navigation.controller.NavigationController
import com.example.camply.shared.navigation.model.NavigationModel
import com.example.camply.shared.navigation.model.Screen
import com.example.camply.shared.navigation.view.CamplyBottomNavBar
import com.example.camply.shared.view.ErrorView
import com.example.camply.shared.view.LoadingView
import com.example.camply.shared.view.PrimaryButton
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.user.controller.OwnerController
import com.example.camply.user.controller.UserController
import com.example.camply.user.model.CampsiteOwnerUser
import com.example.camply.user.model.UserModel
import com.example.camply.user.view.CampsiteFormView
import com.example.camply.user.view.CampsiteListView
import com.example.camply.user.view.UserProfileView
import com.example.camply.user.view.UserSignUpView
import kotlinx.coroutines.launch

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

    // Owner lottery registration. US 02.04.01, 02.04.03, 02.04.04, 02.04.05.
    val lotteryCreateModel = remember { LotteryCreateModel() }
    val lotteryRepository = remember { FirestoreLotteryRepository() }
    val lotteryDrawService = remember {
        LotteryDrawService(
            FirestoreLotteryDrawRepository(),
            NotificationService(FirestoreNotificationRepository())
        )
    }
    val lotteryCreateController = remember {
        LotteryCreateController(lotteryCreateModel, lotteryRepository, campsiteRepository)
    }
    val lotteryCreateView = remember {
        LotteryCreateView(lotteryCreateModel, lotteryCreateController)
    }

    // ID of the campsite currently open from the browse screen (null = show the list).
    var selectedCampsiteId by remember { mutableStateOf<String?>(null) }

    // Entrant lottery management
    val entrantLotteryModel = remember { EntrantLotteryModel() }
    val entrantLotteryView = remember(entrantLotteryModel) { EntrantLotteryView(entrantLotteryModel) }
    val entrantLotteryRepository = remember { FirestoreLotteryRepository() }
    val lotteryEntryRepository = remember { FirestoreLotteryEntryRepository() }
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
    val notificationService = remember { NotificationService(FirestoreNotificationRepository()) }
    val reservationBookingController = remember {
        ReservationBookingController(
            bookingService = bookingService,
            repository = reservationRepository,
            notificationService = notificationService,
            lotteryRepository = lotteryRepository,
            lotteryEntryRepository = lotteryEntryRepository,
            model = reservationBookingModel
        )
    }
    val reservationBookingView = remember { ReservationBookingView(reservationBookingModel, reservationBookingController) }


    // Entrant lottery booking
    val lotteryBookingModel = remember { LotteryBookingModel() }
    val lotteryBookingController = remember { LotteryBookingController(lotteryBookingModel, campsiteRepository) }
    var bookingRequest by remember { mutableStateOf<Lottery?>(null) }

    // Entrant active reservations
    val activeReservationsModel = remember { ActiveReservationsModel() }
    val activeReservationsController = remember {
        ActiveReservationsController(activeReservationsModel, reservationRepository, campsiteRepository)
    }
    val activeReservationsView = remember {
        ActiveReservationsView(activeReservationsModel, activeReservationsController)
    }

    // Owner reservation management
    val ownerReservationsModel = remember { OwnerReservationsModel() }

    val ownerReservationsController = remember {
        OwnerReservationsController(
            ownerReservationsModel,
            campsiteRepository,
            reservationRepository
        )
    }

    val ownerReservationsView = remember {
        OwnerReservationsView(
            ownerReservationsModel,
            ownerReservationsController
        )
    }

    //Campsite Owner Handling
    val campsiteListView = remember { CampsiteListView() }
    val campsiteFormView = remember { CampsiteFormView() }
    var ownerDetailCampsiteId by remember { mutableStateOf<String?>(null) }
    var ownerShowForm by remember { mutableStateOf(false) }
    var ownerEditCampsite by remember { mutableStateOf<Campsite?>(null) }
    var ownerShowLottery by remember { mutableStateOf(false) }
    var ownerShowReservations by remember { mutableStateOf(false) }

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

    val currentScreen = navigationModel.getCurrentScreen()

    LaunchedEffect(currentScreen) {
        if (currentScreen != Screen.LOTTERIES) {
            reservationBookingController.resetBooking()
            lotteryBookingController.clearBooking()
        }
    }

    LaunchedEffect(bookingRequest) {
        val lottery = bookingRequest

        if (lottery != null) {
            lotteryBookingController.loadBooking(lottery)
            bookingRequest = null
        }
    }

    LaunchedEffect(Unit) {
        try {
            lotteryDrawService.keepChecking()
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        }
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
            android.util.Log.d("Camply", "startup=${userModel.getStartupState()} user=$currentUser role=${currentUser?.getRole()} isOwner=${currentUser is CampsiteOwnerUser}")
            val owner = currentUser as? CampsiteOwnerUser
            val scope = rememberCoroutineScope()
            val ownerController = remember(owner) {
                owner?.let { OwnerController(it, campsiteRepository) }
            }

            LaunchedEffect(ownerController) {
                if (ownerController != null) {
                    navigationModel.navigateTo(Screen.CAMPSITE_LIST)
                    ownerController.loadCampsite()
                }
            }
            if (currentUser != null)
            {
                DisposableEffect(currentUser.getUid(), currentUser.getRole()) {
                    notificationsController.startObserving(currentUser.getUid(), currentUser.getRole())
                    onDispose { notificationsController.stopObserving() }
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (navigationModel.getShowNavBar()) {
                            bottomNavBar.Display(currentUser.getRole(), notificationsModel.hasUnread)
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

                                val campsite = lotteryBookingModel.campsite

                                if (campsite == null) {
                                    Column {
                                        if (lotteryBookingModel.isLoading) {
                                            Text("Loading campsite...")
                                        }

                                        if (lotteryBookingModel.errorMessage != null) {
                                            Text(lotteryBookingModel.errorMessage ?: "")
                                        }

                                        entrantLotteryView.Display { lottery ->
                                            val opportunityModel = remember(lottery.id, currentUser.getUid()) {
                                                LotteryOpportunityModel()
                                            }
                                            val opportunityView = remember(opportunityModel) {
                                                LotteryOpportunityView(
                                                    opportunityModel,
                                                    LotteryOpportunityController(
                                                        opportunityModel,
                                                        lotteryEntryRepository,
                                                        reservationRepository,
                                                        bookingService
                                                    )
                                                )
                                            }
                                            opportunityView.Display(
                                                lotteryId = lottery.id,
                                                entrantId = currentUser.getUid(),
                                                onBook = {
                                                    bookingRequest = lottery
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    BackHandler {
                                        reservationBookingController.resetBooking()
                                        lotteryBookingController.clearBooking()
                                    }

                                    reservationBookingView.Display(
                                        campsite = campsite,
                                        entrantId = currentUser.getUid(),
                                        lotteryId = lotteryBookingModel.lotteryId ?: "",
                                        lotteryStayDates = lotteryBookingModel.stayDates,
                                        onBack = {
                                            reservationBookingController.resetBooking()
                                            lotteryBookingController.clearBooking()
                                        }
                                    )
                                }
                            }

                            Screen.RESERVATIONS -> activeReservationsView.Display(currentUser.getUid())

                            Screen.CAMPSITE_LIST -> {
                                if (ownerController != null) {
                                    val detailId = ownerDetailCampsiteId

                                    if (ownerShowReservations) {
                                        ownerReservationsView.Display(
                                            ownerId = currentUser.getUid(),
                                            onBack = { ownerShowReservations = false }
                                        )
                                    } else if (ownerShowLottery) {
                                        BackHandler {
                                            ownerShowLottery = false
                                        }

                                        lotteryCreateView.Display(
                                            ownerId = currentUser.getUid(),
                                            onDone = { ownerShowLottery = false }
                                        )
                                    } else if (ownerShowForm) {
                                        BackHandler {
                                            ownerController.removeErrorsFromList()
                                            ownerShowForm = false
                                            ownerEditCampsite = null
                                        }

                                        campsiteFormView.Display(
                                            campsite = ownerEditCampsite,
                                            errors = ownerController.errors,
                                            onSubmit = { name, location, description, amenities, occupancy, price, tags, rules, cancellation ->
                                                scope.launch {
                                                    val existing = ownerEditCampsite
                                                    val ok = if (existing == null) {
                                                        ownerController.registerCampsite(name, location, description, amenities, occupancy, price, tags, rules, cancellation)
                                                    } else {
                                                        ownerController.updateCampsite(existing, name, location, description, amenities, occupancy, price, tags, rules, cancellation)
                                                    }
                                                    if (ok) {
                                                        ownerShowForm = false
                                                        ownerEditCampsite = null
                                                        ownerDetailCampsiteId = null
                                                    }
                                                }
                                            },
                                            onBack = {
                                                ownerController.removeErrorsFromList()
                                                ownerShowForm = false
                                                ownerEditCampsite = null
                                            },
                                            navigationModel = navigationModel,
                                            navigationController = navigationController
                                        )

                                    } else if (detailId != null) {
                                        BackHandler { ownerDetailCampsiteId = null }
                                        campsiteDetailView.Display(
                                            campsiteId = detailId,
                                            onBack = { ownerDetailCampsiteId = null },
                                            extraContent = { campsite ->
                                                Button(onClick = {
                                                    ownerEditCampsite = campsite
                                                    ownerShowForm = true
                                                }) {
                                                    Text("Edit campsite")
                                                }
                                            }
                                        )
                                    } else {
                                        campsiteListView.Display(
                                            ownerName = "Owner 1",
                                            campsites = ownerController.campsites,
                                            addCampsite = { ownerShowForm = true },
                                            editCampsite = {
                                                ownerEditCampsite = it
                                                ownerShowForm = true
                                            },
                                            viewCampsite = { ownerDetailCampsiteId = it.id },
                                            lotteryRegistration = { campsite ->
                                                lotteryCreateController.onCampsiteSelected(campsite.id)
                                                ownerShowLottery = true
                                            },
                                            reservationList = { ownerShowReservations = true },
                                            viewOrCreateLottery = { }
                                        )
                                    }
                                }
                            }

                            Screen.PROFILE -> userProfileView.Display(currentUser, userController)

                            Screen.NOTIFICATIONS -> notificationsView.Display(
                                currentUser.getUid(), currentUser.getRole(), manageObservation = false
                            )

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