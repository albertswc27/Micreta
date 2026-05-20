package com.micreta.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.micreta.app.MicretaApp
import com.micreta.app.domain.model.CustomCommand
import com.micreta.app.ui.components.MicretaCard
import com.micreta.app.ui.components.PrimaryActionButton
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CustomCommandsScreen() {
    val app = MicretaApp.get()
    val items by app.container.customCommandsRepository.commands.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Comandos personalizados", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Crea atajos de voz propios. Cuando Micreta oiga la frase exacta " +
            "ejecutará la acción seleccionada. Útil para destinos específicos, " +
            "abrir apps o frases que quieras que diga.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.forEach { c ->
            CommandRow(c,
                onSave = { updated -> scope.launch { app.container.customCommandsRepository.upsert(updated) } },
                onDelete = { scope.launch { app.container.customCommandsRepository.remove(c.id) } }
            )
        }

        PrimaryActionButton(
            label = "Añadir comando",
            icon = Icons.Filled.Add,
            onClick = {
                scope.launch {
                    app.container.customCommandsRepository.upsert(
                        CustomCommand(
                            id = UUID.randomUUID().toString(),
                            phrase = "",
                            action = CustomCommand.Action.SPEAK,
                            payload = ""
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CommandRow(
    cmd: CustomCommand,
    onSave: (CustomCommand) -> Unit,
    onDelete: () -> Unit
) {
    var phrase by remember(cmd.id) { mutableStateOf(cmd.phrase) }
    var payload by remember(cmd.id) { mutableStateOf(cmd.payload) }
    var action by remember(cmd.id) { mutableStateOf(cmd.action) }
    var enabled by remember(cmd.id) { mutableStateOf(cmd.enabled) }
    var menuOpen by remember { mutableStateOf(false) }

    MicretaCard(title = phrase.ifBlank { "(frase vacía)" }) {
        OutlinedTextField(
            value = phrase,
            onValueChange = { phrase = it; onSave(cmd.copy(phrase = it, payload = payload, action = action, enabled = enabled)) },
            label = { Text("Frase a reconocer") },
            modifier = Modifier.fillMaxWidth()
        )
        Box {
            Button(
                onClick = { menuOpen = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
            ) { Text("Acción: ${actionLabel(action)}") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                CustomCommand.Action.values().forEach { a ->
                    DropdownMenuItem(text = { Text(actionLabel(a)) }, onClick = {
                        action = a
                        menuOpen = false
                        onSave(cmd.copy(phrase = phrase, payload = payload, action = a, enabled = enabled))
                    })
                }
            }
        }
        if (action.needsPayload()) {
            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it; onSave(cmd.copy(phrase = phrase, payload = it, action = action, enabled = enabled)) },
                label = { Text(payloadLabel(action)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activado", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                onSave(cmd.copy(phrase = phrase, payload = payload, action = action, enabled = it))
            })
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}

private fun actionLabel(a: CustomCommand.Action): String = when (a) {
    CustomCommand.Action.NAVIGATE_TO -> "Navegar a"
    CustomCommand.Action.OPEN_APP -> "Abrir app"
    CustomCommand.Action.OPEN_URL -> "Abrir URL"
    CustomCommand.Action.SPEAK -> "Decir frase"
    CustomCommand.Action.DIAGNOSTIC -> "Diagnóstico"
    CustomCommand.Action.STOP_DRIVING -> "Salir conducción"
}

private fun payloadLabel(a: CustomCommand.Action): String = when (a) {
    CustomCommand.Action.NAVIGATE_TO -> "Destino o alias"
    CustomCommand.Action.OPEN_APP -> "Package (com.empresa.app)"
    CustomCommand.Action.OPEN_URL -> "https://..."
    CustomCommand.Action.SPEAK -> "Frase que dirá Micreta"
    else -> ""
}

private fun CustomCommand.Action.needsPayload(): Boolean = when (this) {
    CustomCommand.Action.DIAGNOSTIC, CustomCommand.Action.STOP_DRIVING -> false
    else -> true
}
