package com.example.ritmo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ritmo.ui.theme.RitmoBlack
import com.example.ritmo.ui.theme.RitmoCream
import com.example.ritmo.ui.theme.RitmoGray
import com.example.ritmo.ui.theme.RitmoSage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

data class BoardPost(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val category: String = "",
    val content: String = "",
    val isBoosted: Boolean = false,
    val createdAtMillis: Long = 0L,
    val imageBase64: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteRoomScreen(
    routeName: String,
    timeWindow: String,
    onNavigateToPost: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: (otherUserId: String, otherUserName: String) -> Unit = { _, _ -> }
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val roomId = remember(routeName, timeWindow) { roomIdFor(routeName, timeWindow) }

    var memberCount by remember { mutableStateOf(0) }
    var isMicOn by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<BoardPost>>(emptyList()) }
    var selectedAuthorId by remember { mutableStateOf<String?>(null) }
    var selectedAuthorName by remember { mutableStateOf<String>("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler { onNavigateHome() }

    LaunchedEffect(roomId) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("rooms").document(roomId).set(
            mapOf(
                "routeName" to routeName,
                "timeWindow" to timeWindow,
                "memberIds" to FieldValue.arrayUnion(uid)
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
        // Joining a room counts as today's activity for the streak.
        updateStreakOnActivity(db, uid)
    }

    DisposableEffect(roomId) {
        val roomListener: ListenerRegistration = db.collection("rooms").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.get("memberIds") as? List<*>
                memberCount = ids?.size ?: 0
            }

        val postsListener: ListenerRegistration = db.collection("posts")
            .whereEqualTo("roomId", roomId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // This is what used to fail silently. Most likely cause:
                    // Firestore needs a composite index for this query and one
                    // hasn't been created yet — the real error message (visible
                    // in Logcat/Build Output) includes a direct link to create it.
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Couldn't load posts: ${error.message ?: "unknown error"}"
                        )
                    }
                    return@addSnapshotListener
                }
                posts = snapshot?.documents?.mapNotNull { doc ->
                    val createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: return@mapNotNull null
                    // Posts older than 24 hours quietly drop off the board.
                    if (hoursUntilExpiry(createdAtMillis) <= 0) return@mapNotNull null
                    BoardPost(
                        postId = doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "Someone",
                        category = doc.getString("category") ?: "",
                        content = doc.getString("content") ?: "",
                        isBoosted = doc.getBoolean("isBoosted") ?: false,
                        createdAtMillis = createdAtMillis,
                        imageBase64 = doc.getString("imageBase64")
                    )
                } ?: emptyList()
            }

        onDispose {
            roomListener.remove()
            postsListener.remove()
        }
    }

    if (selectedAuthorId != null) {
        val authorIdToShow = selectedAuthorId!!
        AuthorProfileDialog(
            authorId = authorIdToShow,
            authorName = selectedAuthorName,
            onDismiss = { selectedAuthorId = null },
            onMessageClick = {
                selectedAuthorId = null
                onNavigateToChat(authorIdToShow, selectedAuthorName)
            }
        )
    }

    Scaffold(
        containerColor = RitmoCream,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RitmoSage)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = RitmoCream,
                        modifier = Modifier.clickable { onNavigateHome() }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = routeName,
                    color = RitmoCream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "$timeWindow  \u00b7  $memberCount in this room",
                    color = RitmoCream.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { isMicOn = !isMicOn }
                        .background(
                            if (isMicOn) RitmoCream else RitmoCream.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMicOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = "Toggle mic",
                        tint = if (isMicOn) RitmoSage else RitmoCream
                    )
                }
            }

            // Board
            Box(modifier = Modifier.weight(1f)) {
                if (posts.isEmpty()) {
                    Text(
                        text = "No posts yet \u2014 be the first to share something.",
                        color = RitmoGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(posts) { post ->
                            PostCard(
                                post,
                                isOwnPost = post.authorId == auth.currentUser?.uid,
                                onAuthorClick = {
                                    selectedAuthorId = post.authorId
                                    selectedAuthorName = post.authorName
                                },
                                onDelete = { db.collection("posts").document(post.postId).delete() }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onNavigateToPost,
                    containerColor = RitmoSage,
                    contentColor = RitmoCream,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New post")
                }
            }

            // Bottom nav
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavIcon(Icons.Filled.Home, "Home", onClick = onNavigateHome)
                NavIcon(Icons.Filled.Groups, "Board", isSelected = true)
                NavIcon(Icons.Filled.Chat, "Messages", onClick = onNavigateToMessages)
                NavIcon(Icons.Filled.Notifications, "Notifications", onClick = onNavigateToNotifications)
                NavIcon(Icons.Filled.Person, "Profile", onClick = onNavigateToProfile)
            }
        }
    }
}

@Composable
private fun PostCard(post: BoardPost, isOwnPost: Boolean, onAuthorClick: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAuthorClick() }
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Author",
                    tint = RitmoSage,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    post.authorName,
                    color = RitmoSage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isOwnPost) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete post",
                    tint = RitmoGray,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDelete() }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val tagColor = if (post.category == "Lift") RitmoSage else RitmoGray
            Box(
                modifier = Modifier
                    .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(post.category, color = tagColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (post.isBoosted) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = "Boosted",
                    tint = RitmoSage,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(post.content, color = RitmoBlack, fontSize = 14.sp)

        val bitmap = remember(post.imageBase64) { base64ToBitmap(post.imageBase64) }
        if (bitmap != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Post photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(RitmoGray.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun AuthorProfileDialog(
    authorId: String,
    authorName: String,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var route by remember { mutableStateOf<String?>(null) }
    var streak by remember { mutableStateOf(0L) }

    LaunchedEffect(authorId) {
        db.collection("users").document(authorId).get()
            .addOnSuccessListener { doc ->
                route = doc.getString("homeRouteName")
                streak = doc.getLong("streakCount") ?: 0L
            }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    tint = RitmoSage,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(authorName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RitmoBlack)
                if (route != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Route: $route", color = RitmoGray, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$streak-day streak", color = RitmoGray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onMessageClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RitmoSage),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Message", color = RitmoCream)
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("Close", color = RitmoBlack) }
            }
        }
    }
}