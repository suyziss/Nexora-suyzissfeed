package com.suyziss.feed.ui

import android.content.Intent
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.suyziss.feed.data.MessagePrivacy
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(store: FeedStore, onOpenPost: (String) -> Unit) {
    val posts = remember(store.posts) { store.posts.filter { !it.privatePost } }
    val listState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val context = LocalContext.current
    var commentsFor by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val pageHeight = maxHeight
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
                flingBehavior = snapFlingBehavior,
            ) {
                itemsIndexed(posts, key = { _, post -> post.id }, contentType = { _, post -> if (post.video) "video" else "image" }) { index, post ->
                    Box(
                        Modifier.fillMaxWidth().height(pageHeight).background(Color.Black),
                    ) {
                        MediaPreview(
                            path = store.fileOf(post).absolutePath,
                            isVideo = post.video,
                            play = post.video && index == listState.firstVisibleItemIndex,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(post.id) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        val firstUp = waitForUpOrCancellation() ?: return@awaitEachGesture
                                        val timeout = viewConfiguration.doubleTapTimeoutMillis.toLong()
                                        val secondDown = withTimeoutOrNull(timeout) {
                                            awaitFirstDown(requireUnconsumed = false)
                                        }
                                        if (secondDown == null) return@awaitEachGesture
                                        val secondUp = waitForUpOrCancellation() ?: return@awaitEachGesture
                                        val thirdDown = withTimeoutOrNull(timeout) {
                                            awaitFirstDown(requireUnconsumed = false)
                                        }
                                        if (thirdDown == null) {
                                            store.toggleLike(post.id)
                                        } else {
                                            val thirdUp = waitForUpOrCancellation()
                                            if (thirdUp != null) store.toggleRepost(post.id)
                                            else store.toggleLike(post.id)
                                        }
                                    }
                                },
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
private fun CommentReplies(
    comments: List<LocalComment>,
    parentId: String,
    depth: Int,
    onReply: (String) -> Unit,
) {
    comments.filter { it.parentId == parentId }.forEach { reply ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = (14 + depth * 12).dp, top = 3.dp, bottom = 5.dp),
        ) {
            Text("@${reply.author} · reply", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(reply.text)
            TextButton(onClick = { onReply(reply.id) }) { Text("Reply") }
            if (depth < 4) {
                CommentReplies(comments, reply.id, depth + 1, onReply)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsDialog(store: FeedStore, postId: String, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<String?>(null) }
    var mediaPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val comments = store.commentsFor(postId)
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) scope.launch {
            mediaPath = store.stageCommentMedia(uri)
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.68f),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)))
                Text("Comments · ${comments.size}", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                HorizontalDivider()
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                val roots = comments.filter { it.parentId == null }
                items(roots, key = { it.id }) { comment ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("@${comment.author}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (comment.text.isNotBlank()) Text(comment.text, modifier = Modifier.padding(top = 3.dp))
                        comment.mediaFile?.let { path ->
                            AsyncImage(
                                model = java.io.File(path),
                                contentDescription = "Comment attachment",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(top = 6.dp).clip(RoundedCornerShape(12.dp)),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { replyTo = comment.id }) { Text("Reply") }
                            IconButton(onClick = { store.toggleCommentLike(postId, comment.id) }) {
                                Icon(
                                    if (comment.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like comment",
                                    tint = if (comment.liked) Color(0xFFFF365D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        CommentReplies(comments, comment.id, 0) { replyTo = it }
                    }
                }
                if (roots.isEmpty()) item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("No comments yet. Be the first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            if (replyTo != null) Text("Replying to a comment", Modifier.padding(start = 16.dp, top = 6.dp), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            if (mediaPath != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Attachment added", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    TextButton(onClick = { mediaPath = null }) { Text("Remove") }
                }
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Icon(Icons.Default.Image, contentDescription = "Add photo or GIF") }
                OutlinedTextField(value = draft, onValueChange = { if (it.length <= 500) draft = it }, modifier = Modifier.weight(1f), placeholder = { Text("Add a comment…") }, maxLines = 3)
                IconButton(enabled = draft.isNotBlank() || mediaPath != null, onClick = {
                    store.addComment(postId, draft, replyTo, mediaPath)
                    draft = ""
                    mediaPath = null
                    replyTo = null
                }) { Icon(Icons.Default.Check, contentDescription = "Send comment", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
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
fun InboxScreen(store: FeedStore) {
    var composeOpen by remember { mutableStateOf(false) }
    var savedOpen by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Inbox", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { savedOpen = true }) { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Saved messages") }
            IconButton(onClick = { composeOpen = true; error = null }) { Icon(Icons.Default.Add, contentDescription = "New message") }
        }
        if (store.outgoingMessages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No conversations yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { savedOpen = true }) { Text("Open Saved Messages") }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                items(store.outgoingMessages, key = { "${it.username}-${it.createdAt}" }) { item ->
                    Column(Modifier.fillMaxWidth().clickable { username = item.username; message = item.text; composeOpen = true }.padding(12.dp)) {
                        Text("@${item.username}", fontWeight = FontWeight.Bold)
                        Text(item.text, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Saved locally · not delivered", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    if (composeOpen) AlertDialog(
        onDismissRequest = { composeOpen = false },
        title = { Text("New message") },
        text = {
            Column {
                OutlinedTextField(value = username, onValueChange = { username = it.removePrefix("@").take(24) }, label = { Text("Username") }, prefix = { Text("@") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = message, onValueChange = { if (it.length <= 2000) message = it }, label = { Text("Message") }, minLines = 3, maxLines = 5)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Text("No server connection yet. This is stored on this device only.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    store.messagePrivacy == MessagePrivacy.Nobody -> error = "Messaging is disabled in privacy settings."
                    store.sendLocalMessage(username, message) -> { composeOpen = false; username = ""; message = "" }
                    else -> error = "Check the username and message."
                }
            }) { Text("Save draft") }
        },
        dismissButton = { TextButton(onClick = { composeOpen = false }) { Text("Cancel") } },
    )
    if (savedOpen) AlertDialog(
        onDismissRequest = { savedOpen = false },
        title = { Text("Saved Messages") },
        text = {
            Column {
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 2000) note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Write a note to yourself") }, minLines = 2, maxLines = 4)
                TextButton(enabled = note.isNotBlank(), onClick = { store.saveNote(note); note = "" }) { Text("Save note") }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                    items(store.savedNotes, key = { it.id }) { saved ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(saved.text)
                            TextButton(onClick = { store.deleteSavedNote(saved.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { savedOpen = false }) { Text("Done") } },
    )
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
    var accountDialog by remember { mutableStateOf(false) }
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
        SettingsRow("Switch account", onClick = { accountDialog = true })
        SettingsRow(
            label = "Private account",
            trailing = {
                Switch(
                    checked = store.profile.privateAccount,
                    onCheckedChange = { store.setPrivateAccount(it) },
                )
            },
        )

        SectionTitle("Who can message you")
        MessagePrivacy.entries.forEach { option ->
            SettingsRow(
                label = when (option) {
                    MessagePrivacy.Friends -> "Friends (requests)"
                    MessagePrivacy.Everyone -> "Everyone"
                    MessagePrivacy.Nobody -> "Nobody"
                },
                trailing = {
                    if (store.messagePrivacy == option) Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                },
                onClick = { store.updateMessagePrivacy(option) },
            )
        }

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
    if (accountDialog) AlertDialog(
        onDismissRequest = { accountDialog = false },
        title = { Text("Switch account") },
        text = {
            Column {
                store.savedAccounts.forEach { account ->
                    SettingsRow(
                        label = "@${account.username}" + if (account.username == store.profile.username) " · Active" else "",
                        onClick = {
                            store.switchAccount(account.username)
                            accountDialog = false
                        },
                    )
                }
                if (store.savedAccounts.isEmpty()) {
                    Text("No saved accounts on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { accountDialog = false; store.logout() }) { Text("Add account") }
        },
        dismissButton = { TextButton(onClick = { accountDialog = false }) { Text("Close") } },
    )
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
