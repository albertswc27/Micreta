# ADR-0005: Audio ducking via AudioFocus + USAGE_ASSISTANCE_NAVIGATION_GUIDANCE

- Status: Accepted
- Date: 2026-05-19
- Sprint: v0.2.0

## Context

D06 requires music / podcasts to dip down when Micreta speaks and ramp
back up when she finishes — the same behaviour as Google Maps spoken
navigation.

## Decision

`TextToSpeechManager`:

- Sets `AudioAttributes` with `CONTENT_TYPE_SPEECH` +
  `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`. Music streams treat this as a
  ducking-eligible utterance.
- Requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` immediately before each
  TTS utterance starts (`UtteranceProgressListener.onStart`).
- Abandons audio focus on `onDone` / `onError`.

The duck behaviour is toggleable via `AppSettings.audioDuckingEnabled`,
plumbed into the `duckingEnabled` volatile on the TTS manager so changing
the setting takes effect immediately.

## Consequences

- **+** Works with Spotify, YT Music, Apple Music, podcast apps — anything
  honouring the standard AudioFocus contract.
- **+** No need to talk to a specific player API.
- **−** Apps that ignore AudioFocus (rare, mostly broken implementations)
  won't duck. The user has the manual volume controls as fallback.
