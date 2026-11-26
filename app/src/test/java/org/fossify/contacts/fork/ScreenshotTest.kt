package org.fossify.contacts.fork

import android.graphics.Bitmap
import android.provider.ContactsContract.CommonDataKinds.Event as EventKind
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.contacts.Address
import org.fossify.commons.models.contacts.Contact
import org.fossify.commons.models.contacts.Email
import org.fossify.commons.models.contacts.Event
import org.fossify.commons.models.contacts.Group
import org.fossify.commons.models.contacts.IM
import org.fossify.commons.models.contacts.Organization
import org.fossify.commons.models.contacts.SocialAction
import org.fossify.contacts.fork.ui.details.DetailsActions
import org.fossify.contacts.fork.ui.details.DetailsScreen
import org.fossify.contacts.fork.ui.details.DetailsState
import org.fossify.contacts.fork.ui.home.ContactItem
import org.fossify.contacts.fork.ui.home.GroupItem
import org.fossify.contacts.fork.ui.home.HomeActions
import org.fossify.contacts.fork.ui.home.HomeScreen
import org.fossify.contacts.fork.ui.home.HomeState
import org.fossify.contacts.fork.ui.home.HomeTab
import org.fossify.contacts.fork.ui.home.toItem
import org.fossify.contacts.fork.ui.theme.ForkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the fork's screens with sample people and writes PNGs to app/build/fork-screenshots,
 * so the UI can be reviewed without a phone or emulator.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun person(id: Int, first: String, last: String, phone: String? = null, starred: Boolean = false, company: String = "") = Contact(
        id = id,
        contactId = id,
        firstName = first,
        surname = last,
        phoneNumbers = if (phone != null) arrayListOf(PhoneNumber(phone, 2, "", phone)) else arrayListOf(),
        starred = if (starred) 1 else 0,
        organization = Organization(company, ""),
    )

    private val people = listOf(
        person(1, "Anna", "Smith", "+31 6 1234 5678", starred = true),
        person(2, "Amir", "Haddad", "+31 6 2345 6789", company = "Haddad & Co"),
        person(3, "Ben", "Okafor", "+31 6 3456 7890", starred = true),
        person(4, "Bianca", "Rossi"),
        person(5, "Chen", "Wei", "+31 20 555 0101", starred = true),
        person(6, "Clara", "Jansen", "+31 6 4567 8901"),
        person(7, "Daan", "de Vries", "+31 6 5678 9012"),
        person(8, "Elif", "Kaya", "+31 6 6789 0123", starred = true),
        person(9, "Émile", "Laurent", "+33 6 12 34 56 78"),
        person(10, "Femke", "Bakker", "+31 6 7890 1234"),
    ).map { it.toItem(showPhoneNumbers = false) }

    private val home = HomeState(
        loading = false,
        contacts = people,
        favorites = people.filter { it.starred },
        groups = listOf(
            GroupItem(Group(1L, "Family"), "Family", 3, people.take(3)),
            GroupItem(Group(2L, "Climbing"), "Climbing", 5, people.drop(3).take(5)),
            GroupItem(Group(3L, "Book club"), "Book club", 0, emptyList()),
        ),
        sections = people.map { it.section }.distinct(),
    )

    private val homeActions = object : HomeActions {
        override fun openContact(item: ContactItem) = Unit
        override fun call(item: ContactItem) = Unit
        override fun message(item: ContactItem) = Unit
        override fun createContact() = Unit
        override fun createGroup() = Unit
        override fun openGroup(group: GroupItem) = Unit
        override fun share(items: List<ContactItem>) = Unit
        override fun sort() = Unit
        override fun filter() = Unit
        override fun settings() = Unit
        override fun about() = Unit
        override fun requestPermission() = Unit
        override fun tabChanged(tab: HomeTab) = Unit
    }

    private fun homeShot(name: String, state: HomeState, tab: HomeTab = HomeTab.Contacts, dark: Boolean = false) {
        compose.setContent {
            ForkTheme(darkTheme = dark) {
                HomeScreen(
                    state = state,
                    tabs = HomeTab.entries,
                    initialTab = tab,
                    actions = homeActions,
                    onQueryChange = {},
                    onToggleSelect = {},
                    onSelectAll = {},
                    onClearSelection = {},
                    onSetFavorite = {},
                    onDelete = {},
                )
            }
        }
        save(name)
    }

    private fun save(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_000)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/fork-screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun homeContacts() = homeShot("home_contacts", home)
    @Test fun homeContactsDark() = homeShot("home_contacts_dark", home, dark = true)
    @Test fun homeFavorites() = homeShot("home_favorites", home, HomeTab.Favorites)
    @Test fun homeGroups() = homeShot("home_groups", home, HomeTab.Groups)
    @Test fun homeSelecting() = homeShot("home_selecting", home.copy(selected = setOf(1, 3)))

    @Test fun homeSearch() {
        val query = "an"
        val results = org.fossify.contacts.fork.data.ContactSearch.search(
            query,
            people.map { org.fossify.contacts.fork.data.ContactSearch.Candidate(it.id, it.name, emptyList(), emptyList()) },
        ).map { m -> org.fossify.contacts.fork.ui.home.SearchResult(people.first { it.id == m.key }, null) }
        homeShot("home_search", home.copy(query = query, results = results))
    }

    @Test fun homeEmptySearch() = homeShot("home_search_empty", home.copy(query = "zzz", results = emptyList()))

    private val anna = Contact(
        id = 1,
        contactId = 1,
        firstName = "Anna",
        surname = "Smith",
        nickname = "Annie",
        phoneNumbers = arrayListOf(
            PhoneNumber("+31 6 1234 5678", 2, "", "+31612345678", isPrimary = true),
            PhoneNumber("+31 20 555 0101", 3, "", "+31205550101"),
        ),
        emails = arrayListOf(Email("anna@example.org", 1, "")),
        addresses = arrayListOf(Address("Prinsengracht 263\n1016 GV Amsterdam", 1, "")),
        events = arrayListOf(Event("1990-12-09", EventKind.TYPE_BIRTHDAY), Event("--06-21", EventKind.TYPE_ANNIVERSARY)),
        notes = "Met at the climbing gym. Prefers Signal.",
        organization = Organization("Acme", "Product designer"),
        websites = arrayListOf("anna.example.org"),
        IMs = arrayListOf(IM("@anna:matrix.org", 3, "")),
        groups = arrayListOf(Group(1L, "Family"), Group(2L, "Climbing")),
        starred = 1,
    )

    private val detailsActions = object : DetailsActions {
        override fun back() = Unit
        override fun call(number: String?) = Unit
        override fun message(number: String?) = Unit
        override fun email(address: String?) = Unit
        override fun openAddress(address: Address) = Unit
        override fun openWebsite(url: String) = Unit
        override fun openSocial(action: SocialAction) = Unit
        override fun openGroup(group: Group) = Unit
        override fun edit() = Unit
        override fun share() = Unit
        override fun openWith() = Unit
        override fun delete() = Unit
        override fun toggleFavorite() = Unit
        override fun pickRingtone() = Unit
        override fun ringtoneTitle(): String? = null
        override fun phoneType(number: org.fossify.commons.models.PhoneNumber) = if (number.isPrimary) "Mobile" else "Work"
        override fun emailType(email: Email) = "Home"
        override fun addressType(address: Address) = "Home"
        override fun imType(im: IM) = "Matrix"
        override fun copy(text: String) = Unit
        override fun monthDayPattern() = "d MMMM"
    }

    private fun detailsShot(name: String, dark: Boolean, tall: Boolean = false) {
        val state = DetailsState(
            loading = false,
            contact = anna,
            linked = listOf(anna),
            sources = listOf("anna@nextcloud.example"),
            social = listOf(SocialAction(1, 0, "Signal message", "", 1, "org.thoughtcrime.securesms")),
            starred = true,
            visibleFields = Int.MAX_VALUE,
        )
        compose.setContent { ForkTheme(darkTheme = dark) { DetailsScreen(state, detailsActions) } }
        save(name)
    }

    @Test fun details() = detailsShot("details", dark = false)
    @Test fun detailsDark() = detailsShot("details_dark", dark = true)

    @Config(qualifiers = "w411dp-h2200dp-xxhdpi")
    @Test fun detailsFull() = detailsShot("details_full", dark = false)
}
