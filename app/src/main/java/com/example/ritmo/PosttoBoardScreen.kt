package com.example.ritmo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

private val categories = listOf("Lift", "Accommodation", "Job", "Event")

@Composable
fun PostToBoardScreen(
    routeName: String,
    timeWindow: String,
    onPosted: () -> Unit,
    onBack: () -> Unit = onPosted
) {
    // Makes the phone's physical/gesture back action leave this screen
    // instead of closing the app.
    BackHandler { onBack() }

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val roomId = remember(routeName, timeWindow) { roomIdFor(routeName, timeWindow) }

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var isBoosted by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun post() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            errorMessage = "You need to be logged in to post."
            return
        }
        if (content.isBlank()) {
            errorMessage = "Write something before posting."
            return
        }
        isPosting = true
        val authorName = auth.currentUser?.email?.substringBefore("@") ?: "Ritmo user"
        val data = mapOf(
            "roomId" to roomId,
            "authorId" to uid,
            "authorName" to authorName,
            "category" to selectedCategory,
            "content" to content.trim(),
            "isBoosted" to (isBoosted && selectedCategory == "Event"),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("posts").add(data)
            .addOnCompleteListener { result ->
                isPosting = false
                if (result.isSuccessful) {
                    onPosted()
                } else {
                    errorMessage = "Couldn't post right now. Check your connection."
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RitmoCream)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = RitmoBlack,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "New post",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = RitmoBlack
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                val selected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) RitmoSage else Color.White,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        category,
                        color = if (selected) RitmoCream else RitmoBlack,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (selectedCategory == "Event") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RitmoSage.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                listOf("Organic" to false, "Boosted" to true).forEach { (label, boosted) ->
                    val selected = isBoosted == boosted
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) Color.White else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { isBoosted = boosted }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = RitmoBlack, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Share something with your route room...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RitmoSage,
                unfocusedBorderColor = RitmoGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMessage ?: "", color = androidx.compose.ui.graphics.Color.Red, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { post() },
            enabled = !isPosting,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RitmoBlack),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isPosting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = RitmoCream,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Post", color = RitmoCream)
            }
        }
    }
}