# Micreta — catálogo de automatizaciones

Marca con `[x]` las que quieras priorizar. Cada item lleva un código (`A01`, `B07`…) que sirve de identificador para futuras conversaciones.

**Leyenda de esfuerzo**
- ⚡ rápido (<1 día)
- 🔧 medio (1-3 días)
- 🏗️ grande (1+ semana)
- 🧪 experimental / depende de hardware o ML

**Estado**
- ✅ ya hecho en `v0.1.0`
- ⭐ alta utilidad práctica
- ⚠️ requiere permiso/sensor delicado

---

## A · Detección y activación automática

- [x] `A01` ✅ Detección por Bluetooth del coche
- [x] `A02` ✅ Activación opcional por conexión del cargador
- [x] `A03` ✅ v0.2.0 Activación por velocidad GPS >15 km/h (fused location, sin necesidad de BT)
- [ ] `A04` 🔧 ⭐ Geofence "parking del coche" — auto-activa cuando entras en la zona habitual
- [ ] `A05` ⚡ Activación programada por hora (Lun-Vie 7:45, Sáb 10:00, etc.)
- [ ] `A06` 🧪 Wake word local "Hola Micreta" usando Picovoice Porcupine o Vosk (sin tocar nada)
- [ ] `A07` ⚡ Activación al colocar el móvil en el dock/soporte (sensor de proximidad + orientación landscape)
- [ ] `A08` ⚡ Auto-desactivación si el coche lleva parado >10 minutos
- [x] `A09` ✅ v0.2.0 Modo nocturno automático (UI más oscura + voz más bajita)
- [ ] `A10` ⚡ Activación por NFC tag pegado en el salpicadero (tocar móvil = activar)

## B · Voz y conversación

- [x] `B01` ✅ Comandos core: llévame, pon música, diagnóstico, salir
- [x] `B02` ✅ v0.2.0 Conversación multi-turno (Micreta puede repreguntar, confirmar, encadenar)
- [ ] `B03` 🧪 ⭐ LLM on-device para preguntas libres (Gemma 2B / Phi-3 mini con MLC Chat o llama.cpp)
- [x] `B04` ✅ v0.2.0 Saludo proactivo contextual ("buenos días, hoy hace frío" / "vienes de currar, ¿a casa?")
- [ ] `B05` ⚡ Resumen de notificaciones al arrancar el coche
- [ ] `B06` 🔧 ⚠️ Dictado de mensajes WhatsApp/SMS por voz con confirmación
- [ ] `B07` 🔧 ⚠️ Lectura por voz de mensajes entrantes (requiere `BIND_NOTIFICATION_LISTENER`)
- [x] `B08` ✅ v0.2.0 Personalidad ajustable (formal / gamberra / infantil / JARVIS robótica)
- [ ] `B09` ⚡ Multi-idioma (es-ES, es-MX, ca, en) — cambiable en ajustes
- [x] `B10` ✅ v0.2.0 Comandos personalizados por el usuario (mapeo frase → intent), editables desde la app
- [ ] `B11` ⚡ Voz cambiable (elegir entre voces TTS instaladas en Android)

## C · Navegación inteligente

- [x] `C01` ✅ Apertura de Waze con favorito + alias de voz
- [ ] `C02` 🔧 ⭐ Sugerencia predictiva de destino (aprende patrones día/hora → "¿al gym?")
- [x] `C03` ✅ v0.2.0 ETA automático a contacto vía WhatsApp/SMS al arrancar ruta
- [ ] `C04` ⚡ Aviso de tráfico antes de salir ("hoy la AP-7 tiene 25 min de atasco")
- [ ] `C05` ⚡ Multi-mapa: alternar entre Waze / Google Maps / Sygic
- [ ] `C06` 🔧 "¿Cuánto se tarda por la alternativa?" — pregunta de ruta sin perder la actual
- [x] `C07` ✅ v0.2.0 Memoria de ruta inversa (al llegar guarda origen → vuelta lo propone)
- [ ] `C08` 🔧 Gasolinera más barata en la ruta (API Geoportal Gasolineras del Min. Industria)
- [x] `C09` ✅ v0.2.0 Guardar ubicación de aparcamiento al desconectar BT
- [ ] `C10` ⚡ Recordatorio de tiempo de parking expirado
- [ ] `C11` 🔧 Avisos de radares/cámaras de tráfico (API DGT abierta)
- [x] `C12` ✅ v0.2.0 "Llévame a la última gasolinera" / "vuelve a casa" — destinos especiales

## D · Música y multimedia

- [x] `D01` ✅ Play / pause / next / prev por voz y botón
- [x] `D02` ✅ App musical configurable
- [x] `D03` ✅ v0.2.0 Resume última canción/podcast automáticamente al entrar en modo conducción
- [ ] `D04` 🔧 Sugerencia de playlist por mood (clima + hora + ruta corta/larga)
- [x] `D05` ✅ v0.2.5 "Pon mi playlist [nombre]" — lanzar búsqueda de playlist en la app musical configurada
- [x] `D06` ✅ v0.2.0 Ducking: baja la música cuando Micreta habla y la sube cuando termina
- [ ] `D07` 🔧 Comandos para podcasts: skip 30s, slow down, marcar capítulo
- [ ] `D08` ⚡ Modo audiolibro: pausa al desconectar BT, retoma al volver
- [ ] `D09` ⚡ Anuncia título de la canción al cambiar ("ahora suena: X de Y")
- [ ] `D10` ⚡ "Sin tocar nada": multimedia 100% por voz mientras conduces

