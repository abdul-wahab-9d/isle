package io.github.abdulwahab9d.isle

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.abdulwahab9d.isle.controller.IsleController
import io.github.abdulwahab9d.isle.controller.rememberIsleController
import io.github.abdulwahab9d.isle.core.Content
import io.github.abdulwahab9d.isle.core.IsleMetrics
import io.github.abdulwahab9d.isle.core.LayoutInput
import io.github.abdulwahab9d.isle.cutout.rememberAnchorFrame
import io.github.abdulwahab9d.isle.geometry.layoutIsle
import io.github.abdulwahab9d.isle.render.IsleCapsule
import kotlin.math.max
import kotlin.math.min

@Composable
fun IsleHost(content: @Composable () -> Unit) {
    val controller = rememberIsleController()
    val context = LocalContext.current
    remember(context) { applyShortEdgesCutout(context) }
    CompositionLocalProvider(LocalIsleController provides controller) {
        Box(Modifier.fillMaxSize()) {
            content()
            val frame = controller.frame
            val shown = frame.content
            if (shown != null) {
                IsleOverlay(
                    controller = controller,
                    content = shown,
                    expanded = frame.expanded,
                )
            }
        }
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Composable
private fun IsleOverlay(
    controller: IsleController,
    content: Content,
    expanded: Boolean,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val animationsOff = remember(context) { animatorScaleIsZero(context) }
    val anchor = rememberAnchorFrame(content.anchor)
    val compactHeight = with(density) { IsleMetrics.COMPACT_HEIGHT_DP.dp.toPx() }
    val roundness = with(density) { IsleMetrics.ROUNDNESS_DP.dp.toPx() }
    val titleWidth = textMeasurer.measure(
        text = content.title,
        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        overflow = TextOverflow.Clip,
        softWrap = false,
        maxLines = 1,
    ).size.width.toFloat()
    val rawPill = titleWidth + with(density) { 48.dp.toPx() }
    val pillWidth = rawPill.coerceIn(
        min(compactHeight, anchor.compactMaxWidthPx),
        max(compactHeight, anchor.compactMaxWidthPx),
    )
    val descriptionHeight = content.description
        ?.takeIf { it.isNotBlank() }
        ?.let { description ->
            textMeasurer.measure(
                text = description,
                style = TextStyle(fontSize = 14.sp),
                constraints = Constraints(
                    maxWidth = anchor.cardWidthPx.toInt().coerceAtLeast(1),
                ),
            ).size.height.toFloat()
        }
        ?: 0f

    key(content.id) {
        val target = if (expanded) 1f else 0f
        val animated by animateFloatAsState(
            targetValue = target,
            animationSpec = if (animationsOff) {
                snap()
            } else {
                spring(
                    dampingRatio = IsleMetrics.SPRING_DAMPING,
                    stiffness = IsleMetrics.SPRING_STIFFNESS,
                )
            },
            label = "isleExpanded",
        )
        val fraction = if (animationsOff) target else animated
        val contentHeight = if (expanded || fraction > 0f) descriptionHeight else 0f
        val layout = layoutIsle(
            LayoutInput(
                anchorCenterX = anchor.centerXPx,
                pillTop = anchor.pillTopPx,
                cardTop = anchor.cardTopPx,
                cardLeft = anchor.cardLeftPx,
                cardWidth = anchor.cardWidthPx,
                pillWidth = pillWidth,
                compactHeight = compactHeight,
                contentHeight = contentHeight,
                expandedFraction = fraction,
                roundness = roundness,
            ),
        )
        IsleCapsule(
            layout = layout,
            title = content.title,
            description = content.description,
            tone = content.tone,
            actionLabel = content.actionLabel,
            onAction = controller.action,
            progress = content.progress,
            expanded = controller.customBody,
            fill = Color(IsleMetrics.NOTCH_FILL_ARGB.toInt()),
            onDismiss = { controller.dismiss(content.id) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The runtime controller exposes these as members. A member outranks an extension,
 * so the getters below are only hit while that class still has no such properties.
 */
@Suppress("unused")
private val IsleController.action: (() -> Unit)?
    get() = null

@Suppress("unused")
private val IsleController.customBody: (@Composable () -> Unit)?
    get() = null

private fun animatorScaleIsZero(context: Context): Boolean {
    return runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)
}

@SuppressLint("NewApi")
private fun applyShortEdgesCutout(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    runCatching {
        val window = context.findActivity()?.window ?: return
        val params = window.attributes
        val mode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        if (params.layoutInDisplayCutoutMode == mode) return
        params.layoutInDisplayCutoutMode = mode
        window.attributes = params
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val next = current.baseContext
        if (next === current) return null
        current = next
    }
    return null
}
