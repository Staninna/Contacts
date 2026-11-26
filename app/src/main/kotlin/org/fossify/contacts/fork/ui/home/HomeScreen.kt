package org.fossify.contacts.fork.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.fossify.contacts.R

enum class HomeTab { Contacts, Favorites, Groups }

/** Everything the home screen asks its activity to do. */
interface HomeActions {
    fun openContact(item: ContactItem)
    fun call(item: ContactItem)
    fun message(item: ContactItem)
    fun createContact()
    fun createGroup()
    fun openGroup(group: GroupItem)
    fun share(items: List<ContactItem>)
    fun sort()
    fun filter()
    fun settings()
    fun about()
    fun requestPermission()
    fun tabChanged(tab: HomeTab)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: HomeState,
    tabs: List<HomeTab>,
    initialTab: HomeTab,
    actions: HomeActions,
    onQueryChange: (String) -> Unit,
    onToggleSelect: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSetFavorite: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var confirmDelete by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val groupsState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current

    val fabExpanded by remember {
        derivedStateOf {
            when (tab) {
                HomeTab.Contacts -> listState.firstVisibleItemIndex == 0
                HomeTab.Favorites -> gridState.firstVisibleItemIndex == 0
                HomeTab.Groups -> groupsState.firstVisibleItemIndex == 0
            }
        }
    }

    BackHandler(enabled = state.selecting) { onClearSelection() }
    BackHandler(enabled = !state.selecting && state.searching) {
        onQueryChange("")
        focus.clearFocus()
    }

