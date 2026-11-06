package com.example.camply.reservations.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.camply.campsite.model.Campsite
import com.example.camply.campsite.model.DateRange
import com.example.camply.lottery.model.Lottery
import com.example.camply.lottery.model.LotteryEntry
import com.example.camply.lottery.repository.DeclineResult
import com.example.camply.lottery.repository.LotteryEntryRepository
import com.example.camply.lottery.repository.LotteryRepository
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.Notification
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import com.example.camply.notification.service.NotificationService
import com.example.camply.reservations.controller.ReservationBookingController
import com.example.camply.reservations.model.Reservation
import com.example.camply.reservations.model.ReservationBookingModel
import com.example.camply.reservations.repository.ReservationRepository
import com.example.camply.reservations.service.BookingService
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the rules acknowledgement on the booking screen (US 02.11.01).
 *
 * Uses the real view, model, controller and service with a fake repository, so no Firebase is needed.
 */
class ReservationBookingViewTest {

    @get:Rule
    val compose = createComposeRule()

    private val campsite = Campsite(
        id = "1",
        name = "Banff Pines",
        availability = listOf(DateRange("2027-06-01", "2027-06-10")),
        rules = "Quiet hours are from 10 PM to 7 AM.",
        cancellationPolicy = "Cancel at least 7 days before arrival."
    )

    /**
     * Displays the booking screen before each test.
     */
    @Before
    fun setUp() {
        val model = ReservationBookingModel()
        val controller = ReservationBookingController(
            bookingService = BookingService(),
            repository = TestReservationRepository(),
            notificationService = NotificationService(TestNotificationRepository()),
            lotteryRepository = TestLotteryRepository(),
            lotteryEntryRepository = TestLotteryEntryRepository(),
            model = model
        )
        val view = ReservationBookingView(model, controller)

        compose.setContent {
            view.Display(
                campsite = campsite,
                entrantId = "entrant1",
                lotteryId = "lottery1",
                lotteryStayDates = DateRange("2027-06-01", "2027-06-10"),
                onBack = {}
            )
        }
    }

    /**
     * Checks that the campsite's rules and policies are shown with the acknowledgement.
     */
    @Test
    fun display_showsRulesAndCancellationPolicy() {
        compose.onNodeWithText("Campsite Rules & Policies").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(campsite.rules).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(campsite.cancellationPolicy).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("I have read and agree to the campsite rules and policies.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Checks that the confirm button starts disabled.
     */
    @Test
    fun confirmButton_initially_isDisabled() {
        confirmButton().assertIsNotEnabled()
    }

    /**
     * Checks that valid dates alone do not enable the confirm button.
     */
    @Test
    fun confirmButton_rulesNotAcknowledged_isDisabled() {
        enterAndCheckValidDates()

        confirmButton().assertIsNotEnabled()
    }

    /**
     * Checks that acknowledging the rules alone does not enable the confirm button.
     */
    @Test
    fun confirmButton_datesNotChecked_isDisabled() {
        clickRulesCheckbox()

        confirmButton().assertIsNotEnabled()
    }

    /**
     * Checks that valid dates and an acknowledgement enable the confirm button.
     */
    @Test
    fun confirmButton_validDatesAndRulesAcknowledged_isEnabled() {
        enterAndCheckValidDates()
        clickRulesCheckbox()

        confirmButton().assertIsEnabled()
    }

    /**
     * Checks that removing the acknowledgement disables the confirm button again.
     */
    @Test
    fun confirmButton_rulesUnticked_isDisabled() {
        enterAndCheckValidDates()
        clickRulesCheckbox()
        clickRulesCheckbox()

        confirmButton().assertIsNotEnabled()
    }

    /**
     * Checks that changing a date after it was checked disables the confirm button again.
     */
    @Test
    fun confirmButton_datesEditedAfterCheck_isDisabled() {
        enterAndCheckValidDates()
        clickRulesCheckbox()

        compose.onNodeWithText(END_DATE_LABEL).performScrollTo().performTextReplacement("2027-06-08")

        confirmButton().assertIsNotEnabled()
    }

    /**
     * Checks that the booking screen displays the lottery's permitted stay dates.
     * US 01.04.01.
     */
    @Test
    fun display_showsLotteryStayDates() {
        compose.onNodeWithText("2027-06-01 to 2027-06-10")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun confirmButton() = compose.onNodeWithText("Confirm Reservation").performScrollTo()

    private fun clickRulesCheckbox() {
        compose.onNode(isToggleable()).performScrollTo().performClick()
    }

    private fun enterAndCheckValidDates() {
        compose.onNodeWithText(START_DATE_LABEL).performScrollTo().performTextInput("2027-06-03")
        compose.onNodeWithText(END_DATE_LABEL).performScrollTo().performTextInput("2027-06-05")
        compose.onNodeWithText("Check Dates").performScrollTo().performClick()
        compose.onNodeWithText("Selected dates are available. Reservation is not yet confirmed.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Provides an empty reservation list without accessing Firestore.
     */
    private class TestReservationRepository : ReservationRepository {

        override suspend fun getReservationsForEntrant(entrantId: String): List<Reservation> = emptyList()

        override suspend fun getReservationsForCampsite(campsiteId: String): List<Reservation> = emptyList()

        override suspend fun createReservation(reservation: Reservation): String = "testReservationId"
    }

    /**
     * Accepts notifications without accessing Firestore.
     */
    private class TestNotificationRepository : NotificationRepository {
        override suspend fun createIfAbsent(id: String, notification: NewNotification): SendResult =
            SendResult.CREATED

        override fun observeReceived(
            recipientId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by view tests")

        override fun observeSent(
            senderId: String,
            onUpdate: (List<Notification>) -> Unit,
            onError: (Exception) -> Unit
        ): () -> Unit = error("Not used by view tests")

        override suspend fun markAsRead(notificationId: String, recipientId: String) =
            error("Not used by view tests")
    }

    private companion object {
        const val START_DATE_LABEL = "Start date (yyyy-MM-dd)"
        const val END_DATE_LABEL = "End date (yyyy-MM-dd)"
    }

    /**
     * Provides lottery information without accessing Firestore.
     */
    private class TestLotteryRepository : LotteryRepository {

        override suspend fun getLotteries(): List<Lottery> = emptyList()

        override suspend fun getLottery(id: String): Lottery? =
            if (id == "lottery1") {
                Lottery(
                    id = "lottery1",
                    campsiteIds = listOf("1"),
                    lotteryStart = "2027-06-01",
                    lotteryEnd = "2027-06-10",
                    bookingStart = "2020-01-01",
                    bookingEnd = "2099-12-31"
                )
            } else {
                null
            }

        override suspend fun create(lottery: Lottery): String = lottery.id

        override suspend fun getJoinedLotteries(entrantId: String): List<Lottery> =
            emptyList()
    }

    /**
     * Provides lottery entry information without accessing Firestore.
     */
    private class TestLotteryEntryRepository : LotteryEntryRepository {

        override suspend fun getEntry(
            lotteryId: String,
            entrantId: String
        ): LotteryEntry? = null

        override suspend fun declineOpportunity(
            lotteryId: String,
            entrantId: String
        ): DeclineResult = DeclineResult.NOT_ELIGIBLE
    }
}
