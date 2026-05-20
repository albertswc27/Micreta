package com.micreta.app.feature.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.service.MicretaForegroundService
import com.micreta.app.ui.components.MicretaAvatar
import com.micreta.app.ui.components.PrimaryActionButton

/**
 * Big-buttons screen optimized for driving:
 *  - avatar + state on top
 *  - voice trigger (large)
 *  - media row: prev / play-pause / next
 *  - stop driving (full width)
 *
 * Buttons are sized so a thumb on the steering wheel can hit them — no chevrons,
 * no small fonts, no nested menus.
 */
@Composable
fun DrivingScreen(
    onStartVoice: () -> Unit
) {
    val app = MicretaApp.get()
    val state by app.container.micretaState.collectAsStateWithLifecycle()
    val media = app.container.media

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.height(180.dp), contentAlignment = Alignment.Center) {
            MicretaAvatar(state = state, size = 160.dp)
        }
        Text("Modo conducción", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        PrimaryActionButton(
            label = "Hablar con Micreta",
            icon = Icons.Filled.Mic,
            onClick = onStartVoice,
            modifier = Modifier.fillMaxWidth().height(96.dp),
            container = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryActionButton(
                label = "◀",
                icon = Icons.Filled.SkipPrevious,
                onClick = { media.previous() },
                modifier = Modifier.weight(1f).height(80.dp),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface
            )
            PrimaryActionButton(
                label = "Play",
                icon = Icons.Filled.PlayCircle,
                onClick = { media.playPause() },
                modifier = Modifier.weight(1f).height(80.dp),
                container = MaterialTheme.colorScheme.secondary,
                content = MaterialTheme.colorScheme.onSecondary
            )
            PrimaryActionButton(
                label = "▶",
                icon = Icons.Filled.SkipNext,
                onClick = { media.next() },
                modifier = Modifier.weight(1f).height(80.dp),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.weight(1f))

        PrimaryActionButton(
            label = "Salir del modo conducción",
            icon = Icons.Filled.Stop,
            onClick = { MicretaForegroundService.stop(app) },
            modifier = Modifier.fillMaxWidth().height(72.dp),
            container = MaterialTheme.colorScheme.error,
            content = MaterialTheme.colorScheme.onError
        )
    }
}
