package io.github.abdulwahab9d.isle.render

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.abdulwahab9d.isle.core.Cubic
import io.github.abdulwahab9d.isle.core.IsleLayout
import io.github.abdulwahab9d.isle.core.IsleMetrics
import io.github.abdulwahab9d.isle.core.IsleTiming
import io.github.abdulwahab9d.isle.core.NeckBridge
import io.github.abdulwahab9d.isle.core.RoundBox
import io.github.abdulwahab9d.isle.core.Tone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws one island from [layout]. Coordinates are pixels in the parent, which should be the
 * window overlay. The spring is already applied. Blur, when available, is only this capsule's shape.
 */
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
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { IsleTiming.SWIPE_DISMISS_DP.dp.toPx() }
    val dismiss = rememberUpdatedState(onDismiss)
    val titleColor = Color(titleArgb(tone, actionLabel))
    val tonePaint = Color(toneArgb(tone))
    val blurRadius = layout.pill.radius.finiteOrZero().coerceAtLeast(0f) * IsleMetrics.BLUR_RATIO
    val goo = remember(blurRadius) { platformGooOrNull(blurRadius) }
    val bounds = layout.shapeBounds()
    val pad = if (goo != null) blurRadius * 2f else 0f
    val drawLeft = bounds.left - pad
    val drawTop = bounds.top - pad
    val drawWidth = (bounds.right - bounds.left + pad * 2f).finiteOrZero().coerceAtLeast(0f)
    val drawHeight = (bounds.bottom - bounds.top + pad * 2f).finiteOrZero().coerceAtLeast(0f)
    val swipe = Modifier.swipeDismiss(thresholdPx) { dismiss.value.invoke() }

    Box(modifier.fillMaxSize()) {
        CanvasShape(
            density = density,
            left = drawLeft,
            top = drawTop,
            width = drawWidth,
            height = drawHeight,
            goo = goo,
            layout = layout,
            fill = fill,
        )
        val body = layout.body
        if (body != null) {
            NeckGap(
                layout = layout,
                body = body,
                density = density,
                swipe = swipe,
            )
        }
        PillChrome(
            pill = layout.pill,
            density = density,
            title = title,
            titleColor = titleColor,
            progress = if (body == null) progress else null,
            progressColor = tonePaint,
            swipe = swipe,
        )
        if (body != null) {
            BodyChrome(
                body = body,
                density = density,
                description = description,
                actionLabel = actionLabel,
                onAction = onAction,
                progress = progress,
                progressColor = tonePaint,
                expanded = expanded,
                swipe = swipe,
            )
        }
    }
}

@Composable
private fun CanvasShape(
    density: Density,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    goo: androidx.compose.ui.graphics.RenderEffect?,
    layout: IsleLayout,
    fill: Color,
) {
    val layer = if (goo != null) {
        Modifier.graphicsLayer {
            renderEffect = goo
            clip = false
        }
    } else {
        Modifier
    }
    Canvas(
        Modifier
            .placePx(density, left, top, width, height)
            .then(layer),
    ) {
        translate(left = -left.finiteOrZero(), top = -top.finiteOrZero()) {
            drawCapsule(layout, fill)
        }
    }
}

@Composable
private fun PillChrome(
    pill: RoundBox,
    density: Density,
    title: String,
    titleColor: Color,
    progress: Float?,
    progressColor: Color,
    swipe: Modifier,
) {
    val radius = pill.radius.finiteOrZero().coerceAtLeast(0f)
    Box(
        Modifier
            .placePx(density, pill.x, pill.y, pill.width, pill.height)
            .clip(RoundedCornerShape(with(density) { radius.toDp() }))
            .then(swipe),
    ) {
        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    bottom = if (progress != null) 6.dp else 0.dp,
                )
                .fillMaxWidth(),
            color = titleColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (progress != null) {
            ThinProgress(
                progress = progress,
                color = progressColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun BodyChrome(
    body: RoundBox,
    density: Density,
    description: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    progress: Float?,
    progressColor: Color,
    expanded: (@Composable () -> Unit)?,
    swipe: Modifier,
) {
    val radius = body.radius.finiteOrZero().coerceAtLeast(0f)
    Column(
        Modifier
            .placePx(density, body.x, body.y, body.width, body.height)
            .clip(RoundedCornerShape(with(density) { radius.toDp() }))
            .then(swipe)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (description != null) {
            Text(
                text = description,
                color = DescriptionColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
            )
        }
        if (actionLabel != null) {
            ActionButton(label = actionLabel, onAction = onAction)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
        ) {
            expanded?.invoke()
        }
        if (progress != null) {
            ThinProgress(progress = progress, color = progressColor)
        }
    }
}

@Composable
private fun NeckGap(
    layout: IsleLayout,
    body: RoundBox,
    density: Density,
    swipe: Modifier,
) {
    val pill = layout.pill
    val pillBottom = pill.y + pill.height
    if (!pillBottom.isFinite() || !body.y.isFinite() || body.y < pillBottom + 1f) return
    val left = min(pill.x, body.x)
    val right = max(pill.x + pill.width, body.x + body.width)
    Box(
        Modifier
            .placePx(density, left, pillBottom, right - left, body.y - pillBottom)
            .then(swipe),
    )
}

@Composable
private fun ActionButton(
    label: String,
    onAction: (() -> Unit)?,
) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(IsleMetrics.ACTION_ARGB))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = onAction != null,
                role = Role.Button,
                onClick = { onAction?.invoke() },
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun ThinProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.unitFraction()
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color.White.copy(alpha = 0.24f)),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color),
            )
        }
    }
}

