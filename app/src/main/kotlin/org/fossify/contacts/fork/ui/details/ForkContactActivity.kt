package org.fossify.contacts.fork.ui.details

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.fossify.commons.extensions.getContactPublicUri
import org.fossify.commons.extensions.getContactUriRawId
import org.fossify.commons.extensions.getLookupKeyFromUri
import org.fossify.commons.extensions.getLookupUriRawId
import org.fossify.commons.extensions.getPhoneNumberTypeText
import org.fossify.commons.extensions.launchSendSMSIntent
import org.fossify.commons.extensions.launchViewContactIntent
import org.fossify.commons.extensions.launchViewIntent
import org.fossify.commons.extensions.sendEmailIntent
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.CONTACT_ID
import org.fossify.commons.helpers.ContactsHelper
import org.fossify.commons.helpers.IS_PRIVATE
import org.fossify.commons.helpers.LocalContactsHelper
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.SILENT
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.contacts.Address
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.Email
import org.fossify.commons.models.contacts.Group
import org.fossify.commons.models.contacts.IM
import org.fossify.commons.models.contacts.SocialAction
import org.fossify.commons.extensions.tryInitiateCall
import org.fossify.contacts.activities.ContactActivity
import org.fossify.contacts.activities.GroupContactsActivity
import org.fossify.contacts.extensions.config
import org.fossify.contacts.extensions.editContact
import org.fossify.contacts.extensions.startCallIntent
import org.fossify.contacts.fork.ui.theme.ForkTheme
import org.fossify.contacts.helpers.GROUP

/**
 * The fork's contact details screen. It extends upstream's [ContactActivity], so sharing,
 * type labels and the ringtone picker work exactly as before; only the UI is new.
 */
class ForkContactActivity : ContactActivity(), DetailsActions {
    private val viewModel: DetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForkTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                contact = state.contact
                if (state.missing) {
                    finish()
                }
                DetailsScreen(state = state, actions = this)
            }
        }

        val external = intent.action == ContactsContract.QuickContact.ACTION_QUICK_CONTACT || intent.action == Intent.ACTION_VIEW
        if (external) {
            handlePermission(PERMISSION_READ_CONTACTS) { granted ->
                if (granted) {
                    viewModel.load(refFromIntent())
                } else {
                    toast(org.fossify.commons.R.string.no_contacts_permission)
                    finish()
                }
            }
        } else if (savedInstanceState == null || viewModel.state.value.contact == null) {
            viewModel.load(refFromIntent())
        }
    }

    override fun onResume() {
        super.onResume()
        org.fossify.contacts.fork.ForkUi.fixSystemBars(this)
    }

    private fun refFromIntent(): ContactRef {
        val id = intent.getIntExtra(CONTACT_ID, 0)
        if (id != 0) return ContactRef.ById(id, intent.getBooleanExtra(IS_PRIVATE, false))
        val data = intent.data
        return ContactRef.Resolved {
            if (data == null) return@Resolved null
            val helper = ContactsHelper(this)
            if (data.path?.contains("lookup") == true) {
                getLookupKeyFromUri(data)?.let { key -> helper.getContactWithLookupKey(key)?.let { return@Resolved it } }
                val raw = getLookupUriRawId(data)
                if (raw != -1) helper.getContactWithId(raw, false) else null
            } else {
                val raw = getContactUriRawId(data)
                if (raw != -1) helper.getContactWithId(raw, false) else null
            }
        }
    }

    private fun current(): Contact? = viewModel.state.value.contact

    // DetailsActions

    override fun back() = finish()

    override fun call(number: String?) {
        val c = current() ?: return
        if (number != null) startCallIntent(number) else tryInitiateCall(c) { startCallIntent(it) }
    }

    override fun message(number: String?) {
        if (number != null) launchSendSMSIntent(number) else trySendSMS()
    }

    override fun email(address: String?) {
        if (address != null) sendEmailIntent(address) else trySendEmail()
    }

    override fun openAddress(address: Address) {
        val query = Uri.encode(address.value.replace('\n', ' '))
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
        } catch (e: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.no_app_found)
        }
    }

    override fun openWebsite(url: String) {
        launchViewIntent(if (url.startsWith("http")) url else "https://$url")
    }

    override fun openSocial(action: SocialAction) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, action.dataId), action.mimetype)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        try {
            startActivity(intent)
        } catch (e: SecurityException) {
            handlePermission(org.fossify.commons.helpers.PERMISSION_CALL_PHONE) { ok ->
                if (ok) runCatching { startActivity(intent) } else toast(org.fossify.commons.R.string.no_phone_call_permission)
            }
        } catch (e: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.no_app_found)
        }
    }

    override fun openGroup(group: Group) {
        startActivity(Intent(this, GroupContactsActivity::class.java).putExtra(GROUP, group))
    }

    override fun edit() {
        val base = viewModel.state.value.linked.firstOrNull() ?: return
        editContact(base, config.mergeDuplicateContacts)
    }

    override fun share() {
        current()?.let { shareContact(it) }
    }

    override fun openWith() {
        val base = viewModel.state.value.linked.firstOrNull() ?: return
        launchViewContactIntent(getContactPublicUri(base))
    }

    override fun delete() {
        lifecycleScope.launch {
            val linked = viewModel.state.value.linked.size > 1
            viewModel.delete(deleteClones = linked && config.mergeDuplicateContacts)
            finish()
        }
    }

    override fun toggleFavorite() = viewModel.toggleFavorite()

    override fun pickRingtone() {
        contact = viewModel.state.value.linked.firstOrNull() ?: return
        try {
            startActivityForResult(getRingtonePickerIntent(), INTENT_SELECT_RINGTONE)
        } catch (e: ActivityNotFoundException) {
            toast(org.fossify.commons.R.string.no_app_found)
        }
    }

    /** The custom ringtone's name, or null when the contact uses the default one. */
    override fun ringtoneTitle(): String? {
        val ringtone = current()?.ringtone ?: return null
        return when {
            ringtone.isEmpty() || ringtone == SILENT -> getString(org.fossify.commons.R.string.no_sound)
            ringtone == getDefaultRingtoneUri().toString() -> null
            else -> runCatching { RingtoneManager.getRingtone(this, Uri.parse(ringtone))?.getTitle(this) }.getOrNull()
        }
    }

    override fun customRingtoneSelected(ringtonePath: String) = ringtoneUpdated(ringtonePath)

    override fun systemRingtoneSelected(uri: Uri?) = ringtoneUpdated(uri?.toString() ?: "")

    private fun ringtoneUpdated(path: String) {
        val base = viewModel.state.value.linked.firstOrNull() ?: return
        ensureBackgroundThread {
            if (base.isPrivate()) {
                LocalContactsHelper(this).updateRingtone(base.contactId, path)
            } else {
                ContactsHelper(this).updateRingtone(base.contactId.toString(), path)
            }
            runOnUiThread { viewModel.reload() }
        }
    }

    override fun phoneType(number: PhoneNumber) = getPhoneNumberTypeText(number.type, number.label)
    override fun emailType(email: Email) = getEmailTypeText(email.type, email.label)
    override fun addressType(address: Address) = getAddressTypeText(address.type, address.label)
    override fun imType(im: IM) = getIMTypeText(im.type, im.label)

    override fun copy(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(text, text))
    }

    override fun monthDayPattern(): String = DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), "MMMMd")

    companion object {
        fun intent(context: Context, contact: Contact) = Intent(context, ForkContactActivity::class.java)
            .putExtra(CONTACT_ID, contact.id)
            .putExtra(IS_PRIVATE, contact.isPrivate())

    }
}
