package io.github.abdulwahab9d.isle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.abdulwahab9d.isle.controller.LocalIsleController
import io.github.abdulwahab9d.isle.controller.rememberIsleController

/** Stub overlay. The SDK agent draws the capsule from the controller state. */
@Composable
fun IsleHost(content: @Composable () -> Unit) {
    val controller = rememberIsleController()
    CompositionLocalProvider(LocalIsleController provides controller) {
        content()
    }
}