private fun DrawScope.drawCapsule(layout: IsleLayout, fill: Color) {
    drawRoundBox(layout.pill, fill)
    layout.body?.let { drawRoundBox(it, fill) }
    val neck = layout.neck ?: return
    val path = neckPath(layout.pill, neck) ?: return
    drawPath(path, fill)
}

private fun DrawScope.drawRoundBox(box: RoundBox, fill: Color) {
    if (!box.x.isFinite() || !box.y.isFinite() || !box.width.isFinite() || !box.height.isFinite()) return
    if (box.width <= 0f || box.height <= 0f) return
    val radius = box.radius.finiteOrZero().coerceAtLeast(0f)
    drawRoundRect(
        color = fill,
        topLeft = Offset(box.x, box.y),
        size = Size(box.width, box.height),
        cornerRadius = CornerRadius(radius, radius),
    )
}

/** Starts on the pill bottom corners. Cubic ends are the body top corners. */
private fun neckPath(pill: RoundBox, neck: NeckBridge): Path? {
    val bottom = pill.y + pill.height
    val leftX = pill.x
    val rightX = pill.x + pill.width
    if (!bottom.isFinite() || !leftX.isFinite() || !rightX.isFinite()) return null
    if (!neck.left.isFinite() || !neck.right.isFinite()) return null
    return Path().apply {
        moveTo(leftX, bottom)
        cubicTo(neck.left.c1x, neck.left.c1y, neck.left.c2x, neck.left.c2y, neck.left.x, neck.left.y)
        lineTo(neck.right.x, neck.right.y)
        cubicTo(
            neck.right.c2x,
            neck.right.c2y,
            neck.right.c1x,
            neck.right.c1y,
            rightX,
            bottom,
        )
        close()
    }
}

private fun Modifier.placePx(density: Density, x: Float, y: Float, width: Float, height: Float): Modifier {
    val safeX = x.finiteOrZero()
    val safeY = y.finiteOrZero()
    val safeW = width.finiteOrZero().coerceAtLeast(0f)
    val safeH = height.finiteOrZero().coerceAtLeast(0f)
    return offset { IntOffset(safeX.roundToInt(), safeY.roundToInt()) }
        .size(with(density) { safeW.toDp() }, with(density) { safeH.toDp() })
}

private fun Modifier.swipeDismiss(thresholdPx: Float, onDismiss: () -> Unit): Modifier =
    pointerInput(thresholdPx) {
        var dragged = 0f
        var fired = false
        detectVerticalDragGestures(
            onDragStart = {
                dragged = 0f
                fired = false
            },
            onDragEnd = { fired = false },
            onDragCancel = { fired = false },
            onVerticalDrag = { _, amount ->
                dragged += amount
                if (!fired && abs(dragged) >= thresholdPx) {
                    fired = true
                    onDismiss()
                }
            },
        )
    }

private fun IsleLayout.shapeBounds(): PxRect {
    var left = pill.x.finiteOrZero()
    var top = pill.y.finiteOrZero()
    var right = (pill.x + pill.width).finiteOrZero()
    var bottom = (pill.y + pill.height).finiteOrZero()
    body?.let { box ->
        if (box.x.isFinite()) left = min(left, box.x)
        if (box.y.isFinite()) top = min(top, box.y)
        if (box.x.isFinite() && box.width.isFinite()) right = max(right, box.x + box.width)
        if (box.y.isFinite() && box.height.isFinite()) bottom = max(bottom, box.y + box.height)
    }
    if (right < left) right = left
    if (bottom < top) bottom = top
    return PxRect(left, top, right, bottom)
}

private data class PxRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private fun platformGooOrNull(blurRadius: Float): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || blurRadius <= 0f || !blurRadius.isFinite()) {
        return null
    }
    return gooRenderEffect(blurRadius)
}

private fun gooRenderEffect(blurRadius: Float): androidx.compose.ui.graphics.RenderEffect {
    val blur = android.graphics.RenderEffect.createBlurEffect(
        blurRadius,
        blurRadius,
        android.graphics.Shader.TileMode.CLAMP,
    )
    val matrix = android.graphics.ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, IsleMetrics.GOO_ALPHA_SCALE, IsleMetrics.GOO_ALPHA_BIAS,
        ),
    )
    // Blur the capsule pixels first, then threshold alpha into a goo edge.
    return android.graphics.RenderEffect.createColorFilterEffect(
        android.graphics.ColorMatrixColorFilter(matrix),
        blur,
    ).asComposeRenderEffect()
}

private fun titleArgb(tone: Tone, actionLabel: String?): Long =
    if (actionLabel != null) IsleMetrics.ACTION_ARGB else toneArgb(tone)

private fun toneArgb(tone: Tone): Long = when (tone) {
    Tone.Success -> IsleMetrics.SUCCESS_ARGB
    Tone.Error -> IsleMetrics.ERROR_ARGB
    Tone.Warning -> IsleMetrics.WARNING_ARGB
    Tone.Info -> IsleMetrics.INFO_ARGB
    Tone.Loading -> IsleMetrics.LOADING_ARGB
}

private fun Cubic.isFinite(): Boolean =
    c1x.isFinite() && c1y.isFinite() && c2x.isFinite() && c2y.isFinite() && x.isFinite() && y.isFinite()

private fun Float.finiteOrZero(): Float = if (isFinite()) this else 0f

private fun Float.unitFraction(): Float {
    if (!isFinite()) return 0f
    return coerceIn(0f, 1f)
}

private val DescriptionColor = Color.White.copy(alpha = 0.5f)
