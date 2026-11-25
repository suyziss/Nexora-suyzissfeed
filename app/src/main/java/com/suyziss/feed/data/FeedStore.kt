package com.suyziss.feed.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

enum class AppTheme { System, Light, Dark }
enum class MessagePrivacy { Friends, Everyone, Nobody }

data class LocalMessage(val username: String, val text: String, val createdAt: Long = System.currentTimeMillis())
data class SavedNote(val id: String, val text: String, val createdAt: Long = System.currentTimeMillis())
enum class Tab { Home, Friends, Create, Inbox, Profile }
enum class ProfileSection { Posts, Private, Reposts, Saved, Liked }
enum class InboxSection { Notifications, Messages }
enum class Overlay { None, Settings, EditProfile, Followers, Following, Likes }

data class Profile(
    val name: String,
    val username: String,
    val bio: String,
    val avatarFile: String,
    val privateAccount: Boolean,
)

data class LocalPost(
    val id: String,
    val fileName: String,
    val video: Boolean,
    val caption: String,
    val createdAt: Long,
    val privatePost: Boolean,
    val liked: Boolean,
    val bookmarked: Boolean,
    val reposted: Boolean = false,
)

data class LocalComment(
    val id: String,
    val author: String,
    val text: String,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

class FeedStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val postsDir = File(app.filesDir, "posts").apply { mkdirs() }
    private val draftDir = File(app.filesDir, "draft").apply { mkdirs() }
    private val avatarFile = File(app.filesDir, "avatar.jpg")

    var profile by mutableStateOf(readProfile())
        private set
    var posts by mutableStateOf(readPosts())
        private set
    var theme by mutableStateOf(readTheme())
        private set
    var commentsByPost by mutableStateOf(readComments())
        private set
    var messagePrivacy by mutableStateOf(readMessagePrivacy())
        private set
    var outgoingMessages by mutableStateOf(readOutgoingMessages())
        private set
    var savedNotes by mutableStateOf(readSavedNotes())
        private set
    var isLoggedIn by mutableStateOf(prefs.getBoolean(KEY_LOGGED_IN, false))
        private set

    val likesCount: Int get() = posts.count { it.liked }

    init {
        if (!prefs.contains(KEY_NAME)) persistProfile()
    }

    fun register(username: String, password: String, displayName: String): String? {
        val user = normalizeUsername(username)
        if (!isUsernameValid(user)) return "Username: 2–24 letters/digits/_."
        if (password.length < 4) return "Password must be at least 4 characters"
        val hash = hashPassword(password)
        prefs.edit()
            .putString(KEY_AUTH_USER, user)
            .putString(KEY_AUTH_HASH, hash)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
        profile = profile.copy(
            name = displayName.trim().ifBlank { user }.take(30),
            username = user,
        )
        persistProfile()
        isLoggedIn = true
        return null
    }

    fun login(username: String, password: String): String? {
        val user = normalizeUsername(username)
        val storedUser = prefs.getString(KEY_AUTH_USER, null)
        val storedHash = prefs.getString(KEY_AUTH_HASH, null)
        if (storedUser == null || storedHash == null) return "No account yet. Please register first."
        if (user != storedUser || hashPassword(password) != storedHash) return "Wrong username or password"
        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        isLoggedIn = true
        return null
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
        isLoggedIn = false
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun chooseTheme(value: AppTheme) {
        theme = value
        prefs.edit().putString(KEY_THEME, value.name).apply()
    }

    fun setMessagePrivacy(value: MessagePrivacy) {
        messagePrivacy = value
        prefs.edit().putString(KEY_MESSAGE_PRIVACY, value.name).apply()
    }

    fun sendLocalMessage(username: String, text: String): Boolean {
        val user = normalizeUsername(username.removePrefix("@"))
        val clean = text.trim().take(2000)
        if (!isUsernameValid(user) || user == profile.username || clean.isBlank()) return false
        if (messagePrivacy == MessagePrivacy.Nobody) return false
        outgoingMessages = listOf(LocalMessage(user, clean)) + outgoingMessages
        persistOutgoingMessages()
        return true
    }

    fun saveNote(text: String) {
        val clean = text.trim().take(2000)
        if (clean.isBlank()) return
        savedNotes = listOf(SavedNote(System.currentTimeMillis().toString(), clean)) + savedNotes
        persistSavedNotes()
    }

    fun deleteSavedNote(id: String) {
        savedNotes = savedNotes.filterNot { it.id == id }
        persistSavedNotes()
    }

    fun setPrivateAccount(value: Boolean) {
        profile = profile.copy(privateAccount = value)
        persistProfile()
    }

    suspend fun updateProfile(
        name: String,
        username: String,
        bio: String,
        newAvatar: Uri?,
        removeAvatar: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            var storedAvatar = profile.avatarFile
            if (newAvatar != null) {
                app.contentResolver.openInputStream(newAvatar)?.use { input ->
                    avatarFile.outputStream().use { input.copyTo(it) }
                } ?: return@withContext false
                if (avatarFile.length() == 0L) {
                    avatarFile.delete()
                    return@withContext false
                }
                storedAvatar = avatarFile.absolutePath
            } else if (removeAvatar) {
                avatarFile.delete()
                if (storedAvatar.isNotBlank()) File(storedAvatar).delete()
                storedAvatar = ""
            }
            val updated = profile.copy(
                name = name.trim().take(30),
                username = username.trim().take(24),
                bio = bio.trim().take(BIO_LIMIT),
                avatarFile = storedAvatar,
            )
            withContext(Dispatchers.Main) {
                profile = updated
                persistProfile()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun stageDraft(uri: Uri, video: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            draftDir.listFiles()?.forEach { it.delete() }
            val dest = File(draftDir, if (video) "draft.mp4" else "draft.jpg")
            app.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: return@withContext null
            if (dest.length() == 0L) {
                dest.delete()
                null
            } else dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun discardDraft(path: String?) {
        if (path.isNullOrBlank()) return
        File(path).delete()
    }

    suspend fun publishFile(
        path: String,
        video: Boolean,
        caption: String,
        privatePost: Boolean,
    ): LocalPost? = withContext(Dispatchers.IO) {
        try {
            val src = File(path)
            if (!src.exists() || src.length() == 0L) return@withContext null
            val id = System.currentTimeMillis().toString()
            val ext = if (video) "mp4" else "jpg"
            val dest = File(postsDir, "$id.$ext")
            if (!src.renameTo(dest)) {
                src.copyTo(dest, overwrite = true)
                src.delete()
            }
            val post = LocalPost(
                id = id,
                fileName = dest.name,
                video = video,
                caption = caption.trim().take(120),
                createdAt = System.currentTimeMillis(),
                privatePost = privatePost,
                liked = false,
                bookmarked = false,
            )
            withContext(Dispatchers.Main) {
                posts = listOf(post) + posts
                persistPosts()
            }
            post
        } catch (_: Exception) {
            null
        }
    }

    fun fileOf(post: LocalPost): File = File(postsDir, post.fileName)
    fun toggleLike(id: String) = updatePost(id) { it.copy(liked = !it.liked) }
    fun toggleBookmark(id: String) = updatePost(id) { it.copy(bookmarked = !it.bookmarked) }
    fun toggleRepost(id: String) = updatePost(id) { it.copy(reposted = !it.reposted) }

    fun addComment(postId: String, text: String, parentId: String? = null) {
        val clean = text.trim().take(500)
        if (clean.isBlank() || posts.none { it.id == postId }) return
        val current = commentsByPost[postId].orEmpty()
        commentsByPost = commentsByPost + (postId to (current + LocalComment(
            id = "${System.currentTimeMillis()}-${current.size}",
            author = profile.username,
            text = clean,
            parentId = parentId,
        )))
        persistComments()
    }

    fun commentsFor(postId: String): List<LocalComment> = commentsByPost[postId].orEmpty()

    fun delete(id: String) {
        val post = posts.find { it.id == id } ?: return
        fileOf(post).delete()
        posts = posts.filterNot { it.id == id }
        persistPosts()
    }

    private fun updatePost(id: String, block: (LocalPost) -> LocalPost) {
        posts = posts.map { if (it.id == id) block(it) else it }
        persistPosts()
    }

    private fun readMessagePrivacy(): MessagePrivacy =
        runCatching {
            MessagePrivacy.valueOf(prefs.getString(KEY_MESSAGE_PRIVACY, MessagePrivacy.Everyone.name) ?: MessagePrivacy.Everyone.name)
        }.getOrDefault(MessagePrivacy.Everyone)

    private fun readOutgoingMessages(): List<LocalMessage> {
        val raw = prefs.getString(KEY_OUTGOING_MESSAGES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(LocalMessage(item.optString("username"), item.optString("text"), item.optLong("createdAt")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistOutgoingMessages() {
        val array = JSONArray()
        outgoingMessages.forEach { item ->
            array.put(JSONObject().put("username", item.username).put("text", item.text).put("createdAt", item.createdAt))
        }
        prefs.edit().putString(KEY_OUTGOING_MESSAGES, array.toString()).apply()
    }

    private fun readSavedNotes(): List<SavedNote> {
        val raw = prefs.getString(KEY_SAVED_NOTES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(SavedNote(item.optString("id"), item.optString("text"), item.optLong("createdAt")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistSavedNotes() {
        val array = JSONArray()
        savedNotes.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("text", item.text).put("createdAt", item.createdAt))
        }
        prefs.edit().putString(KEY_SAVED_NOTES, array.toString()).apply()
    }

    private fun readTheme(): AppTheme {
        val raw = prefs.getString(KEY_THEME, AppTheme.System.name)
        return runCatching { AppTheme.valueOf(raw ?: AppTheme.System.name) }.getOrDefault(AppTheme.System)
    }

    private fun readProfile(): Profile {
        val legacy = app.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        return Profile(
            name = prefs.getString(KEY_NAME, null) ?: legacy.getString("name", "SUYZISS") ?: "SUYZISS",
            username = prefs.getString(KEY_USER, null) ?: legacy.getString("username", "suyziss") ?: "suyziss",
            bio = prefs.getString(KEY_BIO, null) ?: legacy.getString("bio", "") ?: "",
            avatarFile = prefs.getString(KEY_AVATAR, "") ?: "",
            privateAccount = prefs.getBoolean(KEY_PRIVATE, false),
        )
    }

    private fun persistProfile() {
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_USER, profile.username)
            .putString(KEY_BIO, profile.bio)
            .putString(KEY_AVATAR, profile.avatarFile)
            .putBoolean(KEY_PRIVATE, profile.privateAccount)
            .apply()
    }

    private fun readPosts(): List<LocalPost> {
        val raw = prefs.getString(KEY_POSTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val post = LocalPost(
                        id = item.getString("id"),
                        fileName = item.getString("fileName"),
                        video = item.getBoolean("video"),
                        caption = item.optString("caption"),
                        createdAt = item.getLong("createdAt"),
                        privatePost = item.optBoolean("privatePost"),
                        liked = item.optBoolean("liked"),
                        bookmarked = item.optBoolean("bookmarked"),
                        reposted = item.optBoolean("reposted"),
                    )
                    if (File(postsDir, post.fileName).exists()) add(post)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readComments(): Map<String, List<LocalComment>> {
        val raw = prefs.getString(KEY_COMMENTS, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val postId = keys.next()
                    val items = root.optJSONArray(postId) ?: JSONArray()
                    val list = buildList {
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            add(LocalComment(
                                id = item.optString("id"),
                                author = item.optString("author"),
                                text = item.optString("text"),
                                parentId = item.optString("parentId").takeIf { it.isNotBlank() },
                                createdAt = item.optLong("createdAt"),
                            ))
                        }
                    }
                    put(postId, list)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun persistComments() {
        val root = JSONObject()
        commentsByPost.forEach { (postId, list) ->
            val items = JSONArray()
            list.forEach { comment ->
                items.put(JSONObject()
                    .put("id", comment.id)
                    .put("author", comment.author)
                    .put("text", comment.text)
                    .put("parentId", comment.parentId ?: "")
                    .put("createdAt", comment.createdAt))
            }
            root.put(postId, items)
        }
        prefs.edit().putString(KEY_COMMENTS, root.toString()).apply()
    }

    private fun persistPosts() {
        val array = JSONArray()
        posts.forEach { post ->
            array.put(
                JSONObject()
                    .put("id", post.id)
                    .put("fileName", post.fileName)
                    .put("video", post.video)
                    .put("caption", post.caption)
                    .put("createdAt", post.createdAt)
                    .put("privatePost", post.privatePost)
                    .put("liked", post.liked)
                    .put("bookmarked", post.bookmarked)
                    .put("reposted", post.reposted),
            )
        }
        prefs.edit().putString(KEY_POSTS, array.toString()).apply()
    }

    companion object {
        const val BIO_LIMIT = 50
        private const val PREFS = "suyziss_feed"
        private const val LEGACY_PREFS = "suyziss_profile"
        private const val KEY_NAME = "name"
        private const val KEY_USER = "username"
        private const val KEY_BIO = "bio"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_PRIVATE = "private"
        private const val KEY_THEME = "theme"
        private const val KEY_POSTS = "posts"
        private const val KEY_COMMENTS = "comments"
        private const val KEY_MESSAGE_PRIVACY = "message_privacy"
        private const val KEY_OUTGOING_MESSAGES = "outgoing_messages"
        private const val KEY_SAVED_NOTES = "saved_notes"
        private const val KEY_AUTH_USER = "auth_user"
        private const val KEY_AUTH_HASH = "auth_hash"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}

fun normalizeUsername(raw: String): String =
    raw.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(24)

fun isUsernameValid(value: String): Boolean =
    value.length in 2..24 && value.any { it.isLetterOrDigit() }
