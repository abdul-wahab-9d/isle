package io.github.abdulwahab9d.isle.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.abdulwahab9d.isle.core.IsleLayout
import io.github.abdulwahab9d.isle.core.Tone

/** Stub. The shape agent replaces this with the goo capsule. */
@Composable
fun IsleCapsule(
    layout: IsleLayout,
    title: String,
    description: String?,
    tone: Tone,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    progress: Float?,
    expanded: (@Composable () -> Unit)?,
    fill: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
