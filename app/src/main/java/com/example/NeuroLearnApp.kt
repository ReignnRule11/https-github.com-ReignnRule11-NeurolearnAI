package com.example

import android.app.Application
import com.example.production.FirebaseBootstrap

class NeuroLearnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseBootstrap.initialize(this)
    }
}
