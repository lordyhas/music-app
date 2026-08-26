package com.lordyhas.muzik.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lordyhas.muzik.data.model.Song
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.SurfaceCard
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
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onShowDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) SurfaceElevated else SurfaceCard,
        label = "item_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SurfaceElevated, Color(0xFF2E2C44))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Pochette d'album",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) AccentPrimary else TextMuted,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "En lecture",
                        tint = AccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Titles and artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isSelected) AccentPrimary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.size(3.dp))

            Text(
                text = song.artist + " • " + song.formattedDuration(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Options Menu
        Box {
            IconButton(
                onClick = { isMenuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options du morceau",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier
                    .background(SurfaceElevated)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (onPlayNext != null) {
                    DropdownMenuItem(
                        text = { Text("Lire juste après", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onPlayNext()
                        },
                        colors = MenuDefaults.itemColors()
                    )
                }

                if (onAddToQueue != null) {
                    DropdownMenuItem(
                        text = { Text("Ajouter à la file", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onAddToQueue()
                        }
                    )
                }

                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Ajouter à une playlist", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                }

                if (onGoToArtist != null) {
                    DropdownMenuItem(
                        text = { Text("Aller à l'artiste", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onGoToArtist()
                        }
                    )
                }

                if (onGoToAlbum != null) {
                    DropdownMenuItem(
                        text = { Text("Aller à l'album", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Album,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onGoToAlbum()
                        }
                    )
                }

                if (onRemoveFromPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Retirer de la playlist", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onRemoveFromPlaylist()
                        }
                    )
                }

                if (onShowDetails != null) {
                    DropdownMenuItem(
                        text = { Text("Détails du morceau", color = TextPrimary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onShowDetails()
                        }
                    )
                }
            }
        }
    }
}

