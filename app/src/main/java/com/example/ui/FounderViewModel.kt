package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.CompanyDto
import com.example.data.firestore.UserProfileDto
import com.example.data.model.*
import com.example.data.priority.PriorityAlgorithm
import com.example.data.repository.FirebaseFounderRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    data class Main(val tab: NavigationTab = NavigationTab.DASHBOARD) : Screen()
    data class MemberDashboard(val memberId: String) : Screen()
    data class TaskDetail(val taskId: String) : Screen()
}

enum class NavigationTab(val label: String) {
    DASHBOARD("Dashboard"),
    MY_TASKS("My Tasks"),
    TEAM("Team"),
    MILESTONES("Milestones"),
    CHAT("Chat"),
    PROFILE("Profile")
}

enum class TaskViewMode {
    LIST,
    BOARD
}

class FounderViewModel(
    private val repository: FirebaseFounderRepository
) : ViewModel() {

    // Auth & User Profile State
    val authState = repository.authState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _manualProfile = MutableStateFlow<UserProfileDto?>(null)
    private val _profileLoadComplete = MutableStateFlow(false)
    val profileLoadComplete = _profileLoadComplete.asStateFlow()
    private val _profileLoadError = MutableStateFlow(false)
    val profileLoadError = _profileLoadError.asStateFlow()
    val currentUserProfile = combine(repository.currentUserProfile, _manualProfile) { repoProfile, manual ->
        manual ?: repoProfile
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _manualCompany = MutableStateFlow<CompanyDto?>(null)
    val currentCompany = combine(repository.currentCompany, _manualCompany) { repoCompany, manual ->
        manual ?: repoCompany
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.authState.collectLatest { isAuthenticated ->
                if (isAuthenticated) {
                    loadCurrentUserProfile()
                } else {
                    _manualProfile.value = null
                    _manualCompany.value = null
                    _profileLoadComplete.value = false
                    _profileLoadError.value = false
                }
            }
        }
    }

    private suspend fun loadCurrentUserProfile() {
        _profileLoadComplete.value = false
        _profileLoadError.value = false
        _manualProfile.value = null
        _manualCompany.value = null
        try {
            val uid = com.google.firebase.Firebase.auth.currentUser?.uid
            if (uid != null) {
                val profile = repository.fetchUserProfile(uid)
                if (profile != null && profile.companyId.isNotBlank()) {
                    _manualProfile.value = profile
                }
            } else {
                _profileLoadError.value = true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _profileLoadError.value = true
            android.util.Log.e("FounderViewModel", "Unable to resolve saved onboarding profile", e)
        } finally {
            _profileLoadComplete.value = true
        }
    }

    fun retryProfileLoad() {
        if (com.google.firebase.Firebase.auth.currentUser == null) return
        viewModelScope.launch { loadCurrentUserProfile() }
    }

    // Screen navigation stack
    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Main(NavigationTab.DASHBOARD)))
    val currentScreen: StateFlow<Screen> = _screenStack.map { it.lastOrNull() ?: Screen.Main(NavigationTab.DASHBOARD) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Screen.Main(NavigationTab.DASHBOARD))

    // Active bottom navigation tab
    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Data streams from Firestore
    val teamMembers = repository.teamMembers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val milestones = repository.milestones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTasks = repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activityLogs = repository.activityLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<MemberItem?> = combine(currentUserProfile, teamMembers) { profile, members ->
        members.find { it.isCurrentUser } ?: profile?.let {
            MemberItem(
                id = it.uid,
                workspaceId = it.companyId,
                name = it.name,
                email = it.email,
                role = it.role,
                avatarInitials = it.avatarInitials,
                avatarColorHex = it.avatarColorHex,
                presenceStatus = it.presenceStatus,
                isCurrentUser = true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Chat
    private val _activeChannel = MutableStateFlow("general")
    val activeChannel: StateFlow<String> = _activeChannel.asStateFlow()

    private val _chatSearchQuery = MutableStateFlow("")
    val chatSearchQuery: StateFlow<String> = _chatSearchQuery.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessageItem>> = _activeChannel.flatMapLatest { channel ->
        repository.getChatMessages(channel)
    }.combine(_chatSearchQuery) { messages, query ->
        if (query.isBlank()) messages
        else messages.filter { it.content.contains(query, ignoreCase = true) || it.sender?.name?.contains(query, ignoreCase = true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // My Tasks UI filters
    val myTasksViewMode = MutableStateFlow(TaskViewMode.LIST)
    val myTasksPriorityFilter = MutableStateFlow<TaskPriority?>(null)
    val myTasksStatusFilter = MutableStateFlow<TaskStatus?>(null)
    val showLeftAndDoneTasks = MutableStateFlow(false)

    val workspace = currentCompany

    // Dialog & Action States
    val showCreateTaskDialog = MutableStateFlow(false)
    val editingTask = MutableStateFlow<TaskItem?>(null)
    val taskToPinMessage = MutableStateFlow<ChatMessageItem?>(null)
    val showCreateMilestoneDialog = MutableStateFlow(false)
    val showInviteMemberDialog = MutableStateFlow(false)
    val showUserSwitcher = MutableStateFlow(false)
    val taskToDelete = MutableStateFlow<TaskItem?>(null)

    // Quick notification / snackbar
    private val _userFeedback = MutableSharedFlow<String>()
    val userFeedback = _userFeedback.asSharedFlow()

    // Navigation methods
    fun navigateToTab(tab: NavigationTab) {
        _currentTab.value = tab
        _screenStack.value = listOf(Screen.Main(tab))
    }

    fun openMemberDashboard(memberId: String) {
        _screenStack.value = _screenStack.value + Screen.MemberDashboard(memberId)
    }

    fun openTaskDetail(taskId: String) {
        _screenStack.value = _screenStack.value + Screen.TaskDetail(taskId)
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        return if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    // Onboarding Actions
    suspend fun createCompanyAsFounder(name: String, email: String, companyName: String, role: String) {
        val id = repository.createCompanyAsFounder(companyName, name, email, role)
        val uid = com.google.firebase.Firebase.auth.currentUser?.uid ?: ""
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "FC" }
        _manualProfile.value = UserProfileDto(
            uid = uid,
            email = email,
            name = name,
            role = role.ifBlank { "Founder & CEO" },
            companyId = id,
            companyName = companyName,
            isFounder = true,
            avatarInitials = initials
        )
        _manualCompany.value = CompanyDto(
            id = id,
            name = companyName,
            founderUid = uid,
            founderName = name,
            founderEmail = email,
            memberUids = listOf(uid)
        )
        _userFeedback.emit("Startup workspace '$companyName' ($id) created!")
    }

    suspend fun joinCompanyAsMember(companyId: String, name: String, email: String, role: String): Result<Unit> {
        val result = repository.joinCompanyAsMember(companyId, name, email, role)
        if (result.isSuccess) {
            val uid = com.google.firebase.Firebase.auth.currentUser?.uid ?: ""
            val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "TM" }
            _manualProfile.value = UserProfileDto(
                uid = uid,
                email = email,
                name = name,
                role = role.ifBlank { "Core Team" },
                companyId = companyId.trim().uppercase(),
                companyName = "Startup Team",
                isFounder = false,
                avatarInitials = initials
            )
            _userFeedback.emit("Successfully joined team workspace!")
        }
        return result
    }

    // Chat actions
    fun setChannel(channel: String) {
        _activeChannel.value = channel
    }

    fun setChatSearchQuery(query: String) {
        _chatSearchQuery.value = query
    }

    fun sendChatMessage(text: String) {
        val profile = currentUserProfile.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendChatMessage(profile.companyId, _activeChannel.value, text, profile)
        }
    }

    fun pinMessageToTask(message: ChatMessageItem, task: TaskItem) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.pinMessageToTask(profile.companyId, message, task)
            taskToPinMessage.value = null
            _userFeedback.emit("Message pinned to task context")
        }
    }

    fun unpinMessage(pinnedContextId: String, taskId: String, originalMessageId: String) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.unpinMessage(profile.companyId, pinnedContextId, taskId, originalMessageId)
            _userFeedback.emit("Context unpinned")
        }
    }

    // Task actions
    fun updateTaskStatus(taskId: String, status: TaskStatus, taskTitle: String = "Task") {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.updateTaskStatus(profile.companyId, taskId, taskTitle, status)
            _userFeedback.emit("Status updated to ${status.displayName}")
        }
    }

    fun updateTaskStatus(taskId: String, taskTitle: String, status: TaskStatus) {
        updateTaskStatus(taskId, status, taskTitle)
    }

    fun switchUser(memberId: String) {
        openMemberDashboard(memberId)
    }

    fun switchUser(member: MemberItem) {
        openMemberDashboard(member.id)
    }

    fun reassignTask(taskId: String, taskTitle: String, assignee: MemberItem?) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.reassignTask(profile.companyId, taskId, taskTitle, assignee)
            _userFeedback.emit("Task reassigned")
        }
    }

    fun saveTask(
        taskId: String?,
        title: String,
        description: String,
        assignee: MemberItem?,
        priority: TaskPriority,
        dueDate: Long?,
        minimumTimeRequired: String,
        milestone: MilestoneItem?,
        tags: List<TaskCategory>,
        status: TaskStatus = TaskStatus.NOT_STARTED
    ) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.createTask(
                companyId = profile.companyId,
                title = title,
                description = description,
                assignee = assignee,
                priority = priority,
                dueDate = dueDate,
                minimumTimeRequired = minimumTimeRequired,
                milestone = milestone,
                tags = tags,
                status = status
            )
            _userFeedback.emit("Task created")
            showCreateTaskDialog.value = false
            editingTask.value = null
        }
    }

    fun deleteTask(taskId: String, taskTitle: String = "Task") {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.deleteTask(profile.companyId, taskId, taskTitle)
            taskToDelete.value = null
            if (currentScreen.value is Screen.TaskDetail) {
                navigateBack()
            }
            _userFeedback.emit("Task deleted")
        }
    }

    fun signOut(context: android.content.Context) {
        viewModelScope.launch {
            _manualProfile.value = null
            _manualCompany.value = null
            val cm = androidx.credentials.CredentialManager.create(context)
            com.google.firebase.Firebase.auth.signOut()
            try {
                cm.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            } catch (e: Exception) {
                // Ignore
            }
            _userFeedback.emit("Signed out")
        }
    }

    fun addTaskComment(taskId: String, comment: String, taskTitle: String = "Task") {
        val profile = currentUserProfile.value ?: return
        if (comment.isBlank()) return
        viewModelScope.launch {
            repository.addTaskComment(profile.companyId, taskId, taskTitle, comment, profile)
        }
    }

    fun createMilestone(title: String, description: String, owner: MemberItem?, targetDate: Long) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.createMilestone(profile.companyId, title, description, owner, targetDate)
            showCreateMilestoneDialog.value = false
            _userFeedback.emit("Milestone created")
        }
    }

    fun updatePresenceStatus(status: String) {
        viewModelScope.launch {
            repository.updatePresenceStatus(status)
        }
    }

    fun createTaskFromChatMessage(
        message: ChatMessageItem,
        taskTitle: String,
        assignee: MemberItem?,
        priority: TaskPriority,
        dueDate: Long?,
        minimumTimeRequired: String,
        milestone: MilestoneItem?
    ) {
        val profile = currentUserProfile.value ?: return
        viewModelScope.launch {
            val taskId = repository.createTask(
                companyId = profile.companyId,
                title = taskTitle,
                description = "Created from chat context: \"${message.content}\"",
                assignee = assignee,
                priority = priority,
                dueDate = dueDate,
                minimumTimeRequired = minimumTimeRequired,
                milestone = milestone,
                tags = listOf(TaskCategory.PRODUCT)
            )
            // Pin the message to the newly created task
            val taskItem = allTasks.value.find { it.id == taskId } ?: TaskItem(
                id = taskId,
                workspaceId = profile.companyId,
                title = taskTitle,
                description = message.content,
                status = TaskStatus.NOT_STARTED,
                priority = priority,
                assignee = assignee,
                creator = currentUser.value,
                dueDate = dueDate,
                minimumTimeRequired = minimumTimeRequired,
                milestone = milestone,
                timeLeftResult = PriorityAlgorithm.calculateTimeLeft(dueDate, minimumTimeRequired),
                quadrantResult = PriorityAlgorithm.calculateQuadrant(TaskStatus.NOT_STARTED, priority, dueDate, minimumTimeRequired)
            )
            repository.pinMessageToTask(profile.companyId, message, taskItem)
            taskToPinMessage.value = null
            _userFeedback.emit("Task created and message pinned")
        }
    }

    fun getTaskDetail(taskId: String): Flow<TaskItem?> = allTasks.map { list -> list.find { it.id == taskId } }
    fun getTaskComments(taskId: String): Flow<List<TaskCommentItem>> = repository.getTaskComments(taskId)
    fun getPinnedContexts(taskId: String): Flow<List<PinnedContextItem>> = repository.getPinnedContexts(taskId)
}

class FounderViewModelFactory(
    private val repository: FirebaseFounderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FounderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FounderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
