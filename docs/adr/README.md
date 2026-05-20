# Architecture Decision Records — Micreta

Decisiones arquitectónicas de Micreta v0.1.0 / v0.2.0. Formato compatible
con [ruflo-adr](../../RUFLO_INTEGRATION.md) — la herramienta detecta y
mantiene este directorio automáticamente cuando se instale.

| # | Decisión | Sprint |
|---|---|---|
| [0001](0001-obd-on-demand.md) | OBD2 telemetry is on-demand only | v0.2.0 |
| [0002](0002-manual-dependency-injection.md) | Manual DI instead of Hilt | v0.1.0 |
| [0003](0003-bluetooth-state-machine.md) | Closed state machine for Bluetooth car detection | v0.2.0 |
| [0004](0004-trip-telemetry-from-phone.md) | Trip telemetry comes from phone sensors, not OBD | v0.2.0 |
| [0005](0005-audio-ducking.md) | Audio ducking via AudioFocus + USAGE_ASSISTANCE_NAVIGATION_GUIDANCE | v0.2.0 |
| [0006](0006-sos-dial-intent.md) | SOS uses ACTION_DIAL, not ACTION_CALL | v0.2.0 |

## Cómo añadir un ADR nuevo

1. Copia uno existente como plantilla.
2. Numera secuencialmente (`NNNN-slug.md`, 4 dígitos).
3. Estructura mínima: Status, Date, Sprint, Context, Decision, Consequences.
4. Añade la fila a esta tabla.

Si tienes ruflo instalado: `/adr create "tu decisión"` lo hace por ti.
