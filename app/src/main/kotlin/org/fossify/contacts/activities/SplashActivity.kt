package org.fossify.contacts.activities

import android.content.Intent
import org.fossify.commons.activities.BaseSplashActivity

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        startActivity(Intent(this, org.fossify.contacts.fork.ui.home.ForkHomeActivity::class.java)) // fork: Compose home screen
        finish()
    }
}
