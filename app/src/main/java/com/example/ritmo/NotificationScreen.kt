package com.example.ritmo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritmo.ui.theme.RitmoBlack
import com.example.ritmo.ui.theme.RitmoCream
import com.example.ritmo.ui.theme.RitmoGray
import com.example.ritmo.ui.theme.RitmoSage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlin.math.roundToInt

private sealed class NotificationItem {
    data class Streak(val count: Long) : NotificationItem()
    data class NewPost(val authorName: String, val category: String, val content: String) : NotificationItem()
    data class ExpiringPost(val category: String, val content: String, val hoursLeft: Double) : NotificationItem()
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = auth.currentUser?.uid

    BackHandler { onBack() }

    var streakCount by remember { mutableStateOf(0L) }
    var homeRoute by remember { mutableStateOf<String?>(null) }
    var homeTime by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }

    // Load the user's streak and saved room, so we know which room's posts
    // to surface here.
    LaunchedEffect(myUid) {
        if (myUid == null) return@LaunchedEffect
        db.collection("users").document(myUid).get().addOnSuccessListener { doc ->
            streakCount = doc.getLong("streakCount") ?: 0L
            homeRoute = doc.getString("homeRouteName")
            homeTime = doc.getString("preferredTimeWindow")
        }
    }

    DisposableEffect(homeRoute, homeTime, myUid) {
        val route = homeRoute
        val time = homeTime
        if (myUid == null || route.isNullOrBlank() || time.isNullOrBlank()) {
            return@DisposableEffect onDispose {}
        }
        val roomId = roomIdFor(route, time)

        val listener: ListenerRegistration = db.collection("posts")
            .whereEqualTo("roomId", roomId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val newPosts = mutableListOf<NotificationItem>()
                val myExpiring = mutableListOf<NotificationItem>()

                snapshot?.documents?.forEach { doc ->
                    val createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: return@forEach
                    val hoursLeft = hoursUntilExpiry(createdAtMillis)
                    if (hoursLeft <= 0) return@forEach // already gone from the board

                    val authorId = doc.getString("authorId") ?: ""
                    val category = doc.getString("category") ?: ""
                    val content = doc.getString("content") ?: ""

                    if (authorId == myUid) {
                        myExpiring.add(NotificationItem.ExpiringPost(category, content, hoursLeft))
                    } else {
                        val authorName = doc.getString("authorName") ?: "Someone"
                        newPosts.add(NotificationItem.NewPost(authorName, category, content))
                    }
                }

                items = listOf(NotificationItem.Streak(streakCount)) + newPosts + myExpiring
            }

        onDispose { listener.remove() }
    }

    // Keep the streak entry in sync even if no posts exist yet.
    LaunchedEffect(streakCount) {
        if (items.isEmpty() || items.first() !is NotificationItem.Streak) {
            items = listOf(NotificationItem.Streak(streakCount)) + items.filterNot { it is NotificationItem.Streak }
        } else {
            items = listOf(NotificationItem.Streak(streakCount)) + items.drop(1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RitmoCream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = RitmoBlack,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Notifications", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = RitmoBlack)
        }

        if (homeRoute.isNullOrBlank() || homeTime.isNullOrBlank()) {
            Text(
                "Set a route and time on Home to see room activity here.",
                color = RitmoGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                NotificationRow(item)
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, title, subtitle) = when (item) {
            is NotificationItem.Streak -> Triple(
                Icons.Filled.Star,
                "${item.count}-day streak",
                "Keep your rhythm going today."
            )
            is NotificationItem.NewPost -> Triple(
                Icons.Filled.NotificationsNone,
                "${item.authorName} posted in your room",
                "${item.category}: ${item.content}"
            )
            is NotificationItem.ExpiringPost -> Triple(
                Icons.Filled.Schedule,
                "Your post will be removed in ${formatHours(item.hoursLeft)}",
                "${item.category}: ${item.content}"
            )
        }

        Icon(icon, contentDescription = null, tint = RitmoSage, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = RitmoBlack, fontSize = 14.sp)
            Text(subtitle, color = RitmoGray, fontSize = 13.sp, maxLines = 2)
        }
    }
}

private fun formatHours(hours: Double): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h <= 0 -> "$m minute${if (m == 1) "" else "s"}"
        m == 0 -> "$h hour${if (h == 1) "" else "s"}"
        else -> "${h}h ${m}m"
    }
}