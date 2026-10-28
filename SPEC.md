# Isle

A small Android library. One capsule is born from the device notch, opens into a card, and closes back into the notch. Other apps pick a lifetime. The drawing, the anchor, and the lifetimes are the whole product.

Isle is not a system overlay. It draws inside the host app's window. It does not move the status-bar clock or battery.

## Scenarios

| Lifetime | Call | Behavior |
|---|---|---|
| Flash | `flash` | Born from the notch, opens, leaves on its own |
| Task | `task` | Compact while the job runs, then a result, then leaves |
| Session | `session` | Stays parked until the app dismisses it. Tap opens the card |

Shared options on every lifetime: `Tone` (`Loading`, `Success`, `Error`, `Warning`, `Info`), title, optional description, optional action label, optional progress `0f..1f`, optional custom expanded body, `AnchorRequest` (`Notch` default, or `TopCenter`).

## Two slots

- `foreground` holds the current flash, running task, or task result.
- `session` stays underneath.
- A new flash or task replaces `foreground` only. The session remains.
- When `foreground` ends or is dismissed, the session is visible again, at its stored expanded flag (default compact).
- A new session replaces the stored session only. It does not cancel a flash that is already showing.
- A newer flash replaces the flash on top. The session stays underneath.
- `Dismiss(id)` clears every slot whose content id equals `id`.
- Same-id `ShowFlash` replaces the foreground flash and resets `ageMillis` to 0.
- Same-id `UpsertSession` replaces session content and keeps `expanded`.
- `FinishTask` applies only when `foreground` is `TaskRunning` with the same id. Otherwise it is ignored.
- `Tick` ages a `Flash` or `TaskResult`. At `ageMillis >= durationMillis`, `foreground` becomes null. `TaskRunning` does not age out.
- There is no third slot and no queue.

## Expand rules

`isExpanded(state)`:

- `Flash` with no description, no action label, and `hasCustomBody == false`: always compact.
- `Flash` with a body: expanded while `ageMillis` is in `EXPAND_DELAY_MILLIS` until `durationMillis - COLLAPSE_BEFORE_END_MILLIS`. If that end is not greater than the expand delay, stay expanded until dismiss.
- `TaskRunning`: always compact.
- `TaskResult`: same window as flash, using `TASK_RESULT_COLLAPSE_BEFORE_END_MILLIS`.
- No foreground: `session.expanded`.

`visibleContent(state)` is `foreground.content`, or `session.content` when foreground is null.

## Anchor

`resolveAnchor(input)` in package `io.github.abdulwahab9d.isle.anchor`.

A cutout qualifies when `portrait` is true, `request` is `Notch`, `cutout.top < screenHeight * TOP_BAND`, and `cutout.centerX` is inside `[screenWidth * MIDDLE_BAND_START, screenWidth * MIDDLE_BAND_END]`.

Use the qualifying cutout with the smallest `top`. Tie-break by the smallest absolute distance of `centerX` to the screen center.

If none qualify, mode is `VirtualTopCenter`. This covers no cutout, landscape, a corner hole, and `AnchorRequest.TopCenter`.

`compactHeight = COMPACT_HEIGHT_DP * density`.

`cardWidth = min(CARD_WIDTH_DP * density, screenWidth - 2 * CARD_HORIZONTAL_MARGIN_DP * density)`, at least `compactHeight`.

`cardLeft = (screenWidth - cardWidth) / 2`.

Notch mode:

- `centerX = cutout.centerX`
- `pillTop = min(cutout.centerY - compactHeight / 2, cutout.top.toFloat()).coerceAtLeast(0)`
- `minPill = cutout.width + MIN_PILL_GROWTH_DP * density`
- `safeWidth = screenWidth - 2 * SIDE_SAFE_DP * density`
- `compactMaxWidth = max(minPill, min(safeWidth, screenWidth - 2 * SCREEN_EDGE_DP * density))`
- `cardTop = max(statusBarBottom, pillTop + compactHeight)`

Virtual top center:

- `centerX = screenWidth / 2`
- `pillTop = max(0, statusBarBottom - compactHeight)`
- `compactMaxWidth = max(MIN_VIRTUAL_PILL_DP * density, min(safeWidth, screenWidth - 2 * SCREEN_EDGE_DP * density))`
- `cardTop` as above

Notch mode paints the capsule with `NOTCH_FILL_ARGB` so the hardware hole sits inside a dark pill. Virtual top center uses the same fill in v1.

## Geometry

`layoutIsle(input)` in package `io.github.abdulwahab9d.isle.geometry`.

Coerce `expandedFraction` to `0..1`. Pill width is at least `compactHeight`. Pill `x = anchorCenterX - width / 2`, `y = pillTop`, height `compactHeight`, radius `roundness`.

Full body height = `max(contentHeight, compactHeight * EXPANDED_MIN_RATIO - compactHeight)`.

Body is null when `expandedFraction == 0` or the animated height is below `1`. Otherwise body `x = cardLeft`, `y = cardTop`, width `cardWidth`, height `fullBody * expandedFraction`, radius `roundness`.

Neck is non-null only when body height is at least `NECK_MIN_BODY_PX` and `expandedFraction >= NECK_FRACTION`. The cubics connect the pill's bottom edge to the body's top edge. Tests check presence, not control-point values. Pill center x stays on `anchorCenterX` in both states. Body top stays on `cardTop`.

## Motion and goo

Compose spring uses `SPRING_DAMPING` and `SPRING_STIFFNESS`. When animator duration scale is 0, snap to the target.

