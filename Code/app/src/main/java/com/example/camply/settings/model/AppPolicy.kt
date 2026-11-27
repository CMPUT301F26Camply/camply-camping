package com.example.camply.settings.model

// AppPolicy.kt - The app policy and guidelines shown to entrants (US 01.11.01).
// Kept as static data because it changes with app releases, not per user.
// Outstanding issues: wording should be reviewed by the team before the final demo.

/**
 * One titled section of the app policy.
 *
 * @property title The section heading.
 * @property body The section text.
 */
data class PolicySection(val title: String, val body: String)

/** The full policy and guidelines content. */
object AppPolicy {
    val sections: List<PolicySection> = listOf(
        PolicySection(
            "Fair lottery",
            "Campsites are reserved through a lottery, not first-come first-served. Joining a waiting " +
                    "list gives you a chance to win, and every entrant in the draw has the same chance. " +
                    "Please join once per campsite lottery."
        ),
        PolicySection(
            "Reservations and cancellations",
            "If you win, you can book the dates offered or decline the opportunity. A declined or " +
                    "used opportunity cannot be reused. Each campsite has its own rules and cancellation " +
                    "policy, which you must confirm before booking. Owners may reject a reservation, for " +
                    "example because of safety warnings."
        ),
        PolicySection(
            "Ratings and reviews",
            "Ratings and reviews should reflect your own experience. Do not post abusive, " +
                    "discriminatory, misleading or personal content. Administrators may remove ratings and " +
                    "reviews that break these guidelines."
        ),
        PolicySection(
            "Your information",
            "Camply identifies you by your device. We store the name, email and phone number you " +
                    "provide, and your reservations, ratings and reviews. Your name is shown beside the " +
                    "reviews you post."
        ),
        PolicySection(
            "Notifications",
            "Camply notifies you about lottery results and reservation confirmations. Owners may also " +
                    "send messages to entrants on their waiting list."
        ),
        PolicySection(
            "Respect the campsite",
            "Follow each campsite's rules, respect other campers and leave no trace. Accounts that " +
                    "break app policy may be removed by an administrator."
        )
    )
}