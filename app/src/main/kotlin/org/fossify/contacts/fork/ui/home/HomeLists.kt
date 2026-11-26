package org.fossify.contacts.fork.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fossify.contacts.R
import org.fossify.contacts.fork.data.ContactSearch
import org.fossify.contacts.fork.ui.common.ContactAvatar
import org.fossify.contacts.fork.ui.common.SectionLabel
import org.fossify.contacts.fork.ui.common.SegmentGap
import org.fossify.contacts.fork.ui.common.SegmentPosition
import org.fossify.contacts.fork.ui.common.SegmentedRow
import org.fossify.contacts.fork.ui.common.segmentPosition
import org.fossify.contacts.fork.ui.common.segmentShape
import kotlin.math.roundToInt

private val Horizontal = 16.dp

/** Shapes for favorites, picked per person so the grid feels hand-arranged but stays stable. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun favoriteShape(key: Int) = remember(key) {
    val shapes = listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Sunny,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Circle,
        MaterialShapes.Pentagon,
        MaterialShapes.SoftBurst,
        MaterialShapes.Cookie4Sided,
    )
    shapes[Math.floorMod(key, shapes.size)]
}.toShape()

@Composable
internal fun ContactsTab(
    state: HomeState,
    listState: LazyListState,
    actions: HomeActions,
    onToggleSelect: (Int) -> Unit,
) {
    if (state.contacts.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.PersonAdd,
            title = stringResource(org.fossify.commons.R.string.no_contacts_found),
            body = stringResource(R.string.fork_no_contacts_body),
            action = stringResource(R.string.fork_create_contact),
            onAction = actions::createContact,
        )
        return
    }

    // Item index of each section header, for the alphabet rail.
    val sectionStarts = remember(state.contacts, state.favorites) {
        val starts = linkedMapOf<String, Int>()
        var index = if (state.favorites.isNotEmpty()) 2 else 0
        state.contacts.groupBy { it.section }.forEach { (section, items) ->
            starts[section] = index
            index += 1 + items.size
        }
        starts
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = Horizontal, end = Horizontal + 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.favorites.isNotEmpty()) {
                item(key = "fav-label", contentType = "label") {
                    SectionLabel(
                        stringResource(org.fossify.commons.R.string.favorites),
                        Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
                    )
                }
                item(key = "fav-row", contentType = "carousel") {
                    FavoritesCarousel(state.favorites, state.selected, actions, onToggleSelect)
                }
            }

            state.contacts.groupBy { it.section }.forEach { (section, items) ->
                item(key = "s-$section", contentType = "label") {
                    SectionLabel(section, Modifier.padding(start = 4.dp, top = 18.dp, bottom = 6.dp))
                }
                itemsIndexed(items, key = { _, it -> it.id }, contentType = { _, _ -> "contact" }) { index, item ->
                    SwipeableContactRow(
                        item = item,
                        position = segmentPosition(index, items.size),
                        selected = item.id in state.selected,
                        selecting = state.selecting,
                        showPhoto = state.showThumbnails,
                        actions = actions,
                        onToggleSelect = onToggleSelect,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        if (sectionStarts.size > 4) {
            AlphabetRail(
                sections = sectionStarts.keys.toList(),
                onSection = { section ->
                    sectionStarts[section]?.let { index ->
                        listState.requestScrollToItem(index)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp),
            )
        }
    }
}

@Composable
private fun FavoritesCarousel(
    favorites: List<ContactItem>,
    selected: Set<Int>,
    actions: HomeActions,
    onToggleSelect: (Int) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(favorites, key = { it.id }) { item ->
            FavoriteTile(
                item = item,
                size = 72,
                selected = item.id in selected,
                selecting = selected.isNotEmpty(),
                actions = actions,
                onToggleSelect = onToggleSelect,
                modifier = Modifier.width(80.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteTile(
    item: ContactItem,
    size: Int,
    selected: Boolean,
    selecting: Boolean,
    actions: HomeActions,
    onToggleSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        if (selected) 0.86f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "favScale",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (selecting) onToggleSelect(item.id) else actions.openContact(item) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleSelect(item.id)
                },
            )
            .padding(vertical = 6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ContactAvatar(
                name = item.name,
                photoUri = item.photoUri,
                size = size.dp,
                shape = favoriteShape(item.id),
                colorKey = item.name,
                modifier = Modifier.scale(scale),
            )
            SelectedBadge(visible = selected, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun SelectedBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible, modifier = modifier, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(3.dp).size(16.dp))
        }
    }
}

/** A contact row: swipe right to call, left to text. Tap opens, long-press selects. */
@Composable
private fun SwipeableContactRow(
    item: ContactItem,
    position: SegmentPosition,
    selected: Boolean,
    selecting: Boolean,
    showPhoto: Boolean,
    actions: HomeActions,
    onToggleSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlight: String = "",
    secondLine: String = item.subtitle,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val swipe = rememberSwipeToDismissBoxState()
    val canSwipe = item.hasPhone && !selecting

    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = canSwipe,
        enableDismissFromEndToStart = canSwipe,
        gesturesEnabled = canSwipe,
        onDismiss = { direction ->
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> actions.call(item)
                SwipeToDismissBoxValue.EndToStart -> actions.message(item)
                else -> Unit
            }
            scope.launch { swipe.reset() }
        },
        backgroundContent = { SwipeBackground(swipe.dismissDirection, position) },
    ) {
        ContactRow(
            item = item,
            position = position,
            selected = selected,
            showPhoto = showPhoto,
            highlight = highlight,
            secondLine = secondLine,
            onClick = {
                if (selecting) onToggleSelect(item.id) else actions.openContact(item)
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleSelect(item.id)
            },
        )
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, position: SegmentPosition) {
    val (color, content, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Quad(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Outlined.Call,
            Alignment.CenterStart,
        )
        SwipeToDismissBoxValue.EndToStart -> Quad(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.AutoMirrored.Outlined.Message,
            Alignment.CenterEnd,
        )
        else -> Quad(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.onSurface,
            null,
            Alignment.Center,
        )
    }
    val animated by animateColorAsState(color, label = "swipeBg")
    Box(
        Modifier
            .fillMaxSize()
            .background(animated, segmentShape(position))
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content)
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
internal fun ContactRow(
    item: ContactItem,
    position: SegmentPosition,
    selected: Boolean,
    showPhoto: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    highlight: String = "",
    secondLine: String = item.subtitle,
) {
    SegmentedRow(
        position = position,
        selected = selected,
        onClick = onClick,
        onLongClick = onLongClick,
        minHeight = 68.dp,
    ) {
        Box {
            if (showPhoto || selected) {
                ContactAvatar(name = item.name, photoUri = if (showPhoto) item.photoUri else "", size = 44.dp)
            }
            SelectedBadge(visible = selected, modifier = Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = highlighted(item.name, highlight),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.starred) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = stringResource(org.fossify.commons.R.string.favorites),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(start = 6.dp).size(16.dp),
                    )
                }
            }
            if (secondLine.isNotEmpty()) {
                Text(
                    text = highlighted(secondLine, highlight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Bolds and tints the parts of [text] that match the words of [query], ignoring accents. */
@Composable
private fun highlighted(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val color = MaterialTheme.colorScheme.primary
    return remember(text, query, color) {
        val folded = ContactSearch.normalize(text)
        val ranges = mutableListOf<IntRange>()
        ContactSearch.normalize(query).split(' ').filter { it.isNotEmpty() }.forEach { word ->
            var start = folded.indexOf(word)
            while (start >= 0) {
                ranges += start until start + word.length
                start = folded.indexOf(word, start + word.length)
            }
        }
        // Folding keeps one char per base letter only when text is precomposed; bail out otherwise.
        if (folded.length != text.length) return@remember AnnotatedString(text)
        buildAnnotatedString {
            append(text)
            ranges.forEach { addStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold), it.first, it.last + 1) }
        }
    }
}

@Composable
internal fun SearchResults(
    state: HomeState,
    actions: HomeActions,
    onToggleSelect: (Int) -> Unit,
) {
    if (state.results.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.fork_no_results_title),
            body = stringResource(R.string.fork_no_results_body, state.query.trim()),
            action = stringResource(R.string.fork_create_contact),
            onAction = actions::createContact,
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = Horizontal, end = Horizontal, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(SegmentGap),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "count", contentType = "label") {
            SectionLabel(
                androidx.compose.ui.res.pluralStringResource(R.plurals.fork_results_count, state.results.size, state.results.size),
                Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
            )
        }
        itemsIndexed(state.results, key = { _, r -> r.item.id }, contentType = { _, _ -> "contact" }) { index, result ->
            SwipeableContactRow(
                item = result.item,
                position = segmentPosition(index, state.results.size),
                selected = result.item.id in state.selected,
                selecting = state.selecting,
                showPhoto = state.showThumbnails,
                actions = actions,
                onToggleSelect = onToggleSelect,
                highlight = state.query,
                secondLine = result.matchedPhone ?: result.item.subtitle,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
internal fun FavoritesTab(
    state: HomeState,
    gridState: LazyGridState,
    actions: HomeActions,
    onToggleSelect: (Int) -> Unit,
) {
    if (state.favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.StarOutline,
            title = stringResource(R.string.fork_no_favorites_title),
            body = stringResource(R.string.fork_no_favorites_body),
        )
        return
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 104.dp),
        contentPadding = PaddingValues(start = Horizontal, end = Horizontal, top = 8.dp, bottom = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "label") {
            SectionLabel(
                androidx.compose.ui.res.pluralStringResource(R.plurals.fork_favorites_count, state.favorites.size, state.favorites.size),
                Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        items(state.favorites, key = { it.id }) { item ->
            FavoriteTile(
                item = item,
                size = 88,
                selected = item.id in state.selected,
                selecting = state.selecting,
                actions = actions,
                onToggleSelect = onToggleSelect,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
internal fun GroupsTab(groups: List<GroupItem>, listState: LazyListState, actions: HomeActions) {
    if (groups.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Groups,
            title = stringResource(R.string.no_groups),
            body = stringResource(R.string.fork_no_groups_body),
            action = stringResource(R.string.create_group),
            onAction = actions::createGroup,
        )
        return
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = Horizontal, end = Horizontal, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(SegmentGap),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(groups, key = { _, g -> g.group.id ?: g.title.hashCode().toLong() }) { index, group ->
            SegmentedRow(
                position = segmentPosition(index, groups.size),
                onClick = { actions.openGroup(group) },
                minHeight = 76.dp,
                modifier = Modifier.animateItem(),
            ) {
                GroupFaces(group)
                Column(Modifier.weight(1f)) {
                    Text(group.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        androidx.compose.ui.res.pluralStringResource(R.plurals.fork_group_members, group.count, group.count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Up to three overlapping member avatars, or a group icon for an empty group. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupFaces(group: GroupItem) {
    val faces = group.members.take(3)
    if (faces.isEmpty()) {
        Surface(
            shape = MaterialShapes.Cookie6Sided.toShape(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Groups, contentDescription = null) }
        }
        return
    }
    Box(Modifier.width(48.dp + 14.dp * (faces.size - 1))) {
        faces.forEachIndexed { i, face ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.offset(x = 14.dp * i).size(48.dp),
            ) {
                Box(Modifier.padding(2.dp)) { ContactAvatar(face.name, face.photoUri, 44.dp) }
            }
        }
    }
}

/** A friendly empty state: an icon on a soft Expressive shape, a title, a line, and maybe a button. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(112.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp)) }
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (action != null && onAction != null) {
            FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 24.dp)) { Text(action) }
        }
    }
}

/**
 * The A–Z rail on the right edge. Drag along it to jump through the list: each new letter
 * ticks, and a bubble shows the letter under your finger.
 */
@Composable
private fun AlphabetRail(sections: List<String>, onSection: (String) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var active by remember { mutableStateOf<String?>(null) }
    var height by remember { mutableStateOf(1) }
    var activeY by remember { mutableStateOf(0f) }

    fun pick(y: Float) {
        val index = ((y / height) * sections.size).toInt().coerceIn(0, sections.lastIndex)
        val section = sections[index]
        activeY = y.coerceIn(0f, height.toFloat())
        if (section != active) {
            active = section
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            onSection(section)
        }
    }

    Box(modifier.fillMaxHeight(0.8f)) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .width(20.dp)
                .semantics { contentDescription = "A–Z" }
                .onSizeChanged { height = it.height }
                .pointerInput(sections) {
                    detectTapGestures(
                        onPress = { offset ->
                            pick(offset.y)
                            tryAwaitRelease()
                            active = null
                        },
                    )
                }
                .pointerInput(sections) {
                    detectVerticalDragGestures(
                        onDragStart = { pick(it.y) },
                        onDragEnd = { active = null },
                        onDragCancel = { active = null },
                    ) { change, _ -> pick(change.position.y) }
                },
        ) {
            sections.forEach { s ->
                Text(
                    s,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (s == active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (s == active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
        val letter = active
        if (letter != null) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 28.dp, bottomEnd = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset { IntOffset(x = -72.dp.roundToPx(), y = (activeY - 28.dp.toPx()).roundToInt()) }
                    .size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(letter, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
