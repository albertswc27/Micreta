# Micreta — copiloto IA para Nissan Micra K13

Micreta es un copiloto / mascota digital para coche, pensado primero como app
Android y más adelante como módulo físico en la rejilla de ventilación. Se
activa cuando detecta que has entrado en el coche (BT del coche, cargador o
**velocidad GPS sostenida**), te pregunta a dónde vas, abre Waze, controla
música y lee datos del motor por OBD2 Bluetooth **solo cuando se lo pides**.

La versión actual es **v0.2.8 "Daily driver"** — todo corre localmente, sin backend ni
nube. Para la integración de [ruvnet/ruflo](https://github.com/ruvnet/ruflo)
en el workflow de desarrollo ver [`RUFLO_INTEGRATION.md`](RUFLO_INTEGRATION.md).

[![Descargar APK](https://img.shields.io/badge/Descargar%20APK-Micreta%20debug-2ea44f?style=for-the-badge&logo=android)](https://github.com/albertswc27/Micreta/releases/latest/download/Micreta-debug.apk)
[![Descargar guion de pruebas](https://img.shields.io/badge/Descargar%20guion-PDF-0969da?style=for-the-badge&logo=readme)](docs/guion-pruebas-en-parado.pdf)

La APK más reciente se publica en [GitHub Releases](https://github.com/albertswc27/Micreta/releases/latest).
No necesitas conectar el móvil al portátil: abre el enlace desde Android,
descarga `Micreta-debug.apk` y permite instalar apps desconocidas para tu
navegador o gestor de archivos. Si prefieres instalar desde el portátil,
también puedes usar `adb install app/build/outputs/apk/debug/app-debug.apk`.

Para revisar la app con el coche parado, descarga el
[guion de pruebas en PDF](docs/guion-pruebas-en-parado.pdf) o consulta la
[versión editable en Markdown](docs/guion-pruebas-en-parado.md).

---

## Estado v0.2.7 — auditoría de flujos

Leyenda: ✅ funciona · 🧪 experimental · ⏳ pendiente · 🎭 mock/demo.

| Área | Estado |
|---|---|
| Navegación entre pantallas (Home/Conducción/Voz/Coche/Viajes/Ajustes) | ✅ back stack único y predecible |
| Voz: enrutado de comandos (música, gasolinera, diagnóstico, destinos) | ✅ se parsea antes de tratar como destino; tolera frases imperfectas |
| Voz: se detiene el micrófono al salir de la pantalla Voz | ✅ |
| Música: "pon música" reanuda la app activa (no fuerza abrir Spotify) | ✅ |
| Gasolineras: ubicación real (FusedLocation) + precios Ministerio | ✅ (precios donde la API los da, si no "Precio no disponible") |
| Recordatorios mantenimiento por km y por fecha (ITV, seguro…) | ✅ |
| Aviso de conducción > 2 h | ✅ |
| Wake word "Micra" (Picovoice Porcupine v4, español) | 🧪 integrado; necesita AccessKey de Picovoice. V1 solo con la app en primer plano |
| Anuncio de canción al cambiar | ⏳ requiere acceso a notificaciones |
| Radares DGT | ⏳ pendiente de empaquetar dataset |
| OBD real (ELM327) | 🎭 mock por defecto; real al configurar adaptador |

### Cambios de esta versión
- **Enrutado de voz robusto**: "pon música", "música", "pon Spotify/Velune", "gasolinera cercana",
  "diagnóstico", "cómo está el coche" se interpretan correctamente; una frase solo se trata como
  destino si el parser no la reconoce y se estaba esperando destino. Cubierto con tests.
- **Navegación**: back stack único (sin estados raros al volver a Home).
- **Micrófono**: se detiene al abandonar la pantalla Voz (no arrastra estado).
- **Gasolineras**: nunca usan Barcelona como fallback; usan tu ubicación real o muestran error claro.
- **Wake word**: interfaz `WakeWordManager` + impl deshabilitada + ajuste "Activar Hola Micreta"
  (desactivado mientras no haya motor) + aviso "todavía no disponible".
- **Paleta visual** navy/azul/cian centralizada en `ui/theme/Color.kt` + `Theme.kt`.
- **Pantalla Voz**: chips de sugerencias ("Pon música", "Llévame a casa", "Gasolinera cercana", "Diagnóstico").

### Limitaciones actuales
- Wake word "Micra": motor integrado (Porcupine v4), pero **necesita una AccessKey de Picovoice**
  para arrancar. Sin clave, el ajuste queda desactivado. V1 escucha solo con la app en primer plano
  (sin micrófono en segundo plano, por batería/privacidad); en background se usa la acción "Hablar".
- Precios de gasolina: solo donde la API del Ministerio los publica; nunca se inventan.
- Radares DGT y anuncio de canción: pendientes (ver tabla).

### Activar el wake word "Micra"
La clave de Picovoice **nunca se guarda en el repo**. Dos formas:

1. **Para la APK publicada (Releases):** en GitHub → *Settings → Secrets and variables → Actions →
   New repository secret* → nombre `PICOVOICE_ACCESS_KEY`, valor tu AccessKey. El workflow la inyecta
   al compilar. Vuelve a publicar (tag `v*`) y la descarga ya traerá el wake word.
2. **Para compilar en local / Android Studio:** añade a `local.properties`:
   `picovoice.accessKey=TU_ACCESS_KEY`

Luego, en el móvil: concede permiso de **micrófono** (toca el botón de voz una vez) y en
**Ajustes** activa *"Activar wake word Micra"*. Con la app abierta, di **"Micra"** para activar la voz.

## Último añadido en v0.2.6

- J04: avatar visual de Micreta sincronizado con el estado real de la app/coche.
- Dashboard principal con estado compacto: modo conducción, coche, Bluetooth y estado de Micreta.
- Nuevos assets pixel-art integrados: neutra, escuchando, pensando, feliz y secuencia bailando.

## Qué hay nuevo en v0.2.0

**Activación inteligente**
- A03 Activación por velocidad GPS sostenida (>15 km/h durante 12 s).
- A09 Modo nocturno automático (21:00–07:00).

**Voz**
- B02 Conversación multi-turno (Micreta repregunta destino si lo omites).
- B04 Saludo proactivo contextual (hora del día + clima).
- B08 Personalidad ajustable: amigable / formal / gamberra / robótica.
- B10 Comandos personalizados editables desde la app.

**Navegación**
- C03 ETA automático a contacto vía WhatsApp/SMS o picker de Android.
- C07 Memoria de ruta inversa al coche.
- C09 Parking memorizado al desconectar BT del coche.
- C12 Destinos especiales por voz ("a casa", "a la gasolinera").

**Música**
- D03 Reanudación automática de la última app musical al entrar.
- D06 Audio ducking durante TTS (AudioFocus + USAGE_ASSISTANCE_NAVIGATION).

**Telemetría — solo bajo demanda por voz**
- E04 Historial de viajes persistente (JSON en DataStore).
- E05 Eco-score por viaje basado en aceleraciones / frenadas / excesos.
- E06 Diccionario local de DTCs en español (~200 códigos curados).
- E09 Detección de aceleración brusca por acelerómetro del móvil.
- E11 Resumen de viaje hablado al apagar el modo conducción.

**Seguridad**
- F01 Aviso de exceso de velocidad usando GPS + OpenStreetMap Overpass.
- F06 No molestar estricto durante conducción (priority filter).
- F11 SOS por voz con countdown cancelable (ACTION_DIAL, no auto-call).

**Mantenimiento y productividad**
- G01 Recordatorio de cambio de aceite por km.
- G06 Log de repostajes con consumo real derivado.
- I01 Resumen del calendario al activarse.
- I03 Resumen meteorológico (Open-Meteo).

**Gamificación**
- J05 Reacciones emocionales habladas a eventos del viaje.

**Plataforma**
- L08 Pantalla "Salud del sistema".
- Script de copia a `C:\Users\alber\OneDrive\Escritorio\Proyectos\CarModule`
  (`scripts/copy_to_carmodule.bat` — un doble click).
- 6 ADRs en `docs/adr/` documentando las decisiones arquitectónicas clave.

**Endurecimiento v0.1.0 (feedback Drive)**
- A01 Máquina de estados Bluetooth estricta (no auriculares ≠ coche).
- B01 Tabla cerrada de intents con fallback verbal útil.
- E01 Separación visual explícita mock/real en pantalla del coche, con
  unidades en cada métrica y descripción humana de los DTCs.

## Características v0.1.0 (recordatorio)

- Avatar Micreta con animación de respiración y estados emocionales.
- Modo conducción con foreground service.
- Detección por Bluetooth del coche + cargador.
- TTS en español + SpeechRecognizer para comandos.
- Apertura de Waze con favoritos y alias de voz.
- Control multimedia universal.
- OBD2 Bluetooth (ELM327) con parser de PIDs estándar y mock.

## Stack técnico

- Kotlin 1.9.22 + Jetpack Compose (Material 3) · AGP 8.2.2 · Gradle 8.5.
- Google Play Services Location 21.1.0 (FusedLocationProvider).
- DataStore Preferences + Navigation Compose + Lifecycle Service.
- Coroutines + Flow. Ningún Firebase, ningún backend obligatorio.
- DI manual ([ADR-0002](docs/adr/0002-manual-dependency-injection.md)).

## Estructura del proyecto

```
Micreta/
├── README.md
├── FEATURES.md                ← catálogo de automatizaciones marcable
├── RUFLO_INTEGRATION.md       ← cómo integrar ruflo en el workflow
├── docs/adr/                  ← 6 ADRs (ruflo-adr compatible)
├── scripts/copy_to_carmodule.* ← copia el proyecto al Escritorio
├── tools/ruflo/               ← bootstrap notes para ruflo
└── app/src/main/java/com/micreta/app/
    ├── MicretaApp.kt           ← Application + seeding + auto-start triggers
    ├── AppContainer.kt         ← Manual DI singleton container
    ├── MainActivity.kt
    ├── navigation/             ← NavHost (12 rutas) + bottom bar
    ├── core/
    │   ├── activation/         ← GpsSpeedActivationWatcher, NightMode
    │   ├── bluetooth/          ← BluetoothCarStateMachine, receivers
    │   ├── calendar/           ← CalendarReader
    │   ├── location/           ← LocationService (Fused)
    │   ├── logging/            ← EventLogger
    │   ├── media/              ← MediaControllerManager
    │   ├── navigation/         ← WazeNavigator
    │   ├── net/                ← HttpJson (tiny GET helper)
    │   ├── permissions/        ← PermissionsManager
    │   ├── safety/             ← DoNotDisturbController, SosController, SpeedLimitWatcher
    │   ├── sensors/            ← MotionSensor (acelerómetro)
    │   ├── share/              ← ShareIntents (SMS/share/geo)
    │   ├── storage/            ← DataStore singleton
    │   ├── traffic/            ← SpeedLimitClient (Overpass)
    │   ├── voice/              ← TTS (con ducking), SpeechRecognizer, CommandParser
    │   └── weather/            ← WeatherClient (Open-Meteo)
    ├── data/
    │   ├── obd/                ← Elm327Client, ObdPidParser, ObdRepository, MockObdSource, DtcDictionary
    │   ├── preferences/        ← Settings/Favorites/Maintenance/Refuel/Parking/CustomCommands repos
    │   └── trip/               ← TripRecorder, TripRepository
    ├── domain/
    │   ├── model/              ← 12 modelos (TripSession, TripSummary, etc.)
    │   └── personality/        ← MicretaPersonalityEngine (4 perfiles)
    ├── service/
    │   └── MicretaForegroundService.kt
    ├── ui/{components,theme}/
    └── feature/
        ├── home/ driving/ voice/ obd/ settings/ debug/ about/
        ├── trips/ maintenance/ refuel/ parking/ health/
        └── settings/CustomCommandsScreen.kt
```

## Cómo abrir el proyecto

1. Instala Android Studio Hedgehog (2023.1.1) o superior.
2. **File → Open…** y selecciona la carpeta raíz `Micreta/` (o, si has
   ejecutado el script de copia, `CarModule/`).
3. Android Studio descarga Gradle 8.5, AGP 8.2 y las dependencias de Compose
   en la primera apertura (5-10 min).
4. SDK 34 instalado + dispositivo / emulador con Android 8.0+ (API 26+).

## Cómo compilar

```bash
# APK debug (recomendado para probar en el coche)
./gradlew assembleDebug

# Instalar en móvil conectado por USB
./gradlew installDebug
```

En Windows: `gradlew.bat` en lugar de `./gradlew`.

## Copiar el proyecto a CarModule

```powershell
powershell -ExecutionPolicy Bypass -File scripts\copy_to_carmodule.ps1
```

O doble click sobre `scripts\copy_to_carmodule.bat`.

Copia el proyecto sin `build/`, `.gradle/`, `.idea/` ni APKs a
`C:\Users\alber\OneDrive\Escritorio\Proyectos\CarModule`.

## Permisos

| Permiso | Sprint | Uso |
|---|---|---|
| `RECORD_AUDIO` | v0.1.0 | Escuchar destinos y comandos. |
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` (API 31+) | v0.1.0 | Detectar coche + conectar ELM327. |
| `BLUETOOTH` + `ACCESS_FINE_LOCATION` (API ≤30) | v0.1.0 | Equivalente legado. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE` + `..._LOCATION` | v0.1.0 / v0.2.0 | Modo conducción persistente. |
| `POST_NOTIFICATIONS` (API 33+) | v0.1.0 | Notificación persistente. |
| `ACCESS_FINE_LOCATION` (foreground + background) | v0.2.0 | A03 activación por velocidad, F01 límite, C09 parking. |
| `ACTIVITY_RECOGNITION` (API 29+) | v0.2.0 | Eco-driving por acelerómetro. |
| `READ_CALENDAR` | v0.2.0 | Resumen agenda (I01). |
| `CALL_PHONE` (opcional) | v0.2.0 | SOS — solo si el usuario configura un número no-emergencias. |
| `VIBRATE` | v0.2.0 | Feedback háptico de alertas. |

## Comandos por voz que entiende Micreta

**Navegación**
- "llévame a la UAB", "vamos al gimnasio", "ir al taller"
- "a casa" / "volver a casa"
- "a la gasolinera"
- "dónde he aparcado" / "busca mi coche" / "llévame al coche"
- "volver" / "ruta de vuelta"
- "avisa a Marta que voy al gimnasio"

**Música**
- "pon música" / "pausa" / "siguiente" / "anterior" / "más volumen"
- "pon mi playlist [nombre]"

**Coche (on-demand)**
- "diagnóstico" / "cómo está el coche" / "lee el coche"
- "monitoriza el coche" / "para de monitorizar"
- "resumen del viaje"

**Productividad**
- "qué tiempo hace" / "meteorología"
- "qué tengo hoy" / "agenda" / "próxima reunión"
- "apunta el repostaje"

**Seguridad**
- "SOS" / "llama a emergencias" / "necesito ayuda" → countdown 5 s
- "cancela" / "para" → aborta SOS

**Lifecycle**
- "para Micreta" / "salir del modo conducción" / "apágate"
- "sí" / "no" → multi-turno

## Cómo probar Bluetooth

1. Empareja primero el dispositivo desde **Ajustes Android → Bluetooth**.
2. App → **Ajustes → Configurar Bluetooth** → toca el dispositivo bajo
   "Mi coche" y, si tienes, bajo "Adaptador OBD2".
3. En **Ajustes**, activa "Activar al conectar Bluetooth del coche".
4. Al subir al coche y arrancar, Android emite `ACL_CONNECTED` → la
   máquina de estados ([ADR-0003](docs/adr/0003-bluetooth-state-machine.md))
   valida que el MAC coincide → la app arranca driving mode.

## Cómo probar Waze

1. **Ajustes → Favoritos** → edita "Universidad" con dirección
   `Universitat Autònoma de Barcelona, Bellaterra` y alias `UAB, uni`.
2. **Preguntar destino** → "vamos a la UAB".
3. Micreta lo encuentra, confirma, abre Waze con la dirección.
4. Sin Waze instalado → fallback a `https://waze.com/ul?q=...`.

## Cómo probar OBD2 (on-demand)

### Modo mock
1. **Ajustes → Modo demo** ON.
2. **Coche** → **Lectura única** o **Monitorizar**.
3. La pantalla muestra banner azul "Demo/mock".

### Modo real (ELM327)
1. Empareja el ELM327 (PIN típico `1234` / `0000`).
2. **Ajustes → Configurar Bluetooth** → tócalo bajo "Adaptador OBD2".
3. **Modo demo** en OFF.
4. **Coche** → **Lectura única** o **Monitorizar** → banner verde "OBD real".
5. O por voz: "cómo está el coche" → lectura única + resumen hablado.

PIDs poleados: `010C 010D 0105 0104 0111 010F 012F 0142 03`. K13 no expone
combustible/odómetro de forma fiable — campos vacíos quedan "No disponible"
(nunca inventamos datos).

## Cómo probar A03 GPS speed activation

1. **Ajustes → Activar por velocidad GPS** ON.
2. Concede permiso de ubicación (fine + background).
3. Una vez en el coche, tras ~12 s a >15 km/h Micreta arranca sola.

## Pruebas manuales recomendadas (sprint v0.2.0)

1. ✅ Apertura app → avatar respirando, sin crashes.
2. ✅ Probar TTS (Debug) con cada uno de los 4 perfiles de personalidad.
3. ✅ Pedir destino → "llévame a la UAB" → Waze se abre.
4. ✅ "qué tiempo hace" → Micreta dicta temperatura + condición.
5. ✅ "qué tengo hoy" → Micreta dicta próximos eventos.
6. ✅ "diagnóstico" en modo demo → snapshot mock, sin polling continuo.
7. ✅ Modo demo + activar Micreta + dejar 3 min en silencio + desactivar →
   resumen de viaje hablado con eco-score.
8. ✅ SOS → "llama a emergencias" → countdown 5 s + dialer abierto.
9. ✅ "cancela" durante el countdown → abortado correctamente.
10. ✅ Agregar comando personalizado "tema chill" → SPEAK "modo relax" →
    decirlo por voz → Micreta lo ejecuta.
11. ✅ Activar GPS speed activation + pasear en coche a >15 km/h durante 15 s
    sin BT del coche → driving mode arranca solo.
12. ✅ Conectar auriculares Bluetooth con la app abierta → no debe activar
    driving mode (BT state machine descarta no-coches).
13. ✅ Reanudación música: configurar Spotify, activar driving mode → tras
    2.5 s Spotify abre y empieza.
14. ✅ Hablar con Micreta mientras Spotify reproduce → música baja
    automáticamente y vuelve al terminar (ducking).
15. ✅ Salud del sistema → todos los subsistemas en OK.

## Roadmap V2 — módulo físico

1. Módulo ESP32-S3 con pantalla OLED/IPS 1.69" en la rejilla.
2. Avatar Micreta animado externo sincronizado por BLE.
3. LEDs ambientales WS2812 sincronizados con el estado.
4. Wake word offline en el propio ESP32 (TFLite Micro) — A06.
5. Lectura OBD2 directa desde ESP32 → módulo funcional sin móvil.
6. Carcasa 3D imprimible (clip de rejilla estándar).
7. Sensor de calidad de aire CO2/VOC.
8. Companion app Wear OS.

Cuando entre el hardware, [`ruflo-iot-cognitum`](RUFLO_INTEGRATION.md)
nos da trust scoring + anomaly detection out-of-the-box.

## Decisiones arquitectónicas

Ver [`docs/adr/`](docs/adr/README.md) para los 6 ADRs:

1. [OBD on-demand only](docs/adr/0001-obd-on-demand.md)
2. [Manual DI sin Hilt](docs/adr/0002-manual-dependency-injection.md)
3. [Máquina de estados Bluetooth](docs/adr/0003-bluetooth-state-machine.md)
4. [Trip telemetry from phone sensors](docs/adr/0004-trip-telemetry-from-phone.md)
5. [Audio ducking via AudioFocus](docs/adr/0005-audio-ducking.md)
6. [SOS via ACTION_DIAL](docs/adr/0006-sos-dial-intent.md)
