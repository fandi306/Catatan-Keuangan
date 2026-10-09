package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class CatatanKeuanganApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase(this)
    }

    companion object {
        fun initFirebase(context: Context) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (_: Exception) {
                    try {
                        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                        val dbId = if (resId != 0) context.getString(resId) else "catatank-648777d4-a30b"
                        val projectId = dbId.substringBefore('-').ifBlank { "ai-studio-catatanuang" }
                        val options = FirebaseOptions.Builder()
                            .setApplicationId(context.packageName)
                            .setProjectId(projectId)
                            .setApiKey("AIzaSyCatatanUangFallbackKeyForInitialization")
                            .build()
                        FirebaseApp.initializeApp(context, options)
                        Log.i("CatatanKeuanganApp", "FirebaseApp successfully initialized with fallback options")
                    } catch (e: Exception) {
                        Log.e("CatatanKeuanganApp", "FirebaseApp initialization failed: ${e.message}")
                    }
                }
            }
        }
    }
}
