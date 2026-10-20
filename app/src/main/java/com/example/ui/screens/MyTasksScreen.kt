package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.ui.FounderViewModel
import com.example.ui.TaskViewMode
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.StatusPill
import com.example.ui.components.TaskCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    viewModel: FounderViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val viewMode by viewModel.myTasksViewMode.collectAsState()
    val priorityFilter by viewModel.myTasksPriorityFilter.collectAsState()
    val statusFilter by viewModel.myTasksStatusFilter.collectAsState()
    val showLeftAndDone by viewModel.showLeftAndDoneTasks.collectAsState()

    // Tasks assigned to current signed-in user
    val myTasks = tasks.filter { it.assignee?.id == currentUser?.id }

    // Filtered tasks
    val filteredTasks = myTasks.filter { task ->
        val matchesPriority = priorityFilter == null || task.priority == priorityFilter
        val matchesStatus = statusFilter == null || task.status == statusFilter
        val matchesVisibility = if (!showLeftAndDone) {
            task.status != TaskStatus.DONE && task.status != TaskStatus.LEFT
        } else {
            true
        }
        matchesPriority && matchesStatus && matchesVisibility
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_tasks_screen")
    ) {
        // Top Header & Controls Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MemberAvatar(member = currentUser, sizeDp = 36)
                    Column {
                        Text(
                            text = "My Assigned Work",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${filteredTasks.size} tasks in focus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // View Mode Toggle (List vs Board)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.testTag("view_mode_toggle")
                ) {
                    SegmentedButton(
                        selected = viewMode == TaskViewMode.LIST,
                        onClick = { viewModel.myTasksViewMode.value = TaskViewMode.LIST },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Icon(imageVector = Icons.Default.ViewList, contentDescription = "List", modifier = Modifier.size(16.dp))
                    }
                    SegmentedButton(
                        selected = viewMode == TaskViewMode.BOARD,
                        onClick = { viewModel.myTasksViewMode.value = TaskViewMode.BOARD },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Icon(imageVector = Icons.Default.ViewKanban, contentDescription = "Board", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority filter
                FilterChip(
                    selected = priorityFilter == TaskPriority.HIGH,
                    onClick = {
                        viewModel.myTasksPriorityFilter.value =
                            if (priorityFilter == TaskPriority.HIGH) null else TaskPriority.HIGH
                    },
                    label = { Text("High Priority") },
                    leadingIcon = {
                        if (priorityFilter == TaskPriority.HIGH) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )

                FilterChip(
                    selected = priorityFilter == TaskPriority.MEDIUM,
                    onClick = {
                        viewModel.myTasksPriorityFilter.value =
                            if (priorityFilter == TaskPriority.MEDIUM) null else TaskPriority.MEDIUM
                    },
                    label = { Text("Medium") }
                )

                // Show completed & Left (abandoned) toggle
                FilterChip(
                    selected = showLeftAndDone,
                    onClick = { viewModel.showLeftAndDoneTasks.value = !showLeftAndDone },
                    label = { Text(if (showLeftAndDone) "Showing All (Done & Left)" else "Hide Done & Left") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showLeftAndDone) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = CardBorderLight)

        // Content Area: List View vs Board View
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Default.TaskAlt,
                    title = "No Tasks In This View",
                    description = if (!showLeftAndDone) "You have cleared all active tasks! Toggle 'Show Done & Left' or create a new task."
                    else "No tasks found matching your selected filters.",
                    actionLabel = "Create Task",
                    onActionClick = { viewModel.showCreateTaskDialog.value = true }
                )
            }
        } else if (viewMode == TaskViewMode.LIST) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("my_tasks_list_view"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { viewModel.openTaskDetail(task.id) },
                        onStatusChange = { newStatus -> viewModel.updateTaskStatus(task.id, task.title, newStatus) }
                    )
                }
            }
        } else {
            // Kanban Board View
            BoardView(
                tasks = filteredTasks,
                onTaskClick = { viewModel.openTaskDetail(it.id) },
                onStatusChange = { task, newStatus -> viewModel.updateTaskStatus(task.id, task.title, newStatus) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BoardView(
    tasks: List<TaskItem>,
    onTaskClick: (TaskItem) -> Unit,
    onStatusChange: (TaskItem, TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = listOf(
        TaskStatus.NOT_STARTED to "Not Started",
        TaskStatus.WORKING to "Working",
        TaskStatus.PAUSED to "Paused",
        TaskStatus.DONE to "Done",
        TaskStatus.LEFT to "Left (Abandoned)"
    )

    LazyRow(
        modifier = modifier.testTag("my_tasks_board_view"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(columns) { (status, columnTitle) ->
            val columnTasks = tasks.filter { it.status == status }
            BoardColumn(
                status = status,
                title = columnTitle,
                tasks = columnTasks,
                onTaskClick = onTaskClick,
                onStatusChange = onStatusChange,
                modifier = Modifier.width(280.dp)
            )
        }
    }
}

@Composable
private fun BoardColumn(
    status: TaskStatus,
    title: String,
    tasks: List<TaskItem>,
    onTaskClick: (TaskItem) -> Unit,
    onStatusChange: (TaskItem, TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .testTag("board_column_${status.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(status = status)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Mist
                ) {
                    Text(
                        text = "${tasks.size}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Steel
                    )
                }
            }

            HorizontalDivider(color = Mist)

            // Column Task List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = Steel
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task) },
                            onStatusChange = { newStatus -> onStatusChange(task, newStatus) }
                        )
                    }
                }
            }
        }
    }
}