## E · OBD2 / Telemetría avanzada

- [x] `E01` ✅ PIDs básicos: RPM, velocidad, refrigerante, batería, DTCs (con mock)
- [ ] `E02` 🔧 ⭐ Consumo instantáneo L/100 km (calculado con MAF/MAP + velocidad)
- [ ] `E03` 🔧 Consumo medio por viaje + persistencia histórica
- [x] `E04` ✅ v0.2.0 Historial de viajes (timestamp, km, duración, consumo medio estimado)
- [x] `E05` ✅ v0.2.0 Eco-driving score por viaje (aceleraciones bruscas + frenadas + excesos)
- [x] `E06` ✅ v0.2.0 Traducción humana de DTCs en español (BD local de ~200 códigos comunes + family hint)
- [ ] `E07` 🧪 PIDs específicos Nissan Mode 22 (combustible real, km, batería 12V) — requiere ingeniería inversa
- [ ] `E08` ⚡ Alerta de subida rápida de temperatura motor (>5°C/min)
- [x] `E09` ✅ v0.2.0 Detección de aceleración brusca (>0.4g) registrada
- [ ] `E10` ⚡ ⭐ Detección de frenada de emergencia registrada
- [x] `E11` ✅ v0.2.0 Resumen del viaje al apagar ("23 km, 6.2 L/100, eco-score 78, 1 frenazo")
- [ ] `E12` 🔧 Modo race/track: gráficos en tiempo real (RPM, throttle, G-force)
- [ ] `E13` ⚡ Borrado de DTCs por voz (con confirmación)
- [ ] `E14` ⚡ Comparación de consumo del viaje vs media histórica
- [ ] `E15` 🧪 Detección de anomalías (ML local) — coolant variando raro, batería bajando antes
- [ ] `E16` ⚡ Aviso "ojo, motor frío" si arrancas y aceleras antes de calentar

## F · Seguridad y conducción consciente

- [x] `F01` ✅ v0.2.0 Aviso de exceso de velocidad usando GPS + límites OSM (Overpass API + fallbacks ES)
- [ ] `F02` 🧪 ⚠️ Detección de fatiga por cámara frontal (eye-tracking + yawn, ML Kit Face)
- [ ] `F03` 🔧 ⭐ Detección de fatiga por patrones (zigzag de carril, microparones)
- [ ] `F04` 🔧 ⭐ Crash detection por acelerómetro + llamada de emergencia con countdown
- [ ] `F05` ⚡ Auto-respuesta SMS "estoy conduciendo, te llamo luego"
- [x] `F06` ✅ v0.2.0 Modo "no molestar" estricto durante conducción
- [ ] `F07` 🧪 Lectura de señales de tráfico por cámara (OCR + ML Kit)
- [x] `F08` ✅ v0.2.5 Aviso de conducción continua >2h ("Albert, llevas 2 horas, una pausa")
- [ ] `F09` 🧪 ⚠️ Detección de uso del móvil en mano mientras se conduce
- [ ] `F10` 🔧 Compartir ubicación en vivo con familia al iniciar viaje largo
- [x] `F11` ✅ v0.2.0 SOS por voz con countdown cancelable (ACTION_DIAL, no auto-call)
- [ ] `F12` 🧪 Aviso con cámara trasera del móvil ("se acerca un coche por detrás")
- [ ] `F13` ⚡ Modo lluvia: si llueve (API tiempo) sugiere bajar velocidad

## G · Mantenimiento del coche

- [x] `G01` ✅ v0.2.0 Recordatorio de cambio de aceite por km (también ITV y seguro por defecto)
- [x] `G02` ✅ v0.2.5 Recordatorio de revisión por fecha
- [x] `G03` ✅ v0.2.5 Cambio de neumáticos por km / fecha configurable
- [x] `G04` ✅ v0.2.5 Recordatorio de ITV (España)
- [x] `G05` ✅ v0.2.5 Recordatorio de seguro / vencimiento
- [x] `G06` ✅ v0.2.0 Log de repostajes (precio €, litros, km, consumo derivado)
- [ ] `G07` 🔧 Coste mensual del coche (gasolina + seguro + mantenimiento)
- [ ] `G08` ⚡ Aviso cambio de filtros / bujías / correa
- [ ] `G09` ⚡ Recordatorio limpieza interior/exterior cada X días
- [ ] `G10` 🔧 Export a Google Calendar de los recordatorios

## H · Hogar y geofencing

