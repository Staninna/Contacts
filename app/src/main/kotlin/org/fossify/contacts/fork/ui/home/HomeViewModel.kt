package org.fossify.contacts.fork.ui.home

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.Group
import org.fossify.contacts.fork.data.ContactSearch
import org.fossify.contacts.fork.data.ForkContactsRepository

/** One row of the contact list, with everything the row needs precomputed. */
@Immutable
data class ContactItem(
    val id: Int,
    val name: String,
    val subtitle: String,
    val section: String,
    val photoUri: String,
    val starred: Boolean,
    val hasPhone: Boolean,
    val contact: Contact,
)

@Immutable
data class GroupItem(val group: Group, val title: String, val count: Int, val members: List<ContactItem>)

@Immutable
data class SearchResult(val item: ContactItem, val matchedPhone: String?)

@Immutable
data class HomeState(
    val loading: Boolean = true,
    val hasPermission: Boolean = true,
    val contacts: List<ContactItem> = emptyList(),
    val favorites: List<ContactItem> = emptyList(),
    val groups: List<GroupItem> = emptyList(),
    val sections: List<String> = emptyList(),
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val selected: Set<Int> = emptySet(),
    val showThumbnails: Boolean = true,
) {
    val selecting get() = selected.isNotEmpty()
    val searching get() = query.isNotBlank()
}

@OptIn(FlowPreview::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ForkContactsRepository(app)
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var observing = false

    /** Loads (or reloads) everything. Safe to call often: a running load is replaced. */
    fun refresh() {
        if (!repo.hasReadPermission()) {
            _state.update { it.copy(loading = false, hasPermission = false) }
            return
        }
        startObserving()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val contacts = repo.loadContacts()
            val groups = repo.loadGroups()
            val config = getApplication<Application>().baseConfig
            val items = contacts.map { it.toItem(config.showPhoneNumbers) }
            val byId = items.associateBy { it.id }
            val groupItems = groups.map { group ->
                val members = items.filter { item -> item.contact.groups.any { it.id == group.id } }
                GroupItem(group, group.title, members.size, members)
            }.sortedBy { it.title.lowercase() }
            _state.update { s ->
                s.copy(
                    loading = false,
                    hasPermission = true,
                    contacts = items,
                    favorites = items.filter { it.starred },
                    groups = groupItems,
                    sections = items.map { it.section }.distinct(),
                    selected = s.selected.filterTo(mutableSetOf()) { it in byId },
                    showThumbnails = config.showContactThumbnails,
                    results = search(s.query, items),
                )
            }
        }
    }

    private fun startObserving() {
        if (observing) return
        observing = true
        viewModelScope.launch {
            repo.changes().debounce(400).collect { refresh() }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, results = search(query, it.contacts)) }
    }

    fun toggleSelection(id: Int) {
        _state.update { s ->
            val next = if (id in s.selected) s.selected - id else s.selected + id
            s.copy(selected = next)
        }
    }

    fun selectAll() {
        _state.update { s -> s.copy(selected = s.contacts.mapTo(mutableSetOf()) { it.id }) }
    }

    fun clearSelection() {
        _state.update { it.copy(selected = emptySet()) }
    }

    fun selectedContacts(): List<Contact> {
        val s = _state.value
        return s.contacts.filter { it.id in s.selected }.map { it.contact }
    }

    fun setFavorite(contacts: List<Contact>, favorite: Boolean) {
        viewModelScope.launch {
            repo.setFavorite(contacts, favorite)
            clearSelection()
            refresh()
        }
    }

    fun delete(contacts: List<Contact>) {
        viewModelScope.launch {
            repo.delete(contacts)
            clearSelection()
            refresh()
        }
    }

    private fun search(query: String, items: List<ContactItem>): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val byId = items.associateBy { it.id }
        val candidates = items.map { item ->
            val c = item.contact
            ContactSearch.Candidate(
                key = item.id,
                name = item.name,
                extra = listOfNotNull(
                    c.nickname,
                    c.organization.company,
                    c.organization.jobPosition,
                ) + c.emails.map { it.value },
                phoneNumbers = c.phoneNumbers.map { it.value },
            )
        }
        return ContactSearch.search(query, candidates).mapNotNull { m ->
            byId[m.key]?.let { SearchResult(it, m.matchedPhone) }
        }
    }
}

internal fun Contact.toItem(showPhoneNumbers: Boolean): ContactItem {
    val display = getNameToDisplay().ifBlank { emails.firstOrNull()?.value ?: phoneNumbers.firstOrNull()?.value ?: "" }
    val primaryPhone = (phoneNumbers.firstOrNull { it.isPrimary } ?: phoneNumbers.firstOrNull())?.value
    val company = listOf(organization.jobPosition, organization.company).filter { it.isNotBlank() }.joinToString(" · ")
    val subtitle = when {
        showPhoneNumbers && primaryPhone != null -> primaryPhone
        company.isNotEmpty() && company != display -> company
        else -> ""
    }
    return ContactItem(
        id = id,
        name = display,
        subtitle = subtitle,
        section = ContactSearch.sectionOf(getBubbleText().ifBlank { display }),
        photoUri = thumbnailUri.ifEmpty { photoUri },
        starred = starred == 1,
        hasPhone = phoneNumbers.isNotEmpty(),
        contact = this,
    )
}
