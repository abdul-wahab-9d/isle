package io.github.abdulwahab9d.isle.cutout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import io.github.abdulwahab9d.isle.anchor.resolveAnchor
import io.github.abdulwahab9d.isle.core.AnchorFrame
import io.github.abdulwahab9d.isle.core.AnchorInput
import io.github.abdulwahab9d.isle.core.AnchorRequest

/** Stub. The anchor agent reads the real display cutout here. */
@Composable
fun rememberAnchorFrame(request: AnchorRequest): AnchorFrame {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val widthPx = (configuration.screenWidthDp * density).toInt()
    val heightPx = (configuration.screenHeightDp * density).toInt()
    return resolveAnchor(
        AnchorInput(
            screenWidthPx = widthPx,
            screenHeightPx = heightPx,
            statusBarBottomPx = 0,
            cutouts = emptyList(),
            portrait = heightPx >= widthPx,
            request = request,
            density = density,
        ),
    )
}
