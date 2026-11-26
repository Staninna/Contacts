package org.fossify.contacts.fork.ui.home

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.fossify.commons.extensions.appLaunched
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.convertToBitmap
import org.fossify.commons.extensions.launchSendSMSIntent
import org.fossify.commons.extensions.shortcutManager
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.LICENSE_AUTOFITTEXTVIEW
import org.fossify.commons.helpers.LICENSE_GLIDE
import org.fossify.commons.helpers.LICENSE_GSON
import org.fossify.commons.helpers.LICENSE_INDICATOR_FAST_SCROLL
import org.fossify.commons.helpers.LICENSE_JODA
import org.fossify.commons.helpers.PERMISSION_GET_ACCOUNTS
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.PERMISSION_WRITE_CONTACTS
import org.fossify.commons.helpers.TAB_CONTACTS
import org.fossify.commons.helpers.TAB_FAVORITES
import org.fossify.commons.helpers.TAB_GROUPS
import org.fossify.commons.helpers.TAB_LAST_USED
import org.fossify.commons.models.FAQItem
import org.fossify.commons.models.RadioItem
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.contacts.BuildConfig
import org.fossify.contacts.R
import org.fossify.contacts.activities.EditContactActivity
import org.fossify.contacts.activities.GroupContactsActivity
import org.fossify.contacts.activities.SettingsActivity
import org.fossify.contacts.activities.SimpleActivity
import org.fossify.contacts.dialogs.ChangeSortingDialog
import org.fossify.contacts.dialogs.CreateNewGroupDialog
import org.fossify.contacts.dialogs.FilterContactSourcesDialog
import org.fossify.contacts.extensions.callContact
import org.fossify.contacts.extensions.config
import org.fossify.contacts.extensions.editContact
import org.fossify.contacts.extensions.shareContacts
import org.fossify.contacts.extensions.tryImportContactsFromFile
import org.fossify.contacts.fork.ui.details.ForkContactActivity
import org.fossify.contacts.fork.ui.theme.ForkTheme
import org.fossify.contacts.helpers.GROUP
import org.fossify.commons.helpers.ON_CLICK_CALL_CONTACT
import org.fossify.commons.helpers.ON_CLICK_EDIT_CONTACT

/** The fork's home screen: contacts, favorites and groups in Material 3 Expressive. */
class ForkHomeActivity : SimpleActivity(), HomeActions {
    private val viewModel: HomeViewModel by viewModels()
    private var permissionsAsked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appLaunched(BuildConfig.APPLICATION_ID)

