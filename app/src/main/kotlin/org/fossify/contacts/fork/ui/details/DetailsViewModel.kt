package org.fossify.contacts.fork.ui.details

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getSocialActions
import org.fossify.commons.extensions.getVisibleContactSources
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.SMT_PRIVATE
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.ContactSource
import org.fossify.commons.models.contacts.SocialAction
import org.fossify.contacts.R
import org.fossify.contacts.fork.data.ContactSearch
import org.fossify.contacts.fork.data.ForkContactsRepository
import kotlin.coroutines.resume

@Immutable
data class DetailsState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val contact: Contact? = null,
    /** The contact itself plus any duplicates merged into this view. */
    val linked: List<Contact> = emptyList(),
    val sources: List<String> = emptyList(),
    val social: List<SocialAction> = emptyList(),
    val starred: Boolean = false,
    val visibleFields: Int = 0,
)

/** Where the screen's contact comes from: an id from inside the app, or a resolver for outside links. */
sealed interface ContactRef {
    data class ById(val id: Int, val isPrivate: Boolean) : ContactRef
    data class Resolved(val resolve: () -> Contact?) : ContactRef
}

@OptIn(FlowPreview::class)
class DetailsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ForkContactsRepository(app)
    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()
    private var ref: ContactRef? = null
    private var observing = false

    fun load(ref: ContactRef) {
        this.ref = ref
        reload()
        if (!observing) {
            observing = true
            viewModelScope.launch { repo.changes().debounce(400).collect { reload() } }
        }
    }

    fun reload() {
        val current = ref ?: return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val base = withContext(Dispatchers.IO) {
                when (current) {
                    is ContactRef.ById -> ContactsHelper(app).getContactWithId(current.id, current.isPrivate)
                    is ContactRef.Resolved -> current.resolve()
                }
            }
            if (base == null) {
                _state.update { it.copy(loading = false, missing = true) }
                return@launch
            }
            // Pin later reloads (after an edit) to the id, even when we came in by lookup key.
            if (current is ContactRef.Resolved) this@DetailsViewModel.ref = ContactRef.ById(base.id, base.isPrivate())

            val config = app.baseConfig
            val duplicates = if (config.mergeDuplicateContacts) duplicatesOf(base) else emptyList()
            val merged = merge(base, duplicates)
            val linked = listOf(base) + duplicates
            val sources = sourceNames(linked)
            val social = withContext(Dispatchers.IO) {
                linked.flatMap { c -> runCatching { app.getSocialActions(c.id) }.getOrDefault(arrayListOf()) }
                    .distinctBy { it.packageName + it.label }
            }
            _state.update {
                DetailsState(
                    loading = false,
                    contact = merged,
                    linked = linked,
                    sources = sources,
                    social = social,
                    starred = linked.any { c -> c.starred == 1 },
                    visibleFields = config.showContactFields,
                )
            }
        }
    }

    fun toggleFavorite() {
        val s = _state.value
        val next = !s.starred
        _state.update { it.copy(starred = next) }
        viewModelScope.launch { repo.setFavorite(s.linked, next) }
    }

    suspend fun delete(deleteClones: Boolean): Boolean {
        val contact = _state.value.linked.firstOrNull() ?: return false
        return repo.deleteOne(contact, deleteClones)
    }

    private suspend fun duplicatesOf(contact: Contact): List<Contact> {
        val app = getApplication<Application>()
        val duplicates: List<Contact> = suspendCancellableCoroutine { cont ->
            ContactsHelper(app).getDuplicatesOfContact(contact, false) { if (cont.isActive) cont.resume(it) }
        }
        return withContext(Dispatchers.IO) {
            val visible = app.getVisibleContactSources()
            duplicates.filter { it.source in visible }
                .mapNotNull { ContactsHelper(app).getContactWithId(it.id, it.isPrivate()) }
        }
    }

    private suspend fun sourceNames(contacts: List<Contact>): List<String> {
        val app = getApplication<Application>()
        val all: List<ContactSource> = suspendCancellableCoroutine { cont ->
            ContactsHelper(app).getContactSources { if (cont.isActive) cont.resume(it) }
        }
        return contacts.map { c ->
            when (c.source) {
                SMT_PRIVATE -> app.getString(org.fossify.commons.R.string.phone_storage_hidden)
                else -> all.firstOrNull { it.name == c.source }?.publicName?.takeIf { it.isNotEmpty() }
                    ?: c.source.ifEmpty { app.getString(R.string.phone_storage) }
            }
        }.distinct()
    }

    companion object {
        /** One contact with the details of its duplicates folded in, without repeats. */
        fun merge(base: Contact, others: List<Contact>): Contact {
            if (others.isEmpty()) return base
            val all = listOf(base) + others
            return base.copy(
                phoneNumbers = ArrayList(all.flatMap { it.phoneNumbers }.distinctBy { ContactSearch.digitsOf(it.normalizedNumber.ifEmpty { it.value }).takeLast(9) }),
                emails = ArrayList(all.flatMap { it.emails }.distinctBy { it.value.trim().lowercase() }),
                addresses = ArrayList(all.flatMap { it.addresses }.distinctBy { it.value.trim().lowercase() }),
                events = ArrayList(all.flatMap { it.events }.distinctBy { it.type to it.value }),
                websites = ArrayList(all.flatMap { it.websites }.distinctBy { it.trim().lowercase() }),
                IMs = ArrayList(all.flatMap { it.IMs }.distinctBy { it.value.trim().lowercase() }),
                groups = ArrayList(all.flatMap { it.groups }.distinctBy { it.id }),
                photoUri = all.firstOrNull { it.photoUri.isNotEmpty() }?.photoUri ?: "",
                thumbnailUri = all.firstOrNull { it.thumbnailUri.isNotEmpty() }?.thumbnailUri ?: "",
                notes = all.map { it.notes }.filter { it.isNotBlank() }.distinct().joinToString("\n\n"),
                nickname = base.nickname.ifEmpty { others.firstOrNull { it.nickname.isNotEmpty() }?.nickname ?: "" },
                organization = if (base.organization.isNotEmpty()) base.organization else others.firstOrNull { it.organization.isNotEmpty() }?.organization ?: base.organization,
            )
        }
    }
}
