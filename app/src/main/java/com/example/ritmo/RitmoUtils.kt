package com.example.ritmo.ui

// Turns "Route 4 - Wynberg to Rosebank" + "07:00-07:30" into a stable,
// Firestore-safe document ID like "route-4-wynberg-to-rosebank_07-00-07-30"
// so everyone on the same route+time window lands in the same room document.
fun roomIdFor(routeName: String, timeWindow: String): String {
    fun slug(s: String) = s
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return "${slug(routeName)}_${slug(timeWindow)}"
}

// Turns two user IDs into one stable, order-independent conversation ID, so
// both people always land in the same conversation document no matter who
// opens the chat first.
fun conversationIdFor(uidA: String, uidB: String): String =
    listOf(uidA, uidB).sorted().joinToString("_")

// --- Streak + post-expiry helpers ---

private fun todayString(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date())

// Call this whenever the user does their "daily" action (joining a room).
// Increments the streak if they were active yesterday too, resets it to 1
// if they skipped a day, and leaves it alone if they already counted today.
fun updateStreakOnActivity(
    db: com.google.firebase.firestore.FirebaseFirestore,
    uid: String
) {
    val userRef = db.collection("users").document(uid)
    userRef.get().addOnSuccessListener { doc ->
        val today = todayString()
        val lastActive = doc.getString("lastActiveDate")
        val currentStreak = doc.getLong("streakCount") ?: 0L

        if (lastActive == today) return@addOnSuccessListener // already counted today

        val newStreak: Long = if (lastActive != null) {
            try {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val diffMillis = fmt.parse(today)!!.time - fmt.parse(lastActive)!!.time
                val diffDays = diffMillis / (1000 * 60 * 60 * 24)
                if (diffDays == 1L) currentStreak + 1 else 1L
            } catch (e: Exception) {
                1L
            }
        } else {
            1L
        }

        userRef.set(
            mapOf("lastActiveDate" to today, "streakCount" to newStreak),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }
}

// Returns hours remaining before a post (created at createdAtMillis) expires,
// 24 hours after it was posted. Negative/zero means it has already expired.
fun hoursUntilExpiry(createdAtMillis: Long): Double {
    val ageMillis = System.currentTimeMillis() - createdAtMillis
    val remainingMillis = (24 * 60 * 60 * 1000) - ageMillis
    return remainingMillis / (1000.0 * 60.0 * 60.0)
}

// A "Good morning"/"afternoon"/"evening" greeting based on the real current time.
fun greetingForNow(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

// --- Image helpers (no Firebase Storage / Blaze plan needed) ---
//
// Pictures are stored directly as small compressed Base64 strings inside the
// Firestore document itself, instead of uploading to Firebase Storage. This
// works fine for one profile picture or a single post photo, but keep images
// small (maxDimensionPx) since Firestore documents are capped at 1MB total.

fun uriToCompressedBase64(
    context: android.content.Context,
    uri: android.net.Uri,
    maxDimensionPx: Int = 400,
    quality: Int = 55
): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = android.graphics.BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null

        val scale = minOf(
            maxDimensionPx.toFloat() / original.width,
            maxDimensionPx.toFloat() / original.height,
            1f
        )
        val scaled = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else original

        val outputStream = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
        android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

fun base64ToBitmap(base64: String?): android.graphics.Bitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}