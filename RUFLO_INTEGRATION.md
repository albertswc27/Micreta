# Integración Ruflo en Micreta

[Ruflo](https://github.com/ruvnet/ruflo) es la plataforma de orquestación
multi-agente para Claude Code de Reuven Cohen: 100+ agentes especializados,
60+ comandos, 30 skills, MCP server, hooks, daemon, swarm intelligence,
RAG, federation, etc.

**Importante:** Ruflo es una herramienta de **workflow de desarrollo**, no
una librería Android. Corre como CLI / MCP server al lado de Claude Code y
asiste a quien desarrolla Micreta — generando tests, manteniendo docs,
proponiendo ADRs, auditando seguridad, etc. **No se embebe dentro de la
APK.** El runtime de Micreta sigue siendo offline-first.

## Por qué integrar ruflo en Micreta

Micreta es un proyecto Android con varias dimensiones críticas que ruflo
acelera enormemente:

- **Telemetría coche + ML local** (futura B03 LLM on-device, F02 fatiga por
  cámara): los agentes de ruflo razonan sobre el código, plantean ADRs y
  generan tests sin que el coste cognitivo recaiga 100% en el dev.
- **IoT V2 (módulo ESP32 + OBD2)**: `ruflo-iot-cognitum` ya cubre trust
  scoring, anomaly detection y gestión de flotas — directamente reutilizable
  para el módulo físico futuro.
- **Seguridad runtime** (BT pairing, geolocalización, calendario, llamadas
  de emergencia): `ruflo-security-audit` y `ruflo-aidefence` ayudan a no
  dejar permisos colgando ni superficie de prompt-injection cuando entre el
  LLM local.
- **Documentación viva** del proyecto (README, FEATURES, ADRs, runbooks de
  prueba en coche): `ruflo-docs` mantiene esto sincronizado con el código.

## Set recomendado de plugins para Micreta

| Plugin | Por qué para Micreta |
|---|---|
| `ruflo-core` | Base obligatoria — server, health checks, plugin discovery. |
| `ruflo-sparc` | Metodología guiada de 5 fases con quality gates — encaja con los sprints v0.1.0 → v0.2.0 → V2 hardware. |
| `ruflo-testgen` | Genera unit tests Kotlin para `ObdPidParser`, `CommandParser`, `TripRecorder`, `DtcDictionary`. |
| `ruflo-docs` | Mantiene `README.md`, `FEATURES.md` y los `docs/adr/` al día con el código. |
| `ruflo-adr` | Captura decisiones arquitectónicas (OBD on-demand, manual DI, BT state machine, sensor-only trip telemetry, etc.). |
| `ruflo-jujutsu` | Analiza diffs antes de subirlos a CarModule — útil cuando se cierran sprints. |
| `ruflo-security-audit` | Escanea por CVEs y vulnerabilidades en dependencias Gradle. |
| `ruflo-aidefence` | Capa de defensa para el día que entre B03 (LLM on-device): prompt injection, PII detection. |
| `ruflo-iot-cognitum` | Preparado para V2 hardware: trust scoring del ESP32, anomaly detection en telemetría. |
| `ruflo-observability` | Logs estructurados + traces + métricas; útil cuando metamos crashlytics-like local. |

## Cómo bootstrapear ruflo en este repo

Desde la raíz del proyecto (`C:\Users\alber\OneDrive\Documentos\Claude\Projects\Micreta\`
o `C:\Users\alber\OneDrive\Escritorio\Proyectos\CarModule\`):

### Path A — Plugins ligeros (solo slash commands)

```bash
# Dentro de una sesión Claude Code
/plugin marketplace add ruvnet/ruflo
/plugin install ruflo-core@ruflo
/plugin install ruflo-sparc@ruflo
/plugin install ruflo-testgen@ruflo
/plugin install ruflo-docs@ruflo
/plugin install ruflo-adr@ruflo
/plugin install ruflo-iot-cognitum@ruflo
/plugin install ruflo-security-audit@ruflo
```

Esto añade slash commands y agent definitions, **sin** registrar el MCP
server. Suficiente para empezar.

### Path B — Instalación completa (recomendada a medio plazo)

PowerShell / cmd:
```powershell
npx ruflo@latest init wizard
```

Bash / WSL:
```bash
curl -fsSL https://cdn.jsdelivr.net/gh/ruvnet/ruflo@main/scripts/install.sh | bash
```

Después registra el MCP server en Claude Code:
```bash
claude mcp add ruflo -- npx ruflo@latest mcp start
```

Esto crea `.claude/`, `.claude-flow/`, hooks, settings y deja toda la
orquestación operativa (98 agentes, 60+ comandos, 30 skills, daemon, etc.).

## Comandos típicos en Micreta tras `init`

| Slash command | Para qué en Micreta |
|---|---|
| `/sparc start "v0.2.1 driver score"` | Inicia un sprint guiado por las 5 fases SPARC: Specification, Pseudocode, Architecture, Refinement, Completion. |
| `/testgen target=app/src/main/java/com/micreta/app/data/obd` | Genera tests JUnit para todos los parsers OBD. |
| `/docs sync` | Reescaneo de código y actualización de `README.md`, `FEATURES.md`, `docs/adr/`. |
| `/adr create "OBD only on voice command"` | Crea un ADR formal capturando la decisión que ya está implementada. |
| `/security-audit deps` | Pasa `npm audit`-equivalente sobre las dependencias Gradle (`build.gradle.kts`). |
| `/iot-cognitum trust score ESP32-S3` | Cuando entre el módulo físico, calcular trust score del device. |
| `/swarm coordinate "implement A06 wake word"` | Lanza un swarm hierarchical para implementar feature compleja. |

## ADRs ya capturados

El directorio `docs/adr/` contiene las decisiones clave de Micreta v0.1.0 /
v0.2.0, escritas en formato compatible con `ruflo-adr` para que la
herramienta las recoja sin reescribirlas. Ver:

- [ADR-0001 OBD on-demand only](docs/adr/0001-obd-on-demand.md)
- [ADR-0002 Manual DI sin Hilt](docs/adr/0002-manual-dependency-injection.md)
- [ADR-0003 Máquina de estados Bluetooth para detección de coche](docs/adr/0003-bluetooth-state-machine.md)
- [ADR-0004 Telemetría de viaje desde sensores del móvil, no OBD](docs/adr/0004-trip-telemetry-from-phone.md)
- [ADR-0005 Audio ducking por AudioFocus](docs/adr/0005-audio-ducking.md)
- [ADR-0006 SOS por ACTION_DIAL en vez de ACTION_CALL](docs/adr/0006-sos-dial-intent.md)

## Qué hacer con ruflo HOY (sin instalar nada)

Aunque no instales el daemon completo, puedes ya:

1. **Leer los ADRs** en `docs/adr/` — están en el formato que ruflo entiende.
2. **Pedir a Claude Code en este proyecto** generar tests o actualizar docs;
   los slash commands de ruflo no son necesarios, pero su lógica está
   documentada arriba como referencia.
3. **Cuando tengas un sprint largo** (V2 hardware, B03 LLM), arranca ruflo
   y deja que swarm + autopilot lo gestione en background mientras tú
   conduces.

## Lo que ruflo NO va a hacer (por diseño)

- Embeber código dentro de la APK Android — ruflo es Node.js / TypeScript.
- Sustituir el TTS / SpeechRecognizer del móvil — eso sigue siendo nativo.
- Procesar telemetría OBD en tiempo real durante la conducción — eso lo
  hace Micreta on-device.
- Compartir datos del coche con la nube por defecto — la privacidad de
  Micreta es offline-first y ruflo respeta ese contrato.

## Roadmap de integración

| Fase | Cuándo | Qué |
|---|---|---|
| 0 | Ya hecho | ADRs capturados en `docs/adr/`. Esta guía. |
| 1 | Cuando quieras empezar | `npx ruflo@latest init wizard` en el repo y registrar MCP. |
| 2 | Próximo sprint v0.2.x | Activar `ruflo-testgen` y `ruflo-docs` continuos. |
| 3 | V2 hardware | `ruflo-iot-cognitum` para el ESP32, `ruflo-security-audit` antes de cada release. |
| 4 | Futuro LLM (B03) | `ruflo-aidefence` para la capa de seguridad del LLM on-device. |