On API 31+, blur the capsule layer by `ROUNDNESS_DP * density * BLUR_RATIO` and then apply a color matrix whose alpha row is `0 0 0 GOO_ALPHA_SCALE GOO_ALPHA_BIAS`. Blur the capsule only.

Below API 31, fill the pill, the body, and the neck path. No blur.

Vertical drag of the visible capsule past `SWIPE_DISMISS_DP` dismisses that slot only: foreground if one is showing, otherwise the session.

## Public SDK

Package `io.github.abdulwahab9d.isle`. Re-export `Tone` and `AnchorRequest`.

```kotlin
@Composable
fun IsleHost(content: @Composable () -> Unit)

@Composable
fun rememberIsleController(): IsleController

class IsleController {
    fun flash(
        title: String,
        description: String? = null,
        tone: Tone = Tone.Success,
        durationMillis: Long = IsleTiming.FLASH_DURATION_MILLIS,
        durationSeconds: Double? = null,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        anchor: AnchorRequest = AnchorRequest.Notch,
        id: String = /* generated */,
    )

    fun task(
        title: String,
        tone: Tone = Tone.Loading,
        anchor: AnchorRequest = AnchorRequest.Notch,
        id: String = /* generated */,
        work: suspend () -> TaskResult,
    )

    fun session(
        id: String,
        title: String,
        description: String? = null,
        tone: Tone = Tone.Info,
        progress: Float? = null,
        anchor: AnchorRequest = AnchorRequest.Notch,
        expanded: (@Composable () -> Unit)? = null,
    ): SessionHandle

    fun dismiss(id: String)
}

class TaskResult(
    val title: String,
    val description: String? = null,
    val tone: Tone,
    val durationMillis: Long = IsleTiming.TASK_RESULT_DURATION_MILLIS,
    val durationSeconds: Double? = null,
)

When `durationSeconds` is set, it replaces `durationMillis`. Omit both to keep the 6 second default. A session stays until `dismiss()`.

interface SessionHandle {
    fun update(
        title: String? = null,
        description: String? = null,
        tone: Tone? = null,
        progress: Float? = null,
    )
    fun dismiss()
}
```

`IsleController.frame` is an `IsleFrame` (`content`, `expanded`). The runtime updates it. The host only reads it. `IsleHost` provides the controller through `LocalIsleController`, draws the host content, and overlays one capsule. `task` runs in a scope cancelled when the host leaves the composition; cancellation dismisses that task id. Custom expanded content and action clicks live on the controller, not in the pure reducer. The reducer sees `hasCustomBody` and `actionLabel` only.

Tap toggles `SetSessionExpanded` only when the visible slot is the session.

## Modules

| Module | Owner | Writes |
|---|---|---|
| `isle-core` | frozen | nobody |
| `isle-geometry` | shape | `layoutIsle` and JVM tests |
| `isle-anchor` | anchor | `resolveAnchor` and JVM tests |
| `isle-runtime` | runtime | `reduce`, `isExpanded`, `visibleContent`, JVM tests |
| `isle-compose` `render` | shape | `IsleCapsule` |
| `isle-compose` `cutout` | anchor | `rememberAnchorFrame` |
| `isle-compose` `controller` | runtime | `IsleController`, `rememberIsleController` |
| `isle-compose` public + `sample` + README | sdk | `IsleHost`, sample, Maven publish, README |

Do not edit `settings.gradle.kts`, `libs.versions.toml`, or files under `isle-core`.

## Tests that must pass

`./gradlew :isle-geometry:test :isle-anchor:test :isle-runtime:test :isle-compose:assembleRelease :sample:assembleDebug`

Anchor vectors use density `1` so dp equals px. Screen `400 x 800`, status bar bottom `40`, unless a vector says otherwise.

1. Cutout `(180, 0, 220, 30)`, portrait, `Notch` → mode `Notch`, centerX `200`, pillTop `0`, cardWidth `350`, cardLeft `25`, cardTop `40`.
2. Cutout `(0, 0, 40, 40)`, portrait, `Notch` → `VirtualTopCenter`, centerX `200`.
3. No cutouts, portrait, `Notch` → `VirtualTopCenter`.
4. Cutout `(180, 0, 220, 30)`, portrait false, `Notch` → `VirtualTopCenter`.
5. Cutout `(180, 0, 220, 30)`, portrait, `TopCenter` → `VirtualTopCenter`.

Geometry: center `200`, pillTop `0`, cardTop `40`, cardLeft `25`, cardWidth `350`, pillWidth `120`, compact `40`, content `80`, roundness `16`. Fraction `0` has a pill at x `140` and a null body. Fraction `1` keeps that pill center, body top `40`, body height `80`, and a non-null neck.

Runtime:

1. Flash with a description, duration `6000`. Age `0` compact. Age `150` expanded. Age `4000` compact. Age `6000` foreground null.
2. Session `s`, then flash `f`. Visible is `f`. After the flash ages out, visible is `s` and the session is compact.
3. Flash `f1` then flash `f2`. Visible is `f2`.
4. Session `s1` then session `s2` while a flash is showing. Stored session is `s2`. Flash stays.
5. `StartTask` then `FinishTask` success. Foreground becomes `TaskResult`, then null after the result duration.
6. Session under a task is still there after the result ages out.
7. `UpsertSession` same id changes the title and keeps `expanded`.
8. `Dismiss` of the session id during a flash clears the session and keeps the flash.
9. `FinishTask` for a different id does not change state.

## Publishing

Artifact `com.github.abdul-wahab-9d:isle:0.1.2`. JitPack coordinate `com.github.abdul-wahab-9d:isle:0.1.2`. MIT license. `minSdk` 24. `compileSdk` 36.