    val selectedItems = remember(state.selected, state.contacts) { state.contacts.filter { it.id in state.selected } }

    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AnimatedContent(
                targetState = state.selecting,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "topBar",
            ) { selecting ->
                if (selecting) {
                    SelectionBar(
                        count = state.selected.size,
                        onClose = onClearSelection,
                        onSelectAll = onSelectAll,
                    )
                } else {
                    SearchTopBar(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        tab = tab,
                        actions = actions,
                    )
                }
            }
        },
        bottomBar = {
            if (tabs.size > 1 && !state.selecting && !state.searching) {
                ShortNavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    tabs.forEach { t ->
                        val selected = t == tab
                        ShortNavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (t != tab) {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    tab = t
                                    actions.tabChanged(t)
                                }
                            },
                            icon = { Icon(if (selected) t.selectedIcon else t.icon, contentDescription = null) },
                            label = { Text(stringResource(t.label)) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.selecting && !state.searching && state.hasPermission,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                val isGroups = tab == HomeTab.Groups
                ExtendedFloatingActionButton(
                    onClick = { if (isGroups) actions.createGroup() else actions.createContact() },
                    expanded = fabExpanded,
                    icon = { Icon(if (isGroups) Icons.Outlined.GroupAdd else Icons.Outlined.PersonAdd, contentDescription = null) },
                    text = {
                        Text(
                            if (isGroups) {
                                stringResource(R.string.create_group)
                            } else {
                                stringResource(R.string.fork_create_contact)
                            },
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                !state.hasPermission -> PermissionState(onGrant = actions::requestPermission)
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                state.searching -> SearchResults(
                    state = state,
                    actions = actions,
                    onToggleSelect = onToggleSelect,
                )
                else -> AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(effects) + slideInVertically(spatial) { it / 24 }) togetherWith fadeOut(fastEffects)
                    },
                    label = "tab",
                ) { t ->
                    when (t) {
                        HomeTab.Contacts -> ContactsTab(
                            state = state,
                            listState = listState,
                            actions = actions,
                            onToggleSelect = onToggleSelect,
                        )
                        HomeTab.Favorites -> FavoritesTab(
                            state = state,
                            gridState = gridState,
                            actions = actions,
                            onToggleSelect = onToggleSelect,
                        )
                        HomeTab.Groups -> GroupsTab(
                            groups = state.groups,
                            listState = groupsState,
                            actions = actions,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.selecting,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp),
                enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                val allFavorite = selectedItems.isNotEmpty() && selectedItems.all { it.starred }
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                ) {
                    ToolbarAction(
                        icon = if (allFavorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                        label = stringResource(
                            if (allFavorite) org.fossify.commons.R.string.remove_from_favorites else org.fossify.commons.R.string.add_to_favorites,
                        ),
                        onClick = { onSetFavorite(!allFavorite) },
                    )
                    ToolbarAction(
                        icon = Icons.Outlined.Share,
                        label = stringResource(org.fossify.commons.R.string.share),
                        onClick = { actions.share(selectedItems) },
                    )
                    ToolbarAction(
                        icon = Icons.Outlined.Delete,
                        label = stringResource(org.fossify.commons.R.string.delete),
                        onClick = { confirmDelete = true },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        val count = state.selected.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(pluralStringResource(R.plurals.fork_delete_contacts_title, count, count)) },
            text = { Text(stringResource(R.string.fork_delete_contacts_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onDelete()
                }) { Text(stringResource(org.fossify.commons.R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

private val HomeTab.label
    get() = when (this) {
        HomeTab.Contacts -> org.fossify.commons.R.string.contacts_tab
        HomeTab.Favorites -> org.fossify.commons.R.string.favorites_tab
        HomeTab.Groups -> org.fossify.commons.R.string.groups_tab
    }

private val HomeTab.icon
    get() = when (this) {
        HomeTab.Contacts -> Icons.Outlined.Person
        HomeTab.Favorites -> Icons.Outlined.StarOutline
        HomeTab.Groups -> Icons.Outlined.Groups
    }

private val HomeTab.selectedIcon
    get() = when (this) {
        HomeTab.Contacts -> Icons.Rounded.Person
        HomeTab.Favorites -> Icons.Rounded.Star
        HomeTab.Groups -> Icons.Rounded.Groups
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
    }
}

/** A pill search field with the overflow menu tucked inside it, like the system apps. */
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    tab: HomeTab,
    actions: HomeActions,
) {
    val focusRequester = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.fork_search_contacts),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onQueryChange("")
                        focus.clearFocus()
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.fork_clear_search))
                    }
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.fork_more_options))
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            shape = MaterialTheme.shapes.large,
                        ) {
                            if (tab != HomeTab.Groups) {
                                MenuItem(Icons.AutoMirrored.Outlined.Sort, stringResource(org.fossify.commons.R.string.sort_by)) {
                                    menuOpen = false; actions.sort()
                                }
                                MenuItem(Icons.Outlined.FilterList, stringResource(org.fossify.commons.R.string.filter)) {
                                    menuOpen = false; actions.filter()
                                }
                            }
                            MenuItem(Icons.Outlined.Settings, stringResource(org.fossify.commons.R.string.settings)) {
                                menuOpen = false; actions.settings()
                            }
                            MenuItem(Icons.Outlined.Info, stringResource(org.fossify.commons.R.string.about)) {
                                menuOpen = false; actions.about()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onSelectAll: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.fork_clear_selection)) }
        AnimatedContent(targetState = count, label = "count", modifier = Modifier.weight(1f)) { c ->
            Text(
                text = pluralStringResource(R.plurals.fork_selected_count, c, c),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        IconButton(onClick = onSelectAll) {
            Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(org.fossify.commons.R.string.select_all))
        }
    }
}

@Composable
private fun PermissionState(onGrant: () -> Unit) {
    EmptyState(
        icon = Icons.Outlined.Person,
        title = stringResource(org.fossify.commons.R.string.no_contacts_permission),
        body = stringResource(R.string.fork_permission_body),
        action = stringResource(org.fossify.commons.R.string.grant_permission),
        onAction = onGrant,
    )
}

internal val ListBottomPadding = PaddingValues(bottom = 112.dp)

