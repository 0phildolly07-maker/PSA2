package com.philapp.psa2

import android.app.Application
import com.google.firebase.FirebaseApp

class PSAApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 