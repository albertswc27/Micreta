package com.micreta.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micreta.app.domain.model.MicretaState

/**
 * The Micreta avatar — a minimal stylized companion.
 *
 * Two animations:
 *  - Outer pulse: slow breathing, always present, communicates "alive".
 *  - Inner glow:  faster, only when LISTENING or NAVIGATING.
 *
 * Tints shift with [state]:
 *  - default → primary blue
 *  - alert → error red
 *  - sleeping → muted
 */
@Composable
fun MicretaAvatar(
    state: MicretaState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val transition = rememberInfiniteTransition(label = "micreta-avatar")

    val pulse by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200), repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.10f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == MicretaState.LISTENING) 700 else 1800),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    // Resolve theme colors here — DrawScope can't access MaterialTheme directly.
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.background

    val tint: Color = when (state) {
        MicretaState.ALERT, MicretaState.ERROR -> errorColor
        MicretaState.SLEEPING -> muted
        MicretaState.NAVIGATING -> secondary
        MicretaState.HAPPY -> secondary
        else -> primary
    }

    val mouthOpen by animateFloatAsState(
        targetValue = when (state) {
            MicretaState.LISTENING, MicretaState.THINKING -> 1f
            else -> 0f
        },
        animationSpec = tween(400),
        label = "mouth"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Outer halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w / 2f
                ),
                radius = (w / 2f) * pulse,
                center = Offset(cx, cy)
            )

            // Body
            val bodyRadius = (w / 3.2f) * pulse
            drawCircle(color = tint, radius = bodyRadius, center = Offset(cx, cy))

            // Antenna
            val antennaTop = cy - bodyRadius - (w * 0.10f)
            drawLine(
                color = secondary,
                start = Offset(cx, cy - bodyRadius + 4f),
                end = Offset(cx, antennaTop),
                strokeWidth = 6f
            )
            drawCircle(
                color = secondary,
                radius = 12f * pulse,
                center = Offset(cx, antennaTop)
            )

            // Eyes
            val eyeY = cy - bodyRadius * 0.10f
            val eyeOffset = bodyRadius * 0.40f
            val eyeRadius = bodyRadius * 0.10f
            drawCircle(color = background, radius = eyeRadius, center = Offset(cx - eyeOffset, eyeY))
            drawCircle(color = background, radius = eyeRadius, center = Offset(cx + eyeOffset, eyeY))

            // Mouth
            val mouthY = cy + bodyRadius * 0.30f
            val mouthHalfWidth = bodyRadius * 0.28f
            if (mouthOpen > 0.01f) {
                drawOval(
                    color = background,
                    topLeft = Offset(cx - mouthHalfWidth, mouthY - 6f),
                    size = Size(mouthHalfWidth * 2f, 18f * mouthOpen + 4f)
                )
            } else {
                drawLine(
                    color = background,
                    start = Offset(cx - mouthHalfWidth, mouthY),
                    end = Offset(cx + mouthHalfWidth, mouthY),
                    strokeWidth = 5f
                )
            }
        }
    }
}