        val tabs = enabledTabs()
        val initial = initialTab(tabs)
        setContent {
            ForkTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    tabs = tabs,
                    initialTab = initial,
                    actions = this,
                    onQueryChange = viewModel::onQueryChange,
                    onToggleSelect = viewModel::toggleSelection,
                    onSelectAll = viewModel::selectAll,
                    onClearSelection = viewModel::clearSelection,
                    onSetFavorite = { fav -> viewModel.setFavorite(viewModel.selectedContacts(), fav) },
                    onDelete = { viewModel.delete(viewModel.selectedContacts()) },
                )
            }
        }

        askPermissions()
        handleExternalIntent(intent)
        updateShortcut()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleExternalIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        org.fossify.contacts.fork.ForkUi.fixSystemBars(this)
        if (permissionsAsked) viewModel.refresh()
    }

    private fun askPermissions() {
        handlePermission(PERMISSION_READ_CONTACTS) { granted ->
            permissionsAsked = true
            if (granted) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    handlePermission(PERMISSION_GET_ACCOUNTS) { viewModel.refresh() }
                }
            } else {
                viewModel.refresh()
            }
        }
    }

    private fun enabledTabs(): List<HomeTab> {
        val mask = config.showTabs
        val tabs = buildList {
            if (mask and TAB_CONTACTS != 0) add(HomeTab.Contacts)
            if (mask and TAB_FAVORITES != 0) add(HomeTab.Favorites)
            if (mask and TAB_GROUPS != 0) add(HomeTab.Groups)
        }
        return tabs.ifEmpty { listOf(HomeTab.Contacts) }
    }

    private fun initialTab(tabs: List<HomeTab>): HomeTab {
        val wanted = when (config.defaultTab) {
            TAB_LAST_USED -> tabs.getOrNull(config.lastUsedViewPagerPage)
            TAB_FAVORITES -> HomeTab.Favorites
            TAB_GROUPS -> HomeTab.Groups
            else -> HomeTab.Contacts
        }
        return wanted?.takeIf { it in tabs } ?: tabs.first()
    }

    private fun handleExternalIntent(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        } ?: return
        tryImportContactsFromFile(uri) { success -> if (success) runOnUiThread { viewModel.refresh() } }
        intent?.action = null
    }

    private fun updateShortcut() {
        val appIconColor = config.appIconColor
        if (config.lastHandledShortcutColor == appIconColor) return
        runCatching {
            val label = getString(org.fossify.commons.R.string.create_new_contact)
            val drawable = resources.getDrawable(org.fossify.commons.R.drawable.shortcut_plus, theme) as LayerDrawable
            drawable.findDrawableByLayerId(org.fossify.commons.R.id.shortcut_plus_background).applyColorFilter(appIconColor)
            val shortcut = ShortcutInfo.Builder(this, "create_new_contact")
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(Icon.createWithBitmap(drawable.convertToBitmap()))
                .setIntent(Intent(this, EditContactActivity::class.java).setAction(Intent.ACTION_VIEW))
                .build()
            shortcutManager.dynamicShortcuts = listOf(shortcut)
            config.lastHandledShortcutColor = appIconColor
        }
    }

    // HomeActions

    override fun openContact(item: ContactItem) {
        when (config.onContactClick) {
            ON_CLICK_CALL_CONTACT -> callContact(item.contact)
            ON_CLICK_EDIT_CONTACT -> editContact(item.contact, config.mergeDuplicateContacts)
            else -> startActivity(ForkContactActivity.intent(this, item.contact))
        }
    }

    override fun call(item: ContactItem) = callContact(item.contact)

    override fun message(item: ContactItem) {
        val numbers = item.contact.phoneNumbers
        val primary = numbers.firstOrNull { it.isPrimary }
        when {
            numbers.isEmpty() -> toast(org.fossify.commons.R.string.no_phone_number_found)
            numbers.size == 1 || primary != null -> launchSendSMSIntent((primary ?: numbers.first()).value)
            else -> RadioGroupDialog(this, ArrayList(numbers.mapIndexed { i, n -> RadioItem(i, n.value, n.value) })) {
                launchSendSMSIntent(it as String)
            }
        }
    }

    override fun createContact() {
        startActivity(Intent(this, EditContactActivity::class.java))
    }

    override fun createGroup() {
        CreateNewGroupDialog(this) { viewModel.refresh() }
    }

    override fun openGroup(group: GroupItem) {
        startActivity(Intent(this, GroupContactsActivity::class.java).putExtra(GROUP, group.group))
    }

    override fun share(items: List<ContactItem>) {
        shareContacts(ArrayList(items.map { it.contact }))
    }

    override fun sort() {
        ChangeSortingDialog(this, showCustomSorting = false) { viewModel.refresh() }
    }

    override fun filter() {
        FilterContactSourcesDialog(this) { viewModel.refresh() }
    }

    override fun settings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun about() {
        val licenses = LICENSE_JODA or LICENSE_GLIDE or LICENSE_GSON or LICENSE_INDICATOR_FAST_SCROLL or LICENSE_AUTOFITTEXTVIEW
        val faq = arrayListOf(
            FAQItem(R.string.faq_1_title, R.string.faq_1_text),
            FAQItem(org.fossify.commons.R.string.faq_9_title_commons, org.fossify.commons.R.string.faq_9_text_commons),
        )
        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faq, true)
    }

    override fun requestPermission() = askPermissions()

    override fun tabChanged(tab: HomeTab) {
        config.lastUsedViewPagerPage = enabledTabs().indexOf(tab).coerceAtLeast(0)
    }
}
