package com.micreta.app.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micreta.app.BuildConfig
import com.micreta.app.ui.components.MicretaCard

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Acerca de Micreta", style = MaterialTheme.typography.headlineMedium)

        MicretaCard(title = "Versión") {
            Text("Micreta v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyLarge)
            Text("Compilada para Nissan Micra K13 (genérico OBD-II).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        MicretaCard(title = "Qué hace") {
            Text(
                "Micreta es un copiloto / mascota digital para tu Micra K13. " +
                "Detecta cuando arrancas el coche (Bluetooth o cargador), te saluda, " +
                "te pregunta a dónde vas y abre Waze. Controla música, lee datos del " +
                "coche por OBD2 y avisa de cosas raras.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        MicretaCard(title = "Privacidad") {
            Text(
                "Todo corre localmente en el móvil. No hay servidor, no hay nube, " +
                "no se envía nada por internet. Los favoritos y ajustes se guardan " +
                "en almacenamiento privado del propio dispositivo.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        MicretaCard(title = "Roadmap V2 (hardware)") {
            Text(
                "Tras esta app móvil viene un módulo físico en la rejilla de " +
                "ventilación: ESP32 BLE, pantalla OLED/IPS con la cara de Micreta, " +
                "LEDs ambientales y comunicación móvil ↔ ESP32. Carcasa 3D, " +
                "wake-word local y posible companion para wearables.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
