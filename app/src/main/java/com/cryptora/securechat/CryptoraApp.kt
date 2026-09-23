package com.cryptora.securechat

import android.app.Application
import com.cryptora.securechat.core.activity.CurrentActivityHolder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CryptoraApp : Application() {

    @Inject
    lateinit var activityHolder: CurrentActivityHolder

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityHolder)
        // Authoritative background periodic synchronization for content expiry
        com.cryptora.securechat.core.sync.ContentExpirySyncWorker.schedulePeriodicSync(this)
    }
}
