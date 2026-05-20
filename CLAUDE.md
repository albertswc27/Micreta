Esta carpeta contiene el desarrollo completo de “Micreta”, un copiloto IA/mascota digital para automoción centrado inicialmente en un Nissan Micra K13.

OBJETIVO DEL PROYECTO
Micreta debe funcionar como un asistente inteligente con personalidad propia integrado entre:
- móvil Android,
- coche,
- voz,
- navegación,
- música,
- telemetría OBD2,
- y posteriormente hardware físico personalizado.

La prioridad actual es construir una APK Android funcional para pruebas reales en coche.

STACK PRINCIPAL
- Kotlin
- Android Studio
- Jetpack Compose
- MVVM / Clean Architecture ligera
- Bluetooth Android APIs
- OBD2 Bluetooth ELM327
- TextToSpeech
- SpeechRecognizer
- Foreground Services
- Waze intents
- MediaSession Android
- DataStore/Preferences

VISIÓN DEL PRODUCTO
Micreta NO debe sentirse como:
- una app técnica,
- un launcher Android,
- ni una pantalla de diagnóstico.

Debe sentirse como:
- una entidad viva,
- un copiloto,
- una mascota digital del coche.

El tono UX debe ser:
- minimalista,
- tecnológico,
- OEM+,
- limpio,
- emocional pero no infantil,
- estilo Porsche/Tesla/Nothing/JARVIS.

FUNCIONALIDADES PRINCIPALES
- Activación automática al detectar coche.
- Detección por Bluetooth del coche.
- Detección opcional por carga USB.
- Voz bidireccional:
  - Micreta habla.
  - Usuario responde.
- Integración Waze.
- Control multimedia básico.
- Integración OBD2 Bluetooth.
- Alertas coche:
  - temperatura,
  - batería,
  - errores DTC,
  - combustible si disponible.
- Pantalla copiloto minimalista.
- Modo conducción.
- Personalidad persistente.

ARQUITECTURA
Separar claramente:
- UI
- dominio
- voz
- navegación
- OBD2
- Bluetooth
- personalidad
- servicios foreground
- automatización Android

No mezclar lógica de negocio dentro de composables.

ESTILO DE CÓDIGO
- Código limpio y escalable.
- Modular.
- Evitar hacks rápidos.
- Evitar clases gigantes.
- Priorizar legibilidad.
- Comentar solo lo importante.
- Mantener naming consistente.
- Evitar dependencias innecesarias.

PRIORIDADES DE DESARROLLO
1. APK estable.
2. UX conducción simple.
3. Voz funcional.
4. Apertura Waze.
5. Bluetooth coche.
6. OBD2 básico.
7. Personality engine.
8. Integración hardware futura.

IMPORTANTE SOBRE OBD2
No asumir que el Nissan Micra K13 expone todos los PIDs estándar.
Si un dato no está disponible:
- mostrar “No disponible”.
- nunca inventar datos.
- diferenciar claramente datos reales y estimados.

IMPORTANTE SOBRE SEGURIDAD
- No tocar CAN bus directo.
- No asumir acceso root Android.
- No depender de APIs privadas.
- Minimizar distracciones conduciendo.
- Botones grandes y UI limpia.
- Acciones simples durante conducción.

ROADMAP FUTURO
El proyecto evolucionará hacia:
- módulo físico en ventilación,
- ESP32 BLE,
- pantalla IPS/OLED,
- LEDs ambientales,
- wake word,
- animaciones externas,
- integración hardware personalizada,
- comunicación móvil ↔ módulo físico.

ESTILO VISUAL
- Dark mode por defecto.
- Negro/gris oscuro.
- Azul eléctrico o verde suave como acento.
- Animaciones sutiles.
- Nada recargado.
- Nada “gaming RGB”.

MICRETA PERSONALITY
Micreta debe tener:
- estados emocionales,
- frases contextuales,
- sensación de presencia,
- comportamiento coherente.

Ejemplos:
- “Hola Albert. ¿A dónde vamos?”
- “He detectado tráfico.”
- “La temperatura del motor es más alta de lo normal.”
- “Todo parece correcto.”

ESTRUCTURA DE ENTREGABLES
Mantener:
- README actualizado,
- instrucciones de compilación,
- notas técnicas,
- roadmap,
- logs de pruebas reales,
- changelog por versiones.

Cada nueva funcionalidad debe:
- compilar,
- tener fallback,
- no romper flujo conducción,
- y poder probarse manualmente.

Priorizar siempre:
- estabilidad,
- experiencia de usuario,
- sensación de producto real,
sobre complejidad innecesaria.