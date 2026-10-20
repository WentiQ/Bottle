package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.data.model.TaskStatus
import com.example.data.model.UrgencyLevel
import com.example.ui.FounderViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FounderViewModel,
    modifier: Modifier = Modifier
) {
    val workspace by viewModel.currentCompany.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Filter active tasks (exclude Done and Left from default dashboard views)
    val activeTasks = tasks.filter { it.status != TaskStatus.DONE && it.status != TaskStatus.LEFT }
    val urgentTasks = activeTasks.filter {
        it.timeLeftResult.urgencyLevel == UrgencyLevel.OVERDUE ||
                it.timeLeftResult.urgencyLevel == UrgencyLevel.URGENT ||
                it.timeLeftResult.urgencyLevel == UrgencyLevel.ATTENTION_NEEDED
    }

    val workingTasksCount = activeTasks.count { it.status == TaskStatus.WORKING }
    val doneTasksCount = tasks.count { it.status == TaskStatus.DONE }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Welcome & Workspace Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workspace_hero_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoidBlack),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Carbon
                        ) {
                            Text(
                                text = "STARTUP WORKSPACE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = Silver
                            )
                        }

                        if (currentUser != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Carbon)
                                    .clickable { viewModel.navigateToTab(NavigationTab.PROFILE) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("current_user_badge")
                            ) {
                                MemberAvatar(member = currentUser, sizeDp = 20, showPresence = false)
                                Text(
                                    text = currentUser?.name ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite
                                )
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch user",
                                    tint = Silver,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = workspace?.name ?: "FounderSync Workspace",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    )

                    Text(
                        text = "Startup ID: ${workspace?.id ?: ""} • Co-founders and team sync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver
                    )
                }
            }
        }

        // 2. Needs Attention Alert Box (if urgent/overdue tasks exist)
        if (urgentTasks.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("needs_attention_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = UrgencyUrgentOrangeRedBg),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(UrgencyUrgentOrangeRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "What Needs Attention",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = UrgencyUrgentOrangeRed
                            )
                            Text(
                                text = "${urgentTasks.size} task(s) in critical window where time remaining is less than estimated work duration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick KPI Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricKpiCard(
                    title = "Attention Needed",
                    value = "${urgentTasks.size}",
                    subtitle = "Urgent or Overdue",
                    accentColor = UrgencyUrgentOrangeRed,
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "In Progress",
                    value = "$workingTasksCount",
                    subtitle = "Active focus",
                    accentColor = ElectricIndigo,
                    icon = Icons.Default.DirectionsRun,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "Shipped",
                    value = "$doneTasksCount",
                    subtitle = "Done to date",
                    accentColor = UrgencyOnTrackGreen,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Milestone Progress Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Milestones",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { viewModel.navigateToTab(NavigationTab.MILESTONES) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("View all", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (milestones.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.Flag,
                        title = "No Milestones Yet",
                        description = "Create sprint or fundraising milestones to rally the team.",
                        actionLabel = "Add Milestone",
                        onActionClick = { viewModel.showCreateMilestoneDialog.value = true }
                    )
                } else {
                    milestones.take(2).forEach { ms ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("milestone_card_${ms.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ms.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val percentInt = (ms.progressPercent * 100).toInt()
                                    Text(
                                        text = "$percentInt%",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricIndigo
                                        )
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { ms.progressPercent },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (ms.progressPercent >= 1f) UrgencyOnTrackGreen else ElectricIndigo,
                                    trackColor = Mist
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ms.doneTasks} of ${ms.totalTasks - ms.leftTasks} tasks completed" +
                                                (if (ms.leftTasks > 0) " (${ms.leftTasks} abandoned)" else ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms.targetDate))
                                    Text(
                                        text = "Target: $dateStr",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Steel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Prioritized Team Tasks Section (Tascyn ranking)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Prioritized Work Queue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ranked deterministically by due date vs required time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { viewModel.showCreateTaskDialog.value = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("dashboard_new_task_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Task", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (activeTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.DoneAll,
                    title = "All Caught Up!",
                    description = "No active tasks in the queue. Create one to keep startup velocity moving.",
                    actionLabel = "Create Task",
                    onActionClick = { viewModel.showCreateTaskDialog.value = true }
                )
            }
        } else {
            items(activeTasks.take(5), key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { viewModel.openTaskDetail(task.id) },
                    onStatusChange = { newStatus -> viewModel.updateTaskStatus(task.id, task.title, newStatus) }
                )
            }
        }

        // 6. Recent Activity Timeline
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Recent Team Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (activityLogs.isEmpty()) {
                            Text(
                                text = "No recent activity recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Steel
                            )
                        } else {
                            activityLogs.take(5).forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val (icon, iconColor) = when (log.actionType) {
                                        "STATUS_CHANGE" -> Icons.Default.Sync to ElectricIndigo
                                        "PINNED_CONTEXT" -> Icons.Default.PushPin to UrgencyAttentionOrange
                                        "CREATED" -> Icons.Default.AddCircleOutline to UrgencyOnTrackGreen
                                        "COMMENTED" -> Icons.Default.ChatBubbleOutline to UrgencyNeutralBlue
                                        else -> Icons.Default.History to Steel
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(iconColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                        Text(
                                            text = "${log.actorName} • $timeStr",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Steel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
