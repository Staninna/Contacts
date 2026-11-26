package org.fossify.contacts.fork.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.Group
import kotlin.coroutines.resume

/**
 * The fork's way into the address book. It reuses Fossify's own [ContactsHelper], so sorting,
 * the account filter, merged duplicates and private (phone-only) contacts behave exactly as
 * in upstream; this class only makes it coroutine-friendly and observable.
 */
class ForkContactsRepository(private val context: Context) {

    fun hasReadPermission() = context.hasPermission(PERMISSION_READ_CONTACTS)

    suspend fun loadContacts(): List<Contact> = suspendCancellableCoroutine { cont ->
        ContactsHelper(context).getContacts { contacts -> if (cont.isActive) cont.resume(contacts) }
    }

    suspend fun loadGroups(): List<Group> = suspendCancellableCoroutine { cont ->
        ContactsHelper(context).getStoredGroups { groups -> if (cont.isActive) cont.resume(groups) }
    }

    suspend fun loadContact(id: Int, isPrivate: Boolean): Contact? = background {
        ContactsHelper(context).getContactWithId(id, isPrivate)
    }

    suspend fun setFavorite(contacts: List<Contact>, favorite: Boolean) = background {
        val list = ArrayList(contacts)
        if (favorite) ContactsHelper(context).addFavorites(list) else ContactsHelper(context).removeFavorites(list)
    }

    suspend fun delete(contacts: List<Contact>) = background {
        ContactsHelper(context).deleteContacts(ArrayList(contacts))
    }

    suspend fun deleteOne(contact: Contact, deleteClones: Boolean): Boolean = suspendCancellableCoroutine { cont ->
        ContactsHelper(context).deleteContact(contact, deleteClones) { success -> if (cont.isActive) cont.resume(success) }
    }

    /** Emits whenever anything in the system address book changes (edits, syncs, other apps). */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
        }
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    private suspend fun <T> background(block: () -> T): T = suspendCancellableCoroutine { cont ->
        ensureBackgroundThread {
            val result = block()
            if (cont.isActive) cont.resume(result)
        }
    }
}
