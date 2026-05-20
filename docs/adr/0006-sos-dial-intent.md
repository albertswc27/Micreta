# ADR-0006: SOS uses ACTION_DIAL, not ACTION_CALL

- Status: Accepted
- Date: 2026-05-19
- Sprint: v0.2.0

## Context

F11 (SOS por voz) needs to place an emergency call after a cancelable
countdown. Two implementations were possible:

1. `Intent.ACTION_CALL` + `CALL_PHONE` permission → auto-dials immediately.
2. `Intent.ACTION_DIAL` → opens the dialer with the number prefilled; one
   user tap to actually call.

## Decision

Always use `ACTION_DIAL`. Reasoning:

- **Safety**: a voice misfire ("emergencias" detected when the user said
  something else) should not auto-call 112. The user confirms with one tap.
- **OEM alignment**: BMW, Volvo, modern Renault dashboards all show a
  dialer-style screen before placing the SOS call.
- **Play Store policy**: apps that auto-dial emergency numbers attract
  manual review and require extra disclosures.
- **`CALL_PHONE` permission stays optional**: we only ask if the user
  configures a non-emergency SOS contact (future).

The cancelable countdown stays — the user has `cancela` voice + the
in-app cancel button before the dialer opens.

## Consequences

- **+** Zero risk of accidental call.
- **+** Lower permission surface (no runtime CALL_PHONE prompt by default).
- **−** One extra tap during a real emergency. Acceptable: 0.5 s vs the
  cost of a false-positive call.
