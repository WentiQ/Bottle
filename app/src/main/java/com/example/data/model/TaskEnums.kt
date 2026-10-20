package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/**
 * Task Status values required by product brief:
 * - Not started
 * - Working
 * - Paused
 * - Left (deliberately abandoned or no longer pursued)
 * - Done
 */
enum class TaskStatus(val displayName: String, val formulaIndex: Int) {
    NOT_STARTED("Not started", 1),
    WORKING("Working", 2),
    PAUSED("Paused", 3),
    LEFT("Left", 4),
    DONE("Done", 5);

    val isActive: Boolean
        get() = this != DONE && this != LEFT

    companion object {
        fun fromString(value: String): TaskStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: NOT_STARTED
        }
    }
}

/**
 * Task Priority matching Tascyn Notion Task Manager
 */
enum class TaskPriority(val displayName: String, val formulaIndex: Int) {
    HIGH("High", 1),
    MEDIUM("Medium", 2),
    LOW("Low", 3);

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: MEDIUM
        }
    }
}

/**
 * Urgency levels computed by the verified Tascyn Formula 2.2
 */
enum class UrgencyLevel(val formulaIndex: Int, val label: String) {
    OVERDUE(1, "Overdue"),
    URGENT(2, "Urgent"),
    ATTENTION_NEEDED(3, "Attention Needed"),
    ON_TRACK(4, "On Track"),
    NORMAL_TIME_LEFT(5, "Time Buffer"),
    NO_DUE_DATE(0, "No Due Date")
}

/**
 * Startup task categories / domains
 */
enum class TaskCategory(val displayName: String, val colorHex: String) {
    PRODUCT("Product", "#6366F1"),
    ENGINEERING("Engineering", "#2563EB"),
    DESIGN("Design", "#EC4899"),
    GROWTH("Growth", "#10B981"),
    FUNDRAISING("Fundraising", "#8B5CF6"),
    OPERATIONS("Ops", "#F59E0B"),
    LEGAL("Legal", "#64748B"),
    CUSTOMER("Customer", "#06B6D4");

    companion object {
        fun fromString(value: String): TaskCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: PRODUCT
        }
    }
}
