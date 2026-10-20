package com.example.data.model

/**
 * Result of Notion / Tascyn Formula 2.2: Time Left & Urgency
 */
data class TimeLeftResult(
    val rawText: String,
    val urgencyLevel: UrgencyLevel,
    val minutesLeft: Long?,
    val formattedTime: String
)

/**
 * Result of Notion / Tascyn Formula 2.3: Quadrant Matrix Engine
 * Formula: qNumber = (uIndex - 1) * 15 + (pIndex - 1) * 5 + sIndex
 */
data class QuadrantResult(
    val qNumber: Int,
    val action: String,
    val fullLabel: String,
    val explanation: String,
    val urgencyIndex: Int,
    val priorityIndex: Int,
    val statusIndex: Int,
    val rankScore: Int // lower = higher priority
)

/**
 * Rich domain task model used throughout UI
 */
data class TaskItem(
    val id: String,
    val workspaceId: String,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val assignee: MemberItem?,
    val creator: MemberItem?,
    val dueDate: Long?,
    val minimumTimeRequired: String = "0d 2h 0m", // Tascyn parameter e.g. "0d 2h 0m"
    val milestone: MilestoneItem?,
    val tags: List<TaskCategory> = emptyList(),
    val pinnedContextsCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val timeLeftResult: TimeLeftResult,
    val quadrantResult: QuadrantResult
)

/**
 * Team member model
 */
data class MemberItem(
    val id: String,
    val workspaceId: String,
    val name: String,
    val email: String,
    val role: String,
    val avatarInitials: String,
    val avatarColorHex: String,
    val presenceStatus: String = "Active", // "Active", "In Deep Work", "Away"
    val isCurrentUser: Boolean = false,
    val activeTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val currentFocusTaskTitle: String? = null
)

/**
 * Milestone model
 */
data class MilestoneItem(
    val id: String,
    val workspaceId: String,
    val title: String,
    val description: String,
    val owner: MemberItem?,
    val targetDate: Long,
    val isCompleted: Boolean = false,
    val totalTasks: Int = 0,
    val doneTasks: Int = 0,
    val workingTasks: Int = 0,
    val leftTasks: Int = 0,
    val progressPercent: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Team Chat Message model
 */
data class ChatMessageItem(
    val id: String,
    val workspaceId: String,
    val sender: MemberItem?,
    val content: String,
    val channel: String = "general", // "general", "product-eng", "fundraising"
    val timestamp: Long = System.currentTimeMillis(),
    val isPinnedToTask: Boolean = false,
    val pinnedToTaskId: String? = null,
    val pinnedToTaskTitle: String? = null
)

/**
 * Pinned Chat Context attached to a task
 */
data class PinnedContextItem(
    val id: String,
    val taskId: String,
    val originalMessageId: String,
    val authorName: String,
    val authorRole: String,
    val messageText: String,
    val channel: String,
    val originalTimestamp: Long,
    val pinnedAt: Long = System.currentTimeMillis()
)

/**
 * In-task conversation message / comment
 */
data class TaskCommentItem(
    val id: String,
    val taskId: String,
    val author: MemberItem?,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Activity timeline entry
 */
data class ActivityLogItem(
    val id: String,
    val workspaceId: String,
    val actorName: String,
    val actionType: String, // "CREATED", "STATUS_CHANGE", "ASSIGNED", "PINNED_CONTEXT", "COMMENTED", "MILESTONE"
    val description: String,
    val targetTaskId: String? = null,
    val targetTaskTitle: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
