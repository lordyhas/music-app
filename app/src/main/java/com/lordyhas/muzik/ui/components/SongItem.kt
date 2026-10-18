package com.lordyhas.muzik.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lordyhas.muzik.data.model.Song
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.DangerRed
import com.lordyhas.muzik.ui.theme.SurfaceElevated
import com.lordyhas.muzik.ui.theme.TextMuted
import com.lordyhas.muzik.ui.theme.TextPrimary
import com.lordyhas.muzik.ui.theme.TextSecondary

@Composable
fun SongItem(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onShowDetails: (() -> Unit)? = null,
    showTrackNumber: Boolean = false,
    showArtwork: Boolean = true,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) AccentPrimary.copy(alpha = 0.10f) else Color.Transparent,
        label = "item_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTrackNumber) {
            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                if (isSelected) {
                    EqualizerBars(isAnimating = isPlaying, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = song.trackNumber?.takeIf { it > 0 }?.toString() ?: "–",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (showArtwork) {
            Box(contentAlignment = Alignment.Center) {
                Artwork(
                    uri = song.albumArtUri,
                    shape = RoundedCornerShape(10.dp),
                    iconSize = 22.dp,
                    modifier = Modifier.size(52.dp)
                )
                if (isSelected && !showTrackNumber) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        EqualizerBars(isAnimating = isPlaying, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) AccentPrimary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLiked) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Liked",
                        tint = AccentPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${song.artist} • ${song.formattedDuration()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box {
            IconButton(
                onClick = { isMenuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Song options",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier.background(SurfaceElevated)
            ) {
                @Composable
                fun MenuItem(label: String, icon: ImageVector, tint: Color, action: () -> Unit) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                color = if (tint == DangerRed) DangerRed else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuExpanded = false
                            action()
                        }
                    )
                }

                onToggleLike?.let {
                    MenuItem(
                        if (isLiked) "Remove from Liked Songs" else "Add to Liked Songs",
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        AccentPrimary,
                        it
                    )
                }
                onAddToQueue?.let { MenuItem("Add to queue", Icons.AutoMirrored.Filled.QueueMusic, AccentPrimary, it) }
                onAddToPlaylist?.let { MenuItem("Add to playlist", Icons.AutoMirrored.Filled.PlaylistAdd, AccentPrimary, it) }
                onGoToArtist?.let { MenuItem("Go to artist", Icons.Default.Person, TextSecondary, it) }
                onGoToAlbum?.let { MenuItem("Go to album", Icons.Default.Album, TextSecondary, it) }
                onRemoveFromPlaylist?.let { MenuItem("Remove from playlist", Icons.Default.Delete, DangerRed, it) }
                onShowDetails?.let { MenuItem("Song details", Icons.Default.Info, TextSecondary, it) }
            }
        }

        trailing?.invoke()
    }
}

/** Three bouncing bars that indicate the currently playing track. Static when paused. */
@Composable
fun EqualizerBars(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AccentPrimary
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = listOf(520, 380, 450).map { duration ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
            label = "eq_bar"
        )
    }
    val pausedHeights = listOf(0.45f, 0.8f, 0.6f)

    Canvas(modifier = modifier) {
        val gap = size.width * 0.15f
        val barWidth = (size.width - gap * 2) / 3
        bars.forEachIndexed { i, anim ->
            val fraction = if (isAnimating) anim.value else pausedHeights[i]
            val h = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
