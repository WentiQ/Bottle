package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.FounderViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTaskDialog(
    taskToEdit: TaskItem?,
    viewModel: FounderViewModel,
    onDismiss: () -> Unit
) {
    val members by viewModel.teamMembers.collectAsState()
    val milestones by viewModel.milestones.collectAsState()

    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedAssignee by remember { mutableStateOf(taskToEdit?.assignee) }
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: TaskPriority.MEDIUM) }
    var selectedStatus by remember { mutableStateOf(taskToEdit?.status ?: TaskStatus.NOT_STARTED) }
    var selectedMilestone by remember { mutableStateOf(taskToEdit?.milestone) }
    var minimumTimeRequired by remember { mutableStateOf(taskToEdit?.minimumTimeRequired ?: "0d 2h 0m") }
    var dueDateOffsetHours by remember { mutableStateOf<Long?>(null) }
    var selectedTags by remember { mutableStateOf(taskToEdit?.tags?.toSet() ?: setOf(TaskCategory.PRODUCT)) }

    val calculatedDueDate: Long? = remember(dueDateOffsetHours, taskToEdit?.dueDate) {
        if (dueDateOffsetHours != null) {
            System.currentTimeMillis() + (dueDateOffsetHours!! * 3600_000L)
        } else {
            taskToEdit?.dueDate ?: (System.currentTimeMillis() + (24 * 3600_000L))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("create_edit_task_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (taskToEdit == null) "Create Startup Task" else "Edit Task",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Task Title *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description & Notes") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("task_desc_input"),
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Assignee
                    item {
                        Text("Assignee", style = MaterialTheme.typography.labelMedium, color = Steel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedAssignee == null,
                                onClick = { selectedAssignee = null },
                                label = { Text("Unassigned") }
                            )
                            members.forEach { m ->
                                FilterChip(
                                    selected = selectedAssignee?.id == m.id,
                                    onClick = { selectedAssignee = m },
                                    label = { Text(m.name) },
                                    leadingIcon = { MemberAvatar(member = m, sizeDp = 18, showPresence = false) }
                                )
                            }
                        }
                    }

                    // Priority
                    item {
                        Text("Manual Priority", style = MaterialTheme.typography.labelMedium, color = Steel)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TaskPriority.entries.forEach { p ->
                                FilterChip(
                                    selected = selectedPriority == p,
                                    onClick = { selectedPriority = p },
                                    label = { Text(p.displayName) }
                                )
                            }
                        }
                    }

                    // Status
                    item {
                        Text("Status", style = MaterialTheme.typography.labelMedium, color = Steel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskStatus.entries.forEach { s ->
                                FilterChip(
                                    selected = selectedStatus == s,
                                    onClick = { selectedStatus = s },
                                    label = { Text(s.displayName) }
                                )
                            }
                        }
                    }

                    // Estimated Work Duration
                    item {
                        Text("Estimated Duration (Work Buffer)", style = MaterialTheme.typography.labelMedium, color = Steel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val durations = listOf("0d 1h 0m", "0d 2h 0m", "0d 4h 0m", "1d 0h 0m", "2d 0h 0m")
                            durations.forEach { dur ->
                                FilterChip(
                                    selected = minimumTimeRequired == dur,
                                    onClick = { minimumTimeRequired = dur },
                                    label = { Text(dur) }
                                )
                            }
                        }
                    }

                    // Deadline / Due Date
                    item {
                        Text("Deadline (Due Date)", style = MaterialTheme.typography.labelMedium, color = Steel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val offsets = listOf(
                                6L to "In 6 Hours",
                                24L to "Tomorrow",
                                48L to "In 2 Days",
                                120L to "In 5 Days",
                                240L to "In 10 Days"
                            )
                            offsets.forEach { (hours, label) ->
                                FilterChip(
                                    selected = dueDateOffsetHours == hours,
                                    onClick = { dueDateOffsetHours = hours },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Milestone Link
                    item {
                        Text("Link to Milestone", style = MaterialTheme.typography.labelMedium, color = Steel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedMilestone == null,
                                onClick = { selectedMilestone = null },
                                label = { Text("No Milestone") }
                            )
                            milestones.forEach { ms ->
                                FilterChip(
                                    selected = selectedMilestone?.id == ms.id,
                                    onClick = { selectedMilestone = ms },
                                    label = { Text(ms.title) }
                                )
                            }
                        }
                    }

                    // Category Tags
                    item {
                        Text("Categories", style = MaterialTheme.typography.labelMedium, color = Steel)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = cat in selectedTags,
                                    onClick = {
                                        selectedTags = if (cat in selectedTags) selectedTags - cat else selectedTags + cat
                                    },
                                    label = { Text(cat.displayName) }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.saveTask(
                                    taskId = taskToEdit?.id,
                                    title = title.trim(),
                                    description = description.trim(),
                                    assignee = selectedAssignee,
                                    priority = selectedPriority,
                                    dueDate = calculatedDueDate,
                                    minimumTimeRequired = minimumTimeRequired,
                                    milestone = selectedMilestone,
                                    tags = selectedTags.toList(),
                                    status = selectedStatus
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VoidBlack, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_task_submit_btn")
                    ) {
                        Text(if (taskToEdit == null) "Create Task" else "Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun PinMessageToTaskDialog(
    message: ChatMessageItem,
    viewModel: FounderViewModel,
    onDismiss: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val activeTasks = tasks.filter { it.status != TaskStatus.DONE && it.status != TaskStatus.LEFT }

    var isCreatingNewTask by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf(message.content.take(50)) }
    var searchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .testTag("pin_message_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pin Context to Task",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Attach this chat snippet to keep decision context attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = Steel
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Mist.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${message.content}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCreatingNewTask,
                        onClick = { isCreatingNewTask = false },
                        label = { Text("Pin to Existing Task") }
                    )
                    FilterChip(
                        selected = isCreatingNewTask,
                        onClick = { isCreatingNewTask = true },
                        label = { Text("Create New Task") }
                    )
                }

                if (isCreatingNewTask) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("New Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                viewModel.createTaskFromChatMessage(
                                    message = message,
                                    taskTitle = newTaskTitle.trim(),
                                    assignee = message.sender,
                                    priority = TaskPriority.HIGH,
                                    dueDate = System.currentTimeMillis() + (24 * 3600_000L),
                                    minimumTimeRequired = "0d 2h 0m",
                                    milestone = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VoidBlack, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create Task & Pin Context")
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter tasks...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    val matchingTasks = activeTasks.filter {
                        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(matchingTasks, key = { it.id }) { task ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.pinMessageToTask(message, task)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceSubtle,
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${task.assignee?.name ?: "Unassigned"} • ${task.timeLeftResult.formattedTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Steel
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin",
                                        tint = ElectricIndigo,
                                        modifier = Modifier.size(18.dp)
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

@Composable
fun CreateMilestoneDialog(
    viewModel: FounderViewModel,
    onDismiss: () -> Unit
) {
    val members by viewModel.teamMembers.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedOwner by remember { mutableStateOf<MemberItem?>(null) }
    var targetDaysOffset by remember { mutableStateOf(14L) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_milestone_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "New Startup Milestone",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Milestone Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Success Metric") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text("Target Deadline", style = MaterialTheme.typography.labelMedium, color = Steel)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val days = listOf(7L to "1 Week", 14L to "2 Weeks", 30L to "1 Month", 60L to "2 Months")
                    days.forEach { (d, label) ->
                        FilterChip(
                            selected = targetDaysOffset == d,
                            onClick = { targetDaysOffset = d },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val targetDate = System.currentTimeMillis() + (targetDaysOffset * 86400_000L)
                                viewModel.createMilestone(title.trim(), description.trim(), selectedOwner, targetDate)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = VoidBlack, contentColor = PureWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteTaskConfirmDialog(
    task: TaskItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Task?") },
        text = {
            Text("Are you sure you want to delete '${task.title}'? This action removes associated comments and pinned chat context.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = UrgencyOverdueRed, contentColor = PureWhite)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun InviteMemberDialog(
    viewModel: FounderViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val company by viewModel.currentCompany.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val companyId = company?.id ?: userProfile?.companyId ?: ""
    val companyName = company?.name ?: userProfile?.companyName ?: "Startup Team"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Invite Team Member",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Share this Startup ID or scan QR code to add co-founders or employees to '$companyName':",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                QrCodeView(
                    data = companyId,
                    sizeDp = 160.dp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElectricIndigoSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "STARTUP / COMPANY ID",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = ElectricIndigo)
                            )
                            Text(
                                companyId,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Startup ID", companyId))
                            android.widget.Toast.makeText(context, "Startup ID copied!", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = ElectricIndigo)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VoidBlack, contentColor = PureWhite)
            ) {
                Text("Done")
            }
        }
    )
}
