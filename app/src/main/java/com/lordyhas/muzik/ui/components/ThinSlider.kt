package com.lordyhas.muzik.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lordyhas.muzik.ui.theme.AccentPrimary
import com.lordyhas.muzik.ui.theme.TrackBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    thumbColor: Color = AccentPrimary,
    activeTrackColor: Color = AccentPrimary,
    inactiveTrackColor: Color = TrackBackground,
    thumbSize: Dp = 16.dp,
    trackHeight: Dp = 3.dp,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .background(thumbColor, CircleShape)
            )
        },
        track = { sliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
            ) {
                val strokeWidth = trackHeight.toPx()
                val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                val fraction = if (range != 0f) {
                    (sliderState.value - sliderState.valueRange.start) / range
                } else 0f

                // Track inactive (full background)
                drawLine(
                    color = inactiveTrackColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Track active (progression)
                drawLine(
                    color = activeTrackColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width * fraction, size.height / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        },
        modifier = modifier
    )
}
