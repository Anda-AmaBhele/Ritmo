package com.example.ritmo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
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

data class ConversationPreview(
    val conversationId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val lastMessage: String = ""
)

@Composable
fun MessagesListScreen(
    onOpenChat: (otherUserId: String, otherUserName: String) -> Unit,
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = auth.currentUser?.uid

    BackHandler { onBack() }

    var conversations by remember { mutableStateOf<List<ConversationPreview>>(emptyList()) }

    DisposableEffect(myUid) {
        if (myUid == null) return@DisposableEffect onDispose {}
        val listener: ListenerRegistration = db.collection("conversations")
            .whereArrayContains("participantIds", myUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                conversations = snapshot?.documents?.mapNotNull { doc ->
                    val ids = doc.get("participantIds") as? List<*> ?: return@mapNotNull null
                    val otherId = ids.firstOrNull { it != myUid } as? String ?: return@mapNotNull null
                    val names = doc.get("participantNames") as? Map<*, *>
                    val otherName = names?.get(otherId) as? String ?: "Ritmo user"
                    ConversationPreview(
                        conversationId = doc.id,
                        otherUserId = otherId,
                        otherUserName = otherName,
                        lastMessage = doc.getString("lastMessage") ?: ""
                    )
                } ?: emptyList()
            }
        onDispose { listener.remove() }
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
            Text("Messages", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = RitmoBlack)
        }

        if (conversations.isEmpty()) {
            Text(
                "No conversations yet \u2014 message someone from a post in a room to start one.",
                color = RitmoGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations) { convo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .clickable { onOpenChat(convo.otherUserId, convo.otherUserName) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Contact",
                            tint = RitmoSage,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(convo.otherUserName, fontWeight = FontWeight.Bold, color = RitmoBlack, fontSize = 15.sp)
                            if (convo.lastMessage.isNotBlank()) {
                                Text(convo.lastMessage, color = RitmoGray, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}