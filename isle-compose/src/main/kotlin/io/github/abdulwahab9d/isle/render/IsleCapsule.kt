package io.github.abdulwahab9d.isle.render

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import io.github.abdulwahab9d.isle.core.LayoutInput
import io.github.abdulwahab9d.isle.geometry.layoutIsle
import io.github.abdulwahab9d.isle.core.IsleTiming
import io.github.abdulwahab9d.isle.core.NeckBridge
import io.github.abdulwahab9d.isle.core.RoundBox
import io.github.abdulwahab9d.isle.core.Tone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws one island. [openFraction] is the shell clock, from 0 closed to 1 open.
 * Callers must read it only from draw or layout, never during composition.
 * Coordinates are pixels in a window-sized parent. Blur stays on this capsule.
 */
@Composable
fun IsleCapsule(
    input: LayoutInput,
    openFraction: () -> Float,
    title: String,
    description: String?,
    tone: Tone,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    progress: Float?,
    expanded: (@Composable () -> Unit)?,
    fill: Color,
    onDismiss: () -> Unit,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { IsleTiming.SWIPE_DISMISS_DP.dp.toPx() }
    val dismiss = rememberUpdatedState(onDismiss)
    val tap = rememberUpdatedState(onTap)
    val fraction = rememberUpdatedState(openFraction)
    val titleColor = Color(titleArgb(tone, actionLabel))
    val tonePaint = Color(toneArgb(tone))
    val openLayout = remember(input) { layoutIsle(input.copy(expandedFraction = 1f)) }
    val blurRadius = openLayout.pill.radius.finiteOrZero().coerceAtLeast(0f) * IsleMetrics.BLUR_RATIO
    val goo = remember(blurRadius) { platformGooOrNull(blurRadius) }
    val bounds = openLayout.shapeBounds()
    val pad = if (goo != null) blurRadius * 2f else 0f
    val drawLeft = bounds.left - pad
    val drawTop = bounds.top - pad
    val drawWidth = (bounds.right - bounds.left + pad * 2f).finiteOrZero().coerceAtLeast(0f)
    val drawHeight = (bounds.bottom - bounds.top + pad * 2f).finiteOrZero().coerceAtLeast(0f)
    val swipe = Modifier.swipeDismiss(thresholdPx) { dismiss.value.invoke() }
    val shell = { fraction.value.invoke() }

    Box(modifier.fillMaxSize()) {
        CanvasShape(
            density = density,
            left = drawLeft,
            top = drawTop,
            width = drawWidth,
            height = drawHeight,
            goo = goo,
            input = input,
            shell = shell,
            fill = fill,
        )
        val body = openLayout.body
        if (body != null) {
            NeckGap(
                layout = openLayout,
                body = body,
                density = density,
                swipe = swipe,
            )
        }
        PillChrome(
            pill = openLayout.pill,
            density = density,
            title = title,
            tone = tone,
            titleColor = titleColor,
            progress = if (body == null) progress else null,
            progressColor = tonePaint,
            swipe = swipe,
            onTap = { tap.value.invoke() },
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
                onTap = { tap.value.invoke() },
                shell = shell,
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
    input: LayoutInput,
    shell: () -> Float,
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
    val neck = remember { Path() }
    Canvas(
        Modifier
            .placePx(density, left, top, width, height)
            .then(layer),
    ) {
        val layout = layoutIsle(input.copy(expandedFraction = shell().unitFraction()))
        translate(left = -left.finiteOrZero(), top = -top.finiteOrZero()) {
            drawCapsule(layout, fill, neck)
        }
    }
}

@Composable
private fun PillChrome(
    pill: RoundBox,
    density: Density,
    title: String,
    tone: Tone,
    titleColor: Color,
    progress: Float?,
    progressColor: Color,
    swipe: Modifier,
    onTap: () -> Unit,
) {
    val radius = pill.radius.finiteOrZero().coerceAtLeast(0f)
    Box(
        Modifier
            .placePx(density, pill.x, pill.y, pill.width, pill.height)
            .clip(RoundedCornerShape(with(density) { radius.toDp() }))
            .then(swipe)
            .tapIsland(onTap),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp, end = 12.dp, bottom = if (progress != null) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToneBadge(tone = tone, color = titleColor)
            Text(
                text = title,
                color = titleColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
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
    onTap: () -> Unit,
    shell: () -> Float,
) {
    val radius = body.radius.finiteOrZero().coerceAtLeast(0f)
    Column(
        Modifier
            .offset {
                IntOffset(body.x.finiteOrZero().roundToInt(), body.y.finiteOrZero().roundToInt())
            }
            .revealHeight(body.width, body.height, shell)
            .graphicsLayer { alpha = contentFollows(shell()) }
            .clip(RoundedCornerShape(with(density) { radius.toDp() }))
            .then(swipe)
            .tapIsland(onTap)
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
            ActionButton(label = actionLabel, color = progressColor, onAction = onAction)
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
private fun ToneBadge(tone: Tone, color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        when (tone) {
            Tone.Loading -> LoadingMark(color)
            else -> Canvas(Modifier.size(12.dp)) {
                drawToneMark(tone, color)
            }
        }
    }
}

@Composable
private fun LoadingMark(color: Color) {
    val spin = rememberInfiniteTransition(label = "isle-loading")
    val degrees = spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "isle-loading-spin",
    )
    Canvas(
        Modifier
            .size(12.dp)
            .graphicsLayer { rotationZ = degrees.value },
    ) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawToneMark(tone: Tone, color: Color) {
    val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
    when (tone) {
        Tone.Success -> drawPath(
            Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.52f)
                lineTo(size.width * 0.42f, size.height * 0.76f)
                lineTo(size.width * 0.84f, size.height * 0.28f)
            },
            color,
            style = stroke,
        )
        Tone.Error -> {
            drawLine(color, Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.72f, size.height * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width * 0.72f, size.height * 0.28f), Offset(size.width * 0.28f, size.height * 0.72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        Tone.Warning -> drawLine(
            color,
            Offset(size.width * 0.5f, size.height * 0.18f),
            Offset(size.width * 0.5f, size.height * 0.68f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        ).also {
            drawCircle(color, radius = stroke.width * 0.7f, center = Offset(size.width * 0.5f, size.height * 0.84f))
        }
        else -> {
            drawCircle(color, radius = size.minDimension * 0.36f, style = stroke)
            drawLine(
                color,
                Offset(size.width * 0.5f, size.height * 0.38f),
                Offset(size.width * 0.5f, size.height * 0.68f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    color: Color,
    onAction: (() -> Unit)?,
) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = onAction != null,
                role = Role.Button,
                onClick = { onAction?.invoke() },
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
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

private fun DrawScope.drawCapsule(layout: IsleLayout, fill: Color, scratch: Path) {
    drawRoundBox(layout.pill, fill)
    layout.body?.let { drawRoundBox(it, fill) }
    val bridge = layout.neck ?: return
    scratch.rewind()
    if (!scratch.neckFrom(layout.pill, bridge)) return
    drawPath(scratch, fill)
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
private fun Path.neckFrom(pill: RoundBox, neck: NeckBridge): Boolean {
    val bottom = pill.y + pill.height
    val leftX = pill.x
    val rightX = pill.x + pill.width
    if (!bottom.isFinite() || !leftX.isFinite() || !rightX.isFinite()) return false
    if (!neck.left.isFinite() || !neck.right.isFinite()) return false
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
    return true
}

/** Text waits until the shell has started, then catches the same clock. */
private fun contentFollows(shell: Float): Float {
    val start = 0.22f
    val fraction = shell.unitFraction()
    if (fraction <= start) return 0f
    return ((fraction - start) / (1f - start)).coerceIn(0f, 1f)
}

private fun Modifier.revealHeight(widthPx: Float, heightPx: Float, shell: () -> Float): Modifier =
    layout { measurable, constraints ->
        val fraction = shell().unitFraction()
        val width = widthPx.finiteOrZero().roundToInt().coerceAtLeast(0)
        val fullHeight = heightPx.finiteOrZero().roundToInt().coerceAtLeast(0)
        val height = (fullHeight * fraction).roundToInt().coerceIn(0, fullHeight)
        val placeable = measurable.measure(
            constraints.copy(minWidth = 0, maxWidth = width, minHeight = 0, maxHeight = height),
        )
        layout(width, height) { placeable.place(0, 0) }
    }

private fun Modifier.placePx(density: Density, x: Float, y: Float, width: Float, height: Float): Modifier {
    val safeX = x.finiteOrZero()
    val safeY = y.finiteOrZero()
    val safeW = width.finiteOrZero().coerceAtLeast(0f)
    val safeH = height.finiteOrZero().coerceAtLeast(0f)
    return offset { IntOffset(safeX.roundToInt(), safeY.roundToInt()) }
        .size(with(density) { safeW.toDp() }, with(density) { safeH.toDp() })
}

@Composable
private fun Modifier.tapIsland(onTap: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interaction,
        indication = null,
        role = Role.Button,
        onClick = onTap,
    )
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
