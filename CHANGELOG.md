# Changelog

## 0.1.1

- Toasts follow Sileo's 6 second clock: open at 150ms, fold back at 4s, leave at 6s.
- On a phone with no reported cutout, the pill sits below the status bar.
- The status bar darkens only while a capsule is showing.
- The sample gallery includes success, error, warning, info, action, promise, and a boarding card.

## 0.1.0

- First release of Isle (`minSdk` 24, MIT).
- `IsleHost` draws one capsule inside the host app window.
- Flash, task, and session lifetimes.
- Notch placement, with a top-center fallback when there is no cutout, the device is in landscape, or the hole is in a corner.
- JitPack coordinate `com.github.abdul-wahab-9d:isle:0.1.0`.
