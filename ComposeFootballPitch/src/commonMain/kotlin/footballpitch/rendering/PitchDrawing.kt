package footballpitch.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import footballpitch.PitchScaleCalculator
import footballpitch.model.GradientDirection
import footballpitch.model.PitchBackground
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchStyle
import footballpitch.model.StripeOrientation

internal data class BoxSpec(val topLeft: Offset, val size: Size)

/**
 * Computes the effective line width using the logical scale and style factor.
 */
internal fun PitchStyle.effectiveLineWidth(scale: PitchScaleCalculator): Float {
    val factor = lineThicknessFactor.coerceAtLeast(0f)
    return scale.lineWidth * factor
}

/**
 * Draw the grass/background of the pitch.
 *
 * Delegates to different strategies based on [PitchStyle.background].
 * This function is internal to the module and not part of the public API.
 */
internal fun DrawScope.drawPitchBackground(style: PitchStyle) {
    when (val background = style.background) {
        is PitchBackground.Solid -> {
            drawRect(
                color = background.color,
                topLeft = Offset.Zero,
                size = size,
            )
        }

        is PitchBackground.Stripes -> {
            val colors = background.colors
            if (colors.isEmpty() || background.stripeCount <= 0) return

            when (background.orientation) {
                StripeOrientation.Vertical -> {
                    val stripeWidth = size.width / background.stripeCount
                    repeat(background.stripeCount) { i ->
                        drawRect(
                            color = colors[i % colors.size],
                            topLeft = Offset(x = i * stripeWidth, y = 0f),
                            size = Size(width = stripeWidth, height = size.height),
                        )
                    }
                }
                StripeOrientation.Horizontal -> {
                    val stripeHeight = size.height / background.stripeCount
                    repeat(background.stripeCount) { i ->
                        drawRect(
                            color = colors[i % colors.size],
                            topLeft = Offset(x = 0f, y = i * stripeHeight),
                            size = Size(width = size.width, height = stripeHeight),
                        )
                    }
                }
            }
        }

        is PitchBackground.Checkerboard -> {
            val colors = background.colors
            if (colors.isEmpty() || background.rows <= 0 || background.columns <= 0) return

            val rows = background.rows
            val cols = background.columns
            val cellWidth = size.width / cols
            val cellHeight = size.height / rows

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val colorIndex = (row + col) % colors.size
                    drawRect(
                        color = colors[colorIndex],
                        topLeft =
                            Offset(
                                x = col * cellWidth,
                                y = row * cellHeight,
                            ),
                        size = Size(width = cellWidth, height = cellHeight),
                    )
                }
            }
        }

        is PitchBackground.Gradient -> {
            val colors = background.colors
            if (colors.isEmpty()) return
            if (colors.size == 1) {
                drawRect(
                    color = colors.first(),
                    topLeft = Offset.Zero,
                    size = size,
                )
                return
            }

            val (start, end) =
                when (background.direction) {
                    GradientDirection.Vertical ->
                        Offset(0f, 0f) to Offset(0f, size.height)
                    GradientDirection.Horizontal ->
                        Offset(0f, 0f) to Offset(size.width, 0f)
                    GradientDirection.Diagonal ->
                        Offset(0f, 0f) to Offset(size.width, size.height)
                }

            val brush =
                Brush.linearGradient(
                    colors = colors,
                    start = start,
                    end = end,
                )

            drawRect(
                brush = brush,
                topLeft = Offset.Zero,
                size = size,
            )
        }
    }
}

/**
 * Draw outer boundary and halfway line
 */
internal fun DrawScope.drawBoundaryLines(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)
    val inset = lineWidth / 2f

    val boundary = boundarySpec(lineWidth, size)

    drawRect(
        color = style.lineColor,
        topLeft = boundary.topLeft,
        size = boundary.size,
        style = Stroke(width = lineWidth),
    )

    val halfPrimary = scale.dimensions.length / 2f
    val isVertical = scale.orientation == PitchOrientation.Vertical || scale.orientation == PitchOrientation.VerticalReversed
    val secondaryInset = if (isVertical) Offset(inset, 0f) else Offset(0f, inset)
    val start = scale.pitchOffset(primaryMeters = halfPrimary, secondaryMetersFromTop = 0f) + secondaryInset
    val end = scale.pitchOffset(primaryMeters = halfPrimary, secondaryMetersFromTop = scale.dimensions.width) - secondaryInset

    drawLine(
        color = style.lineColor,
        start = start,
        end = end,
        strokeWidth = lineWidth,
    )
}

