package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FounderApp(
    viewModel: FounderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAuthenticated by viewModel.authState.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val profileLoadComplete by viewModel.profileLoadComplete.collectAsState()
    val profileLoadError by viewModel.profileLoadError.collectAsState()
    val company by viewModel.currentCompany.collectAsState()

    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Dialog states
    val showCreateTask by viewModel.showCreateTaskDialog.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val taskToPinMessage by viewModel.taskToPinMessage.collectAsState()
    val showCreateMilestone by viewModel.showCreateMilestoneDialog.collectAsState()
    val showInviteMember by viewModel.showInviteMemberDialog.collectAsState()
    val taskToDelete by viewModel.taskToDelete.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userFeedback.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 1. Auth Gate: If user is not authenticated with Google, show AuthScreen
    if (!isAuthenticated) {
        AuthScreen(
            onAuthSuccess = { /* authState Flow will automatically update */ }
        )
        return
    }

    // 2. Onboarding Gate: If user is authenticated but hasn't created or joined a company yet, show OnboardingScreen
    val companyId = userProfile?.companyId
    if (!profileLoadComplete) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (profileLoadError) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load your saved workspace. Check your connection and try again.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = viewModel::retryProfileLoad) {
                    Text("Retry")
                }
            }
        }
        return
    }
    if (userProfile == null || companyId.isNullOrBlank()) {
        OnboardingScreen(
            onCreateCompany = { name, email, companyName, role ->
                viewModel.createCompanyAsFounder(name, email, companyName, role)
            },
            onJoinCompany = { targetCompanyId, name, email, role ->
                viewModel.joinCompanyAsMember(targetCompanyId, name, email, role)
            }
        )
        return
    }

    // Handle back button for secondary screens
    if (currentScreen !is Screen.Main) {
        BackHandler {
            viewModel.navigateBack()
        }
    }

    // 3. Main Authenticated Team Workspace
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentScreen is Screen.Main) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(VoidBlack),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Hexagon,
                                            contentDescription = null,
                                            tint = ElectricIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Bottle",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val displayCompName = company?.name ?: userProfile?.companyName ?: "Team"
                                        Text(
                                            text = displayCompName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = ElectricIndigo,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Company ID quick chip with click to copy
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ElectricIndigoSubtle,
                                        modifier = Modifier.clickable {
                                            viewModel.navigateToTab(NavigationTab.PROFILE)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = null,
                                                tint = ElectricIndigo,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = companyId,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ElectricIndigo
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.navigateToTab(NavigationTab.PROFILE) },
                                        modifier = Modifier.testTag("topbar_profile_btn")
                                    ) {
                                        MemberAvatar(member = currentUser, sizeDp = 30)
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen && currentScreen is Screen.Main) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        val navItems = listOf(
                            Triple(NavigationTab.DASHBOARD, Icons.Default.Dashboard, Icons.Outlined.Dashboard),
                            Triple(NavigationTab.MY_TASKS, Icons.Default.TaskAlt, Icons.Outlined.TaskAlt),
                            Triple(NavigationTab.TEAM, Icons.Default.Groups, Icons.Outlined.Groups),
                            Triple(NavigationTab.MILESTONES, Icons.Default.Flag, Icons.Outlined.Flag),
                            Triple(NavigationTab.CHAT, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
                        )

                        navItems.forEach { (tab, filledIcon, outlinedIcon) ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricIndigo,
                                    selectedTextColor = ElectricIndigo,
                                    indicatorColor = ElectricIndigoSubtle
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentScreen is Screen.Main) {
                    when (currentTab) {
                        NavigationTab.DASHBOARD, NavigationTab.MY_TASKS -> {
                            FloatingActionButton(
                                onClick = { viewModel.showCreateTaskDialog.value = true },
                                containerColor = VoidBlack,
                                contentColor = PureWhite,
                                modifier = Modifier.testTag("global_fab_create_task")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Task")
                            }
                        }
                        NavigationTab.MILESTONES -> {
                            FloatingActionButton(
                                onClick = { viewModel.showCreateMilestoneDialog.value = true },
                                containerColor = ElectricIndigo,
                                contentColor = PureWhite,
                                modifier = Modifier.testTag("global_fab_create_milestone")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Milestone")
                            }
                        }
                        else -> {}
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Wide Screen Navigation Rail
                if (isWideScreen && currentScreen is Screen.Main) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("navigation_rail")
                    ) {
                        val navItems = listOf(
                            Triple(NavigationTab.DASHBOARD, Icons.Default.Dashboard, Icons.Outlined.Dashboard),
                            Triple(NavigationTab.MY_TASKS, Icons.Default.TaskAlt, Icons.Outlined.TaskAlt),
                            Triple(NavigationTab.TEAM, Icons.Default.Groups, Icons.Outlined.Groups),
                            Triple(NavigationTab.MILESTONES, Icons.Default.Flag, Icons.Outlined.Flag),
                            Triple(NavigationTab.CHAT, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
                            Triple(NavigationTab.PROFILE, Icons.Default.Person, Icons.Outlined.Person)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        navItems.forEach { (tab, filledIcon, outlinedIcon) ->
                            val isSelected = currentTab == tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricIndigo,
                                    selectedTextColor = ElectricIndigo,
                                    indicatorColor = ElectricIndigoSubtle
                                )
                            )
                        }
                    }
                }

                // Main Screen Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (val screen = currentScreen) {
                        is Screen.Main -> {
                            when (currentTab) {
                                NavigationTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                                NavigationTab.MY_TASKS -> MyTasksScreen(viewModel = viewModel)
                                NavigationTab.TEAM -> TeamScreen(viewModel = viewModel)
                                NavigationTab.MILESTONES -> MilestonesScreen(viewModel = viewModel)
                                NavigationTab.CHAT -> ChatScreen(viewModel = viewModel)
                                NavigationTab.PROFILE -> ProfileScreen(
                                    viewModel = viewModel,
                                    onSignOut = { viewModel.signOut(context) }
                                )
                            }
                        }
                        is Screen.MemberDashboard -> {
                            MemberDashboardScreen(memberId = screen.memberId, viewModel = viewModel)
                        }
                        is Screen.TaskDetail -> {
                            TaskDetailScreen(taskId = screen.taskId, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    // Dialogs Host
    if (showCreateTask || editingTask != null) {
        CreateEditTaskDialog(
            taskToEdit = editingTask,
            viewModel = viewModel,
            onDismiss = {
                viewModel.showCreateTaskDialog.value = false
                viewModel.editingTask.value = null
            }
        )
    }

    taskToPinMessage?.let { msg ->
        PinMessageToTaskDialog(
            message = msg,
            viewModel = viewModel,
            onDismiss = { viewModel.taskToPinMessage.value = null }
        )
    }

    if (showCreateMilestone) {
        CreateMilestoneDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showCreateMilestoneDialog.value = false }
        )
    }

    if (showInviteMember) {
        InviteMemberDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showInviteMemberDialog.value = false }
        )
    }

    taskToDelete?.let { task ->
        DeleteTaskConfirmDialog(
            task = task,
            onConfirm = { viewModel.deleteTask(task.id, task.title) },
            onDismiss = { viewModel.taskToDelete.value = null }
        )
    }
}
