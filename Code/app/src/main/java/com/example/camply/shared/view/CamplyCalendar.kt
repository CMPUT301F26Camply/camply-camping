
package com.example.camply.shared.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.camply.campsite.model.DateRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


// CamplyCalendar.kt - Shared Calendar Component
//
// Contributor: Filip Kosmajac (fkosmajac)
// AI Assistance Reference:
// OpenAI ChatGPT (GPT-6.2)
// https://chatgpt.com/
//
// Purpose of AI assistance:
// AI was used to help design and implement a reusable interactive calendar component
// for selecting date ranges throughout Camply.
//
// Starting Prompt:
// "I want to create a reusable calendar component that can be used across multiple screens
// in Camply instead of selecting start and end dates separately. The user should be able to
// select a start date and then an end date within the same interactive calendar. The component
// should support optional allowed date ranges, grey out unavailable dates, and display blocked
// or already booked dates in a different color. It should prevent users from selecting a range
// containing unavailable or blocked dates and include date summaries, clear and confirm buttons.
// The confirmed selection should return our existing DateRange model. I want the component to
// be reusable both inline and within dialogs, allowing individual screens to provide their
// own instructions, legends, validation and navigation."
//
// Additional follow-up prompts were used to refine the component's responsibilities, presentation
// options, selection behavior and integration with the existing Camply application structure.


/**
 * Displays an interactive calendar with start and end date selection.
 *
 * @param onConfirm Returns the confirmed date range.
 * @param modifier Modifier applied to the calendar.
 * @param initialDates Previously selected dates to display when opening the calendar.
 * @param allowedDates Date ranges within which the user is allowed to select dates.
 * All dates outside these ranges are greyed out and cannot be selected.
 * If null, all dates are allowed. For example, restricting an entrant's
 * selection to a lottery's permitted stay period.
 * @param blockedDates Date ranges that are unavailable for selection, even if
 * they fall within an allowed date range. These dates are displayed in a
 * different color, and selected ranges cannot overlap them. For example,
 * displaying already reserved campsite dates.
 * @param fromLabel Label for the selected start date.
 * @param toLabel Label for the selected end date.
 * @param confirmLabel Label for the confirmation button.
 */
