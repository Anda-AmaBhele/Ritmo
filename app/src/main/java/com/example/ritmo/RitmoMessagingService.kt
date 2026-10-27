package com.example.ritmo

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles push notifications sent through Firebase Cloud Messaging (FCM).
 * Reference: Firebase (2024) Receive messages in an Android app.
 * https://firebase.google.com/docs/cloud-messaging/android/receive
 */
class RitmoMessagingService : FirebaseMessagingService() {

    // Called when Firebase gives this phone a new token
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token received")
        saveTokenToFirestore(token)
    }

    // Called when a message arrives while the app is open
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        Log.d(TAG, "Message received: $title")
        showNotification(this, title, body)
    }

    companion object {
        const val CHANNEL_ID = "ritmo_updates"
        private const val TAG = "RitmoFCM"

        // Notification channels are required from Android 8 (API 26) onwards
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Ritmo updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Route room updates, messages and streak reminders"
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }

        @SuppressLint("MissingPermission")
        fun showNotification(context: Context, title: String, body: String) {
            // Android 13+ needs the user to have allowed notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Notification permission not granted, skipping")
                return
            }

            createChannel(context)

            // Tapping the notification opens the app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context)
                .notify(System.currentTimeMillis().toInt(), notification)
        }

        // Get this phone's token and store it on the user's Firestore document,
        // so a server (or the Firebase console) can target this user.
        fun saveToken() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "FCM token: ${task.result}")
                    saveTokenToFirestore(task.result)
                } else {
                    Log.e(TAG, "Couldn't get FCM token: ${task.exception?.message}")
                }
            }
        }

        private fun saveTokenToFirestore(token: String) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "Token saved to Firestore") }
                .addOnFailureListener { Log.e(TAG, "Saving token failed: ${it.message}") }
        }
    }
}