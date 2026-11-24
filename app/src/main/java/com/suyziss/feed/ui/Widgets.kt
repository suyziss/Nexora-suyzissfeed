package com.suyziss.feed.ui

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaPreview(
    file: File?,
    video: Boolean,
    play: Boolean,
    modifier: Modifier = Modifier,
    description: String = "Медиа",
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (file == null || !file.exists()) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (!video) {
            AsyncImage(
                model = file,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (!play) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Видео",
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> VideoView(context) },
                update = { view ->
                    if (view.tag != file.absolutePath) {
                        view.tag = file.absolutePath
                        view.setVideoPath(file.absolutePath)
                        view.setOnPreparedListener { player ->
                            player.isLooping = true
                            view.start()
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun Modifier.feedInsets(): Modifier = windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
)

fun formatWhen(epochMillis: Long): String {
    val format = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
    return format.format(Date(epochMillis))
}

val AvatarSizeNarrow = 84.dp
val AvatarSizeRegular = 96.dp
