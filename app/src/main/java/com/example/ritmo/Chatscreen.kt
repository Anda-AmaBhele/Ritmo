package com.example.ritmo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val text: String = ""
)

@Composable
fun ChatScreen(
    otherUserId: String,
    otherUserName: String,
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = auth.currentUser?.uid

    BackHandler { onBack() }

    val conversationId = remember(myUid, otherUserId) {
        if (myUid != null) conversationIdFor(myUid, otherUserId) else ""
    }

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Make sure the conversation document exists (with both people's names
    // stored) so it can show up correctly in the Messages inbox for either side.
    val myName = remember { auth.currentUser?.email?.substringBefore("@") ?: "Ritmo user" }

    LaunchedEffect(conversationId) {
        if (myUid == null || conversationId.isBlank()) return@LaunchedEffect
        db.collection("conversations").document(conversationId).set(
            mapOf(
                "participantIds" to listOf(myUid, otherUserId),
                "participantNames" to mapOf(myUid to myName, otherUserId to otherUserName)
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    DisposableEffect(conversationId) {
        if (conversationId.isBlank()) return@DisposableEffect onDispose {}
        val listener: ListenerRegistration = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                messages = snapshot?.documents?.map { doc ->
                    ChatMessage(
                        messageId = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        text = doc.getString("text") ?: ""
                    )
                } ?: emptyList()
                if (messages.isNotEmpty()) {
                    coroutineScope.launch { listState.animateScrollToItem(messages.size - 1) }
                }
            }
        onDispose { listener.remove() }
    }

    fun send() {
        val text = draft.trim()
        if (text.isBlank() || myUid == null || conversationId.isBlank()) return
        isSending = true
        val convoRef = db.collection("conversations").document(conversationId)
        convoRef.collection("messages").add(
            mapOf(
                "senderId" to myUid,
                "text" to text,
                "sentAt" to FieldValue.serverTimestamp()
            )
        ).addOnCompleteListener {
            isSending = false
            draft = ""
        }
        convoRef.set(
            mapOf(
                "participantIds" to listOf(myUid, otherUserId),
                "participantNames" to mapOf(myUid to myName, otherUserId to otherUserName),
                "lastMessage" to text,
                "lastMessageAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RitmoCream)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RitmoSage)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = RitmoCream,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                otherUserName,
                color = RitmoCream,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isMine = message.senderId == myUid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMine) RitmoSage else Color.White,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .widthIn(max = 260.dp)
                    ) {
                        Text(
                            message.text,
                            color = if (isMine) RitmoCream else RitmoBlack,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RitmoSage,
                    unfocusedBorderColor = RitmoGray
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { send() },
                enabled = !isSending && draft.isNotBlank()
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = RitmoSage)
            }
        }
    }
}