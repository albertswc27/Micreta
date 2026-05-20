# ADR-0001: OBD2 telemetry is on-demand only

- Status: Accepted
- Date: 2026-05-19
- Sprint: v0.2.0

## Context

In v0.1.0 the foreground service auto-started OBD polling at 1 Hz the
moment driving mode was entered. While simple, this had three issues:

1. **Battery drain on the ELM327 adapter** — many clones over-heat when
   polled continuously for long drives.
2. **Bluetooth contention** — sharing the BT radio between car handsfree,
   OBD adapter and phone audio causes audible stutter on cheap chipsets.
3. **User feedback (Drive doc edit by Albert):** *"E · OBD2 / Telemetría
   avanzada, solo cuando lo indique por voz"* — Albert explicitly wants
   telemetry to surface only when asked.

## Decision

`ObdRepository` exposes three modes:

- `snapshot(mac)` — connect, read every PID once, disconnect. Triggered
  by the voice command `diagnóstico` / `cómo está el coche`.
- `startContinuous(mac)` — long-lived 1 Hz polling. Only started from
  the Vehicle Status screen or the voice command `monitoriza el coche`.
- `startMock()` — for demo mode.

The foreground service no longer touches OBD. Trip telemetry (distance,
eco-score, harsh events) is captured from **phone sensors only** — see
[ADR-0004](0004-trip-telemetry-from-phone.md).

## Consequences

- **+** Lower BT contention, longer ELM327 lifespan.
- **+** Clearer mental model: the data the user sees was requested by
  them. No surprise polling.
- **−** Vehicle status screen needs an explicit "Lectura única / Monitorizar"
  button (now implemented).
- **−** Trip summaries miss real consumption (MAF/MAP-derived). Estimated
  instead from average speed profile — flagged in UI as estimated.
