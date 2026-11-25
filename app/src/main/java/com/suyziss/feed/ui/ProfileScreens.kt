package com.suyziss.feed.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.suyziss.feed.data.FeedStore
import com.suyziss.feed.data.LocalPost
import com.suyziss.feed.data.ProfileSection
import com.suyziss.feed.data.normalizeUsername
import com.suyziss.feed.data.isUsernameValid
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(
    store: FeedStore,
    onOpenSettings: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenLikes: () -> Unit,
    onOpenPost: (String) -> Unit,
) {
    val profile = store.profile
    var section by remember { mutableStateOf(ProfileSection.Posts) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                profile.username,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth < 360.dp
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileAvatar(
                    path = profile.avatarFile,
                    size = if (narrow) 88.dp else 96.dp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    profile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "@${profile.username}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                if (profile.bio.isNotBlank()) {
                    Text(
                        profile.bio,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Stats row - full width, no letter wrap
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem(
                        count = store.posts.size.toString(),
                        label = "Posts",
                        onClick = { section = ProfileSection.Posts },
                    )
                    StatItem(
                        count = "0",
                        label = "Followers",
                        onClick = onOpenFollowers,
                    )
                    StatItem(
                        count = "0",
                        label = "Following",
                        onClick = onOpenFollowing,
                    )
                    StatItem(
                        count = store.likesCount.toString(),
                        label = "Likes",
                        onClick = onOpenLikes,
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onOpenEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Edit profile")
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Settings")
                    }
                }
            }
        }

        // Tabs
        ProfileTabs(section = section, onSelect = { section = it })

        // Content grid
        val filtered = when (section) {
            ProfileSection.Posts -> store.posts.filter { !it.privatePost }
            ProfileSection.Private -> store.posts.filter { it.privatePost }
            ProfileSection.Reposts -> store.posts.filter { it.reposted }
            ProfileSection.Saved -> store.posts.filter { it.bookmarked }
            ProfileSection.Liked -> store.posts.filter { it.liked }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    when (section) {
                        ProfileSection.Posts -> "No posts yet"
                        ProfileSection.Private -> "No private posts"
                        ProfileSection.Reposts -> "No reposts"
                        ProfileSection.Saved -> "No saved posts"
                        ProfileSection.Liked -> "No liked posts"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(filtered, key = { it.id }) { post ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .background(Color.Black)
                            .clickable { onOpenPost(post.id) },
                    ) {
                        MediaPreview(
                            path = store.fileOf(post).absolutePath,
                            isVideo = post.video,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(count: String, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            count,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ProfileTabs(section: ProfileSection, onSelect: (ProfileSection) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        listOf(
            ProfileSection.Posts to Icons.Outlined.GridView,
            ProfileSection.Private to Icons.Outlined.Lock,
            ProfileSection.Reposts to Icons.Outlined.Repeat,
            ProfileSection.Saved to Icons.Outlined.BookmarkBorder,
            ProfileSection.Liked to Icons.Outlined.FavoriteBorder,
        ).forEach { (sec, icon) ->
            val selected = section == sec
            IconButton(onClick = { onSelect(sec) }) {
                Icon(
                    icon,
                    contentDescription = sec.name,
                    tint = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ProfileAvatar(path: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (path.isNotBlank() && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    store: FeedStore,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(store.profile.name) }
    var username by remember { mutableStateOf(store.profile.username) }
    var bio by remember { mutableStateOf(store.profile.bio) }
    var newAvatar by remember { mutableStateOf<Uri?>(null) }
    var removeAvatar by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            newAvatar = uri
            removeAvatar = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Edit profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    if (saving) return@TextButton
                    if (!isUsernameValid(username)) return@TextButton
                    saving = true
                    scope.launch {
                        val ok = store.updateProfile(
                            name = name,
                            username = normalizeUsername(username),
                            bio = bio,
                            newAvatar = newAvatar,
                            removeAvatar = removeAvatar,
                        )
                        saving = false
                        if (ok) onSaved()
                    }
                },
                enabled = !saving && isUsernameValid(username),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Save")
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            ProfileAvatar(
                path = when {
                    removeAvatar -> ""
                    newAvatar != null -> newAvatar.toString()
                    else -> store.profile.avatarFile
                },
                size = 96.dp,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Change photo") }
            if (store.profile.avatarFile.isNotBlank() || newAvatar != null) {
                TextButton(onClick = {
                    newAvatar = null
                    removeAvatar = true
                }) { Text("Remove") }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 30) name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { if (it.length <= 24) username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = username.isNotEmpty() && !isUsernameValid(username),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 50) bio = it },
            label = { Text("Bio (max 50)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            supportingText = { Text("${bio.length}/50") },
        )
    }
}

@Composable
fun PostDetailScreen(
    store: FeedStore,
    postId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val post = store.posts.find { it.id == postId }
    if (post == null) {
        onBack()
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(Color.Black),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                store.delete(postId)
                onDeleted()
            }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Delete", tint = Color.White)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            MediaPreview(
                path = store.fileOf(post).absolutePath,
                isVideo = post.video,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(Modifier.padding(16.dp)) {
            if (post.caption.isNotBlank()) {
                Text(post.caption, color = Color.White)
            }
            Text(
                formatWhen(post.createdAt),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
            Row(Modifier.padding(top = 8.dp)) {
                IconButton(onClick = { store.toggleLike(postId) }) {
                    Icon(
                        if (post.liked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.liked) Color.Red else Color.White,
                    )
                }
                IconButton(onClick = { store.toggleBookmark(postId) }) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (post.bookmarked) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun CountListScreen(
    title: String,
    empty: String,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LikesScreen(
    store: FeedStore,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
) {
    val liked = store.posts.filter { it.liked }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Likes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (liked.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No liked posts", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(liked, key = { it.id }) { post ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .background(Color.Black)
                            .clickable { onOpenPost(post.id) },
                    ) {
                        MediaPreview(
                            path = store.fileOf(post).absolutePath,
                            isVideo = post.video,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
