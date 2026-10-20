package com.lordyhas.muzik.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.SurfaceElevated
import com.lordyhas.muzik.ui.theme.TextPrimary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** A short confirmation at the bottom of the screen, optionally with one action such as Undo. */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * Shows [messages] one at a time. A new message replaces the one on screen right away, so quick
 * repeated actions (swiping several songs) never queue up stale confirmations.
 */
@Composable
fun MuzikSnackbarHost(messages: Flow<UiMessage>, modifier: Modifier = Modifier) {
    val state = remember { SnackbarHostState() }

    LaunchedEffect(messages) {
        messages.collectLatest { message ->
            val result = state.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
                // Leave time to reach Undo; plain confirmations go away quickly
                duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
        }
    }

    SnackbarHost(hostState = state, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceElevated,
            contentColor = TextPrimary,
            actionColor = AccentPrimary
        )
    }
}
