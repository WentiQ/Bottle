package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CompanyDto(
    val id: String = "",
    val name: String = "",
    val founderUid: String = "",
    val founderName: String = "",
    val founderEmail: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp var createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfileDto(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val isFounder: Boolean = false,
    val avatarInitials: String = "",
    val avatarColorHex: String = "#6366F1",
    val presenceStatus: String = "Active",
    @ServerTimestamp var joinedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskDto(
    val id: String = "",
    val companyId: String = "",
    val title: String = "",
    val description: String = "",
    val assigneeId: String? = null,
    val assigneeName: String? = null,
    val creatorId: String = "",
    val creatorName: String = "",
    val status: String = "NOT_STARTED",
    val priority: String = "MEDIUM",
    val dueDate: Long? = null,
    val minimumTimeRequired: String = "0d 2h 0m",
    val milestoneId: String? = null,
    val milestoneTitle: String? = null,
    val tags: List<String> = emptyList(),
    val pinnedCount: Long = 0L,
    val commentsCount: Long = 0L,
    val completedAt: Long? = null,
    @ServerTimestamp var createdAt: Timestamp? = null,
    @ServerTimestamp var updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MilestoneDto(
    val id: String = "",
    val companyId: String = "",
    val title: String = "",
    val description: String = "",
    val ownerId: String? = null,
    val ownerName: String? = null,
    val targetDate: Long = 0L,
    val isCompleted: Boolean = false,
    @ServerTimestamp var createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessageDto(
    val id: String = "",
    val companyId: String = "",
    val channel: String = "general",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val content: String = "",
    val isPinnedToTask: Boolean = false,
    val pinnedToTaskId: String? = null,
    val pinnedToTaskTitle: String? = null,
    @ServerTimestamp var timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class PinnedContextDto(
    val id: String = "",
    val companyId: String = "",
    val taskId: String = "",
    val originalMessageId: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    val messageText: String = "",
    val channel: String = "general",
    val originalTimestamp: Long = 0L,
    @ServerTimestamp var pinnedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskCommentDto(
    val id: String = "",
    val companyId: String = "",
    val taskId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp var timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLogDto(
    val id: String = "",
    val companyId: String = "",
    val actorName: String = "",
    val actionType: String = "",
    val description: String = "",
    val targetTaskId: String? = null,
    val targetTaskTitle: String? = null,
    @ServerTimestamp var timestamp: Timestamp? = null
)
