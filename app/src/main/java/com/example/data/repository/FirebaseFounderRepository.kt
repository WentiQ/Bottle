package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.*
import com.example.data.model.*
import com.example.data.priority.PriorityAlgorithm
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

private fun getFirestoreInstance(context: Context): FirebaseFirestore {
    val dbId = try {
        context.applicationContext.getString(R.string.firestore_database_id).trim()
    } catch (e: Exception) {
        ""
    }
    return if (dbId.isNotBlank() && dbId != "(default)") {
        FirebaseFirestore.getInstance(dbId)
    } else {
        FirebaseFirestore.getInstance()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseFounderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        getFirestoreInstance(context),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated with Google")
    }

    suspend fun fetchUserProfile(uid: String): UserProfileDto? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) return doc.toObject(UserProfileDto::class.java)

            // A verified email can survive an auth UID change (for example after
            // restoring an account in a newly configured Firebase project). Recover
            // that user's existing workspace instead of sending them through setup again.
            val user = auth.currentUser
            val email = user?.email?.trim()?.lowercase().orEmpty()
            if (email.isBlank() || user?.isEmailVerified != true) return null

            val emailMatch = db.collection("users")
                .whereEqualTo("emailLowercase", email)
                .limit(1)
                .get()
                .await()
            val match = if (!emailMatch.isEmpty) {
                emailMatch.documents.first()
            } else {
                db.collection("users")
                    .whereEqualTo("email", user.email)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            } ?: return null

            val profileData = match.data?.toMutableMap() ?: return null
            profileData["uid"] = uid
            profileData["email"] = user.email
            profileData["emailLowercase"] = email
            db.collection("users").document(uid).set(profileData).await()
            val companyId = match.getString("companyId").orEmpty()
            if (companyId.isNotBlank()) {
                try {
                    db.collection("companies").document(companyId)
                        .update("memberUids", FieldValue.arrayUnion(uid)).await()
                    if (match.id != uid) {
                        db.collection("companies").document(companyId)
                            .update("memberUids", FieldValue.arrayRemove(match.id)).await()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FounderRepo", "Profile restored, but company member IDs could not be refreshed", e)
                }
            }

            db.collection("users").document(uid).get().await().toObject(UserProfileDto::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            throw e
        }
    }

    // Current user Auth state
    val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fAuth ->
            trySend(fAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // Observe current user profile in Firestore
    val currentUserProfile: Flow<UserProfileDto?> = authState.flatMapLatest { isAuthenticated ->
        if (!isAuthenticated || auth.currentUser == null) {
            flowOf(null)
        } else {
            val uid = auth.currentUser!!.uid
            db.collection("users").document(uid).snapshots().map { snapshot ->
                snapshot.toObject(UserProfileDto::class.java)
            }.catch { e ->
                handleFirestoreError(e as Exception, OperationType.GET, "users/$uid")
                emit(null)
            }
        }
    }

    // Observe company info
    val currentCompany: Flow<CompanyDto?> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(null)
        } else {
            db.collection("companies").document(companyId).snapshots().map { snapshot ->
                snapshot.toObject(CompanyDto::class.java)
            }.catch { e ->
                handleFirestoreError(e as Exception, OperationType.GET, "companies/$companyId")
                emit(null)
            }
        }
    }

    // Team members in the same company
    val teamMembers: Flow<List<MemberItem>> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection("users")
                .whereEqualTo("companyId", companyId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfileDto::class.java).map { u ->
                        MemberItem(
                            id = u.uid,
                            workspaceId = u.companyId,
                            name = u.name,
                            email = u.email,
                            role = u.role,
                            avatarInitials = u.avatarInitials.ifBlank { u.name.take(2).uppercase() },
                            avatarColorHex = u.avatarColorHex,
                            presenceStatus = u.presenceStatus,
                            isCurrentUser = u.uid == auth.currentUser?.uid
                        )
                    }
                }.catch { e ->
                    handleFirestoreError(e as Exception, OperationType.LIST, "users")
                    emit(emptyList())
                }
        }
    }

    // Milestones in the company
    val milestones: Flow<List<MilestoneItem>> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            combine(
                db.collection("milestones").whereEqualTo("companyId", companyId).snapshots(),
                db.collection("tasks").whereEqualTo("companyId", companyId).snapshots(),
                teamMembers
            ) { msSnapshot, tasksSnapshot, members ->
                val msList = msSnapshot.toObjects(MilestoneDto::class.java)
                val taskList = tasksSnapshot.toObjects(TaskDto::class.java)
                val memberMap = members.associateBy { it.id }

                msList.map { ms ->
                    val linked = taskList.filter { it.milestoneId == ms.id }
                    val total = linked.size
                    val done = linked.count { it.status == TaskStatus.DONE.name }
                    val working = linked.count { it.status == TaskStatus.WORKING.name }
                    val left = linked.count { it.status == TaskStatus.LEFT.name }
                    val effectiveTarget = (total - left).coerceAtLeast(1)
                    val progress = if (total == 0) 0f else (done.toFloat() / effectiveTarget).coerceIn(0f, 1f)

                    MilestoneItem(
                        id = ms.id,
                        workspaceId = ms.companyId,
                        title = ms.title,
                        description = ms.description,
                        owner = ms.ownerId?.let { memberMap[it] },
                        targetDate = ms.targetDate,
                        isCompleted = ms.isCompleted || (total > 0 && done == effectiveTarget),
                        totalTasks = total,
                        doneTasks = done,
                        workingTasks = working,
                        leftTasks = left,
                        progressPercent = progress
                    )
                }
            }.catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "milestones")
                emit(emptyList())
            }
        }
    }

    // Tasks in the company with Tascyn algorithm calculation
    val tasks: Flow<List<TaskItem>> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            combine(
                db.collection("tasks").whereEqualTo("companyId", companyId).snapshots(),
                teamMembers,
                milestones
            ) { taskSnapshot, members, msList ->
                val taskDtos = taskSnapshot.toObjects(TaskDto::class.java)
                val memberMap = members.associateBy { it.id }
                val msMap = msList.associateBy { it.id }
                val now = System.currentTimeMillis()

                taskDtos.map { dto ->
                    val status = TaskStatus.fromString(dto.status)
                    val priority = TaskPriority.fromString(dto.priority)
                    val timeLeft = PriorityAlgorithm.calculateTimeLeft(dto.dueDate, dto.minimumTimeRequired, now)
                    val quadrant = PriorityAlgorithm.calculateQuadrant(status, priority, dto.dueDate, dto.minimumTimeRequired, now)

                    val assignee = dto.assigneeId?.let { memberMap[it] }
                    val creator = memberMap[dto.creatorId]
                    val milestone = dto.milestoneId?.let { msMap[it] }
                    val categories = dto.tags.map { TaskCategory.fromString(it) }

                    TaskItem(
                        id = dto.id,
                        workspaceId = dto.companyId,
                        title = dto.title,
                        description = dto.description,
                        status = status,
                        priority = priority,
                        assignee = assignee,
                        creator = creator,
                        dueDate = dto.dueDate,
                        minimumTimeRequired = dto.minimumTimeRequired,
                        milestone = milestone,
                        tags = categories,
                        pinnedContextsCount = dto.pinnedCount.toInt(),
                        commentsCount = dto.commentsCount.toInt(),
                        createdAt = dto.createdAt?.toDate()?.time ?: now,
                        updatedAt = dto.updatedAt?.toDate()?.time ?: now,
                        completedAt = dto.completedAt,
                        timeLeftResult = timeLeft,
                        quadrantResult = quadrant
                    )
                }.sortedWith(
                    compareBy<TaskItem> { it.quadrantResult.rankScore }
                        .thenBy { it.dueDate ?: Long.MAX_VALUE }
                )
            }.catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "tasks")
                emit(emptyList())
            }
        }
    }

    // Activity Logs
    val activityLogs: Flow<List<ActivityLogItem>> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection("activity_logs")
                .whereEqualTo("companyId", companyId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ActivityLogDto::class.java)
                        .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                        .take(40)
                        .map { dto ->
                            ActivityLogItem(
                                id = dto.id,
                                workspaceId = dto.companyId,
                                actorName = dto.actorName,
                                actionType = dto.actionType,
                                description = dto.description,
                                targetTaskId = dto.targetTaskId,
                                targetTaskTitle = dto.targetTaskTitle,
                                timestamp = dto.timestamp?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        }
                }.catch { e ->
                    handleFirestoreError(e as Exception, OperationType.LIST, "activity_logs")
                    emit(emptyList())
                }
        }
    }

    // Chat messages for active channel
    fun getChatMessages(channel: String): Flow<List<ChatMessageItem>> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            combine(
                db.collection("chat_messages")
                    .whereEqualTo("companyId", companyId)
                    .whereEqualTo("channel", channel)
                    .snapshots(),
                teamMembers
            ) { snapshot, members ->
                val memberMap = members.associateBy { it.id }
                snapshot.toObjects(ChatMessageDto::class.java)
                    .sortedBy { it.timestamp?.toDate()?.time ?: 0L }
                    .map { dto ->
                        ChatMessageItem(
                            id = dto.id,
                            workspaceId = dto.companyId,
                            sender = memberMap[dto.senderId] ?: MemberItem(
                                id = dto.senderId,
                                workspaceId = dto.companyId,
                                name = dto.senderName,
                                email = "",
                                role = dto.senderRole,
                                avatarInitials = dto.senderName.take(2).uppercase(),
                                avatarColorHex = "#6366F1"
                            ),
                            content = dto.content,
                            channel = dto.channel,
                            timestamp = dto.timestamp?.toDate()?.time ?: System.currentTimeMillis(),
                            isPinnedToTask = dto.isPinnedToTask,
                            pinnedToTaskId = dto.pinnedToTaskId,
                            pinnedToTaskTitle = dto.pinnedToTaskTitle
                        )
                    }
            }.catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "chat_messages")
                emit(emptyList())
            }
        }
    }

    fun getTaskComments(taskId: String): Flow<List<TaskCommentItem>> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            combine(
                db.collection("task_comments")
                    .whereEqualTo("companyId", companyId)
                    .whereEqualTo("taskId", taskId)
                    .snapshots(),
                teamMembers
            ) { snapshot, members ->
                val memberMap = members.associateBy { it.id }
                snapshot.toObjects(TaskCommentDto::class.java)
                    .sortedBy { it.timestamp?.toDate()?.time ?: 0L }
                    .map { dto ->
                        TaskCommentItem(
                            id = dto.id,
                            taskId = dto.taskId,
                            author = memberMap[dto.authorId],
                            text = dto.text,
                            timestamp = dto.timestamp?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
            }.catch { e ->
                handleFirestoreError(e as Exception, OperationType.LIST, "task_comments")
                emit(emptyList())
            }
        }
    }

    fun getPinnedContexts(taskId: String): Flow<List<PinnedContextItem>> = currentUserProfile.flatMapLatest { profile ->
        val companyId = profile?.companyId
        if (companyId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            db.collection("pinned_contexts")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("taskId", taskId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PinnedContextDto::class.java)
                        .sortedByDescending { it.pinnedAt?.toDate()?.time ?: 0L }
                        .map { dto ->
                            PinnedContextItem(
                                id = dto.id,
                                taskId = dto.taskId,
                                originalMessageId = dto.originalMessageId,
                                authorName = dto.authorName,
                                authorRole = dto.authorRole,
                                messageText = dto.messageText,
                                channel = dto.channel,
                                originalTimestamp = dto.originalTimestamp,
                                pinnedAt = dto.pinnedAt?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        }
                }.catch { e ->
                    handleFirestoreError(e as Exception, OperationType.LIST, "pinned_contexts")
                    emit(emptyList())
                }
        }
    }

    // --- Onboarding & Setup Workflows ---

    /**
     * Founder Onboarding: creates startup company, profile, and initial bootstrap tasks
     */
    suspend fun createCompanyAsFounder(
        companyName: String,
        founderName: String,
        founderEmail: String,
        role: String
    ): String {
        val uid = requireUserId()
        fetchUserProfile(uid)?.companyId?.takeIf { it.isNotBlank() }?.let { return it }
        val cleanName = companyName.trim().filter { it.isLetterOrDigit() }.take(4).uppercase()
        val randomSuffix = (1000..9999).random()
        val companyId = "COMP-$cleanName-$randomSuffix"

        val effectiveRole = role.ifBlank { "Founder & CEO" }
        val initials = founderName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            .ifBlank { "FC" }

        val companyData = hashMapOf(
            "id" to companyId,
            "name" to companyName.trim(),
            "founderUid" to uid,
            "founderName" to founderName.trim(),
            "founderEmail" to founderEmail.trim(),
            "memberUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp()
        )

        val profileData = hashMapOf(
            "uid" to uid,
            "email" to founderEmail.trim(),
            "emailLowercase" to founderEmail.trim().lowercase(),
            "name" to founderName.trim(),
            "role" to effectiveRole,
            "companyId" to companyId,
            "companyName" to companyName.trim(),
            "isFounder" to true,
            "avatarInitials" to initials,
            "avatarColorHex" to "#6366F1",
            "presenceStatus" to "Active",
            "joinedAt" to FieldValue.serverTimestamp()
        )

        try {
            android.util.Log.d("FounderRepo", "Writing company: $companyId")
            db.collection("companies").document(companyId).set(companyData).await()

            android.util.Log.d("FounderRepo", "Writing user profile: $uid")
            db.collection("users").document(uid).set(profileData).await()

            android.util.Log.d("FounderRepo", "Bootstrapping starter data for $companyId")
            bootstrapCompanyStarterData(companyId, uid, founderName)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "companies/$companyId")
            throw e
        }

        return companyId
    }

    /**
     * Team Member Onboarding: joins existing company with company ID
     */
    suspend fun joinCompanyAsMember(
        companyIdInput: String,
        memberName: String,
        memberEmail: String,
        role: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val targetCompanyId = companyIdInput.trim().uppercase()

            val companyDoc = db.collection("companies").document(targetCompanyId).get().await()
            if (!companyDoc.exists()) {
                return Result.failure(IllegalArgumentException("Startup/Company ID '$targetCompanyId' not found. Please verify with your founder."))
            }

            val companyName = companyDoc.getString("name") ?: "Startup Team"
            val effectiveRole = role.ifBlank { "Core Team" }
            val initials = memberName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                .ifBlank { "TM" }

            val colors = listOf("#EC4899", "#10B981", "#F59E0B", "#8B5CF6", "#06B6D4", "#2563EB")
            val colorHex = colors[(memberName.hashCode() and 0x7FFFFFFF) % colors.size]

            // Update company member list
            db.collection("companies").document(targetCompanyId)
                .update("memberUids", FieldValue.arrayUnion(uid)).await()

            // Create member profile
            val profileData = hashMapOf(
                "uid" to uid,
                "email" to memberEmail.trim(),
                "emailLowercase" to memberEmail.trim().lowercase(),
                "name" to memberName.trim(),
                "role" to effectiveRole,
                "companyId" to targetCompanyId,
                "companyName" to companyName,
                "isFounder" to false,
                "avatarInitials" to initials,
                "avatarColorHex" to colorHex,
                "presenceStatus" to "Active",
                "joinedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(profileData).await()

            logActivity(targetCompanyId, memberName, "JOINED", "Joined the team as $effectiveRole")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "companies/$companyIdInput")
            Result.failure(e)
        }
    }

    private suspend fun bootstrapCompanyStarterData(companyId: String, founderUid: String, founderName: String) {
        val now = System.currentTimeMillis()
        val day = 86400_000L
        val hour = 3600_000L

        // Initial Milestone
        val msId = "ms_" + UUID.randomUUID().toString().take(8)
        db.collection("milestones").document(msId).set(
            hashMapOf(
                "id" to msId,
                "companyId" to companyId,
                "title" to "MVP Launch & Customer Pilot Readiness",
                "description" to "Finalize key core features, test critical user flows, and deploy live.",
                "ownerId" to founderUid,
                "ownerName" to founderName,
                "targetDate" to now + (14 * day),
                "isCompleted" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // 3 Initial prioritized tasks
        val task1Id = "task_" + UUID.randomUUID().toString().take(8)
        db.collection("tasks").document(task1Id).set(
            hashMapOf(
                "id" to task1Id,
                "companyId" to companyId,
                "title" to "Setup Team Onboarding & Share Company QR",
                "description" to "Share the company ID or profile QR with co-founders and early engineers.",
                "assigneeId" to founderUid,
                "assigneeName" to founderName,
                "creatorId" to founderUid,
                "creatorName" to founderName,
                "status" to "WORKING",
                "priority" to "HIGH",
                "dueDate" to now + (12 * hour),
                "minimumTimeRequired" to "0d 1h 0m",
                "milestoneId" to msId,
                "milestoneTitle" to "MVP Launch & Customer Pilot Readiness",
                "tags" to listOf("Product", "Growth"),
                "pinnedCount" to 0L,
                "commentsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val task2Id = "task_" + UUID.randomUUID().toString().take(8)
        db.collection("tasks").document(task2Id).set(
            hashMapOf(
                "id" to task2Id,
                "companyId" to companyId,
                "title" to "Define Sprint Priorities in Team Chat",
                "description" to "Post discussion messages in #product-eng and pin key context to respective tasks.",
                "assigneeId" to founderUid,
                "assigneeName" to founderName,
                "creatorId" to founderUid,
                "creatorName" to founderName,
                "status" to "NOT_STARTED",
                "priority" to "MEDIUM",
                "dueDate" to now + (2 * day),
                "minimumTimeRequired" to "0d 2h 0m",
                "milestoneId" to msId,
                "milestoneTitle" to "MVP Launch & Customer Pilot Readiness",
                "tags" to listOf("Operations"),
                "pinnedCount" to 0L,
                "commentsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Welcome chat message
        val msgId = "msg_" + UUID.randomUUID().toString().take(8)
        db.collection("chat_messages").document(msgId).set(
            hashMapOf(
                "id" to msgId,
                "companyId" to companyId,
                "channel" to "general",
                "senderId" to founderUid,
                "senderName" to founderName,
                "senderRole" to "Founder",
                "content" to "Welcome to $companyId team chat! Pin messages directly to tasks to keep context organized without scattered messages.",
                "isPinnedToTask" to false,
                "timestamp" to FieldValue.serverTimestamp()
            )
        ).await()

        logActivity(companyId, founderName, "CREATED", "Created startup workspace $companyId")
    }

    // --- Task Actions ---

    suspend fun createTask(
        companyId: String,
        title: String,
        description: String,
        assignee: MemberItem?,
        priority: TaskPriority,
        dueDate: Long?,
        minimumTimeRequired: String,
        milestone: MilestoneItem?,
        tags: List<TaskCategory>,
        status: TaskStatus = TaskStatus.NOT_STARTED
    ): String {
        val uid = requireUserId()
        val user = auth.currentUser
        val taskId = "task_" + UUID.randomUUID().toString().take(8)

        val taskData = hashMapOf<String, Any>(
            "id" to taskId,
            "companyId" to companyId,
            "title" to title.trim(),
            "description" to description.trim(),
            "creatorId" to uid,
            "creatorName" to (user?.displayName ?: "Founder"),
            "status" to status.name,
            "priority" to priority.name,
            "minimumTimeRequired" to minimumTimeRequired.ifBlank { "0d 2h 0m" },
            "tags" to tags.map { it.name },
            "pinnedCount" to 0L,
            "commentsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!assignee?.id.isNullOrBlank()) taskData["assigneeId"] = assignee!!.id
        if (!assignee?.name.isNullOrBlank()) taskData["assigneeName"] = assignee!!.name
        if (dueDate != null) taskData["dueDate"] = dueDate
        if (!milestone?.id.isNullOrBlank()) taskData["milestoneId"] = milestone!!.id
        if (!milestone?.title.isNullOrBlank()) taskData["milestoneTitle"] = milestone!!.title

        db.collection("tasks").document(taskId).set(taskData).await()
        logActivity(companyId, user?.displayName ?: "Founder", "CREATED", "Created task: '$title'", taskId, title)
        return taskId
    }

    suspend fun updateTaskStatus(companyId: String, taskId: String, taskTitle: String, newStatus: TaskStatus) {
        val user = auth.currentUser
        val updates = hashMapOf<String, Any>(
            "status" to newStatus.name,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (newStatus == TaskStatus.DONE) {
            updates["completedAt"] = System.currentTimeMillis()
        }

        db.collection("tasks").document(taskId).update(updates).await()
        val actionText = when (newStatus) {
            TaskStatus.WORKING -> "started working on"
            TaskStatus.PAUSED -> "paused"
            TaskStatus.LEFT -> "abandoned (left)"
            TaskStatus.DONE -> "completed"
            TaskStatus.NOT_STARTED -> "moved to not started"
        }
        logActivity(companyId, user?.displayName ?: "Teammate", "STATUS_CHANGE", "$actionText '$taskTitle'", taskId, taskTitle)
    }

    suspend fun reassignTask(companyId: String, taskId: String, taskTitle: String, newAssignee: MemberItem?) {
        val user = auth.currentUser
        db.collection("tasks").document(taskId).update(
            hashMapOf(
                "assigneeId" to newAssignee?.id,
                "assigneeName" to newAssignee?.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        val assigneeLabel = newAssignee?.name ?: "Unassigned"
        logActivity(companyId, user?.displayName ?: "Founder", "ASSIGNED", "Reassigned '$taskTitle' to $assigneeLabel", taskId, taskTitle)
    }

    suspend fun deleteTask(companyId: String, taskId: String, taskTitle: String) {
        val user = auth.currentUser
        db.collection("tasks").document(taskId).delete().await()
        logActivity(companyId, user?.displayName ?: "Founder", "DELETED", "Deleted task '$taskTitle'")
    }

    // --- Chat & Pinned Context ---

    suspend fun sendChatMessage(companyId: String, channel: String, content: String, sender: UserProfileDto) {
        val msgId = "msg_" + UUID.randomUUID().toString().take(8)
        val data = hashMapOf(
            "id" to msgId,
            "companyId" to companyId,
            "channel" to channel,
            "senderId" to sender.uid,
            "senderName" to sender.name,
            "senderRole" to sender.role,
            "content" to content.trim(),
            "isPinnedToTask" to false,
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("chat_messages").document(msgId).set(data).await()
    }

    suspend fun pinMessageToTask(
        companyId: String,
        message: ChatMessageItem,
        task: TaskItem
    ) {
        val pinnedId = "pinned_" + UUID.randomUUID().toString().take(8)
        val user = auth.currentUser

        // 1. Mark message as pinned
        db.collection("chat_messages").document(message.id).update(
            "isPinnedToTask", true,
            "pinnedToTaskId", task.id,
            "pinnedToTaskTitle", task.title
        ).await()

        // 2. Increment task pinnedCount
        db.collection("tasks").document(task.id).update(
            "pinnedCount", FieldValue.increment(1)
        ).await()

        // 3. Create pinned context doc
        val pinnedData = hashMapOf(
            "id" to pinnedId,
            "companyId" to companyId,
            "taskId" to task.id,
            "originalMessageId" to message.id,
            "authorName" to (message.sender?.name ?: "Teammate"),
            "authorRole" to (message.sender?.role ?: "Team"),
            "messageText" to message.content,
            "channel" to message.channel,
            "originalTimestamp" to message.timestamp,
            "pinnedAt" to FieldValue.serverTimestamp()
        )
        db.collection("pinned_contexts").document(pinnedId).set(pinnedData).await()

        logActivity(companyId, user?.displayName ?: "Founder", "PINNED_CONTEXT", "Pinned message from #${message.channel} to '${task.title}'", task.id, task.title)
    }

    suspend fun unpinMessage(
        companyId: String,
        pinnedContextId: String,
        taskId: String,
        originalMessageId: String
    ) {
        db.collection("pinned_contexts").document(pinnedContextId).delete().await()
        db.collection("chat_messages").document(originalMessageId).update(
            "isPinnedToTask", false,
            "pinnedToTaskId", null,
            "pinnedToTaskTitle", null
        ).await()
        db.collection("tasks").document(taskId).update(
            "pinnedCount", FieldValue.increment(-1)
        ).await()
    }

    suspend fun addTaskComment(
        companyId: String,
        taskId: String,
        taskTitle: String,
        text: String,
        author: UserProfileDto
    ) {
        val commentId = "cmt_" + UUID.randomUUID().toString().take(8)
        val data = hashMapOf(
            "id" to commentId,
            "companyId" to companyId,
            "taskId" to taskId,
            "authorId" to author.uid,
            "authorName" to author.name,
            "text" to text.trim(),
            "timestamp" to FieldValue.serverTimestamp()
        )
        db.collection("task_comments").document(commentId).set(data).await()
        db.collection("tasks").document(taskId).update("commentsCount", FieldValue.increment(1)).await()
        logActivity(companyId, author.name, "COMMENTED", "Commented on '$taskTitle'", taskId, taskTitle)
    }

    suspend fun createMilestone(
        companyId: String,
        title: String,
        description: String,
        owner: MemberItem?,
        targetDate: Long
    ) {
        val user = auth.currentUser
        val msId = "ms_" + UUID.randomUUID().toString().take(8)
        val data = hashMapOf<String, Any>(
            "id" to msId,
            "companyId" to companyId,
            "title" to title.trim(),
            "description" to description.trim(),
            "targetDate" to targetDate,
            "isCompleted" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!owner?.id.isNullOrBlank()) data["ownerId"] = owner!!.id
        if (!owner?.name.isNullOrBlank()) data["ownerName"] = owner!!.name
        db.collection("milestones").document(msId).set(data).await()
        logActivity(companyId, user?.displayName ?: "Founder", "MILESTONE", "Created milestone: '$title'")
    }

    suspend fun updatePresenceStatus(status: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("presenceStatus", status).await()
    }

    private suspend fun logActivity(
        companyId: String,
        actorName: String,
        actionType: String,
        description: String,
        targetTaskId: String? = null,
        targetTaskTitle: String? = null
    ) {
        val logId = "log_" + UUID.randomUUID().toString().take(8)
        val data = hashMapOf<String, Any>(
            "id" to logId,
            "companyId" to companyId,
            "actorName" to actorName,
            "actionType" to actionType,
            "description" to description,
            "timestamp" to FieldValue.serverTimestamp()
        )
        if (!targetTaskId.isNullOrBlank()) data["targetTaskId"] = targetTaskId
        if (!targetTaskTitle.isNullOrBlank()) data["targetTaskTitle"] = targetTaskTitle
        db.collection("activity_logs").document(logId).set(data).await()
    }
}
