package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskStatus
import com.example.data.model.UrgencyLevel
import com.example.ui.FounderViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    memberId: String,
    viewModel: FounderViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val members by viewModel.teamMembers.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val member = members.find { it.id == memberId }

    val memberTasks = allTasks.filter { it.assignee?.id == memberId }
    val activeTasks = memberTasks.filter { it.status != TaskStatus.DONE && it.status != TaskStatus.LEFT }
    val completedTasks = memberTasks.filter { it.status == TaskStatus.DONE }
    val abandonedTasks = memberTasks.filter { it.status == TaskStatus.LEFT }

    val urgentCount = activeTasks.count {
        it.timeLeftResult.urgencyLevel == UrgencyLevel.OVERDUE ||
                it.timeLeftResult.urgencyLevel == UrgencyLevel.URGENT
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(member?.name ?: "Member Dashboard") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("member_dashboard_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Team"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("member_dashboard_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(member = member, sizeDp = 52)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member?.name ?: "Team Member",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = member?.role ?: "Co-Founder",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = member?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Steel
                                )
                            }

                            // Live Presence Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (member?.presenceStatus) {
                                    "In Deep Work" -> UrgencyAttentionOrangeBg
                                    "Away" -> Mist
                                    else -> UrgencyOnTrackGreenBg
                                }
                            ) {
                                Text(
                                    text = member?.presenceStatus ?: "Active",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (member?.presenceStatus) {
                                        "In Deep Work" -> UrgencyAttentionOrange
                                        "Away" -> Steel
                                        else -> UrgencyOnTrackGreen
                                    }
                                )
                            }
                        }

                        // Workload KPIs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricKpiCard(
                                title = "Active Sprint",
                                value = "${activeTasks.size}",
                                subtitle = "In progress/queue",
                                accentColor = ElectricIndigo,
                                icon = Icons.Default.DirectionsRun,
                                modifier = Modifier.weight(1f)
                            )
                            MetricKpiCard(
                                title = "Urgent / Overdue",
                                value = "$urgentCount",
                                subtitle = "Needs immediate time",
                                accentColor = UrgencyOverdueRed,
                                icon = Icons.Default.WarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                            MetricKpiCard(
                                title = "Completed",
                                value = "${completedTasks.size}",
                                subtitle = "Shipped work",
                                accentColor = UrgencyOnTrackGreen,
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Active Tasks Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Workload (${activeTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (activeTasks.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.DoneOutline,
                        title = "No Active Tasks",
                        description = "${member?.name ?: "This member"} has completed all assigned tasks or is awaiting new assignments.",
                        actionLabel = "Assign Task",
                        onActionClick = { viewModel.showCreateTaskDialog.value = true }
                    )
                }
            } else {
                items(activeTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { viewModel.openTaskDetail(task.id) },
                        onStatusChange = { newStatus -> viewModel.updateTaskStatus(task.id, newStatus) }
                    )
                }
            }

            // Completed & Abandoned Section
            if (completedTasks.isNotEmpty() || abandonedTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "History & Closed Tasks",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(completedTasks + abandonedTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { viewModel.openTaskDetail(task.id) },
                        onStatusChange = { newStatus -> viewModel.updateTaskStatus(task.id, newStatus) }
                    )
                }
            }
        }
    }
}
