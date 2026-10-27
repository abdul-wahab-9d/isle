# Isle

One capsule, born from the notch. It opens into a card and closes back into the hole.

Three lifetimes, so the same library fits more than one app:

- **Flash** — saved, copied, sent. It leaves on its own.
- **Task** — upload, translate, pay. It stays compact while the work runs, then shows the result.
- **Session** — recording, timer, navigation. It stays parked until the app dismisses it.

A flash borrows the capsule from a live session and hands it back when the flash leaves.

```kotlin
IsleHost {
    val isle = LocalIsleController.current
    Button(onClick = { isle.flash(title = "Copied") }) {
        Text("Copy")
    }
}
```

Install from JitPack, once `0.1.0` is tagged:

```kotlin
implementation("com.github.abdul-wahab-9d:isle:0.1.0")
```

Minimum Android version is 7.0 (`minSdk` 24). The behavior contract is [SPEC.md](SPEC.md). MIT licensed.
