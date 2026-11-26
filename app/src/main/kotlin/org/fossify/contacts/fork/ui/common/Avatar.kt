package org.fossify.contacts.fork.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fossify.contacts.fork.ui.theme.avatarPaletteFor
import kotlin.math.max

/**
 * A contact's photo, or their initial on a theme-tinted shape when there is none.
 * Photos are decoded off the main thread at the size they're shown and kept in a
 * small memory cache, so long lists scroll without stutter.
 */
@Composable
fun ContactAvatar(
    name: String,
    photoUri: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    colorKey: String = name,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(initialValue = PhotoCache.peek(photoUri, px), photoUri, px) {
        if (value == null && photoUri.isNotEmpty()) {
            value = PhotoCache.load(context, photoUri, px)
        }
    }
    val palette = avatarPaletteFor(colorKey)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = bitmap, label = "avatar") { image ->
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initialOf(name),
                        color = palette.content,
                        fontWeight = FontWeight.Medium,
                        fontSize = (size.value * 0.42f).sp,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

fun initialOf(name: String): String {
    val first = name.trim().firstOrNull { it.isLetterOrDigit() } ?: return "#"
    return if (first.isLetter()) first.uppercase() else "#"
}

object PhotoCache {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    private fun key(uri: String, px: Int) = "$uri@${bucket(px)}"

    /** Sizes are rounded up to a few buckets, so a list and its header can share a decode. */
    private fun bucket(px: Int) = when {
        px <= 128 -> 128
        px <= 256 -> 256
        else -> 640
    }

    fun peek(uri: String, px: Int): ImageBitmap? = if (uri.isEmpty()) null else cache.get(key(uri, px))

    suspend fun load(context: Context, uri: String, px: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val k = key(uri, px)
        cache.get(k)?.let { return@withContext it }
        val target = bucket(px)
        val decoded = runCatching { decode(context, Uri.parse(uri), target) }.getOrNull() ?: return@withContext null
        val image = decoded.asImageBitmap()
        cache.put(k, image)
        image
    }

    fun clear() = cache.evictAll()

    private fun decode(context: Context, uri: Uri, target: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val smallest = minOf(bounds.outWidth, bounds.outHeight)
        while (smallest / (sample * 2) >= target) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = max(1, sample) }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