/**
 * Draw center circle and center spot
 */
internal fun DrawScope.drawCenterArea(
    scale: PitchScaleCalculator,
    style: PitchStyle,
    showCenterCircle: Boolean,
) {
    val lineWidth = style.effectiveLineWidth(scale)
    val centerSpotRadius = lineWidth / 2f

    if (showCenterCircle) {
        val centreCircleRadiusPx = scale.primaryToPx(scale.dimensions.circleRadius)
        drawCircle(
            color = style.lineColor,
            radius = centreCircleRadiusPx,
            center = scale.center,
            style = Stroke(width = lineWidth),
        )
    }

    drawCircle(
        color = style.lineColor,
        radius = centerSpotRadius,
        center = scale.center,
    )
}

internal fun boundarySpec(
    lineWidth: Float,
    canvasSize: Size,
): BoxSpec {
    val inset = lineWidth / 2f
    return BoxSpec(
        topLeft = Offset(inset, inset),
        size = Size(width = canvasSize.width - lineWidth, height = canvasSize.height - lineWidth),
    )
}

internal fun penaltyBoxSpecs(
    scale: PitchScaleCalculator,
    lineWidth: Float,
): List<BoxSpec> {
    val penaltyTopMeters = (scale.dimensions.width - scale.dimensions.penaltyAreaWidth) / 2f
    val boxSize =
        scale.pitchSize(
            primaryMeters = scale.dimensions.penaltyAreaDepth,
            secondaryMeters = scale.dimensions.penaltyAreaWidth,
        )
    val adjustedBoxSize =
        Size(
            width = (boxSize.width - lineWidth).coerceAtLeast(0f),
            height = (boxSize.height - lineWidth).coerceAtLeast(0f),
        )
    val inset = Offset(lineWidth / 2f, lineWidth / 2f)

    val nearBoxTopLeft =
        scale.pitchOffset(
            primaryMeters = 0f,
            secondaryMetersFromTop = penaltyTopMeters,
        )
    val farBoxTopLeft =
        scale.pitchOffset(
            primaryMeters = scale.dimensions.length - scale.dimensions.penaltyAreaDepth,
            secondaryMetersFromTop = penaltyTopMeters,
        )

    return listOf(
        BoxSpec(topLeft = nearBoxTopLeft + inset, size = adjustedBoxSize),
        BoxSpec(topLeft = farBoxTopLeft + inset, size = adjustedBoxSize),
    )
}

/**
 * Draw penalty areas (16-yard boxes)
 */
internal fun DrawScope.drawPenaltyAreas(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)

    penaltyBoxSpecs(scale, lineWidth).forEach { box ->
        drawRect(
            color = style.lineColor,
            topLeft = box.topLeft,
            size = box.size,
            style = Stroke(width = lineWidth),
        )
    }
}

internal fun goalBoxSpecs(
    scale: PitchScaleCalculator,
    lineWidth: Float,
): List<BoxSpec> {
    val goalTopMeters = (scale.dimensions.width - scale.dimensions.goalAreaWidth) / 2f
    val boxSize =
        scale.pitchSize(
            primaryMeters = scale.dimensions.goalAreaDepth,
            secondaryMeters = scale.dimensions.goalAreaWidth,
        )
    val adjustedBoxSize =
        Size(
            width = (boxSize.width - lineWidth).coerceAtLeast(0f),
            height = (boxSize.height - lineWidth).coerceAtLeast(0f),
        )
    val inset = Offset(lineWidth / 2f, lineWidth / 2f)

    val nearBoxTopLeft =
        scale.pitchOffset(
            primaryMeters = 0f,
            secondaryMetersFromTop = goalTopMeters,
        )
    val farBoxTopLeft =
        scale.pitchOffset(
            primaryMeters = scale.dimensions.length - scale.dimensions.goalAreaDepth,
            secondaryMetersFromTop = goalTopMeters,
        )

    return listOf(
        BoxSpec(topLeft = nearBoxTopLeft + inset, size = adjustedBoxSize),
        BoxSpec(topLeft = farBoxTopLeft + inset, size = adjustedBoxSize),
    )
}

