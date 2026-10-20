package com.example.data.priority

import com.example.data.model.QuadrantResult
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TimeLeftResult
import com.example.data.model.UrgencyLevel
import kotlin.math.floor

/**
 * Exact implementation of Tascyn Notion Task Manager prioritization engine
 * Adapted for startup co-founders and team task orchestration.
 *
 * References:
 * - Formula 2.2: Time Left & Urgency Thresholds
 * - Formula 2.3: Deterministic Quadrant Matrix Engine
 *   qNumber = (uIndex - 1) * 15 + (pIndex - 1) * 5 + sIndex
 */
object PriorityAlgorithm {

    /**
     * Parses duration string like "1d 2h 30m" or "0d 4h 0m" into total minutes
     */
    fun parseMinimumTimeToMinutes(minText: String?): Long {
        if (minText.isNullOrBlank()) return 120L // default 2 hours if unspecified
        val parts = minText.trim().split(Regex("\\s+"))
        var days = 0L
        var hours = 0L
        var minutes = 0L
        for (part in parts) {
            val clean = part.trim().lowercase()
            when {
                clean.endsWith("d") -> days = clean.removeSuffix("d").toLongOrNull() ?: 0L
                clean.endsWith("h") -> hours = clean.removeSuffix("h").toLongOrNull() ?: 0L
                clean.endsWith("m") -> minutes = clean.removeSuffix("m").toLongOrNull() ?: 0L
            }
        }
        val total = (days * 1440L) + (hours * 60L) + minutes
        return if (total > 0L) total else 60L
    }

