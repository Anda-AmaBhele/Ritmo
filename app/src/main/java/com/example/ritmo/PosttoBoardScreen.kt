package com.example.ritmo.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
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
import com.example.ritmo.OfflineDb
import com.example.ritmo.R
import com.example.ritmo.SyncManager
import com.example.ritmo.ui.theme.RitmoBlack
import com.example.ritmo.ui.theme.RitmoCream
import com.example.ritmo.ui.theme.RitmoGray
import com.example.ritmo.ui.theme.RitmoSage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Categories are stored in English in Firestore; only the on-screen label is translated.
private val categories = listOf("Lift", "Accommodation", "Job", "Event")

@Composable
fun categoryLabel(category: String): String = when (category) {
    "Lift" -> stringResource(R.string.category_lift)
    "Accommodation" -> stringResource(R.string.category_accommodation)
    "Job" -> stringResource(R.string.category_job)
    "Event" -> stringResource(R.string.category_event)
    else -> category
}

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

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val roomId = remember(routeName, timeWindow) { roomIdFor(routeName, timeWindow) }

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var isBoosted by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageBase64 = uriToCompressedBase64(context, uri)
            if (imageBase64 == null) {
                errorMessage = context.getString(R.string.photo_failed)
            }
        }
    }

    fun post() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            errorMessage = context.getString(R.string.login_required)
            return
        }
        if (content.isBlank()) {
            errorMessage = context.getString(R.string.write_something)
            return
        }
        val authorName = auth.currentUser?.email?.substringBefore("@") ?: "Ritmo user"
        val boostedPost = isBoosted && selectedCategory == "Event"

        // OFFLINE: save to the local SQLite queue. SyncManager uploads it when the phone reconnects.
        if (!SyncManager.isOnline(context)) {
            OfflineDb(context).insert(
                roomId, uid, authorName, selectedCategory, content.trim(),
                boostedPost, imageBase64, System.currentTimeMillis()
            )
            Log.d("RitmoOffline", "No connection, post saved locally")
            Toast.makeText(
                context,
                context.getString(R.string.offline_saved),
                Toast.LENGTH_LONG
            ).show()
            onPosted()
            return
        }

        // ONLINE: post straight to Firestore
        isPosting = true
        val data = mutableMapOf<String, Any?>(
            "roomId" to roomId,
            "authorId" to uid,
            "authorName" to authorName,
            "category" to selectedCategory,
            "content" to content.trim(),
            "isBoosted" to boostedPost,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (imageBase64 != null) {
            data["imageBase64"] = imageBase64
        }
        db.collection("posts").add(data)
            .addOnCompleteListener { result ->
                isPosting = false
                if (result.isSuccessful) {
                    onPosted()
                } else {
                    errorMessage = context.getString(
                        R.string.post_failed,
                        result.exception?.message ?: context.getString(R.string.check_connection)
                    )
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
                contentDescription = stringResource(R.string.back),
                tint = RitmoBlack,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.new_post),
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
                        categoryLabel(category),
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
                listOf(R.string.organic to false, R.string.boosted to true).forEach { (labelRes, boosted) ->
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
                        Text(stringResource(labelRes), color = RitmoBlack, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Photo attach row — useful for selling items, showing a lift's car, etc.
        val previewBitmap = remember(imageBase64) { base64ToBitmap(imageBase64) }
        if (previewBitmap != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.selected_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(RitmoGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_photo),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(RitmoBlack.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .clickable { imageBase64 = null }
                        .padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = RitmoSage)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_photo), color = RitmoBlack)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text(stringResource(R.string.post_hint)) },
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
            Text(errorMessage ?: "", color = Color.Red, fontSize = 13.sp)
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
                Text(stringResource(R.string.post_button), color = RitmoCream)
            }
        }
    }
}