/**
 * Draw goal areas (6-yard boxes)
 */
internal fun DrawScope.drawGoalAreas(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)

    goalBoxSpecs(scale, lineWidth).forEach { box ->
        drawRect(
            color = style.lineColor,
            topLeft = box.topLeft,
            size = box.size,
            style = Stroke(width = lineWidth),
        )
    }
}

/**
 * Draw penalty spots
 */
internal fun DrawScope.drawPenaltySpots(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)
    val penaltySpotRadius = lineWidth / 2f
    val secondaryMid = scale.dimensions.width / 2f
    val nearPenaltySpot =
        scale.pitchOffset(
            primaryMeters = scale.dimensions.penaltyMarkDistance,
            secondaryMetersFromTop = secondaryMid,
        )
    val farPenaltySpot =
        scale.pitchOffset(
            primaryMeters = scale.dimensions.length - scale.dimensions.penaltyMarkDistance,
            secondaryMetersFromTop = secondaryMid,
        )

    drawCircle(
        color = style.lineColor,
        radius = penaltySpotRadius,
        center = nearPenaltySpot,
    )
    drawCircle(
        color = style.lineColor,
        radius = penaltySpotRadius,
        center = farPenaltySpot,
    )
}

/**
 * Draw penalty arcs (the "D")
 */
internal fun DrawScope.drawPenaltyArcs(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)

    val penaltyArcRadiusPx = scale.primaryToPx(scale.dimensions.circleRadius)
    val arcDiameter = penaltyArcRadiusPx * 2f
    val secondaryMid = scale.dimensions.width / 2f

    val nearPenaltySpot =
        scale.pitchOffset(
            primaryMeters = scale.dimensions.penaltyMarkDistance,
            secondaryMetersFromTop = secondaryMid,
        )
    val farPenaltySpot =
        scale.pitchOffset(
            primaryMeters = scale.dimensions.length - scale.dimensions.penaltyMarkDistance,
            secondaryMetersFromTop = secondaryMid,
        )

    val forwardAngle = scale.forwardAngleDegrees()
    val nearStart = forwardAngle - 53f
    val farStart = forwardAngle + 180f - 53f

    drawArc(
        color = style.lineColor,
        startAngle = nearStart,
        sweepAngle = 106f,
        useCenter = false,
        topLeft =
            Offset(
                x = nearPenaltySpot.x - penaltyArcRadiusPx,
                y = nearPenaltySpot.y - penaltyArcRadiusPx,
            ),
        size = Size(width = arcDiameter, height = arcDiameter),
        style = Stroke(width = lineWidth),
    )

    drawArc(
        color = style.lineColor,
        startAngle = farStart,
        sweepAngle = 106f,
        useCenter = false,
        topLeft =
            Offset(
                x = farPenaltySpot.x - penaltyArcRadiusPx,
                y = farPenaltySpot.y - penaltyArcRadiusPx,
            ),
        size = Size(width = arcDiameter, height = arcDiameter),
        style = Stroke(width = lineWidth),
    )
}

/**
 * Draw corner arcs
 */
internal fun DrawScope.drawCornerArcs(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)

    val cornerRadiusPx = scale.primaryToPx(scale.dimensions.cornerArcRadius)
    val cornerDiameter = cornerRadiusPx * 2f

    // Four corners
    val corners =
        listOf(
            // top-left
            Triple(0f, 0f, 0f),
            // top-right
            Triple(90f, size.width, 0f),
            // bottom-left
            Triple(270f, 0f, size.height),
            // bottom-right
            Triple(180f, size.width, size.height),
        )

    corners.forEach { (startAngle, x, y) ->
        drawArc(
            color = style.lineColor,
            startAngle = startAngle,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(x = x - cornerRadiusPx, y = y - cornerRadiusPx),
            size = Size(width = cornerDiameter, height = cornerDiameter),
            style = Stroke(width = lineWidth),
        )
    }
}
