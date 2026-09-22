package com.cryptora.securechat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CryptoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Authoritative background periodic synchronization for content expiry
        com.cryptora.securechat.core.sync.ContentExpirySyncWorker.schedulePeriodicSync(this)
    }
}
