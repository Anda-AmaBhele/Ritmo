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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ritmo.OfflineDb
import com.example.ritmo.R
import com.example.ritmo.ui.theme.RitmoBlack
import com.example.ritmo.ui.theme.RitmoCream
import com.example.ritmo.ui.theme.RitmoGray
import com.example.ritmo.ui.theme.RitmoSage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
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
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val roomId = remember(routeName, timeWindow) { roomIdFor(routeName, timeWindow) }

    var memberCount by remember { mutableStateOf(0) }
    var isMicOn by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<BoardPost>>(emptyList()) }
    var selectedAuthorId by remember { mutableStateOf<String?>(null) }
    var selectedAuthorName by remember { mutableStateOf<String>("") }
    var pendingCount by remember { mutableStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler { onNavigateHome() }

    // Check the local offline queue every 2 seconds so the banner disappears once posts sync
    LaunchedEffect(roomId) {
        while (true) {
            pendingCount = OfflineDb(context).countForRoom(roomId)
            delay(2000)
        }
    }

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
                    // Most likely cause: Firestore needs a composite index for this query.
                    // The real error message includes a direct link to create it.
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.couldnt_load_posts,
                                error.message ?: context.getString(R.string.unknown_error)
                            )
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
                        contentDescription = stringResource(R.string.back),
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
                    text = stringResource(R.string.room_members, timeWindow, memberCount),
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
                        contentDescription = stringResource(R.string.toggle_mic),
                        tint = if (isMicOn) RitmoSage else RitmoCream
                    )
                }
            }

            // Offline banner: shows while posts are waiting in the local queue
            if (pendingCount > 0) {
                Text(
                    text = stringResource(R.string.waiting_sync, pendingCount),
                    color = RitmoBlack,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RitmoSage.copy(alpha = 0.2f))
                        .padding(10.dp)
                )
            }

            // Board
            Box(modifier = Modifier.weight(1f)) {
                if (posts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_posts),
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
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_post))
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
                NavIcon(Icons.Filled.Home, stringResource(R.string.nav_home), onClick = onNavigateHome)
                NavIcon(Icons.Filled.Groups, stringResource(R.string.nav_room), isSelected = true)
                NavIcon(Icons.Filled.Chat, stringResource(R.string.nav_messages), onClick = onNavigateToMessages)
                NavIcon(
                    Icons.Filled.Notifications,
                    stringResource(R.string.nav_notifications),
                    onClick = onNavigateToNotifications
                )
                NavIcon(Icons.Filled.Person, stringResource(R.string.nav_profile), onClick = onNavigateToProfile)
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
                    contentDescription = stringResource(R.string.author),
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
                    contentDescription = stringResource(R.string.delete_post),
                    tint = RitmoGray,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDelete() }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // The stored category stays in English; only the label is translated
            val tagColor = if (post.category == "Lift") RitmoSage else RitmoGray
            Box(
                modifier = Modifier
                    .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    categoryLabel(post.category),
                    color = tagColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (post.isBoosted) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = stringResource(R.string.boosted),
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
                contentDescription = stringResource(R.string.post_photo),
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
                    contentDescription = stringResource(R.string.nav_profile),
                    tint = RitmoSage,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(authorName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RitmoBlack)
                val routeShown = route
                if (routeShown != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.route_label, routeShown),
                        color = RitmoGray,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.day_streak, streak), color = RitmoGray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onMessageClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RitmoSage),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.message_button), color = RitmoCream)
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close), color = RitmoBlack)
                }
            }
        }
    }
}