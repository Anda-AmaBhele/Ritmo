package com.example.ritmo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ritmo.ui.ChatScreen
import com.example.ritmo.ui.HomeScreen
import com.example.ritmo.ui.LoginScreen
import com.example.ritmo.ui.MessagesListScreen
import com.example.ritmo.ui.NotificationsScreen
import com.example.ritmo.ui.PostToBoardScreen
import com.example.ritmo.ui.ProfileScreen
import com.example.ritmo.ui.RouteRoomScreen
import com.google.firebase.auth.FirebaseAuth

// Which screen is currently showing. No NavGraph yet — this is a simple
// manual "router" until real navigation gets added later.
private sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    data class Room(val routeName: String, val timeWindow: String) : Screen()
    data class Post(val routeName: String, val timeWindow: String) : Screen()
    object Messages : Screen()
    // origin remembers which screen to return to on back, since Chat can be
    // opened either from a room's post or from the Messages inbox.
    data class Chat(val otherUserId: String, val otherUserName: String, val origin: Screen) : Screen()
    object Notifications : Screen()
    object Profile : Screen()
}

class MainActivity : ComponentActivity() {

    // Asks the user to allow notifications (required on Android 13+)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Apply the saved language (English / isiZulu / isiXhosa) before the UI loads
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Watches for the network coming back and uploads posts saved offline
        SyncManager.start(this)

        // Push notifications: create the channel, ask permission, save this phone's token
        RitmoMessagingService.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            RitmoMessagingService.saveToken()
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // If the user is already signed in (e.g. the app restarted after a
                    // language change), skip the Login screen.
                    var screen by remember {
                        mutableStateOf<Screen>(
                            if (FirebaseAuth.getInstance().currentUser != null) Screen.Home
                            else Screen.Login
                        )
                    }
                    var lastRoom by remember { mutableStateOf<Screen.Room?>(null) }
                    var notificationsOrigin by remember { mutableStateOf<Screen>(Screen.Home) }
                    var profileOrigin by remember { mutableStateOf<Screen>(Screen.Home) }

                    when (val current = screen) {
                        is Screen.Login -> LoginScreen(
                            onLoginSuccess = {
                                // Save this phone's token for the newly logged-in user
                                RitmoMessagingService.saveToken()
                                screen = Screen.Home
                            }
                        )

                        is Screen.Home -> HomeScreen(
                            onNavigateToRoom = { routeName, timeWindow ->
                                val room = Screen.Room(routeName, timeWindow)
                                lastRoom = room
                                screen = room
                            },
                            onNavigateToMessages = { screen = Screen.Messages },
                            onNavigateToNotifications = {
                                notificationsOrigin = Screen.Home
                                screen = Screen.Notifications
                            },
                            onNavigateToProfile = {
                                profileOrigin = Screen.Home
                                screen = Screen.Profile
                            }
                        )

                        is Screen.Room -> RouteRoomScreen(
                            routeName = current.routeName,
                            timeWindow = current.timeWindow,
                            onNavigateToPost = {
                                screen = Screen.Post(current.routeName, current.timeWindow)
                            },
                            onNavigateHome = { screen = Screen.Home },
                            onNavigateToMessages = { screen = Screen.Messages },
                            onNavigateToNotifications = {
                                notificationsOrigin = current
                                screen = Screen.Notifications
                            },
                            onNavigateToProfile = {
                                profileOrigin = current
                                screen = Screen.Profile
                            },
                            onNavigateToChat = { otherUserId, otherUserName ->
                                screen = Screen.Chat(otherUserId, otherUserName, origin = current)
                            }
                        )

                        is Screen.Post -> PostToBoardScreen(
                            routeName = current.routeName,
                            timeWindow = current.timeWindow,
                            onPosted = {
                                screen = Screen.Room(current.routeName, current.timeWindow)
                            }
                        )

                        is Screen.Messages -> MessagesListScreen(
                            onOpenChat = { otherUserId, otherUserName ->
                                screen = Screen.Chat(otherUserId, otherUserName, origin = Screen.Messages)
                            },
                            onBack = { screen = lastRoom ?: Screen.Home }
                        )

                        is Screen.Chat -> ChatScreen(
                            otherUserId = current.otherUserId,
                            otherUserName = current.otherUserName,
                            onBack = { screen = current.origin }
                        )

                        is Screen.Notifications -> NotificationsScreen(
                            onBack = { screen = notificationsOrigin }
                        )

                        is Screen.Profile -> ProfileScreen(
                            onBack = { screen = profileOrigin },
                            onLoggedOut = { screen = Screen.Login }
                        )
                    }
                }
            }
        }
    }
}