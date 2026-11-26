package org.fossify.contacts.fork.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState

/** Where a row sits in a segmented group: decides which corners are round. */
enum class SegmentPosition { Single, First, Middle, Last }

fun segmentPosition(index: Int, count: Int) = when {
    count == 1 -> SegmentPosition.Single
    index == 0 -> SegmentPosition.First
    index == count - 1 -> SegmentPosition.Last
    else -> SegmentPosition.Middle
}

val SegmentOuterRadius = 24.dp
val SegmentInnerRadius = 6.dp
val SegmentGap = 2.dp

fun segmentShape(position: SegmentPosition, outer: Dp = SegmentOuterRadius, inner: Dp = SegmentInnerRadius) = when (position) {
    SegmentPosition.Single -> RoundedCornerShape(outer)
    SegmentPosition.First -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
    SegmentPosition.Middle -> RoundedCornerShape(inner)
    SegmentPosition.Last -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
}

/**
 * One row of an Expressive segmented list: rows of a group sit 2dp apart, the group is
 * round on the outside and nearly square inside. Pressing a row rounds its inner corners,
 * the same shape-morph feedback the system settings use.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SegmentedRow(
    position: SegmentPosition,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    minHeight: Dp = 64.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val inner by animateDpAsState(
        targetValue = if (pressed || selected) 18.dp else SegmentInnerRadius,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "segmentInner",
    )
    val color = if (selected) MaterialTheme.colorScheme.secondaryContainer else containerColor
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = androidx.compose.material3.ripple(),
            role = Role.Button,
            onClickLabel = onClickLabel,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick,
        )
    } else {
        Modifier
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = segmentShape(position, inner = inner),
        color = color,
    ) {
        Row(
            modifier = clickModifier
                .heightIn(min = minHeight)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** A labelled group of segmented rows. */
@Composable
fun SegmentedGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            SectionLabel(title, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(SegmentGap)) {
            content()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Two lines of text that fill a row: a value and, below it, what it is. */
@Composable
fun RowScope.RowTexts(headline: String, supporting: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.weight(1f)) {
        Text(
            text = headline,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!supporting.isNullOrEmpty()) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** An icon in a tinted circle, as the leading element of a detail row. */
@Composable
fun LeadingIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.padding(0.dp))
        }
    }
}
