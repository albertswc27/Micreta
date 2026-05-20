# ADR-0003: Closed state machine for Bluetooth car detection

- Status: Accepted
- Date: 2026-05-19
- Sprint: v0.2.0 hardening

## Context

v0.1.0 fired driving mode on `ACTION_ACL_CONNECTED` as long as the device
MAC matched the user's configured car. But the receiver could still
process events from headphones, smartwatches, speakers — any BT device the
phone connected to. Albert's review (Drive doc) called this out as a risk
of false positives.

## Decision

Introduce `BluetoothCarStateMachine`:

```
DISCONNECTED  →  DETECTING       (other device, not the car)
DETECTING     →  DISCONNECTED    (immediate — ignore non-car)
DETECTING     →  CONNECTED_CAR   (authorised MAC matched)
CONNECTED_CAR →  DRIVING_MODE    (foreground service started)
DRIVING_MODE  →  DISCONNECTED    (authorised car disconnected)
```

The receiver classifies events as `isAuthorisedCar` based on the stored
MAC, feeds the state machine, and only fires `CarDetectionEvents.Trigger`
when the authorised car connects or disconnects.

## Consequences

- **+** Connecting Bluetooth headphones never triggers driving mode.
- **+** The Debug screen and System Health screen can show the current
  state — useful in the car when something doesn't feel right.
- **−** One more piece to keep in sync when adding alternative detection
  signals (GPS speed, charger). Acceptable trade-off.
