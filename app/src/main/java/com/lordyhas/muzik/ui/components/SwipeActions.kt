package com.lordyhas.muzik.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What a horizontal swipe does. [dismiss] slides the row out before [onTrigger] runs (for removals,
 * so the list can close the gap); otherwise the row springs back after [onTrigger].
 */
data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val dismiss: Boolean = false,
    val onTrigger: () -> Unit
)

/** Fraction of the row width a swipe must pass to trigger its action. */
private const val TRIGGER_FRACTION = 0.35f

/**
 * Spotify-style swipeable list row: swipe right for [startToEnd], left for [endToStart]. The colored
 * background grows with the swipe and lights up, with a haptic tick, once the swipe will trigger.
 */
@Composable
fun SwipeActionBox(
    modifier: Modifier = Modifier,
    startToEnd: SwipeAction? = null,
    endToStart: SwipeAction? = null,
    content: @Composable () -> Unit
) {
    if (startToEnd == null && endToStart == null) {
        Box(modifier) { content() }
        return
    }

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val cornerPx = with(LocalDensity.current) { 14.dp.toPx() }
    val iconRevealPx = with(LocalDensity.current) { 40.dp.toPx() }
    val currentStart by rememberUpdatedState(startToEnd)
    val currentEnd by rememberUpdatedState(endToStart)

    var widthPx by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    // The spring-back/slide-out of the previous swipe, cancelled when a new drag starts
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val thresholdPx = widthPx * TRIGGER_FRACTION

    val action = when {
        offsetX > 0f -> startToEnd
        offsetX < 0f -> endToStart
        else -> null
    }
    val backgroundColor by animateColorAsState(
        targetValue = action?.color?.copy(alpha = if (armed) 1f else 0.45f) ?: Color.Transparent,
        label = "swipe_bg"
    )

    val dragState = rememberDraggableState { delta ->
        val min = if (currentEnd != null) -widthPx.toFloat() else 0f
        val max = if (currentStart != null) widthPx.toFloat() else 0f
        offsetX = (offsetX + delta).coerceIn(min, max)
        val nowArmed = thresholdPx > 0f && abs(offsetX) >= thresholdPx
        if (nowArmed != armed) {
            armed = nowArmed
            if (nowArmed) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .clipToBounds()
            .drawBehind {
                // Only the area the row has uncovered is painted
                if (offsetX != 0f) {
                    val revealed = abs(offsetX)
                    drawRoundRect(
                        color = backgroundColor,
                        topLeft = Offset(if (offsetX > 0f) 0f else size.width - revealed, 0f),
                        size = Size(revealed, size.height),
                        cornerRadius = CornerRadius(cornerPx)
                    )
                }
            }
    ) {
        if (action != null) {
            val progress = ((abs(offsetX) - iconRevealPx) / (thresholdPx - iconRevealPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(if (offsetX > 0f) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 22.dp)
                    .size(24.dp)
                    .graphicsLayer {
                        alpha = progress
                        val scale = if (armed) 1.15f else 0.7f + 0.3f * progress
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        settleJob?.cancel()
                        armed = false
                    },
                    onDragStopped = { velocity ->
                        val trigger = when {
                            !armed -> null
                            offsetX > 0f -> currentStart
                            else -> currentEnd
                        }
                        armed = false
                        settleJob = scope.launch {
                            when {
                                trigger == null -> settle(offsetX, 0f, velocity) { offsetX = it }
                                trigger.dismiss -> {
                                    val out = if (offsetX > 0f) widthPx.toFloat() else -widthPx.toFloat()
                                    animate(offsetX, out, animationSpec = tween(160)) { v, _ -> offsetX = v }
                                    trigger.onTrigger()
                                    // If the row is still shown (e.g. the list kept it), bring it back
                                    delay(400)
                                    offsetX = 0f
                                }
                                else -> {
                                    trigger.onTrigger()
                                    settle(offsetX, 0f, velocity) { offsetX = it }
                                }
                            }
                        }
                    }
                )
        ) {
            content()
        }
    }
}

private suspend fun settle(from: Float, to: Float, velocity: Float, onValue: (Float) -> Unit) {
    animate(from, to, initialVelocity = velocity, animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)) { v, _ ->
        onValue(v)
    }
}
