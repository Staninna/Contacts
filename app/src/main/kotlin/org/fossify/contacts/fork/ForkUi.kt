package org.fossify.contacts.fork

import android.app.Activity
import android.content.Intent
import org.fossify.contacts.fork.ui.details.ForkContactActivity
import org.fossify.contacts.fork.ui.home.ForkHomeActivity

/**
 * The hooks upstream screens call to hand over to the fork's Compose UI. Upstream's
 * activities stay registered, so outside links, shortcuts and other apps keep working:
 * they land on the old screen, which forwards the very same intent here and closes.
 */
object ForkUi {
    fun takeOverMain(from: Activity): Boolean = forward(from, ForkHomeActivity::class.java)

    fun takeOverDetails(from: Activity): Boolean = forward(from, ForkContactActivity::class.java)

    /**
     * Fossify's activities restyle the system bars for their own headers on every resume.
     * The Compose screens draw edge to edge, so they set the bar icons back to match the theme.
     */
    fun fixSystemBars(activity: Activity) {
        val night = activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val dark = night == android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        @Suppress("DEPRECATION")
        activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun forward(from: Activity, target: Class<out Activity>): Boolean {
        val intent = Intent(from.intent).setClass(from, target)
        // A forwarded result request would be lost; keep those on the old screen.
        if (from.callingActivity != null) return false
        from.startActivity(intent)
        from.finish()
        from.overridePendingTransition(0, 0)
        return true
    }
}
