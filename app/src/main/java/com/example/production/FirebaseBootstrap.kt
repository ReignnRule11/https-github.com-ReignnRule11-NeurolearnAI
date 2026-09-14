package com.example.production

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

object FirebaseBootstrap {
    private const val TAG = "FirebaseBootstrap"

    @Volatile
    var isConfigured: Boolean = false
        private set

    fun initialize(context: Context): Boolean {
        return try {
            val appContext = context.applicationContext
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                isConfigured = true
                Log.d(TAG, "FirebaseApp already initialized.")
                return true
            }
            val googleAppIdRes = appContext.resources.getIdentifier(
                "google_app_id",
                "string",
                appContext.packageName
            )
            if (googleAppIdRes == 0) {
                Log.w(TAG, "google-services.json was not packaged. Firebase remains disabled.")
                isConfigured = false
                return false
            }
            val appId = appContext.getString(googleAppIdRes)
            if (appId.isBlank() || appId.contains("REPLACE") || appId == "1:0:android:0") {
                Log.w(TAG, "google_app_id is a placeholder. Firebase remains disabled.")
                isConfigured = false
                return false
            }
            FirebaseApp.initializeApp(appContext)
            isConfigured = FirebaseApp.getApps(appContext).isNotEmpty()
            Log.d(TAG, "FirebaseApp initialized from packaged google-services configuration.")
            isConfigured
        } catch (e: Exception) {
            isConfigured = false
            Log.e(TAG, "Firebase initialization skipped: ${e.message}")
            false
        }
    }
}
