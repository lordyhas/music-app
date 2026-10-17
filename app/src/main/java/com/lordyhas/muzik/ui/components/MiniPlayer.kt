package com.lordyhas.muzik.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lordyhas.muzik.data.model.Song
import com.lordyhas.muzik.ui.theme.BackgroundDark
import com.lordyhas.muzik.ui.theme.StrokeSubtle
import com.lordyhas.muzik.ui.theme.SurfaceElevated
import com.lordyhas.muzik.ui.theme.TextPrimary
import com.lordyhas.muzik.ui.theme.TextSecondary
import com.lordyhas.muzik.ui.theme.TrackBackground
import com.lordyhas.muzik.ui.theme.rememberArtworkColor

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val accent = rememberArtworkColor(song.albumArtUri)
    val progress = if (duration > 0) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(20.dp, shape, spotColor = accent.copy(alpha = 0.5f))
            .clip(shape)
            .background(SurfaceElevated)
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.38f), accent.copy(alpha = 0.10f))
                )
            )
            .border(1.dp, StrokeSubtle, shape)
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(
                    uri = song.albumArtUri,
                    shape = RoundedCornerShape(12.dp),
                    iconSize = 22.dp,
                    placeholderTint = accent,
                    contentDescription = "Album artwork",
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Crossfade title/artist on track change
                AnimatedContent(
                    targetState = song,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentKey = { it.id },
                    label = "mini_title",
                    modifier = Modifier.weight(1f)
                ) { s ->
                    Column {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = s.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = BackgroundDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 6.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = TrackBackground,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}