    /**
     * Formats minutes into human readable duration "Xd Yh Zm"
     */
    fun formatMinutesToDuration(totalMinutes: Long): String {
        val absMinutes = kotlin.math.abs(totalMinutes)
        val days = absMinutes / 1440L
        val remainingAfterDays = absMinutes - (days * 1440L)
        val hours = remainingAfterDays / 60L
        val minutes = remainingAfterDays - (hours * 60L)

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m")
        }.trim()
    }

    /**
     * Formula 2.2: Time Left
     * Compares Due Date vs now(), and evaluates against Minimum Time Required.
     *
     * Urgency thresholds:
     * - minutesLeft < 0 -> OVERDUE (1)
     * - minutesLeft < minTotalMinutes -> URGENT (2) (Remaining time is less than work required!)
     * - minutesLeft < 2 * minTotalMinutes -> ATTENTION_NEEDED (3) (< 2x buffer)
     * - minutesLeft < 4 * minTotalMinutes -> ON_TRACK (4) (< 4x buffer)
     * - else -> NORMAL_TIME_LEFT (5)
     */
    fun calculateTimeLeft(
        dueDate: Long?,
        minimumTimeRequired: String?,
        now: Long = System.currentTimeMillis()
    ): TimeLeftResult {
        if (dueDate == null) {
            return TimeLeftResult(
                rawText = "No deadline set",
                urgencyLevel = UrgencyLevel.NO_DUE_DATE,
                minutesLeft = null,
                formattedTime = "No deadline"
            )
        }

        val minTotalMinutes = parseMinimumTimeToMinutes(minimumTimeRequired)
        val minutesLeft = (dueDate - now) / (1000L * 60L)

        val daysLeft = floor(minutesLeft / 1440.0).toLong()
        val hoursLeft = floor((minutesLeft - daysLeft * 1440L) / 60.0).toLong()
        val minsLeft = minutesLeft - (daysLeft * 1440L) - (hoursLeft * 60L)
        val timeLeftFormatted = if (daysLeft > 0) {
            "${daysLeft}d ${hoursLeft}h left"
        } else if (hoursLeft > 0) {
            "${hoursLeft}h ${minsLeft}m left"
        } else {
            "${minsLeft}m left"
        }

        return when {
            minutesLeft < 0 -> {
                val overdueMinutes = kotlin.math.abs(minutesLeft)
                val overdueFormatted = formatMinutesToDuration(overdueMinutes)
                TimeLeftResult(
                    rawText = "Overdue by $overdueFormatted",
                    urgencyLevel = UrgencyLevel.OVERDUE,
                    minutesLeft = minutesLeft,
                    formattedTime = "Overdue ($overdueFormatted)"
                )
            }
            minutesLeft < minTotalMinutes -> {
                TimeLeftResult(
                    rawText = "Urgent · $timeLeftFormatted",
                    urgencyLevel = UrgencyLevel.URGENT,
                    minutesLeft = minutesLeft,
                    formattedTime = timeLeftFormatted
                )
            }
            minutesLeft < 2 * minTotalMinutes -> {
                TimeLeftResult(
                    rawText = "Attention Needed · $timeLeftFormatted",
                    urgencyLevel = UrgencyLevel.ATTENTION_NEEDED,
                    minutesLeft = minutesLeft,
                    formattedTime = timeLeftFormatted
                )
            }
            minutesLeft < 4 * minTotalMinutes -> {
                TimeLeftResult(
                    rawText = "On Track · $timeLeftFormatted",
                    urgencyLevel = UrgencyLevel.ON_TRACK,
                    minutesLeft = minutesLeft,
                    formattedTime = timeLeftFormatted
                )
            }
            else -> {
                TimeLeftResult(
                    rawText = timeLeftFormatted,
                    urgencyLevel = UrgencyLevel.NORMAL_TIME_LEFT,
                    minutesLeft = minutesLeft,
                    formattedTime = timeLeftFormatted
                )
            }
        }
    }

    /**
     * Formula 2.3: Quadrant Matrix Engine
     * Deterministic Q number:
     * qNumber = (uIndex - 1) * 15 + (pIndex - 1) * 5 + sIndex
     */
    fun calculateQuadrant(
        status: TaskStatus,
        priority: TaskPriority,
        dueDate: Long?,
        minimumTimeRequired: String?,
        now: Long = System.currentTimeMillis()
    ): QuadrantResult {
        val timeLeftResult = calculateTimeLeft(dueDate, minimumTimeRequired, now)
        val uIndex = when (timeLeftResult.urgencyLevel) {
            UrgencyLevel.OVERDUE -> 1
            UrgencyLevel.URGENT -> 2
            UrgencyLevel.ATTENTION_NEEDED -> 3
            UrgencyLevel.ON_TRACK -> 4
            UrgencyLevel.NORMAL_TIME_LEFT -> 5
            UrgencyLevel.NO_DUE_DATE -> 5
        }

        val pIndex = priority.formulaIndex // 1 = High, 2 = Medium, 3 = Low
        val sIndex = status.formulaIndex   // 1 = Not started, 2 = Working, 3 = Paused, 4 = Left, 5 = Done

        val qNumber = (uIndex - 1) * 15 + (pIndex - 1) * 5 + sIndex

        val minDurationText = minimumTimeRequired ?: "2h"
        val timeNotice = timeLeftResult.formattedTime

        val (action, explanation) = when {
            status == TaskStatus.DONE -> {
                "Done / Archive" to "Completed work. Retained for startup velocity metrics and milestones."
            }
            status == TaskStatus.LEFT -> {
                "Abandoned / Parked" to "Deliberately deprioritized or abandoned; excluded from active sprint."
            }
            uIndex == 1 -> {
                "Recover Immediately" to "Deadline passed! Ship partial MVP, reschedule, or communicate blocker."
            }
            uIndex == 2 && pIndex == 1 -> {
                "Do Now" to "High priority crisis: remaining time ($timeNotice) is less than required work duration ($minDurationText)."
            }
            uIndex == 2 -> {
                "Start & Push" to "Urgent: remaining time ($timeNotice) is less than required work duration ($minDurationText)."
            }
            uIndex == 3 && pIndex == 1 -> {
                "Schedule & Focus" to "High priority item with under 2x buffer. Allocate deep work block today."
            }
            status == TaskStatus.PAUSED -> {
                "Unblock & Resume" to "Task currently paused. Clear dependencies before time buffer runs out."
            }
            uIndex == 5 && pIndex == 3 -> {
                "Low Impact, Park" to "Low priority with ample time. Defer in favor of core product/fundraising leverage."
            }
            uIndex == 5 -> {
                "Monitor (Buffer)" to "Healthy schedule buffer ($timeNotice) before active focus is required."
            }
            uIndex == 4 -> {
                "On Track" to "Sufficient progress buffer. On track for targeted delivery."
            }
            else -> {
                "Review & Advance" to "Standard execution queue."
            }
        }

        // Rank score: lower numbers rank higher in team queues
        val rankScore = when (status) {
            TaskStatus.DONE, TaskStatus.LEFT -> 90000 + qNumber // parked at end
            else -> (uIndex * 1000) + (pIndex * 100) + (sIndex * 10)
        }

        val fullLabel = "Q$qNumber • $action"

        return QuadrantResult(
            qNumber = qNumber,
            action = action,
            fullLabel = fullLabel,
            explanation = explanation,
            urgencyIndex = uIndex,
            priorityIndex = pIndex,
            statusIndex = sIndex,
            rankScore = rankScore
        )
    }
}
