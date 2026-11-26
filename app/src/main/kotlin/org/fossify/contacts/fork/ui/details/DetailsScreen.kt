package org.fossify.contacts.fork.ui.details

import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Event
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event as EventIcon
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.getPhoneNumberTypeText
import org.fossify.commons.helpers.SHOW_ADDRESSES_FIELD
import org.fossify.commons.helpers.SHOW_CONTACT_SOURCE_FIELD
import org.fossify.commons.helpers.SHOW_EMAILS_FIELD
import org.fossify.commons.helpers.SHOW_EVENTS_FIELD
import org.fossify.commons.helpers.SHOW_GROUPS_FIELD
import org.fossify.commons.helpers.SHOW_IMS_FIELD
import org.fossify.commons.helpers.SHOW_NICKNAME_FIELD
import org.fossify.commons.helpers.SHOW_NOTES_FIELD
import org.fossify.commons.helpers.SHOW_ORGANIZATION_FIELD
import org.fossify.commons.helpers.SHOW_PHONE_NUMBERS_FIELD
import org.fossify.commons.helpers.SHOW_RINGTONE_FIELD
import org.fossify.commons.helpers.SHOW_WEBSITES_FIELD
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.contacts.Address
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.Email
import org.fossify.commons.models.contacts.Group
import org.fossify.commons.models.contacts.IM
import org.fossify.commons.models.contacts.SocialAction
import org.fossify.contacts.R
import org.fossify.contacts.fork.data.ContactDates
import org.fossify.contacts.fork.ui.common.ContactAvatar
import org.fossify.contacts.fork.ui.common.LeadingIcon
import org.fossify.contacts.fork.ui.common.PhotoCache
import org.fossify.contacts.fork.ui.common.RowTexts
import org.fossify.contacts.fork.ui.common.SegmentedGroup
import org.fossify.contacts.fork.ui.common.SegmentedRow
import org.fossify.contacts.fork.ui.common.segmentPosition
import org.fossify.contacts.fork.ui.theme.avatarPaletteFor