@Composable
fun CamplyCalendar(
    onConfirm: (DateRange) -> Unit,
    modifier: Modifier = Modifier,
    initialDates: DateRange = DateRange(),
    allowedDates: List<DateRange>? = null,
    blockedDates: List<DateRange> = emptyList(),
    fromLabel: String = "From",
    toLabel: String = "To",
    confirmLabel: String = "Confirm dates"
) {
    var selectedFrom by rememberSaveable(initialDates.from, initialDates.to) {
        mutableStateOf(initialDates.from)
    }
    var selectedTo by rememberSaveable(initialDates.from, initialDates.to) {
        mutableStateOf(initialDates.to)
    }

    var displayedMonth by rememberSaveable(initialDates.from, initialDates.to) {
        mutableStateOf(
            YearMonth.from(
                parseCalendarDate(initialDates.from)
                    ?: allowedDates
                        ?.mapNotNull { parseCalendarDate(it.from) }
                        ?.minOrNull()
                    ?: LocalDate.now()
            ).toString()
        )
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val month = YearMonth.parse(displayedMonth)
    val colors = MaterialTheme.colorScheme

    val selectedDates = DateRange(selectedFrom, selectedTo)

    val validSelection = isCalendarRangeSelectable(
        selectedDates,
        allowedDates,
        blockedDates
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalendarDateSummary(
                label = fromLabel,
                date = selectedFrom,
                active = selectedFrom.isBlank() ||
                        selectedTo.isNotBlank(),
                modifier = Modifier.weight(1f)
            )

            CalendarDateSummary(
                label = toLabel,
                date = selectedTo,
                active = selectedFrom.isNotBlank() &&
                        selectedTo.isBlank(),
                modifier = Modifier.weight(1f)
            )
        }

        // Month navigation.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    displayedMonth = month.minusMonths(1).toString()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous month"
                )
            }

            Text(
                text = month.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy")
                ),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )

            IconButton(
                onClick = {
                    displayedMonth = month.plusMonths(1).toString()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next month"
                )
            }
        }

        // Weekday headings.
        Row(Modifier.fillMaxWidth()) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                .forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
        }

        // Sunday is the first column of the calendar.
        val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
        val daysInMonth = month.lengthOfMonth()

        // Six rows maintain a consistent calendar height.
        for (week in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (weekday in 0 until 7) {
                    val day = week * 7 + weekday - firstDayOffset + 1

                    if (day !in 1..daysInMonth) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val date = month.atDay(day).toString()

                        val allowed = allowedDates == null ||
                                allowedDates.any { it.covers(date, date) }

                        val blocked = blockedDates.any {
                            it.covers(date, date)
                        }

                        val selectable = allowed && !blocked

                        val endpoint = selectable &&
                                (date == selectedFrom || date == selectedTo)

                        val inRange = selectable &&
                                validSelection &&
                                date > selectedFrom &&
                                date < selectedTo

                        CalendarDay(
                            day = day,
                            date = date,
                            allowed = allowed,
                            blocked = blocked,
                            endpoint = endpoint,
                            inRange = inRange,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                errorMessage = null

                                if (selectedFrom.isBlank() ||
                                    selectedTo.isNotBlank() ||
                                    date < selectedFrom
                                ) {
                                    selectedFrom = date
                                    selectedTo = ""
                                } else {
                                    val range = DateRange(
                                        selectedFrom,
                                        date
                                    )

                                    if (isCalendarRangeSelectable(
                                            range,
                                            allowedDates,
                                            blockedDates
                                        )
                                    ) {
                                        selectedTo = date
                                    } else {
                                        errorMessage =
                                            "The selected range contains unavailable dates."
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryButton(
                text = "Clear",
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedFrom = ""
                    selectedTo = ""
                    errorMessage = null
                }
            )

            PrimaryButton(
                text = confirmLabel,
                modifier = Modifier.weight(2f),
                enabled = validSelection,
                onClick = {
                    onConfirm(selectedDates)
                }
            )
        }
    }
}

/**
 * Displays one of the calendar's selected date summaries.
 */
@Composable
private fun CalendarDateSummary(
    label: String,
    date: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .background(
                color = if (active) colors.primaryContainer
                else colors.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )

        Text(
            text = parseCalendarDate(date)?.format(
                DateTimeFormatter.ofPattern("MMM d, yyyy")
            ) ?: "Select date",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )
    }
}

/**
 * Displays one calendar date using its selection and
 * availability state.
 */
@Composable
private fun CalendarDay(
    day: Int,
    date: String,
    allowed: Boolean,
    blocked: Boolean,
    endpoint: Boolean,
    inRange: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val backgroundColor = when {
        blocked -> colors.errorContainer
        !allowed -> Color.Transparent
        endpoint -> colors.primary
        inRange -> colors.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        blocked -> colors.onErrorContainer
        !allowed -> colors.onSurface.copy(alpha = 0.30f)
        endpoint -> colors.onPrimary
        inRange -> colors.onPrimaryContainer
        else -> colors.onSurface
    }

    val description = when {
        blocked -> "$date, blocked"
        !allowed -> "$date, unavailable"
        endpoint || inRange -> "$date, selected"
        else -> "$date, available"
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                enabled = allowed && !blocked,
                onClick = onClick
            )
            .semantics {
                contentDescription = description
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (endpoint) FontWeight.Bold
            else FontWeight.Normal
        )
    }
}

/**
 * Checks that a complete date range is valid, contained
 * within an allowed range and does not overlap blocked dates.
 */
internal fun isCalendarRangeSelectable(
    dates: DateRange,
    allowedDates: List<DateRange>?,
    blockedDates: List<DateRange>
): Boolean {
    val from = parseCalendarDate(dates.from) ?: return false
    val to = parseCalendarDate(dates.to) ?: return false

    if (from.isAfter(to)) return false

    if (allowedDates != null &&
        allowedDates.none { it.covers(dates.from, dates.to) }
    ) {
        return false
    }

    return blockedDates.none {
        dates.from <= it.to && dates.to >= it.from
    }
}

/**
 * Parses an ISO date, returning null for empty or invalid dates.
 */
private fun parseCalendarDate(date: String): LocalDate? {
    return try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        null
    }
}
