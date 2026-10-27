package com.example.ritmo

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

/**
 * Uploads posts that were saved offline as soon as the phone is back online.
 * Reference: Android Developers (2024) Monitor connectivity status.
 * https://developer.android.com/training/monitoring-device-state/connectivity-status-type
 */
object SyncManager {
    private const val TAG = "RitmoSync"
    private var syncing = false
    private var registered = false

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Call once when the app starts: watches for the network coming back and syncs
    fun start(context: Context) {
        val app = context.applicationContext
        if (!registered) {
            registered = true
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network available, syncing queued posts")
                    Handler(Looper.getMainLooper()).post { syncPending(app) }
                }
            })
        }
        syncPending(app)
    }

    fun syncPending(context: Context) {
        if (syncing || !isOnline(context)) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val localDb = OfflineDb(context)
        // Only upload posts that belong to the user who is logged in
        val pending = localDb.getAll().filter { it.authorId == uid }
        if (pending.isEmpty()) return

        syncing = true
        var remaining = pending.size
        val firestore = FirebaseFirestore.getInstance()

        pending.forEach { p ->
            val data = mutableMapOf<String, Any?>(
                "roomId" to p.roomId,
                "authorId" to p.authorId,
                "authorName" to p.authorName,
                "category" to p.category,
                "content" to p.content,
                "isBoosted" to p.isBoosted,
                // Keep the time the user actually wrote it, not the time it synced
                "createdAt" to Timestamp(Date(p.createdAtMillis))
            )
            if (p.imageBase64 != null) data["imageBase64"] = p.imageBase64

            firestore.collection("posts").add(data).addOnCompleteListener { result ->
                if (result.isSuccessful) {
                    localDb.delete(p.id)
                    Log.d(TAG, "Synced post ${p.id}")
                } else {
                    Log.e(TAG, "Sync failed for post ${p.id}: ${result.exception?.message}")
                }
                remaining--
                if (remaining == 0) syncing = false
            }
        }
    }
}