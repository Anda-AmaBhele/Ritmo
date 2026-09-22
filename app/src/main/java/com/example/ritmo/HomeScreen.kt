package com.example.ritmo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ritmo.ui.theme.RitmoBlack
import com.example.ritmo.ui.theme.RitmoCream
import com.example.ritmo.ui.theme.RitmoGray
import com.example.ritmo.ui.theme.RitmoSage
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToRoom: (routeName: String, timeWindow: String) -> Unit = { _, _ -> },
    onNavigateToMessages: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedRoute by remember { mutableStateOf<String?>(null) }
    var hour by remember { mutableStateOf<Int?>(null) }
    var minute by remember { mutableStateOf<Int?>(null) }
    var streakCount by remember { mutableStateOf(0L) }
    var isSaving by remember { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }

    val timeLabel = if (hour != null && minute != null) "%02d:%02d".format(hour, minute) else null

    // --- Current-location detection ---
    val context = LocalContext.current
    var isLocating by remember { mutableStateOf(false) }

    fun fetchCurrentLocationIntoRoute() {
        isLocating = true
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    isLocating = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Couldn't get your location. Try moving to an open area.")
                    }
                    return@addOnSuccessListener
                }
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val place = addresses?.firstOrNull()
                    selectedRoute = place?.let {
                        listOfNotNull(it.thoroughfare, it.subLocality ?: it.locality).joinToString(", ")
                    } ?: "${location.latitude}, ${location.longitude}"
                } catch (e: Exception) {
                    selectedRoute = "${location.latitude}, ${location.longitude}"
                }
                isLocating = false
            }.addOnFailureListener {
                isLocating = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Couldn't get your location right now.")
                }
            }
        } catch (e: SecurityException) {
            isLocating = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCurrentLocationIntoRoute()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Location permission is needed to auto-fill your route.")
            }
        }
    }

    fun requestCurrentLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            fetchCurrentLocationIntoRoute()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    // Load the user's saved preferences and streak live, so changes made
    // elsewhere (e.g. streak updating when you join a room) show up here
    // immediately instead of needing a fresh page load.
    DisposableEffect(userId) {
        if (userId == null) return@DisposableEffect onDispose {}
        val listener = db.collection("users").document(userId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null) return@addSnapshotListener
                selectedRoute = doc.getString("homeRouteName")
                streakCount = doc.getLong("streakCount") ?: 0L
                val saved = doc.getString("preferredTimeWindow")
                if (saved != null && saved.contains(":")) {
                    val parts = saved.split(":")
                    hour = parts.getOrNull(0)?.toIntOrNull()
                    minute = parts.getOrNull(1)?.toIntOrNull()
                }
            }
        onDispose { listener.remove() }
    }

    fun confirm() {
        if (userId == null || selectedRoute == null || timeLabel == null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please choose a route and a time.")
            }
            return
        }
        isSaving = true
        val data = mapOf(
            "homeRouteName" to selectedRoute,
            "preferredTimeWindow" to timeLabel
        )
        db.collection("users").document(userId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnCompleteListener { result ->
                isSaving = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (result.isSuccessful) "Saved!" else "Couldn't save right now. Check your connection."
                    )
                }
            }
    }

    if (showTimePicker) {
        RitmoTimePickerDialog(
            initialHour = hour ?: 7,
            initialMinute = minute ?: 0,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m -> hour = h; minute = m; showTimePicker = false }
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
                .padding(24.dp)
        ) {

            Text(
                text = greetingForNow(),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = RitmoBlack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Streak card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RitmoSage, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Streak",
                        tint = RitmoCream
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$streakCount-day streak",
                        color = RitmoCream,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Route + time card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Route", fontWeight = FontWeight.Bold, color = RitmoBlack)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = !isLocating) { requestCurrentLocation() }
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = RitmoSage,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.MyLocation,
                                    contentDescription = "Use my current location",
                                    tint = RitmoSage,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use current location", color = RitmoSage, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = selectedRoute ?: "",
                        onValueChange = { selectedRoute = it },
                        placeholder = { Text("Type your route, or use current location above") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RitmoSage,
                            unfocusedBorderColor = RitmoGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Time", fontWeight = FontWeight.Bold, color = RitmoBlack)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Any 24-hour time \u2014 not limited to mornings",
                        color = RitmoGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(timeLabel ?: "Choose a time", color = RitmoBlack)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { confirm() },
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
                            Text("Confirm")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom nav bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NavIcon(Icons.Filled.Home, "Home", isSelected = true)
                    NavIcon(
                        Icons.Filled.Groups,
                        "Room",
                        onClick = {
                            val route = selectedRoute?.takeIf { it.isNotBlank() } ?: "General"
                            val time = timeLabel ?: "Anytime"
                            onNavigateToRoom(route, time)
                        }
                    )
                    NavIcon(Icons.Filled.Chat, "Messages", onClick = onNavigateToMessages)
                    NavIcon(Icons.Filled.Notifications, "Notifications", onClick = onNavigateToNotifications)
                    NavIcon(Icons.Filled.Person, "Profile", onClick = onNavigateToProfile)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RitmoTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(state.hour, state.minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = RitmoSage)
                    ) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun NavIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val color = if (isSelected) RitmoSage else RitmoGray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color)
        Text(label, color = color, fontSize = 12.sp)
    }
}