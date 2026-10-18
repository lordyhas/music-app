package com.lordyhas.muzik.ui.components

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lordyhas.muzik.data.model.LibraryFilter
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.StrokeSubtle
import com.lordyhas.muzik.ui.theme.SurfaceDark
import com.lordyhas.muzik.ui.theme.SurfaceElevated
import com.lordyhas.muzik.ui.theme.TextPrimary
import com.lordyhas.muzik.ui.theme.TextSecondary

/** Rules that keep non-music audio (voice notes, alarms, recordings) out of the library. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFiltersSheet(
    filter: LibraryFilter,
    hiddenSongCount: Int,
    onHideRecordings: (Boolean) -> Unit,
    onMinDuration: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recordingsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("Library filters", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                text = if (hiddenSongCount > 0) "${pluralize(hiddenSongCount, "audio file")} hidden from your library"
                else "Nothing is hidden right now",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hide voice recordings", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        text = if (recordingsSupported) "Recorder and call recordings Android marks as recordings"
                        else "Needs Android 12 or newer. Hide the recordings folder instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = filter.hideRecordings && recordingsSupported,
                    onCheckedChange = onHideRecordings,
                    enabled = recordingsSupported,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AccentPrimary,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = SurfaceElevated,
                        uncheckedBorderColor = StrokeSubtle
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Skip short audio", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                "Hides clips like voice notes, alarm and notification sounds",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryFilter.MIN_DURATION_OPTIONS.forEach { ms ->
                    FilterChip(
                        selected = filter.minDurationMs == ms,
                        onClick = { onMinDuration(ms) },
                        label = { Text(formatMinDuration(ms)) },
                        colors = chipColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "To hide a whole folder, use the eye button next to it in Folders.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

private fun formatMinDuration(ms: Long): String = when {
    ms <= 0L -> "Off"
    ms < 60_000L -> "< ${ms / 1000} sec"
    else -> "< ${ms / 60_000} min"
}
