package com.suyziss.feed.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.suyziss.feed.data.AppTheme
import com.suyziss.feed.data.FeedStore
import com.suyziss.feed.data.Overlay
import com.suyziss.feed.data.Tab
import kotlinx.coroutines.launch

@Composable
fun FeedApp(initialVideoId: String? = null) {
    val context = LocalContext.current
    val store = remember { FeedStore(context) }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.None) }
    var selectedPostId by rememberSaveable { mutableStateOf(initialVideoId) }

    val dark = when (store.theme) {
        AppTheme.Dark -> true
        AppTheme.Light -> false
        AppTheme.System -> view.context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    val colors = if (dark) darkColorScheme(
        primary = Color(0xFFE94359),
        onPrimary = Color.White,
        background = Color(0xFF000000),
        onBackground = Color.White,
        surface = Color(0xFF121212),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFFB0B0B0),
    ) else lightColorScheme(
        primary = Color(0xFFE94359),
        onPrimary = Color.White,
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFF8F8F8),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFEEEEEE),
        onSurfaceVariant = Color(0xFF666666),
    )

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
    }

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            if (!store.isLoggedIn) {
                AuthScreen(store)
                return@Surface
            }

            BackHandler(enabled = overlay != Overlay.None || selectedPostId != null) {
                when {
                    selectedPostId != null -> selectedPostId = null
                    overlay != Overlay.None -> overlay = Overlay.None
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (overlay == Overlay.None && selectedPostId == null) {
                        FeedBottomBar(tab = tab, dark = dark, onSelect = { tab = it })
                    }
                },
                containerColor = colors.background,
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (tab) {
                        Tab.Home -> HomeScreen(store, onOpenPost = { selectedPostId = it })
                        Tab.Friends -> FriendsScreen()
                        Tab.Create -> CreateScreen(
                            store = store,
                            onPublished = {
                                tab = Tab.Profile
                                scope.launch { snackbar.showSnackbar("Published") }
                            },
                            onError = { scope.launch { snackbar.showSnackbar(it) } },
                        )
                        Tab.Inbox -> InboxScreen()
                        Tab.Profile -> ProfileScreen(
                            store = store,
                            onOpenSettings = { overlay = Overlay.Settings },
                            onOpenEdit = { overlay = Overlay.EditProfile },
                            onOpenFollowers = { overlay = Overlay.Followers },
                            onOpenFollowing = { overlay = Overlay.Following },
                            onOpenLikes = { overlay = Overlay.Likes },
                            onOpenPost = { selectedPostId = it },
                        )
                    }

                    selectedPostId?.let { id ->
                        PostDetailScreen(
                            store = store,
                            postId = id,
                            onBack = { selectedPostId = null },
                            onDeleted = {
                                selectedPostId = null
                                scope.launch { snackbar.showSnackbar("Deleted") }
                            },
                        )
                    }

                    when (overlay) {
                        Overlay.Settings -> SettingsScreen(
                            store = store,
                            onBack = { overlay = Overlay.None },
                            onOpenTelegram = { openTelegram(context) },
                        )
                        Overlay.EditProfile -> EditProfileScreen(
                            store = store,
                            onBack = { overlay = Overlay.None },
                            onSaved = {
                                overlay = Overlay.None
                                scope.launch { snackbar.showSnackbar("Profile updated") }
                            },
                        )
                        Overlay.Followers -> CountListScreen(
                            title = "Followers",
                            empty = "No followers yet",
                            onBack = { overlay = Overlay.None },
                        )
                        Overlay.Following -> CountListScreen(
                            title = "Following",
                            empty = "Not following anyone",
                            onBack = { overlay = Overlay.None },
                        )
                        Overlay.Likes -> LikesScreen(
                            store = store,
                            onBack = { overlay = Overlay.None },
                            onOpenPost = {
                                overlay = Overlay.None
                                selectedPostId = it
                            },
                        )
                        Overlay.None -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedBottomBar(tab: Tab, dark: Boolean, onSelect: (Tab) -> Unit) {
    val bg = if (dark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val muted = Color(0xFF8A8A8A)
    val active = if (dark) Color.White else Color.Black

    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        BottomItem("Home", Icons.Filled.Home, tab == Tab.Home, active, muted) { onSelect(Tab.Home) }
        BottomItem("Friends", Icons.Filled.People, tab == Tab.Friends, active, muted) { onSelect(Tab.Friends) }
        Box(
            Modifier.size(48.dp).clickable { onSelect(Tab.Create) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .then(if (dark) Modifier else Modifier.shadow(4.dp, RoundedCornerShape(10.dp)))
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (dark) Color.White else Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Create",
                    tint = if (dark) Color.Black else Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        BottomItem("Inbox", Icons.Filled.Notifications, tab == Tab.Inbox, active, muted) { onSelect(Tab.Inbox) }
        BottomItem("Profile", Icons.Filled.Person, tab == Tab.Profile, active, muted) { onSelect(Tab.Profile) }
    }
}

@Composable
private fun BottomItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    active: Color,
    muted: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) active else muted, modifier = Modifier.size(24.dp))
        Text(
            label,
            color = if (selected) active else muted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun openTelegram(context: android.content.Context) {
    val uri = Uri.parse("https://t.me/suyziss")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
