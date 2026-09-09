package com.philapp.psa2

import android.app.Application
import com.google.firebase.FirebaseApp
import com.phild.servicescanner.ServiceScannerDependencies
import com.phild.servicescanner.di.AppContainer

class PSAApplication : Application(), ServiceScannerDependencies {
    override lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        container = AppContainer(this)
    }
}