- [ ] `H01` 🔧 ⭐ "Al llegar a casa": comando a Home Assistant / Google Home (luces, clima)
- [ ] `H02` 🔧 "Al salir de casa": activar alarma, apagar luces
- [ ] `H03` 🔧 ⭐ Apertura puerta garaje al acercarse (geofence + webhook MQTT/HTTP)
- [ ] `H04` ⚡ Notificación "he llegado" a casa/curro/taller
- [ ] `H05` 🔧 Termostato al salir del curro ("calefacción a 22 a las 18:30")
- [ ] `H06` ⚡ Activar luces de bienvenida al aparcar
- [ ] `H07` 🔧 Integración Home Assistant nativa (entidades MQTT del coche en HA)
- [ ] `H08` 🧪 Bridge Matter / HomeKit (V2 con módulo físico)

## I · Productividad y rutinas

- [x] `I01` ✅ v0.2.0 Resumen del calendario por voz ("qué tengo hoy")
- [ ] `I02` 🔧 Aviso si llegas tarde + propuesta de notificar al contacto
- [x] `I03` ✅ v0.2.0 Resumen meteorológico por voz + en saludo contextual (Open-Meteo)
- [ ] `I04` ⚡ Resumen de tráfico de la ruta habitual a esa hora
- [ ] `I05` 🔧 Briefing matutino completo (rutas + clima + agenda + noticias TLDR)
- [ ] `I06` ⚡ Lista de la compra por voz ("Micreta, apunta leche")
- [ ] `I07` ⚡ ⭐ Tomar notas de voz (transcribe y exporta a Keep / Notion / fichero)
- [ ] `I08` 🔧 Recordatorios geolocalizados ("recuérdame comprar pan cuando pase por el super")
- [ ] `I09` 🔧 Exponer "modo conducción" como variable Tasker
- [ ] `I10` 🔧 Webhook saliente al activar/desactivar modo conducción (para tu domótica)
- [ ] `I11` ⚡ Modo "viaje largo": agrupa varios destinos en una sola sesión

## J · Gamificación y emoción

- [ ] `J01` 🔧 Eco-driving streak (días consecutivos con eco-score >80)
- [ ] `J02` 🔧 ⭐ Badges/logros (1000km, conducción suave, ahorro de combustible)
- [ ] `J03` 🔧 Leaderboard familiar (varias instancias compartiendo coche)
- [ ] `J04` ⚡ ⭐ Estados emocionales visuales de Micreta (feliz/triste/preocupada)
- [x] `J05` ✅ v0.2.0 Reacciones a eventos ("¡qué frenazo!", "buen consumo hoy")
- [ ] `J06` 🔧 Diario del coche / blog automático semanal
- [ ] `J07` ⚡ Customización: paleta de colores, animaciones, frases personalizadas
- [ ] `J08` ⚡ Aniversarios y kilometrajes redondos ("¡100.000 km, bravo!")
- [ ] `J09` ⚡ Micreta tiene "hambre" → equivale a gasolina baja (gamifica el repostaje)

## K · Hardware V2 — módulo físico en la rejilla

- [ ] `K01` 🏗️ ESP32-S3 con BLE emparejado al móvil
- [ ] `K02` 🏗️ ⭐ Pantalla OLED/IPS 1.69" en rejilla con avatar Micreta animado
- [ ] `K03` 🏗️ Tira LED WS2812 ambiental sincronizada con el estado
- [ ] `K04` 🧪 Wake word offline en el propio ESP32 (TFLite Micro)
- [ ] `K05` 🔧 Botón físico de pánico / SOS en el módulo
- [ ] `K06` 🏗️ Lectura OBD2 directa desde el ESP32 (no necesita el móvil)
- [ ] `K07` 🏗️ Módulo funciona en cualquier coche, incluso sin móvil
- [ ] `K08` 🏗️ Companion app para Wear OS / smartwatch
- [ ] `K09` 🔧 Carcasa 3D imprimible para clip estándar de rejilla
- [ ] `K10` 🏗️ Sensor de calidad de aire (CO2 / VOC) en el módulo
- [ ] `K11` 🔧 Termómetro / barómetro / humedad en el módulo
- [ ] `K12` 🏗️ Cámara en el módulo para detección de fatiga + dashcam

## L · Privacidad, debug y plataforma

- [ ] `L01` ⚡ Exportar logs debug a archivo (txt/json) compartible
- [ ] `L02` ⚡ Exportar historial de viajes (CSV)
- [ ] `L03` ⚡ Borrar todos los datos con confirmación
- [ ] `L04` 🔧 Encriptación local de favoritos / MAC con Android Keystore
- [ ] `L05` ⚡ Modo invitado (no guarda nada)
- [ ] `L06` ⚡ Backup/restore configuración a archivo
- [ ] `L07` 🔧 Compartir configuración entre dispositivos vía QR
- [x] `L08` ✅ v0.2.0 Pantalla "salud del sistema" (permisos, BT activo, OBD source, apps externas, modos)

---

## Cómo marcarlas

1. Edita este fichero y pon `[x]` en las que quieras
2. O respóndeme en chat con los códigos: por ejemplo *"quiero A03, A04, B02, C09, E04, E06, E11, F04, F06, G01, G06, H03, I01, I03, I07, J04"*
3. Te las priorizo, te propongo orden de implementación y empezamos
