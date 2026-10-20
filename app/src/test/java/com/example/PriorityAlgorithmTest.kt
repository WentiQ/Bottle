package com.example

import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.UrgencyLevel
import com.example.data.priority.PriorityAlgorithm
import org.junit.Assert.*
import org.junit.Test

class PriorityAlgorithmTest {

    @Test
    fun testParseMinimumTimeToMinutes() {
        assertEquals(60L, PriorityAlgorithm.parseMinimumTimeToMinutes("0d 1h 0m"))
        assertEquals(150L, PriorityAlgorithm.parseMinimumTimeToMinutes("0d 2h 30m"))
        assertEquals(1440L + 120L, PriorityAlgorithm.parseMinimumTimeToMinutes("1d 2h 0m"))
        assertEquals(120L, PriorityAlgorithm.parseMinimumTimeToMinutes(null)) // default fallback
    }

    @Test
    fun testCalculateTimeLeftOverdue() {
        val now = 1000000000000L
        val overdueDate = now - (3600_000L * 2) // 2 hours overdue

        val result = PriorityAlgorithm.calculateTimeLeft(overdueDate, "0d 2h 0m", now)
        assertEquals(UrgencyLevel.OVERDUE, result.urgencyLevel)
        assertTrue(result.rawText.contains("Overdue"))
    }

    @Test
    fun testCalculateTimeLeftUrgent() {
        val now = 1000000000000L
        // Needed: 4 hours (240 mins). Remaining: 2 hours (120 mins).
        // Since 120 < 240, this is in critical window: URGENT
        val dueDate = now + (3600_000L * 2)

        val result = PriorityAlgorithm.calculateTimeLeft(dueDate, "0d 4h 0m", now)
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun testCalculateQuadrantDoNow() {
        val now = 1000000000000L
        // Urgent + High priority
        val dueDate = now + (3600_000L * 2)

        val quadrant = PriorityAlgorithm.calculateQuadrant(
            status = TaskStatus.WORKING,
            priority = TaskPriority.HIGH,
            dueDate = dueDate,
            minimumTimeRequired = "0d 4h 0m",
            now = now
        )

        // uIndex = 2, pIndex = 1, sIndex = 2
        // qNumber = (2 - 1) * 15 + (1 - 1) * 5 + 2 = 15 + 0 + 2 = 17
        assertEquals(17, quadrant.qNumber)
        assertEquals("Do Now", quadrant.action)
    }

    @Test
    fun testLeftAndDoneTasksAreParkedAtBottom() {
        val now = 1000000000000L
        val doneQuadrant = PriorityAlgorithm.calculateQuadrant(
            status = TaskStatus.DONE,
            priority = TaskPriority.HIGH,
            dueDate = now - 1000,
            minimumTimeRequired = "0d 2h 0m",
            now = now
        )

        val leftQuadrant = PriorityAlgorithm.calculateQuadrant(
            status = TaskStatus.LEFT,
            priority = TaskPriority.HIGH,
            dueDate = now - 1000,
            minimumTimeRequired = "0d 2h 0m",
            now = now
        )

        val activeQuadrant = PriorityAlgorithm.calculateQuadrant(
            status = TaskStatus.WORKING,
            priority = TaskPriority.HIGH,
            dueDate = now + 1000,
            minimumTimeRequired = "0d 2h 0m",
            now = now
        )

        assertTrue(doneQuadrant.rankScore > 90000)
        assertTrue(leftQuadrant.rankScore > 90000)
        assertTrue(activeQuadrant.rankScore < doneQuadrant.rankScore)
        assertEquals("Done / Archive", doneQuadrant.action)
        assertEquals("Abandoned / Parked", leftQuadrant.action)
    }
}
