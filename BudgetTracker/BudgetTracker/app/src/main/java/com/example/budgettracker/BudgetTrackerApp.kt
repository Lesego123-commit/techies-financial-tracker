package com.example.budgettracker

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

/**
 * Custom Application class for the Budget Tracker app.
 *
 * Its only job is to enable Firebase Realtime Database offline persistence ONCE, before
 * any part of the app touches the database. With persistence enabled the app keeps a local
 * cache of the online data, so it still shows the user's records when the phone briefly
 * loses connectivity and automatically syncs any changes back to Firebase when it reconnects.
 *
 * This class is registered in AndroidManifest.xml via android:name=".BudgetTrackerApp".
 */
class BudgetTrackerApp : Application() {

    companion object {
        private const val TAG = "BudgetTrackerApp"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // Must be called before the database is used anywhere else in the app.
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            Log.d(TAG, "Firebase Realtime Database offline persistence enabled")
        } catch (e: Exception) {
            // Safe to ignore – this only fails if the database was already accessed.
            Log.w(TAG, "Could not enable persistence: ${e.message}")
        }
    }
}
