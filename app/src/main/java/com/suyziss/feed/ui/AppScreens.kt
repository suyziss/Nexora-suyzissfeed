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
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
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
    var sharePost by remember { mutableStateOf<LocalPost?>(null) }
    var shareUsername by remember { mutableStateOf("") }
    var shareError by remember { mutableStateOf<String?>(null) }

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
                                    sharePost = post
                                    shareUsername = ""
                                    shareError = null
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
    sharePost?.let { post ->
        AlertDialog(
            onDismissRequest = { sharePost = null; shareError = null },
            title = { Text("Отправить видео") },
            text = {
                Column {
                    Text("Укажите @username друга. Видео будет добавлено в локальный список чатов.")
                    OutlinedTextField(value = shareUsername, onValueChange = { shareUsername = it.removePrefix("@").take(24) }, label = { Text("@username") }, singleLine = true)
                    if (shareError != null) Text(shareError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val link = "suyzissfeed://video/${post.id}"
                    if (store.sendLocalMessage(shareUsername, "Посмотри видео: $link")) {
                        sharePost = null
                        shareUsername = ""
                        shareError = null
                    } else shareError = "Не удалось сохранить. Проверьте имя и настройки личных сообщений."
                }) { Text("Отправить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val link = "suyzissfeed://video/${post.id}"
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Suyziss Feed video: $link") }
                    context.startActivity(Intent.createChooser(send, "Share video"))
                }) { Text("Другие приложения") }
            },
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
    store: FeedStore,
    postId: String,
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
            if (reply.text.isNotBlank()) Text(reply.text)
            reply.mediaFile?.let { path ->
                AsyncImage(
                    model = java.io.File(path),
                    contentDescription = "Comment attachment",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(10.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onReply(reply.id) }) { Text("Reply") }
                IconButton(onClick = { store.toggleCommentLike(postId, reply.id) }) {
                    Icon(
                        if (reply.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like reply",
                        tint = if (reply.liked) Color(0xFFFF365D) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (depth < 4) {
                CommentReplies(store, postId, comments, reply.id, depth + 1, onReply)
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
                        CommentReplies(store, postId, comments, comment.id, 0) { replyTo = it }
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
    var selectedChat by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf("Messages") }
    var searchOpen by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var composeOpen by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var savedOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val conversations = store.outgoingMessages
        .groupBy { it.username }
        .mapNotNull { (user, messages) -> messages.maxByOrNull { it.createdAt }?.let { user to it } }
        .sortedByDescending { it.second.createdAt }
        .filter { it.first.contains(search, ignoreCase = true) || it.second.text.contains(search, ignoreCase = true) }

    if (selectedChat != null) {
        val chatUser = selectedChat!!
        val messages = store.outgoingMessages.filter { it.username == chatUser }.sortedBy { it.createdAt }
        var draft by remember(chatUser) { mutableStateOf("") }
        Column(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { selectedChat = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(Color(0xFF303036)),
                    contentAlignment = Alignment.Center,
                ) { Text(chatUser.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
                Text(chatUser, Modifier.weight(1f).padding(start = 12.dp), color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                IconButton(onClick = { savedOpen = true }) {
                    Icon(Icons.Outlined.StarBorder, contentDescription = "Saved messages", tint = Color.White)
                }
                IconButton(onClick = { }) { Text("•••", color = Color.White, fontWeight = FontWeight.Bold) }
            }
            HorizontalDivider(color = Color(0xFF202024))
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (messages.isEmpty()) item {
                    Box(Modifier.fillMaxWidth().padding(top = 180.dp), contentAlignment = Alignment.Center) {
                        Text("Начните переписку", color = Color.Gray)
                    }
                }
                items(messages, key = { it.createdAt }) { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                Modifier.clip(RoundedCornerShape(22.dp, 22.dp, 5.dp, 22.dp))
                                    .background(Color(0xFF9147F5))
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
                                    .widthIn(max = 280.dp),
                            ) { Text(item.text, color = Color.White) }
                            Text("Сохранено · не доставлено", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { }) { Text("📷", fontSize = 21.sp) }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 2000) draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Написать…", color = Color.Gray) },
                    maxLines = 4,
                    shape = RoundedCornerShape(28.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF202024),
                        unfocusedContainerColor = Color(0xFF202024),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                IconButton(onClick = { }) { Text("🎙", fontSize = 21.sp) }
                IconButton(onClick = { }) { Text("🖼", fontSize = 21.sp) }
                IconButton(onClick = { }) { Text("☻", fontSize = 22.sp, color = Color.White) }
                IconButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        if (store.sendLocalMessage(chatUser, draft)) draft = ""
                    },
                ) { Icon(Icons.Default.Add, contentDescription = "Send", tint = Color(0xFFB77BFF)) }
            }
            Text(
                "Локальный режим: сообщение не отправлено другому пользователю",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().background(Color.Black).padding(bottom = 5.dp),
                textAlign = TextAlign.Center,
            )
        }
        if (savedOpen) AlertDialog(
            onDismissRequest = { savedOpen = false },
            title = { Text("Saved Messages") },
            text = {
                Column {
                    OutlinedTextField(value = note, onValueChange = { note = it.take(2000) }, label = { Text("Заметка себе") })
                    TextButton(enabled = note.isNotBlank(), onClick = { store.saveNote(note); note = "" }) { Text("Сохранить") }
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(store.savedNotes, key = { it.id }) { saved ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(saved.text, Modifier.weight(1f))
                                TextButton(onClick = { store.deleteSavedNote(saved.id) }) { Text("Удалить") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { savedOpen = false }) { Text("Готово") } },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Friends", tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(16.dp))
            Text("Входящие", Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF202024)).padding(horizontal = 7.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(Color(0xFF20D878)))
                    Text("⌄", color = Color.LightGray, modifier = Modifier.padding(start = 5.dp))
                }
            }
            IconButton(onClick = { searchOpen = !searchOpen }) { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) }
            IconButton(onClick = { savedOpen = true }) { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Saved", tint = Color.White) }
            IconButton(onClick = { composeOpen = true; error = null }) { Icon(Icons.Default.Add, contentDescription = "New message", tint = Color.White) }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Поиск") },
                singleLine = true,
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(76.dp).clip(RoundedCornerShape(50)).background(Color(0xFF303036)), contentAlignment = Alignment.Center) {
                    Text("＋", color = Color(0xFF20C8E8), fontSize = 30.sp)
                }
                Text("Создать", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Messages", "Requests", "Unread").forEach { tab ->
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (activeTab == tab) Color(0xFF073D48) else Color(0xFF202024))
                        .clickable { activeTab = tab }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(tab, color = if (activeTab == tab) Color(0xFF27C6D8) else Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        when (activeTab) {
            "Requests" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет запросов", color = Color.Gray)
            }
            "Unread" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет непрочитанных сообщений", color = Color.Gray)
            }
            else -> {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { activeTab = "Requests" }.padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(58.dp).clip(RoundedCornerShape(50)).background(Color(0xFF08AEEA)), contentAlignment = Alignment.Center) {
                            Text("♟", color = Color.White, fontSize = 25.sp)
                        }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text("Новые подписчики", color = Color.White, fontSize = 16.sp)
                            Text("Здесь будут ваши новые подписчики", color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { activeTab = "Unread" }.padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(58.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE93678)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text("Активность", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Лайки и другие уведомления", color = Color.Gray, maxLines = 1)
                        }
                    }
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(conversations, key = { it.first }) { (user, latest) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedChat = user }.padding(horizontal = 18.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(58.dp).clip(RoundedCornerShape(50)).background(Color(0xFF303036)), contentAlignment = Alignment.Center) {
                                Text(user.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(user, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(latest.text, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Сохранено локально · не доставлено", color = Color(0xFFE05A6F), fontSize = 10.sp)
                            }
                        }
                    }
                    if (conversations.isEmpty()) item {
                        Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) {
                            Text("Нет диалогов", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
    if (savedOpen && selectedChat == null) AlertDialog(
        onDismissRequest = { savedOpen = false },
        title = { Text("Saved Messages") },
        text = {
            Column {
                OutlinedTextField(value = note, onValueChange = { note = it.take(2000) }, label = { Text("Заметка себе") })
                TextButton(enabled = note.isNotBlank(), onClick = { store.saveNote(note); note = "" }) { Text("Сохранить") }
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(store.savedNotes, key = { it.id }) { saved ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(saved.text, Modifier.weight(1f))
                            TextButton(onClick = { store.deleteSavedNote(saved.id) }) { Text("Удалить") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { savedOpen = false }) { Text("Готово") } },
    )
    if (composeOpen) AlertDialog(
        onDismissRequest = { composeOpen = false },
        title = { Text("Новое сообщение") },
        text = {
            Column {
                OutlinedTextField(value = username, onValueChange = { username = it.removePrefix("@").take(24) }, label = { Text("Имя пользователя") }, prefix = { Text("@") }, singleLine = true)
                OutlinedTextField(value = message, onValueChange = { message = it.take(2000) }, label = { Text("Сообщение") }, minLines = 2, maxLines = 4)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    store.messagePrivacy == MessagePrivacy.Nobody -> error = "Сообщения отключены в настройках приватности."
                    store.sendLocalMessage(username, message) -> {
                        selectedChat = username.removePrefix("@").trim().lowercase()
                        composeOpen = false
                        username = ""
                        message = ""
                    }
                    else -> error = "Проверьте имя пользователя и текст."
                }
            }) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = { composeOpen = false }) { Text("Отмена") } },
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
fun SettingsScreen(store: FeedStore, onBack: () -> Unit, onOpenTelegram: () -> Unit) {
    var accountDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var detailTitle by remember { mutableStateOf<String?>(null) }
    var detailText by remember { mutableStateOf("") }
    val panel = if (store.theme == AppTheme.Light) Color(0xFFF0F0F0) else Color(0xFF202020)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val translations = mapOf(
        "ru" to mapOf("Settings and privacy" to "Настройки и конфиденциальность", "Activity" to "Активность", "Account" to "Аккаунт", "Visibility" to "Видимость", "Interactions" to "Взаимодействия", "Content and display" to "Контент и отображение", "Cache and mobile data" to "Кэш и мобильный трафик", "Support" to "Поддержка", "Account login" to "Вход в аккаунт", "Post management" to "Управление публикациями", "Preferred content" to "Предпочитаемый контент", "Notifications" to "Уведомления", "Screen time and wellbeing" to "Время и благополучие", "Family pairing" to "Привязка аккаунтов семьи", "Security and permissions" to "Безопасность и разрешения", "Share profile" to "Поделиться профилем", "Private account" to "Приватный аккаунт", "Blocked accounts" to "Заблокированные аккаунты", "Comments" to "Комментарии", "Mentions" to "Упоминания", "Direct messages" to "Личные сообщения", "Content reuse" to "Повторное использование контента", "Show profile when sharing links" to "Показывать профиль при отправке ссылок", "Downloads" to "Загрузки", "Following" to "Подписки", "Liked videos" to "Понравившиеся видео", "Viewers" to "Зрители", "Music" to "Музыка", "Inbox and messaging" to "Входящие и обмен сообщениями", "Activity center" to "Центр активности", "Audience controls" to "Ограничение аудитории", "Ads" to "Реклама", "Playback" to "Воспроизведение", "Display" to "Показать", "Language" to "Язык", "Accessibility" to "Универсальный доступ", "Contacts and location" to "Контакты и местоположение", "Offline videos" to "Видео офлайн", "Free up space" to "Освобождение места", "Data saver" to "Экономия трафика", "Help center" to "Центр поддержки", "Privacy center" to "Центр конфиденциальности", "Terms and policies" to "Условия и политики", "Switch account" to "Сменить аккаунт", "Log out" to "Выйти", "LIVE" to "ЭФИР"),
        "zh" to mapOf("Settings and privacy" to "设置与隐私", "Activity" to "活动", "Account" to "账户", "Visibility" to "可见性", "Interactions" to "互动", "Content and display" to "内容与显示", "Cache and mobile data" to "缓存和移动数据", "Support" to "帮助与支持", "Language" to "语言", "Private account" to "私人账户", "Switch account" to "切换账户", "Log out" to "退出登录", "Direct messages" to "私信", "Comments" to "评论"),
        "ja" to mapOf("Settings and privacy" to "設定とプライバシー", "Activity" to "アクティビティ", "Account" to "アカウント", "Visibility" to "公開範囲", "Interactions" to "インタラクション", "Content and display" to "コンテンツと表示", "Cache and mobile data" to "キャッシュとモバイルデータ", "Support" to "サポート", "Language" to "言語", "Private account" to "非公開アカウント", "Switch account" to "アカウントを切り替える", "Log out" to "ログアウト", "Direct messages" to "ダイレクトメッセージ", "Comments" to "コメント"),
        "kk" to mapOf("Settings and privacy" to "Баптаулар және құпиялылық", "Activity" to "Белсенділік", "Account" to "Аккаунт", "Visibility" to "Көріну", "Interactions" to "Өзара әрекет", "Content and display" to "Контент және көрсету", "Cache and mobile data" to "Кэш және мобильді трафик", "Support" to "Қолдау", "Language" to "Тіл", "Private account" to "Жеке аккаунт", "Switch account" to "Аккаунтты ауыстыру", "Log out" to "Шығу", "Direct messages" to "Жеке хабарламалар", "Comments" to "Пікірлер")
    )
    fun tr(value: String) = translations[store.language]?.get(value) ?: value
    fun info(title: String) {
        detailTitle = tr(title)
        detailText = when (title) {
            "Help center" -> "Поддержка: @suyziss"
            "Privacy center" -> "Данные приложения пока хранятся локально на устройстве. Серверная синхронизация ещё не подключена."
            "Post management" -> "Управляйте собственными публикациями в профиле."
            "Direct messages" -> "Кто может отправлять вам сообщения, задаётся в разделе приватности."
            else -> "Раздел интерфейса подготовлен. Дополнительные действия будут подключены вместе с соответствующими функциями."
        }
    }
    @Composable
    fun group(title: String, rows: List<Pair<String, () -> Unit>>) {
        Text(tr(title), Modifier.padding(start = 22.dp, top = 22.dp, bottom = 10.dp), color = muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).clip(RoundedCornerShape(16.dp)).background(panel)) {
            rows.forEach { (label, action) -> SettingsRow(tr(label), trailing = { Text("›", color = muted, fontSize = 26.sp) }, onClick = action) }
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(tr("Settings and privacy"), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        group("Activity", listOf("Post management" to { info("Post management") }, "Preferred content" to { info("Preferred content") }, "LIVE" to { info("LIVE") }, "Notifications" to { info("Notifications") }, "Screen time and wellbeing" to { info("Screen time and wellbeing") }, "Family pairing" to { info("Family pairing") }))
        group("Account", listOf("Account" to { detailTitle = tr("Account"); detailText = "@${store.profile.username}" }, "Security and permissions" to { info("Security and permissions") }, "Share profile" to { info("Share profile") }))
        Text(tr("Visibility"), Modifier.padding(start = 22.dp, top = 22.dp, bottom = 10.dp), color = muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).clip(RoundedCornerShape(16.dp)).background(panel)) {
            SettingsRow(tr("Private account"), trailing = { Switch(checked = store.profile.privateAccount, onCheckedChange = store::setPrivateAccount) }, onClick = { store.setPrivateAccount(!store.profile.privateAccount) })
            SettingsRow(tr("Blocked accounts"), trailing = { Text("›", color = muted, fontSize = 26.sp) }, onClick = { info("Blocked accounts") })
        }
        group("Interactions", listOf("Comments" to { info("Comments") }, "Mentions" to { info("Mentions") }, "Direct messages" to { info("Direct messages") }, "Content reuse" to { info("Content reuse") }, "Show profile when sharing links" to { info("Show profile when sharing links") }, "Downloads" to { info("Downloads") }, "Following" to { info("Following") }, "Liked videos" to { info("Liked videos") }, "Viewers" to { info("Viewers") }))
        group("Content and display", listOf("Music" to { info("Music") }, "Inbox and messaging" to { info("Inbox and messaging") }, "Activity center" to { info("Activity center") }, "Audience controls" to { info("Audience controls") }, "Ads" to { info("Ads") }, "Playback" to { info("Playback") }, "Display" to { info("Display") }, "Language" to { languageDialog = true }, "Accessibility" to { info("Accessibility") }, "Contacts and location" to { info("Contacts and location") }))
        group("Cache and mobile data", listOf("Offline videos" to { info("Offline videos") }, "Free up space" to { info("Free up space") }, "Data saver" to { info("Data saver") }))
        group("Support", listOf("Help center" to onOpenTelegram, "Privacy center" to { info("Privacy center") }, "Terms and policies" to { info("Terms and policies") }))
        group("Account login", listOf("Switch account" to { accountDialog = true }, "Log out" to { store.logout() }))
        Text("Suyziss Feed · ${store.language.uppercase()}", Modifier.fillMaxWidth().padding(top = 22.dp), color = muted, textAlign = TextAlign.Center, fontSize = 12.sp)
    }
    if (languageDialog) AlertDialog(onDismissRequest = { languageDialog = false }, title = { Text(tr("Language")) }, text = {
        Column { listOf("ru" to "Русский", "en" to "English", "zh" to "中文", "ja" to "日本語", "kk" to "Қазақша").forEach { (code, name) ->
            SettingsRow(name, trailing = { if (store.language == code) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }, onClick = { store.chooseLanguage(code); languageDialog = false })
        } }
    }, confirmButton = { TextButton(onClick = { languageDialog = false }) { Text("OK") } })
    if (accountDialog) AlertDialog(onDismissRequest = { accountDialog = false }, title = { Text(tr("Switch account")) }, text = {
        Column { store.savedAccounts.forEach { account -> SettingsRow("@${account.username}" + if (account.username == store.profile.username) " · Active" else "", onClick = { store.switchAccount(account.username); accountDialog = false }) }
            if (store.savedAccounts.isEmpty()) Text("No saved accounts on this device.")
        }
    }, confirmButton = { TextButton(onClick = { accountDialog = false; store.logout() }) { Text("Add account") } }, dismissButton = { TextButton(onClick = { accountDialog = false }) { Text("Close") } })
    if (detailTitle != null) AlertDialog(onDismissRequest = { detailTitle = null }, title = { Text(detailTitle!!) }, text = { Text(detailText) }, confirmButton = { TextButton(onClick = { detailTitle = null }) { Text("OK") } })
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
