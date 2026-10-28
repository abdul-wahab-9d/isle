# Isle

Isle is one capsule for an Android app. It is born from the device notch, opens into a card, and closes back into the hole. The host app picks a lifetime. Isle draws inside that app's window only. It is not a system overlay, and it does not move the status-bar clock or battery.

Minimum Android version is 7.0 (`minSdk` 24). Licensed under MIT. See [LICENSE](LICENSE).

## Install

Add the JitPack repository, then:

```kotlin
dependencies {
    implementation("com.github.abdul-wahab-9d:isle:0.1.2")
}
```

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

JitPack builds that coordinate from the git tag `0.1.2`. The sample app in `:sample` is only for trying Isle. It is not part of the published artifact.

## Three calls

Wrap the screen in `IsleHost`. Read `LocalIsleController.current` from `io.github.abdulwahab9d.isle`. A flash or a task replaces whatever is in front. A session stays underneath and is shown again when the front item leaves. A flash borrows the capsule from a live session and hands it back.

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.abdulwahab9d.isle.IsleHost
import io.github.abdulwahab9d.isle.LocalIsleController
import io.github.abdulwahab9d.isle.SessionHandle
import io.github.abdulwahab9d.isle.TaskResult
import io.github.abdulwahab9d.isle.Tone
import kotlinx.coroutines.delay

IsleHost {
    val isle = LocalIsleController.current
    var recording by remember { mutableStateOf<SessionHandle?>(null) }

    Button(onClick = {
        isle.flash(
            title = "Copied",
            description = "On the clipboard",
            tone = Tone.Success,
            durationSeconds = 3.0,
        )
    }) {
        Text("Flash")
    }

    Button(onClick = {
        isle.task(title = "Translating") {
            delay(1_000)
            TaskResult(title = "Done", tone = Tone.Success)
        }
    }) {
        Text("Task")
    }

    Button(onClick = {
        recording = isle.session(
            id = "recording",
            title = "Recording",
            description = "Tap the capsule",
        )
    }) {
        Text("Session")
    }

    Button(onClick = { recording?.dismiss() }) {
        Text("Dismiss")
    }
}
```

- **Flash** leaves on its own. Use it for saved, copied, or sent.
- **Task** stays compact while the suspend block runs, then shows the `TaskResult`, then leaves. The block is cancelled if the host leaves the composition.
- **Session** stays parked until `dismiss()`. Tap the capsule to open the card. `id` is how you update or dismiss that same session.

`Tone` is `Loading`, `Success`, `Error`, `Warning`, or `Info`. `AnchorRequest.Notch` is the default. `AnchorRequest.TopCenter` always uses the fallback below.

## Notch fallback

In portrait, a hole near the top center is the anchor. Isle uses a top-center pill instead when:

- the device has no cutout
- the device is in landscape
- the hole is in a corner

`IsleHost` asks the activity, on API 28 and newer, to lay out into short-edge cutouts so the capsule can sit on the hole. The drawing is still clipped to the app window.

## Sample

The `sample` module has the three calls and a fourth control that dismisses the recording session.