/** What the details screen asks its activity to do. */
interface DetailsActions {
    fun back()
    fun call(number: String?)
    fun message(number: String?)
    fun email(address: String?)
    fun openAddress(address: Address)
    fun openWebsite(url: String)
    fun openSocial(action: SocialAction)
    fun openGroup(group: Group)
    fun edit()
    fun share()
    fun openWith()
    fun delete()
    fun toggleFavorite()
    fun pickRingtone()
    fun ringtoneTitle(): String?
    fun phoneType(number: PhoneNumber): String
    fun emailType(email: Email): String
    fun addressType(address: Address): String
    fun imType(im: IM): String
    fun copy(text: String)
    fun monthDayPattern(): String
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailsScreen(state: DetailsState, actions: DetailsActions) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) }
    val contact = state.contact
    val copiedText = stringResource(org.fossify.commons.R.string.value_copied_to_clipboard)

    val copy: (String) -> Unit = { text ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        actions.copy(text)
        // Android 13+ confirms copies itself; only older versions need our snackbar.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            scope.launch { snackbar.showSnackbar(copiedText) }
        }
    }

    val heroGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val titleAlpha by animateFloatAsState(if (heroGone) 1f else 0f, label = "titleAlpha")

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        contact?.let { displayName(it) } ?: "",
                        maxLines = 1,
                        modifier = Modifier.alpha(titleAlpha),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions::back) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.fork_back))
                    }
                },
                actions = {
                    if (contact != null) {
                        FavoriteButton(starred = state.starred) {
                            haptics.performHapticFeedback(if (state.starred) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                            actions.toggleFavorite()
                        }
                        IconButton(onClick = actions::edit) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(org.fossify.commons.R.string.edit))
                        }
                        OverflowMenu(actions = actions, onDelete = { confirmDelete = true })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            contact == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(org.fossify.commons.R.string.unknown_error_occurred))
            }
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 32.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                item(key = "hero") {
                    Hero(contact = contact, onPhotoClick = { if (contact.photoUri.isNotEmpty()) showPhoto = true })
                }
                item(key = "actions") {
                    QuickActions(contact = contact, actions = actions)
                }
                detailSections(contact, state, actions, copy)
            }
        }
    }

    if (confirmDelete && contact != null) {
        val linked = state.linked.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.fork_delete_contact_title, displayName(contact))) },
            text = {
                Text(
                    if (linked > 1) {
                        stringResource(R.string.fork_delete_contact_linked_body, linked)
                    } else {
                        stringResource(R.string.fork_delete_contacts_body)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    actions.delete()
                }) { Text(stringResource(org.fossify.commons.R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }

    if (showPhoto && contact != null) {
        PhotoDialog(contact = contact, onDismiss = { showPhoto = false })
    }
}

internal fun displayName(contact: Contact): String =
    contact.getNameToDisplay().ifBlank {
        contact.getFullCompany().ifBlank { contact.emails.firstOrNull()?.value ?: contact.phoneNumbers.firstOrNull()?.value ?: "" }
    }

@Composable
private fun FavoriteButton(starred: Boolean, onClick: () -> Unit) {
    val pop = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    IconButton(onClick = onClick) {
        AnimatedContent(
            targetState = starred,
            transitionSpec = { (scaleIn(pop, initialScale = 0.4f) + fadeIn()) togetherWith fadeOut() },
            label = "star",
        ) { on ->
            Icon(
                if (on) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                contentDescription = stringResource(
                    if (on) org.fossify.commons.R.string.remove_from_favorites else org.fossify.commons.R.string.add_to_favorites,
                ),
                tint = if (on) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OverflowMenu(actions: DetailsActions, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.fork_more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.large) {
            DropdownMenuItem(
                text = { Text(stringResource(org.fossify.commons.R.string.share)) },
                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                onClick = { open = false; actions.share() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(org.fossify.commons.R.string.open_with)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                onClick = { open = false; actions.openWith() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(org.fossify.commons.R.string.delete)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

/** Big shaped photo on a soft wash of the contact's color, then the name and what they do. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Hero(contact: Contact, onPhotoClick: () -> Unit) {
    val name = displayName(contact)
    val palette = avatarPaletteFor(name)
    val subtitle = listOf(contact.organization.jobPosition, contact.organization.company)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .takeIf { it != name }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(Brush.verticalGradient(listOf(palette.container.copy(alpha = 0.45f), Color.Transparent)))
            .padding(top = 24.dp, bottom = 12.dp),
    ) {
        ContactAvatar(
            name = name,
            photoUri = contact.photoUri.ifEmpty { contact.thumbnailUri },
            size = 152.dp,
            shape = MaterialShapes.Cookie12Sided.toShape(),
            modifier = Modifier.clickable(enabled = contact.photoUri.isNotEmpty(), onClick = onPhotoClick),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp),
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

/** Call, Text and Email as big tonal buttons that squish into a squircle when pressed. */
@Composable
private fun QuickActions(contact: Contact, actions: DetailsActions) {
    val hasPhone = contact.phoneNumbers.isNotEmpty()
    val hasEmail = contact.emails.isNotEmpty()
    if (!hasPhone && !hasEmail) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        if (hasPhone) {
            QuickAction(Icons.Outlined.Call, stringResource(org.fossify.commons.R.string.call), primary = true) { actions.call(null) }
            QuickAction(Icons.AutoMirrored.Outlined.Message, stringResource(R.string.fork_text)) { actions.message(null) }
        }
        if (hasEmail) {
            QuickAction(Icons.Outlined.Mail, stringResource(org.fossify.commons.R.string.email)) { actions.email(null) }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, primary: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by androidx.compose.animation.core.animateDpAsState(
        if (pressed) 18.dp else 32.dp,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "qaCorner",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 88.dp)) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            shape = RoundedCornerShape(corner),
            color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(width = 88.dp, height = 64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    }
}

private fun LazyListScope.detailSections(
    contact: Contact,
    state: DetailsState,
    actions: DetailsActions,
    copy: (String) -> Unit,
) {
    val show = state.visibleFields
    fun on(flag: Int) = show and flag != 0

    if (on(SHOW_PHONE_NUMBERS_FIELD) || on(SHOW_EMAILS_FIELD) || on(SHOW_ADDRESSES_FIELD) || on(SHOW_IMS_FIELD) || on(SHOW_WEBSITES_FIELD)) {
        val rows = buildList<DetailRow> {
            if (on(SHOW_PHONE_NUMBERS_FIELD)) contact.phoneNumbers.forEach { n ->
                add(
                    DetailRow(
                        icon = Icons.Outlined.Call,
                        value = n.value,
                        label = { actions.phoneType(n) + if (n.isPrimary) " · ★" else "" },
                        onClick = { actions.call(n.value) },
                        trailing = TrailingAction(Icons.AutoMirrored.Outlined.Message, R.string.fork_text) { actions.message(n.value) },
                    ),
                )
            }
            if (on(SHOW_EMAILS_FIELD)) contact.emails.forEach { e ->
                add(DetailRow(Icons.Outlined.Mail, e.value, { actions.emailType(e) }, onClick = { actions.email(e.value) }))
            }
            if (on(SHOW_ADDRESSES_FIELD)) contact.addresses.forEach { a ->
                add(DetailRow(Icons.Outlined.Place, a.value.trim(), { actions.addressType(a) }, onClick = { actions.openAddress(a) }))
            }
            if (on(SHOW_IMS_FIELD)) contact.IMs.forEach { im ->
                add(DetailRow(Icons.Outlined.Chat, im.value, { actions.imType(im) }))
            }
            if (on(SHOW_WEBSITES_FIELD)) contact.websites.forEach { w ->
                add(DetailRow(Icons.Outlined.Language, w, { null }, onClick = { actions.openWebsite(w) }))
            }
        }
        group("contact", R.string.fork_contact_info, rows, copy)
    }

    if (state.social.isNotEmpty()) {
        val rows = state.social.map { action ->
            DetailRow(Icons.Outlined.Apps, action.label, { null }, onClick = { actions.openSocial(action) }, copyable = false)
        }
        group("social", R.string.fork_connected_apps, rows, copy)
    }

    val about = buildList<DetailRow> {
        if (on(SHOW_NICKNAME_FIELD) && contact.nickname.isNotBlank()) {
            add(DetailRow(Icons.Outlined.Badge, contact.nickname, { null }, labelRes = R.string.nickname))
        }
        if (on(SHOW_ORGANIZATION_FIELD) && contact.organization.isNotEmpty()) {
            val org = contact.organization
            add(DetailRow(Icons.Outlined.Work, org.company.ifBlank { org.jobPosition }, { org.jobPosition.takeIf { org.company.isNotBlank() && it.isNotBlank() } }))
        }
        if (on(SHOW_EVENTS_FIELD)) contact.events.forEach { e ->
            val info = ContactDates.parse(e.value, monthDayPattern = actions.monthDayPattern())
            add(
                DetailRow(
                    icon = when (e.type) {
                        Event.TYPE_BIRTHDAY -> Icons.Outlined.Cake
                        Event.TYPE_ANNIVERSARY -> Icons.Outlined.Celebration
                        else -> Icons.Outlined.EventIcon
                    },
                    value = info?.formatted ?: e.value,
                    label = { null },
                    event = EventLine(e.type, info),
                ),
            )
        }
        if (on(SHOW_NOTES_FIELD) && contact.notes.isNotBlank()) {
            add(DetailRow(Icons.Outlined.StickyNote2, contact.notes.trim(), { null }, labelRes = org.fossify.commons.R.string.notes, multiline = true))
        }
        if (on(SHOW_RINGTONE_FIELD)) {
            actions.ringtoneTitle()?.let { title ->
                add(DetailRow(Icons.Outlined.MusicNote, title, { null }, labelRes = R.string.fork_ringtone, onClick = actions::pickRingtone, copyable = false))
            }
        }
    }
    if (about.isNotEmpty()) group("about", R.string.fork_about_contact, about, copy)

    if (on(SHOW_GROUPS_FIELD) && contact.groups.isNotEmpty()) {
        item(key = "groups") {
            SegmentedGroup(title = stringResource(R.string.groups)) {
                SegmentedRow(position = segmentPosition(0, 1), minHeight = 56.dp) {
                    LeadingIcon(Icons.Outlined.Groups)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        contact.groups.forEach { g ->
                            AssistChip(
                                onClick = { actions.openGroup(g) },
                                label = { Text(g.title) },
                                shape = RoundedCornerShape(12.dp),
                                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                border = null,
                            )
                        }
                    }
                }
            }
        }
    }

    if (on(SHOW_CONTACT_SOURCE_FIELD) && state.sources.isNotEmpty()) {
        item(key = "sources") {
            SegmentedGroup(title = stringResource(R.string.fork_saved_in)) {
                state.sources.forEachIndexed { i, source ->
                    SegmentedRow(position = segmentPosition(i, state.sources.size), minHeight = 56.dp) {
                        LeadingIcon(Icons.Outlined.AccountCircle)
                        RowTexts(source, null)
                    }
                }
            }
        }
    }
}

private class TrailingAction(val icon: ImageVector, val labelRes: Int, val onClick: () -> Unit)

private class EventLine(val type: Int, val info: ContactDates.Info?)

private class DetailRow(
    val icon: ImageVector,
    val value: String,
    val label: () -> String?,
    val onClick: (() -> Unit)? = null,
    val trailing: TrailingAction? = null,
    val labelRes: Int? = null,
    val copyable: Boolean = true,
    val multiline: Boolean = false,
    val event: EventLine? = null,
)

private fun LazyListScope.group(key: String, titleRes: Int, rows: List<DetailRow>, copy: (String) -> Unit) {
    if (rows.isEmpty()) return
    item(key = key) {
        SegmentedGroup(title = stringResource(titleRes)) {
            rows.forEachIndexed { index, row ->
                DetailRowView(row, segmentPosition(index, rows.size), copy)
            }
        }
    }
}

@Composable
private fun DetailRowView(row: DetailRow, position: org.fossify.contacts.fork.ui.common.SegmentPosition, copy: (String) -> Unit) {
    val supporting = row.event?.let { eventSupporting(it) } ?: row.labelRes?.let { stringResource(it) } ?: row.label()
    SegmentedRow(
        position = position,
        onClick = row.onClick,
        onLongClick = if (row.copyable) ({ copy(row.value) }) else null,
        minHeight = 68.dp,
    ) {
        LeadingIcon(row.icon)
        RowTexts(row.value, supporting)
        row.trailing?.let { t ->
            FilledTonalIconButton(
                onClick = t.onClick,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(t.icon, contentDescription = stringResource(t.labelRes))
            }
        }
    }
}

@Composable
private fun eventSupporting(line: EventLine): String {
    val kind = stringResource(
        when (line.type) {
            Event.TYPE_BIRTHDAY -> org.fossify.commons.R.string.birthday
            Event.TYPE_ANNIVERSARY -> org.fossify.commons.R.string.anniversary
            else -> org.fossify.commons.R.string.other
        },
    )
    val info = line.info ?: return kind
    val whenText = when (info.daysUntil) {
        0 -> stringResource(R.string.fork_today)
        1 -> stringResource(R.string.fork_tomorrow)
        else -> pluralStringResource(R.plurals.fork_in_days, info.daysUntil, info.daysUntil)
    }
    val age = info.turning?.let {
        if (line.type == Event.TYPE_BIRTHDAY) {
            pluralStringResource(R.plurals.fork_turns, it, it)
        } else {
            pluralStringResource(R.plurals.fork_years, it, it)
        }
    }
    return listOfNotNull(kind, whenText, age).joinToString(" · ")
}

@Composable
private fun PhotoDialog(contact: Contact, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val image by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, contact.photoUri) {
        value = PhotoCache.load(context, contact.photoUri, 1024)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            image?.let {
                val appear by animateFloatAsState(1f, label = "photoIn")
                Image(
                    bitmap = it,
                    contentDescription = displayName(contact),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .graphicsLayer { alpha = appear }
                        .scale(appear),
                )
            }
        }
    }
}

