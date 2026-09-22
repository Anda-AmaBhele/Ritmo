package com.example.ritmo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ritmo.ui.ChatScreen
import com.example.ritmo.ui.HomeScreen
import com.example.ritmo.ui.LoginScreen
import com.example.ritmo.ui.MessagesListScreen
import com.example.ritmo.ui.NotificationsScreen
import com.example.ritmo.ui.PostToBoardScreen
import com.example.ritmo.ui.ProfileScreen
import com.example.ritmo.ui.RouteRoomScreen

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Login) }
                    var lastRoom by remember { mutableStateOf<Screen.Room?>(null) }
                    var notificationsOrigin by remember { mutableStateOf<Screen>(Screen.Home) }
                    var profileOrigin by remember { mutableStateOf<Screen>(Screen.Home) }

                    when (val current = screen) {
                        is Screen.Login -> LoginScreen(
                            onLoginSuccess = { screen = Screen.Home }
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