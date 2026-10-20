package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun StatusPill(
    status: TaskStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, textColor, dotColor, label) = when (status) {
        TaskStatus.NOT_STARTED -> Quadruple(
            StatusNotStartedBg,
            StatusNotStarted,
            StatusNotStarted,
            "Not started"
        )
        TaskStatus.WORKING -> Quadruple(
            StatusWorkingBg,
            StatusWorking,
            StatusWorking,
            "Working"
        )
        TaskStatus.PAUSED -> Quadruple(
            StatusPausedBg,
            StatusPaused,
            StatusPaused,
            "Paused"
        )
        TaskStatus.LEFT -> Quadruple(
            StatusLeftBg,
            StatusLeft,
            StatusLeft,
            "Left (Abandoned)"
        )
        TaskStatus.DONE -> Quadruple(
            StatusDoneBg,
            StatusDone,
            StatusDone,
            "Done"
        )
    }

    val clickableModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier.testTag("status_pill_${status.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = textColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (priority) {
        TaskPriority.HIGH -> Triple(UrgencyOverdueRedBg, UrgencyOverdueRed, Icons.Default.PriorityHigh)
        TaskPriority.MEDIUM -> Triple(UrgencyAttentionOrangeBg, UrgencyAttentionOrange, Icons.Default.Flag)
        TaskPriority.LOW -> Triple(SurfaceSubtle, Steel, Icons.Outlined.Flag)
    }

    Surface(
        modifier = modifier.testTag("priority_badge_${priority.name.lowercase()}"),
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = priority.displayName,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = priority.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

@Composable
fun UrgencyChip(
    timeLeft: TimeLeftResult,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (timeLeft.urgencyLevel) {
        UrgencyLevel.OVERDUE -> UrgencyOverdueRedBg to UrgencyOverdueRed
        UrgencyLevel.URGENT -> UrgencyUrgentOrangeRedBg to UrgencyUrgentOrangeRed
        UrgencyLevel.ATTENTION_NEEDED -> UrgencyAttentionOrangeBg to UrgencyAttentionOrange
        UrgencyLevel.ON_TRACK -> UrgencyOnTrackGreenBg to UrgencyOnTrackGreen
        UrgencyLevel.NORMAL_TIME_LEFT -> SurfaceSubtle to Steel
        UrgencyLevel.NO_DUE_DATE -> SurfaceSubtle to Steel
    }

    Surface(
        modifier = modifier.testTag("urgency_chip"),
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Urgency",
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = timeLeft.formattedTime,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MemberAvatar(
    member: MemberItem?,
    sizeDp: Int = 32,
    showPresence: Boolean = true,
    modifier: Modifier = Modifier
) {
    val initials = member?.avatarInitials ?: "—"
    val color = try {
        Color(android.graphics.Color.parseColor(member?.avatarColorHex ?: "#767883"))
    } catch (_: Exception) {
        ElectricIndigo
    }

    val presenceColor = when (member?.presenceStatus) {
        "In Deep Work" -> UrgencyAttentionOrange
        "Away" -> Steel
        else -> UrgencyOnTrackGreen
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .testTag("member_avatar_${member?.name ?: "unassigned"}"),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp * 0.42).sp,
                textAlign = TextAlign.Center
            )
        }

        if (showPresence && member != null) {
            Box(
                modifier = Modifier
                    .size((sizeDp * 0.3).dp)
                    .clip(CircleShape)
                    .background(presenceColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun TagChip(
    tag: TaskCategory,
    modifier: Modifier = Modifier
) {
    val color = try {
        Color(android.graphics.Color.parseColor(tag.colorHex))
    } catch (_: Exception) {
        ElectricIndigo
    }

    Surface(
        modifier = modifier.testTag("tag_chip_${tag.name.lowercase()}"),
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = tag.displayName,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = color
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_state_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.testTag("empty_state_action_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoidBlack,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun MetricKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("metric_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
