package com.micreta.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micreta.app.R
import com.micreta.app.domain.model.MicretaState

/**
 * State-synced Micreta avatar.
 *
 * The dashboard uses the authored pixel-art captures from micreta_assets while
 * keeping stable layout dimensions. Motion is intentionally small: a halo for
 * state feedback and a short three-frame dance only for happy moments.
 */
@Composable
fun MicretaAvatar(
    state: MicretaState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val transition = rememberInfiniteTransition(label = "micreta-avatar")

    val haloScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == MicretaState.LISTENING) 760 else 2100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-scale"
    )

    val haloAlpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == MicretaState.LISTENING) 760 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-alpha"
    )

    val danceFrame by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2.99f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 960, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dance-frame"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val visual = visualFor(state, danceFrame.toInt())
    val haloColor = when (visual.tone) {
        AvatarTone.PRIMARY -> primary
        AvatarTone.SECONDARY -> secondary
        AvatarTone.ERROR -> error
        AvatarTone.MUTED -> muted
    }
    val intensity = when (state) {
        MicretaState.LISTENING -> 1.15f
        MicretaState.HAPPY, MicretaState.NAVIGATING -> 1.0f
        MicretaState.ALERT, MicretaState.ERROR -> 0.95f
        MicretaState.SLEEPING -> 0.35f
        else -> 0.62f
    }
    val avatarScale = when (state) {
        MicretaState.HAPPY -> 1.02f
        MicretaState.LISTENING -> 1.01f
        else -> 1f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(haloScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            haloColor.copy(alpha = (haloAlpha * intensity).coerceIn(0f, 0.42f)),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Image(
            painter = painterResource(id = visual.resId),
            contentDescription = visual.description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .scale(avatarScale)
                .alpha(if (state == MicretaState.SLEEPING) 0.78f else 1f)
        )
    }
}

private enum class AvatarTone {
    PRIMARY,
    SECONDARY,
    ERROR,
    MUTED
}

private data class AvatarVisual(
    @DrawableRes val resId: Int,
    val description: String,
    val tone: AvatarTone
)

private fun visualFor(state: MicretaState, danceFrame: Int): AvatarVisual = when (state) {
    MicretaState.SLEEPING -> AvatarVisual(
        resId = R.drawable.micreta_neutro,
        description = "Micreta en reposo",
        tone = AvatarTone.MUTED
    )
    MicretaState.DETECTING -> AvatarVisual(
        resId = R.drawable.micreta_neutro,
        description = "Micreta detectando el coche",
        tone = AvatarTone.PRIMARY
    )
    MicretaState.CONNECTED -> AvatarVisual(
        resId = R.drawable.micreta_feliz,
        description = "Micreta conectada al coche",
        tone = AvatarTone.SECONDARY
    )
    MicretaState.LISTENING -> AvatarVisual(
        resId = R.drawable.micreta_escuchando,
        description = "Micreta escuchando",
        tone = AvatarTone.SECONDARY
    )
    MicretaState.THINKING -> AvatarVisual(
        resId = R.drawable.micreta_pensando,
        description = "Micreta pensando",
        tone = AvatarTone.PRIMARY
    )
    MicretaState.NAVIGATING -> AvatarVisual(
        resId = R.drawable.micreta_feliz,
        description = "Micreta navegando",
        tone = AvatarTone.SECONDARY
    )
    MicretaState.ALERT -> AvatarVisual(
        resId = R.drawable.micreta_pensando,
        description = "Micreta en alerta",
        tone = AvatarTone.ERROR
    )
    MicretaState.HAPPY -> AvatarVisual(
        resId = when (danceFrame.coerceIn(0, 2)) {
            0 -> R.drawable.micreta_bailando_1
            1 -> R.drawable.micreta_bailando_2
            else -> R.drawable.micreta_bailando_3
        },
        description = "Micreta contenta",
        tone = AvatarTone.SECONDARY
    )
    MicretaState.NEUTRAL -> AvatarVisual(
        resId = R.drawable.micreta_neutro,
        description = "Micreta lista",
        tone = AvatarTone.PRIMARY
    )
    MicretaState.ERROR -> AvatarVisual(
        resId = R.drawable.micreta_pensando,
        description = "Micreta con error",
        tone = AvatarTone.ERROR
    )
}
