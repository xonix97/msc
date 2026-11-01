package org.msc.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.msc.app.MusicViewModel
import org.msc.app.R
import org.msc.app.data.Artwork
import org.msc.app.data.Track

@Composable fun AppIcon(name: String, description: String? = null, modifier: Modifier = Modifier) {
    val id = when(name) {
        "home" -> R.drawable.ic_home; "library" -> R.drawable.ic_library; "search" -> R.drawable.ic_search
        "play" -> R.drawable.ic_play; "pause" -> R.drawable.ic_pause; "next" -> R.drawable.ic_next
        "previous" -> R.drawable.ic_previous; "heart" -> R.drawable.ic_heart; "queue" -> R.drawable.ic_queue
        "more" -> R.drawable.ic_more; "add" -> R.drawable.ic_add; "settings" -> R.drawable.ic_settings
        "back" -> R.drawable.ic_back; "up" -> R.drawable.ic_up; "down" -> R.drawable.ic_down
        "close" -> R.drawable.ic_close; else -> R.drawable.ic_msc
    }
    Icon(painterResource(id), description, modifier.size(22.dp))
}

@Composable fun Art(model: MusicViewModel, uri: String, modifier: Modifier = Modifier) {
    val art by produceState(Artwork(null), uri) { value = withContext(Dispatchers.IO) { model.artwork.load(uri) } }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xff292e30)), contentAlignment = Alignment.Center) {
        val bitmap = art.bitmap
        if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else AppIcon("disc", modifier = Modifier.padding(10.dp))
    }
}

@Composable fun TrackRow(model: MusicViewModel, track: Track, onClick: () -> Unit, onAddToPlaylist: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Art(model, track.uri, Modifier.size(52.dp)) },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) { AppIcon("more", "Actions for ${track.title}") }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (track.favorite) "Remove favorite" else "Favorite") }, onClick = { menu = false; model.favorite(track) })
                    DropdownMenuItem(text = { Text("Add to queue") }, onClick = { menu = false; model.enqueue(track) })
                    DropdownMenuItem(text = { Text("Add to playlist") }, onClick = { menu = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text("Remove from library") }, onClick = { menu = false; model.remove(track) })
                }
            }
        },
    )
}

@Composable fun Disc(art: Artwork, cd: Boolean, angle: MutableFloatState, modifier: Modifier = Modifier) {
    val accent = Color(art.accent)
    Box(modifier.graphicsLayer { rotationZ = angle.floatValue }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = if (cd) "Compact disc" else "Vinyl record" }) {
            val radius = size.minDimension / 2
            if (cd) {
                drawCircle(Brush.sweepGradient(listOf(Color(0xffc6cbd0), Color(0xff8bb7c4), Color(0xffd8b7ca), Color(0xffe8e6c9), Color(0xffafb3dc), Color(0xffc6cbd0))))
                drawCircle(Color.White.copy(alpha = .45f), radius * .98f, style = Stroke(2f))
                drawCircle(Color.Black.copy(alpha = .18f), radius * .66f, style = Stroke(radius * .015f))
            } else {
                drawCircle(Brush.radialGradient(listOf(Color(0xff363c40), Color(0xff090c0e), Color(0xff252a2e), Color(0xff0c1012))))
                for (i in 20..48) drawCircle(Color.White.copy(alpha = if (i % 4 == 0) .10f else .035f), radius * i / 50, style = Stroke(1f))
                drawLine(Color.White.copy(alpha = .15f), Offset(radius * .2f, radius * .45f), Offset(radius * 1.8f, radius * 1.55f), radius * .04f)
            }
            drawCircle(accent, radius * if (cd) .36f else .38f)
        }
        val image = art.bitmap
        if (image != null) Image(image.asImageBitmap(), null, Modifier.fillMaxSize(if (cd) .34f else .36f).clip(CircleShape), contentScale = ContentScale.Crop)
        else Text("msc", color = Color(0xff172023), style = MaterialTheme.typography.headlineSmall)
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xffb5b5ae), size.minDimension * .047f)
            drawCircle(Color(0xff111719), size.minDimension * .024f)
        }
    }
}

fun timeLabel(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}
