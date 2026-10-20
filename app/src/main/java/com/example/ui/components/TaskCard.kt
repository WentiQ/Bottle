package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.data.model.TaskStatus
import com.example.ui.theme.*

@Composable
fun TaskCard(
    task: TaskItem,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.status == TaskStatus.DONE) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else if (task.status == TaskStatus.LEFT) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Quadrant Action Tag + Priority Badge + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quadrant action tag from algorithm
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (task.quadrantResult.urgencyIndex) {
                            1 -> UrgencyOverdueRedBg
                            2 -> UrgencyUrgentOrangeRedBg
                            3 -> UrgencyAttentionOrangeBg
                            else -> ElectricIndigoSubtle
                        }
                    ) {
                        Text(
                            text = task.quadrantResult.fullLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = when (task.quadrantResult.urgencyIndex) {
                                1 -> UrgencyOverdueRed
                                2 -> UrgencyUrgentOrangeRed
                                3 -> UrgencyAttentionOrange
                                else -> ElectricIndigo
                            }
                        )
                    }

                    PriorityBadge(priority = task.priority)
                }

                // Interactive Status Pill
                Box {
                    StatusPill(
                        status = task.status,
                        onClick = { showStatusMenu = true }
                    )
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        TaskStatus.entries.forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(status = statusOption)
                                        if (statusOption == TaskStatus.LEFT) {
                                            Text(
                                                "• Abandoned",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Steel
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showStatusMenu = false
                                    onStatusChange(statusOption)
                                }
                            )
                        }
                    }
                }
            }

            // Task Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.status == TaskStatus.DONE || task.status == TaskStatus.LEFT) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tags row (if any)
            if (task.tags.isNotEmpty() || task.milestone != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.milestone != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Steel,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = task.milestone.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Steel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    task.tags.take(3).forEach { tag ->
                        TagChip(tag = tag)
                    }
                }
            }

            // Bottom metadata row: Assignee, Due date / Time Left, Pinned Contexts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignee
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemberAvatar(member = task.assignee, sizeDp = 24)
                    Text(
                        text = task.assignee?.name ?: "Unassigned",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Due date / Urgency Chip & Pinned Context pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.pinnedContextsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricIndigoSubtle
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned chat",
                                    tint = ElectricIndigo,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${task.pinnedContextsCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = ElectricIndigo
                                )
                            }
                        }
                    }

                    UrgencyChip(timeLeft = task.timeLeftResult)
                }
            }
        }
    }
}
