package com.narayan.portfolioadmin

import android.app.Application
import com.google.firebase.FirebaseApp
import com.narayan.portfolioadmin.data.tracker.ErrorTracker

class PortfolioAdminApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        ErrorTracker.initialize(this)
    }
}
