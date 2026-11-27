package com.example.camply.notification.service

import com.example.camply.campsite.model.DateRange
import com.example.camply.notification.model.NewNotification
import com.example.camply.notification.model.NotificationOrigin
import com.example.camply.notification.model.NotificationType
import com.example.camply.notification.model.SendResult
import com.example.camply.notification.repository.NotificationRepository
import java.security.MessageDigest

/**
 * Creates and stores notification records for application events.
 *
 * Prevents duplicate notifications for the same event and recipient.
 *
 * @param repository The repository used to persist notifications.
 */
class NotificationService(private val repository: NotificationRepository) {

    /**
     * Creates a notification if one does not already exist for the same event and recipient.
     *
     * Generates a deterministic notification ID using SHA-256.
     *
     * @param notification The notification to be stored.
     * @return [SendResult.CREATED] if a new notification was saved,
     * or [SendResult.ALREADY_EXISTS] if a matching notification already exists.
     */
    suspend fun send(notification: NewNotification): SendResult {
        // Length-prefixing avoids ambiguous event/recipient concatenation.
        val identity = "${notification.eventId.length}:${notification.eventId}${notification.recipientId}"
        val id = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return repository.createIfAbsent(id, notification)
    }

    /**
     * Sends an entrant a notification containing their lottery result.
     *
     * Call only with the persisted result. Retry failures without drawing again.
     *
     * @param ownerId The user ID of the campsite's owner.
     * @param entrantId The user ID of the entrant receiving the lottery result.
     * @param lotteryId The ID of the lottery.
     * @param won Whether the entrant was selected by the lottery.
     * @return [SendResult.CREATED] if a new notification was saved,
     * or [SendResult.ALREADY_EXISTS] if a matching notification already exists.
     */
    suspend fun sendLotteryResult(
        ownerId: String,
        entrantId: String,
        lotteryId: String,
        won: Boolean
    ): SendResult = send(NewNotification(
        eventId = "lottery-result:$lotteryId",
        senderId = ownerId,
        recipientId = entrantId,
        origin = NotificationOrigin.SYSTEM,
        type = if (won) NotificationType.LOTTERY_WIN else NotificationType.LOTTERY_LOSE,
        title = if (won) "You won the campsite lottery!" else "Campsite lottery result",
        content = if (won) {
            "You won an opportunity to reserve a campsite. Check the lottery booking dates and available stays."
        } else {
            "You were not selected in this campsite lottery. You can explore other campsites and lotteries."
        },
        lotteryId = lotteryId
    ))

    /**
     * Sends an entrant the confirmation of a saved reservation (US 01.05.01).
     *
     * Uses the reservation ID to generate a consistent event ID, preventing duplicate
     * notifications for the same reservation event and recipient.
     *
     * @param ownerId The user ID of the campsite's owner.
     * @param entrantId The user ID of the entrant who made the reservation.
     * @param reservationId The ID of the saved reservation.
     * @param campsiteName The name of the reserved campsite.
     * @param dates The reserved dates.
     * @return [SendResult.CREATED] if a new notification was saved,
     * or [SendResult.ALREADY_EXISTS] if a matching notification already exists.
     */
    suspend fun sendReservationConfirmation(
        ownerId: String,
        entrantId: String,
        reservationId: String,
        campsiteName: String,
        dates: DateRange
    ): SendResult = send(NewNotification(
        eventId = "reservation-confirmed:$reservationId",
        senderId = ownerId,
        recipientId = entrantId,
        origin = NotificationOrigin.SYSTEM,
        type = NotificationType.RESERVATION_CONFIRMED,
        title = "Reservation confirmed",
        content = "Your reservation at $campsiteName from ${dates.from} to ${dates.to} is confirmed.",
        reservationId = reservationId
    ))

