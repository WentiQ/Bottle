package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MilestoneItem
import com.example.ui.FounderViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.TaskCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MilestonesScreen(
    viewModel: FounderViewModel,
    modifier: Modifier = Modifier
) {
    val milestones by viewModel.milestones.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("milestones_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Startup Milestones",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Key deliverables driving traction, funding, and releases",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.showCreateMilestoneDialog.value = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoidBlack,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.testTag("add_milestone_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Milestone", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (milestones.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Flag,
                    title = "No Milestones Defined",
                    description = "Break your company roadmap into high-leverage milestones with target dates and linked tasks.",
                    actionLabel = "Create First Milestone",
                    onActionClick = { viewModel.showCreateMilestoneDialog.value = true }
                )
            }
        } else {
            items(milestones, key = { it.id }) { milestone ->
                val linkedTasks = allTasks.filter { it.milestone?.id == milestone.id }
                MilestoneDetailCard(
                    milestone = milestone,
                    linkedTasks = linkedTasks,
                    onTaskClick = { viewModel.openTaskDetail(it.id) },
                    onTaskStatusChange = { taskId, newStatus -> viewModel.updateTaskStatus(taskId, newStatus) },
                    onAddTaskToMilestone = {
                        viewModel.showCreateTaskDialog.value = true
                    }
                )
            }
        }
    }
}

@Composable
fun MilestoneDetailCard(
    milestone: MilestoneItem,
    linkedTasks: List<com.example.data.model.TaskItem>,
    onTaskClick: (com.example.data.model.TaskItem) -> Unit,
    onTaskStatusChange: (String, com.example.data.model.TaskStatus) -> Unit,
    onAddTaskToMilestone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val progressInt = (milestone.progressPercent * 100).toInt()
    val isDone = milestone.progressPercent >= 1.0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("milestone_card_${milestone.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Milestone Title & Target Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDone) UrgencyOnTrackGreenBg else ElectricIndigoSubtle
                        ) {
                            Text(
                                text = if (isDone) "COMPLETED" else "ACTIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (isDone) UrgencyOnTrackGreen else ElectricIndigo
                            )
                        }

                        val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(milestone.targetDate))
                        Text(
                            text = "Due: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = Steel
                        )
                    }

                    Text(
                        text = milestone.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (milestone.description.isNotBlank()) {
                        Text(
                            text = milestone.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Progress Badge
                Text(
                    text = "$progressInt%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) UrgencyOnTrackGreen else ElectricIndigo
                    )
                )
            }

            // Progress Bar & Formula Disclosures
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { milestone.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isDone) UrgencyOnTrackGreen else ElectricIndigo,
                    trackColor = Mist
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeTarget = (milestone.totalTasks - milestone.leftTasks).coerceAtLeast(1)
                    Text(
                        text = "${milestone.doneTasks} of $activeTarget required tasks completed" +
                                (if (milestone.leftTasks > 0) " (${milestone.leftTasks} abandoned/excluded)" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (milestone.owner != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Lead:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Steel
                            )
                            MemberAvatar(member = milestone.owner, sizeDp = 20)
                            Text(
                                text = milestone.owner.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Mist)

            // Linked Tasks Expansion Toggle & List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Linked Tasks (${linkedTasks.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                FilledTonalButton(
                    onClick = onAddTaskToMilestone,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (isExpanded) {
                if (linkedTasks.isEmpty()) {
                    Text(
                        text = "No tasks currently linked to this milestone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Steel,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        linkedTasks.forEach { task ->
                            TaskCard(
                                task = task,
                                onClick = { onTaskClick(task) },
                                onStatusChange = { newStatus -> onTaskStatusChange(task.id, newStatus) }
                            )
                        }
                    }
                }
            }
        }
    }
}
