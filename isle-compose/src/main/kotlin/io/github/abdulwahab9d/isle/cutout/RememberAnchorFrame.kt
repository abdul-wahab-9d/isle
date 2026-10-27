package io.github.abdulwahab9d.isle.cutout

import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import io.github.abdulwahab9d.isle.anchor.resolveAnchor
import io.github.abdulwahab9d.isle.core.AnchorFrame
import io.github.abdulwahab9d.isle.core.AnchorInput
import io.github.abdulwahab9d.isle.core.AnchorRequest
import io.github.abdulwahab9d.isle.core.CutoutRect

@Composable
fun rememberAnchorFrame(request: AnchorRequest): AnchorFrame {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val view = LocalView.current
    // Bottom edge of the top status bar. The status-bar inset's top is that y; its bottom is 0.
    val statusBarBottomPx = WindowInsets.statusBars.getTop(density)
    val layoutDirection = LocalLayoutDirection.current
    val cutoutInsets = WindowInsets.displayCutout
    val cutouts = remember(
        view,
        cutoutInsets.getLeft(density, layoutDirection),
        cutoutInsets.getTop(density),
        cutoutInsets.getRight(density, layoutDirection),
        cutoutInsets.getBottom(density),
        configuration.orientation,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    ) {
        readCutoutRects(view)
    }
    // Cutout rects are in window pixels. Configuration dp rounds, so a center hole can miss the pill.
    val container = LocalWindowInfo.current.containerSize
    val widthPx = container.width.takeIf { it > 0 }
        ?: (configuration.screenWidthDp * density.density).toInt()
    val heightPx = container.height.takeIf { it > 0 }
        ?: (configuration.screenHeightDp * density.density).toInt()
    return resolveAnchor(
        AnchorInput(
            screenWidthPx = widthPx,
            screenHeightPx = heightPx,
            statusBarBottomPx = statusBarBottomPx,
            cutouts = cutouts,
            portrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
            request = request,
            density = density.density,
        ),
    )
}

private fun readCutoutRects(view: View): List<CutoutRect> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptyList()
    val rects = view.rootWindowInsets?.displayCutout?.boundingRects ?: return emptyList()
    return rects.map { rect ->
        CutoutRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
        )
    }
}
