package com.suyziss.feed.ui

import android.content.Intent
import androidx.compose.foundation.layout.fillParentMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suyziss.feed.data.AppTheme
import com.suyziss.feed.data.FeedStore
import com.suyziss.feed.data.LocalPost
import com.suyziss.feed.data.LocalComment
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(store: FeedStore, onOpenPost: (String) -> Unit) {
    val posts = store.posts.filter { !it.privatePost }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var commentsFor by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (posts.isEmpty()) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("For You", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("No posts yet.\nCreate a video from the + button.", color = Color.LightGray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 0.dp),
            ) {
                itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                    Box(
                        Modifier.fillParentMaxSize().background(Color.Black),
                    ) {
                        MediaPreview(
                            path = store.fileOf(post).absolutePath,
                            isVideo = post.video,
                            play = post.video && index == listState.firstVisibleItemIndex,
                            modifier = Modifier.fillMaxSize(),
                            description = "Video by @${store.profile.username}",
                        )

                        Row(
                            Modifier.align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text("For You", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        Column(
                            Modifier.align(Alignment.BottomStart)
                                .fillMaxWidth(0.78f)
                                .padding(start = 16.dp, end = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text("@${store.profile.username}", color = Color.White, fontWeight = FontWeight.Bold)
                            if (post.caption.isNotBlank()) {
                                Text(post.caption, color = Color.White, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                            Text(formatWhen(post.createdAt), color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                        }

                        Column(
                            Modifier.align(Alignment.BottomEnd)
                                .padding(end = 10.dp, bottom = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(13.dp),
                        ) {
                            FeedAction(
                                icon = if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                label = if (post.liked) "1" else "Like",
                                tint = if (post.liked) Color(0xFFFF365D) else Color.White,
                                onClick = { store.toggleLike(post.id) },
                            )
                            FeedAction(
                                icon = Icons.Filled.ChatBubbleOutline,
                                label = store.commentsFor(post.id).size.toString(),
                                onClick = { commentsFor = post.id },
                            )
                            FeedAction(
                                icon = if (post.reposted) Icons.Filled.Repeat else Icons.Filled.Repeat,
                                label = if (post.reposted) "Shared" else "Repost",
                                tint = if (post.reposted) Color(0xFF40D9A0) else Color.White,
                                onClick = { store.toggleRepost(post.id) },
                            )
                            FeedAction(
                                icon = if (post.bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                label = "Save",
                                tint = if (post.bookmarked) Color(0xFFFFD45C) else Color.White,
                                onClick = { store.toggleBookmark(post.id) },
                            )
                            FeedAction(
                                icon = Icons.Filled.Share,
                                label = "Share",
                                onClick = {
                                    val link = "suyzissfeed://video/${post.id}"
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Suyziss Feed video: $link")
                                    }
                                    context.startActivity(Intent.createChooser(send, "Share video"))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    commentsFor?.let { postId ->
        CommentsDialog(
            store = store,
            postId = postId,
            onDismiss = { commentsFor = null },
        )
    }
}

@Composable
private fun FeedAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(30.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun CommentsDialog(
    store: FeedStore,
    postId: String,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<String?>(null) }
    val comments = store.commentsFor(postId)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments · ${comments.size}") },
        text = {
            Column {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                    val roots = comments.filter { it.parentId == null }
                    items(roots, key = { it.id }) { comment ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("@${comment.author}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(comment.text)
                            TextButton(onClick = { replyTo = comment.id }) { Text("Reply") }
                            comments.filter { it.parentId == comment.id }.forEach { reply ->
                                Column(Modifier.padding(start = 18.dp, bottom = 5.dp)) {
                                    Text("@${reply.author} · reply", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    Text(reply.text)
                                    TextButton(onClick = { replyTo = reply.id }) { Text("Reply") }
                                }
                            }
                        }
                    }
                    if (roots.isEmpty()) {
                        item { Text("No comments yet. Start the conversation.") }
                    }
                }
                if (replyTo != null) {
                    Text("Replying to a comment", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 500) draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a comment…") },
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = draft.isNotBlank(),
                onClick = {
                    store.addComment(postId, draft, replyTo)
                    draft = ""
                    replyTo = null
                },
            ) { Text("Send") }
        },
        dismissButton = {
            Row {
                if (replyTo != null) TextButton(onClick = { replyTo = null }) { Text("Cancel reply") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
fun FriendsScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Friends feed coming soon",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun InboxScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "No notifications yet",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    store: FeedStore,
    onPublished: () -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var privatePost by remember { mutableStateOf(false) }
    var draftPath by remember { mutableStateOf<String?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var publishing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val video = uri.toString().contains("video") ||
                (context.contentResolver.getType(uri)?.startsWith("video") == true)
            val path = store.stageDraft(uri, video)
            if (path == null) {
                onError("Failed to load media")
            } else {
                draftPath = path
                isVideo = video
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (draftPath == null) {
            Button(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
                    Text("Select photo or video")
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
            ) {
                MediaPreview(
                    path = draftPath!!,
                    isVideo = isVideo,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = {
                        store.discardDraft(draftPath)
                        draftPath = null
                    },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 120) caption = it },
                label = { Text("Caption") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Private post")
                Switch(checked = privatePost, onCheckedChange = { privatePost = it })
            }

            Button(
                onClick = {
                    if (publishing) return@Button
                    publishing = true
                    scope.launch {
                        val post = store.publishFile(draftPath!!, isVideo, caption, privatePost)
                        publishing = false
                        if (post != null) {
                            draftPath = null
                            caption = ""
                            onPublished()
                        } else {
                            onError("Failed to publish")
                        }
                    }
                },
                enabled = !publishing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (publishing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Publish")
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    store: FeedStore,
    onBack: () -> Unit,
    onOpenTelegram: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        SectionTitle("Account")
        SettingsRow(
            label = "Private account",
            trailing = {
                Switch(
                    checked = store.profile.privateAccount,
                    onCheckedChange = { store.setPrivateAccount(it) },
                )
            },
        )

        SectionTitle("Appearance")
        AppTheme.entries.forEach { theme ->
            SettingsRow(
                label = when (theme) {
                    AppTheme.System -> "System"
                    AppTheme.Light -> "Light"
                    AppTheme.Dark -> "Dark"
                },
                trailing = {
                    if (store.theme == theme) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                onClick = { store.chooseTheme(theme) },
            )
        }

        SectionTitle("Support")
        SettingsRow("Telegram @suyziss", onClick = onOpenTelegram)

        SectionTitle("Session")
        SettingsRow("Log out", onClick = { store.logout() })

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun SettingsRow(
    label: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
        .padding(horizontal = 16.dp, vertical = 12.dp)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        trailing?.invoke()
    }
}

@Composable
private fun PostCard(store: FeedStore, post: LocalPost, onOpen: () -> Unit) {
    val file = store.fileOf(post)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black),
        ) {
            MediaPreview(
                path = file.absolutePath,
                isVideo = post.video,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (post.caption.isNotBlank()) {
            Text(
                post.caption,
                modifier = Modifier.padding(12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatWhen(post.createdAt),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
