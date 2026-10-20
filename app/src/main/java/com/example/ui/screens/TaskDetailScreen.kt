package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PinnedContextItem
import com.example.data.model.TaskCommentItem
import com.example.data.model.TaskStatus
import com.example.ui.FounderViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: FounderViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val taskFlow = remember(taskId) { viewModel.getTaskDetail(taskId) }
    val commentsFlow = remember(taskId) { viewModel.getTaskComments(taskId) }
    val pinnedFlow = remember(taskId) { viewModel.getPinnedContexts(taskId) }

    val task by taskFlow.collectAsState(initial = null)
    val comments by commentsFlow.collectAsState(initial = emptyList())
    val pinnedContexts by pinnedFlow.collectAsState(initial = emptyList())

    var newCommentText by remember { mutableStateOf("") }
    var showStatusDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details", maxLines = 1) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("task_detail_back_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    task?.let { t ->
                        IconButton(
                            onClick = { viewModel.editingTask.value = t },
                            modifier = Modifier.testTag("edit_task_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(
                            onClick = { viewModel.taskToDelete.value = t },
                            modifier = Modifier.testTag("delete_task_btn")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = UrgencyOverdueRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("task_detail_screen")
    ) { padding ->
        if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val currentTask = task!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Title, Status, and Priority Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PriorityBadge(priority = currentTask.priority)
                                    UrgencyChip(timeLeft = currentTask.timeLeftResult)
                                }

                                Box {
                                    StatusPill(
                                        status = currentTask.status,
                                        onClick = { showStatusDropdown = true }
                                    )
                                    DropdownMenu(
                                        expanded = showStatusDropdown,
                                        onDismissRequest = { showStatusDropdown = false }
                                    ) {
                                        TaskStatus.entries.forEach { statusOption ->
                                            DropdownMenuItem(
                                                text = { StatusPill(status = statusOption) },
                                                onClick = {
                                                    showStatusDropdown = false
                                                    viewModel.updateTaskStatus(currentTask.id, statusOption)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = currentTask.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (currentTask.description.isNotBlank()) {
                                Text(
                                    text = currentTask.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Algorithm Explanation & Quadrant Card (Tascyn Engine)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (currentTask.quadrantResult.urgencyIndex) {
                                1 -> UrgencyOverdueRedBg.copy(alpha = 0.6f)
                                2 -> UrgencyUrgentOrangeRedBg.copy(alpha = 0.6f)
                                3 -> UrgencyAttentionOrangeBg.copy(alpha = 0.6f)
                                else -> ElectricIndigoSubtle.copy(alpha = 0.6f)
                            }
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = ElectricIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Priority Matrix Ranking",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VoidBlack
                                ) {
                                    Text(
                                        text = currentTask.quadrantResult.fullLabel,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PureWhite
                                    )
                                }
                            }

                            Text(
                                text = currentTask.quadrantResult.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Estimated Work: ${currentTask.minimumTimeRequired}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Steel
                                )
                                Text(
                                    text = "Urgency Index: ${currentTask.quadrantResult.urgencyIndex}/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Steel
                                )
                            }
                        }
                    }
                }

                // 3. Metadata Details (Assignee, Creator, Due date, Milestone, Tags)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Task Properties",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Assignee row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Assignee", style = MaterialTheme.typography.bodySmall, color = Steel)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MemberAvatar(member = currentTask.assignee, sizeDp = 24)
                                    Text(
                                        text = currentTask.assignee?.name ?: "Unassigned",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            HorizontalDivider(color = Mist)

                            // Creator row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Creator", style = MaterialTheme.typography.bodySmall, color = Steel)
                                Text(
                                    text = currentTask.creator?.name ?: "Workspace Member",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = Mist)

                            // Due Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Due Date", style = MaterialTheme.typography.bodySmall, color = Steel)
                                val dateStr = currentTask.dueDate?.let {
                                    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(it))
                                } ?: "No deadline"
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = Mist)

                            // Milestone
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Milestone", style = MaterialTheme.typography.bodySmall, color = Steel)
                                Text(
                                    text = currentTask.milestone?.title ?: "None",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (currentTask.milestone != null) ElectricIndigo else Steel
                                )
                            }

                            // Tags
                            if (currentTask.tags.isNotEmpty()) {
                                HorizontalDivider(color = Mist)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Categories", style = MaterialTheme.typography.bodySmall, color = Steel)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        currentTask.tags.forEach { tag ->
                                            TagChip(tag = tag)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. PINNED CHAT CONTEXT AREA (Mandatory per brief)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = ElectricIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pinned Team Chat Context (${pinnedContexts.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (pinnedContexts.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PushPin,
                                        contentDescription = null,
                                        tint = Steel,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "No team messages pinned yet. Pin messages from Team Chat to preserve decision context here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Steel
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pinnedContexts.forEach { pinned ->
                                    PinnedContextCard(
                                        item = pinned,
                                        onUnpin = {
                                            viewModel.unpinMessage(
                                                pinnedContextId = pinned.id,
                                                taskId = currentTask.id,
                                                originalMessageId = pinned.originalMessageId
                                            )
                                        },
                                        onJumpToChat = {
                                            viewModel.setChannel(pinned.channel)
                                            viewModel.navigateToTab(com.example.ui.NavigationTab.CHAT)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. IN-TASK CONVERSATION / COMMENTS THREAD
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = ElectricIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Task Conversation (${comments.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Direct task-specific discussion without cluttering general chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Steel
                        )

                        // Comment Composer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Write a comment on this task...", style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("task_comment_input"),
                                shape = RoundedCornerShape(10.dp),
                                maxLines = 3
                            )

                            IconButton(
                                onClick = {
                                    if (newCommentText.isNotBlank()) {
                                        viewModel.addTaskComment(currentTask.id, newCommentText)
                                        newCommentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (newCommentText.isNotBlank()) VoidBlack else Mist)
                                    .testTag("submit_task_comment_btn"),
                                enabled = newCommentText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Post comment",
                                    tint = if (newCommentText.isNotBlank()) PureWhite else Steel,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Comments List
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Start the task discussion above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Steel,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MemberAvatar(member = comment.author, sizeDp = 22)
                                        Text(
                                            text = comment.author?.name ?: "Teammate",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(comment.timestamp))
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Steel
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinnedContextCard(
    item: PinnedContextItem,
    onUnpin: () -> Unit,
    onJumpToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pinned_context_card_${item.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = UrgencyAttentionOrangeBg.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = UrgencyAttentionOrange,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "From #${item.channel} by ${item.authorName} (${item.authorRole})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = UrgencyAttentionOrange
                        )
                    )
                }

                val timeStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.originalTimestamp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Steel
                )
            }

            Text(
                text = item.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onJumpToChat,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = ElectricIndigo
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Jump to #${item.channel}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricIndigo
                        )
                    )
                }

                TextButton(
                    onClick = onUnpin,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = UrgencyOverdueRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Unpin",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = UrgencyOverdueRed
                        )
                    )
                }
            }
        }
    }
}