    /**
     * Sends an entrant the confirmation of a cancelled reservation (US 01.07.02).
     *
     * Uses the reservation ID to generate a consistent event ID, preventing duplicate
     * notifications for the same reservation event and recipient.
     *
     * @param ownerId The user ID of the campsite's owner.
     * @param entrantId The user ID of the entrant who cancelled the reservation.
     * @param reservationId The ID of the cancelled reservation.
     * @param campsiteName The name of the reserved campsite.
     * @param dates The cancelled reservation's dates.
     * @return [SendResult.CREATED] if a new notification was saved,
     * or [SendResult.ALREADY_EXISTS] if a matching notification already exists.
     */
    suspend fun sendReservationCancellation(
        ownerId: String,
        entrantId: String,
        reservationId: String,
        campsiteName: String,
        dates: DateRange
    ): SendResult = send(NewNotification(
        eventId = "reservation-cancelled:$reservationId",
        senderId = ownerId,
        recipientId = entrantId,
        origin = NotificationOrigin.SYSTEM,
        type = NotificationType.RESERVATION_CANCELLED,
        title = "Reservation cancelled",
        content = "Your reservation at $campsiteName from ${dates.from} to ${dates.to} has been cancelled.",
        reservationId = reservationId
    ))
    suspend fun sendLotteryCancelled(
        ownerId: String,
        entrantId: String,
        lotteryId: String,
        campsiteId: String
    ): SendResult = send(NewNotification(
        eventId = "lottery-cancelled:$lotteryId:campsite:$campsiteId",
        senderId = ownerId,
        recipientId = entrantId,
        origin = NotificationOrigin.SYSTEM,
        type = NotificationType.LOTTERY_CANCELLED,
        title = "Lottery cancelled",
        content = "The lottery has been canceled due to the campsite being taken down.",
        lotteryId = lotteryId
    ))

    suspend fun sendCampsiteRemovedFromLottery(
        ownerId: String,
        entrantId: String,
        lotteryId: String,
        campsiteId: String
    ): SendResult = send(NewNotification(
        eventId = "lottery-campsite-removed:$lotteryId:campsite:$campsiteId",
        senderId = ownerId,
        recipientId = entrantId,
        origin = NotificationOrigin.SYSTEM,
        type = NotificationType.CAMPSITE_REMOVED_FROM_LOTTERY,
        title = "Campsite removed",
        content = "One of the campsites has been removed.",
        lotteryId = lotteryId
    ))

    suspend fun sendReservationRemovedForCampsite(
        ownerId: String,
        entrantId: String,
        reservationId: String,
        campsiteName: String,
        dates: com.example.camply.campsite.model.DateRange
    ): SendResult = send(NewNotification(
        eventId = "reservation-removed:$reservationId",
        senderId = ownerId,
        recipientId = entrantId,
        origin = NotificationOrigin.SYSTEM,
        type = NotificationType.RESERVATION_CANCELLED,
        title = "Reservation cancelled",
        content = "Your reservation at $campsiteName from ${dates.from} to ${dates.to} has been canceled because the campsite was removed.",
        reservationId = reservationId
    ))
    /**
     * Tells a lottery owner that an entrant was removed from a lottery that has not ended.
     *
     * The entrant is not notified, because their profile is being deleted.
     *
     * @param ownerId Owner of the lottery. This is the recipient.
     * @param entrantId Entrant whose entry was deleted. Stored as the sender.
     * @param lotteryId Lottery that still exists for its other entrants.
     * @return [SendResult.CREATED] when a new notification is saved,
     * or [SendResult.ALREADY_EXISTS] when that event was already sent.
     */
    suspend fun sendEntrantRemovedFromLottery(
        ownerId: String,
        entrantId: String,
        lotteryId: String
    ): SendResult = send(NewNotification(
        eventId = "entrant-removed-from-lottery:$lotteryId:entrant:$entrantId",
        senderId = entrantId,
        recipientId = ownerId,
        origin = NotificationOrigin.SYSTEM,
        type = NotificationType.ENTRANT_REMOVED_FROM_LOTTERY,
        title = "Entrant removed",
        content = "An entrant has been removed from your lottery.",
        lotteryId = lotteryId
    ))

    /**
     * Tells a campsite owner that a future reservation was cancelled because the entrant was removed (US 03.02.01).
     *
     * @param ownerId Owner of the reserved campsite. This is the recipient.
     * @param entrantId Entrant who held the reservation.
     * @param reservationId Reservation that was deleted.
     * @param campsiteName Name shown in the message.
     * @param dates Stay dates shown in the message.
     * @return [SendResult.CREATED] when a new notification is saved,
     * or [SendResult.ALREADY_EXISTS] when that event was already sent.
     */
    suspend fun sendReservationRemovedForEntrant(
        ownerId: String,
        entrantId: String,
        reservationId: String,
        campsiteName: String,
        dates: com.example.camply.campsite.model.DateRange
    ): SendResult = send(NewNotification(
        eventId = "reservation-removed-for-entrant:$reservationId",
        senderId = entrantId,
        recipientId = ownerId,
        origin = NotificationOrigin.SYSTEM,
        type = NotificationType.RESERVATION_CANCELLED,
        title = "Reservation cancelled",
        content = "The reservation at $campsiteName from ${dates.from} to ${dates.to} was cancelled because the entrant was removed.",
        reservationId = reservationId
    ))
}
