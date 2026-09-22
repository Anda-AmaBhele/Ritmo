package com.example.ritmo.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ritmo.ui.theme.RitmoBlack
import com.example.ritmo.ui.theme.RitmoCream
import com.example.ritmo.ui.theme.RitmoGray
import com.example.ritmo.ui.theme.RitmoSage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private val languages = listOf("English", "isiZulu", "isiXhosa", "Sesotho", "Setswana")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = auth.currentUser?.uid
    val email = auth.currentUser?.email ?: ""

    BackHandler { onBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(languages[0]) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var roomUpdatesEnabled by remember { mutableStateOf(true) }
    var streakRemindersEnabled by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var profilePictureBase64 by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Profile pictures can be a bit smaller than post photos.
            val encoded = uriToCompressedBase64(context, uri, maxDimensionPx = 300, quality = 60)
            if (encoded != null) {
                profilePictureBase64 = encoded
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("Couldn't load that photo.") }
            }
        }
    }

    LaunchedEffect(myUid) {
        if (myUid == null) return@LaunchedEffect
        db.collection("users").document(myUid).get().addOnSuccessListener { doc ->
            displayName = doc.getString("displayName") ?: email.substringBefore("@")
            language = doc.getString("language") ?: languages[0]
            roomUpdatesEnabled = doc.getBoolean("roomUpdatesEnabled") ?: true
            streakRemindersEnabled = doc.getBoolean("streakRemindersEnabled") ?: true
            profilePictureBase64 = doc.getString("profilePictureBase64")
        }
    }

    fun saveSettings() {
        val uid = myUid ?: return
        isSaving = true
        val data = mutableMapOf<String, Any?>(
            "displayName" to displayName,
            "language" to language,
            "roomUpdatesEnabled" to roomUpdatesEnabled,
            "streakRemindersEnabled" to streakRemindersEnabled
        )
        if (profilePictureBase64 != null) {
            data["profilePictureBase64"] = profilePictureBase64
        }
        db.collection("users").document(uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnCompleteListener { result ->
                isSaving = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (result.isSuccessful) "Settings saved!" else "Couldn't save right now."
                    )
                }
            }
    }

    Scaffold(
        containerColor = RitmoCream,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RitmoBlack,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = RitmoBlack)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Profile picture
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val avatarBitmap = remember(profilePictureBase64) { base64ToBitmap(profilePictureBase64) }
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Profile picture",
                            tint = RitmoGray,
                            modifier = Modifier.size(96.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(RitmoSage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account card
            SectionCard(title = "Account") {
                Text("Name", fontWeight = FontWeight.Bold, color = RitmoBlack, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RitmoSage,
                        unfocusedBorderColor = RitmoGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Email", fontWeight = FontWeight.Bold, color = RitmoBlack, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(email, color = RitmoGray, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preferences card
            SectionCard(title = "Preferences") {
                Text("Language", fontWeight = FontWeight.Bold, color = RitmoBlack, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = languageMenuExpanded,
                    onExpandedChange = { languageMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = language,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RitmoSage,
                            unfocusedBorderColor = RitmoGray
                        ),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = { language = lang; languageMenuExpanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Route room updates", color = RitmoBlack, fontSize = 14.sp)
                    Switch(
                        checked = roomUpdatesEnabled,
                        onCheckedChange = { roomUpdatesEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = RitmoSage, checkedTrackColor = RitmoSage.copy(alpha = 0.5f))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Streak reminders", color = RitmoBlack, fontSize = 14.sp)
                    Switch(
                        checked = streakRemindersEnabled,
                        onCheckedChange = { streakRemindersEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = RitmoSage, checkedTrackColor = RitmoSage.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { saveSettings() },
                enabled = !isSaving,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RitmoSage),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = RitmoCream,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save settings")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    auth.signOut()
                    onLoggedOut()
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RitmoBlack),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Log out", color = RitmoCream)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = RitmoBlack, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}