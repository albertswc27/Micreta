# ADR-0004: Trip telemetry comes from phone sensors, not OBD

- Status: Accepted
- Date: 2026-05-19
- Sprint: v0.2.0

## Context

E04 (Historial de viajes) and E05 (Eco-driving score) need continuous data
during the drive: distance, max speed, harsh accelerations, harsh brakings.

Two options were considered:

1. Continuous OBD polling — gives ground truth RPM, throttle, MAF.
2. Phone FusedLocationProvider + `Sensor.TYPE_LINEAR_ACCELERATION`.

OBD polling collides with [ADR-0001](0001-obd-on-demand.md) (on-demand only)
and adds dependency on an adapter the user may not own.

## Decision

`TripRecorder` consumes:

- `LocationService` (FusedLocationProvider, 2 s interval, high accuracy)
  for distance and max speed.
- `MotionSensor` (TYPE_LINEAR_ACCELERATION, ~50 Hz) for harsh events,
  with magnitude thresholds at 0.4 g (harsh) and 0.7 g (emergency).
- `SpeedLimitWatcher` over-speed events.

Eco-score formula: 100 − (6·harshAccel + 8·harshBrake + 4·overSpeed)·(10/km),
clamped [0, 100]. Empirically calibrated for the Micra K13 on the test
drives in the Vallés-Barcelona corridor.

Real consumption is **estimated** (avg-speed profile + harsh-event penalty),
explicitly flagged as estimated in the UI. Switching to MAF-derived
consumption is a follow-up that lives behind ADR-0001's `monitoriza el coche`
voice command — the user can opt-in for trips where they want it precise.

## Consequences

- **+** Trips work for any user, even those without an ELM327.
- **+** Battery + BT load stays low.
- **−** Distance is GPS-jitter-sensitive; mitigated by dropping deltas <5 m.
- **−** Acceleration vs braking classification is heuristic (speed-based) —
  acceptable for eco-score, would need a higher-fidelity model for safety
  alerts (deferred to F02/F03).
