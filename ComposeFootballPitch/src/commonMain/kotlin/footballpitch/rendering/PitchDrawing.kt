package footballpitch.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import footballpitch.PitchScaleCalculator
import footballpitch.model.GradientDirection
import footballpitch.model.PitchBackground
import footballpitch.model.PitchStyle
import footballpitch.model.StripeOrientation

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
                size = size
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
                            size = Size(width = stripeWidth, height = size.height)
                        )
                    }
                }
                StripeOrientation.Horizontal -> {
                    val stripeHeight = size.height / background.stripeCount
                    repeat(background.stripeCount) { i ->
                        drawRect(
                            color = colors[i % colors.size],
                            topLeft = Offset(x = 0f, y = i * stripeHeight),
                            size = Size(width = size.width, height = stripeHeight)
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
                        topLeft = Offset(
                            x = col * cellWidth,
                            y = row * cellHeight
                        ),
                        size = Size(width = cellWidth, height = cellHeight)
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
                    size = size
                )
                return
            }

            val (start, end) = when (background.direction) {
                GradientDirection.Vertical ->
                    Offset(0f, 0f) to Offset(0f, size.height)
                GradientDirection.Horizontal ->
                    Offset(0f, 0f) to Offset(size.width, 0f)
                GradientDirection.Diagonal ->
                    Offset(0f, 0f) to Offset(size.width, size.height)
            }

            val brush = Brush.linearGradient(
                colors = colors,
                start = start,
                end = end
            )

            drawRect(
                brush = brush,
                topLeft = Offset.Zero,
                size = size
            )
        }
    }
}


/**
 * Draw outer boundary and halfway line
 */
internal fun DrawScope.drawBoundaryLines(
    scale: PitchScaleCalculator,
    style: PitchStyle
) {
    val lineWidth = style.effectiveLineWidth(scale)

    // Outer boundary
    drawRect(
        color = style.lineColor,
        topLeft = Offset.Zero,
        size = scale.canvasSize,
        style = Stroke(width = lineWidth)
    )

    // Halfway line
    drawLine(
        color = style.lineColor,
        start = Offset(x = scale.lengthPx / 2f, y = 0f),
        end = Offset(x = scale.lengthPx / 2f, y = scale.widthPx),
        strokeWidth = lineWidth
    )
}


/**
 * Draw center circle and center spot
 */
internal fun DrawScope.drawCenterArea(
    scale: PitchScaleCalculator,
    style: PitchStyle,
    showCenterCircle: Boolean
) {
    val lineWidth = style.effectiveLineWidth(scale)
    val centerSpotRadius = lineWidth / 2f

    if (showCenterCircle) {
        val centreCircleRadiusPx = scale.toPx(scale.dimensions.circleRadius)
        drawCircle(
            color = style.lineColor,
            radius = centreCircleRadiusPx,
            center = scale.center,
            style = Stroke(width = lineWidth)
        )
    }

    drawCircle(
        color = style.lineColor,
        radius = centerSpotRadius,
        center = scale.center
    )
}

/**
 * Draw penalty areas (16-yard boxes)
 */
internal fun DrawScope.drawPenaltyAreas(
    scale: PitchScaleCalculator,
    style: PitchStyle
) {
    val lineWidth = style.effectiveLineWidth(scale)

    val penaltyDepthPx = scale.toPx(scale.dimensions.penaltyAreaDepth)
    val penaltyWidthPx = scale.toPx(scale.dimensions.penaltyAreaWidth)
    val penaltyTop = (scale.widthPx - penaltyWidthPx) / 2f

    // Left penalty area
    drawRect(
        color = style.lineColor,
        topLeft = Offset(x = 0f, y = penaltyTop),
        size = Size(width = penaltyDepthPx, height = penaltyWidthPx),
        style = Stroke(width = lineWidth)
    )

    // Right penalty area
    drawRect(
        color = style.lineColor,
        topLeft = Offset(x = scale.lengthPx - penaltyDepthPx, y = penaltyTop),
        size = Size(width = penaltyDepthPx, height = penaltyWidthPx),
        style = Stroke(width = lineWidth)
    )
}

/**
 * Draw goal areas (6-yard boxes)
 */
internal fun DrawScope.drawGoalAreas(
    scale: PitchScaleCalculator,
    style: PitchStyle
) {
    val lineWidth = style.effectiveLineWidth(scale)

    val goalDepthPx = scale.toPx(scale.dimensions.goalAreaDepth)
    val goalWidthPx = scale.toPx(scale.dimensions.goalAreaWidth)
    val goalTop = (scale.widthPx - goalWidthPx) / 2f

    // Left goal area
    drawRect(
        color = style.lineColor,
        topLeft = Offset(x = 0f, y = goalTop),
        size = Size(width = goalDepthPx, height = goalWidthPx),
        style = Stroke(width = lineWidth)
    )

    // Right goal area
    drawRect(
        color = style.lineColor,
        topLeft = Offset(x = scale.lengthPx - goalDepthPx, y = goalTop),
        size = Size(width = goalDepthPx, height = goalWidthPx),
        style = Stroke(width = lineWidth)
    )
}

/**
 * Draw penalty spots
 */
internal fun DrawScope.drawPenaltySpots(
    scale: PitchScaleCalculator,
    style: PitchStyle
) {
    val lineWidth = style.effectiveLineWidth(scale)
    val penaltySpotRadius = lineWidth / 2f
    val leftPenaltySpot = Offset(
        x = scale.toPx(scale.dimensions.penaltyMarkDistance),
        y = scale.widthPx / 2f
    )
    val rightPenaltySpot = Offset(
        x = scale.lengthPx - scale.toPx(scale.dimensions.penaltyMarkDistance),
        y = scale.widthPx / 2f
    )

    drawCircle(
        color = style.lineColor,
        radius = penaltySpotRadius,
        center = leftPenaltySpot
    )
    drawCircle(
        color = style.lineColor,
        radius = penaltySpotRadius,
        center = rightPenaltySpot
    )
}

/**
 * Draw penalty arcs (the "D")
 */
internal fun DrawScope.drawPenaltyArcs(
    scale: PitchScaleCalculator,
    style: PitchStyle
) {
    val lineWidth = style.effectiveLineWidth(scale)

    val penaltyArcRadiusPx = scale.toPx(scale.dimensions.circleRadius)
    val arcDiameter = penaltyArcRadiusPx * 2f

    val leftPenaltySpot = Offset(
        x = scale.toPx(scale.dimensions.penaltyMarkDistance),
        y = scale.widthPx / 2f
    )
    val rightPenaltySpot = Offset(
        x = scale.lengthPx - scale.toPx(scale.dimensions.penaltyMarkDistance),
        y = scale.widthPx / 2f
    )

    // Left penalty arc
    drawArc(
        color = style.lineColor,
        startAngle = -53f,
        sweepAngle = 106f,
        useCenter = false,
        topLeft = Offset(
            x = leftPenaltySpot.x - penaltyArcRadiusPx,
            y = leftPenaltySpot.y - penaltyArcRadiusPx
        ),
        size = Size(width = arcDiameter, height = arcDiameter),
        style = Stroke(width = lineWidth)
    )

    // Right penalty arc
    drawArc(
        color = style.lineColor,
        startAngle = 180f - 53f,
        sweepAngle = 106f,
        useCenter = false,
        topLeft = Offset(
            x = rightPenaltySpot.x - penaltyArcRadiusPx,
            y = rightPenaltySpot.y - penaltyArcRadiusPx
        ),
        size = Size(width = arcDiameter, height = arcDiameter),
        style = Stroke(width = lineWidth)
    )
}

/**
 * Draw corner arcs
 */
internal fun DrawScope.drawCornerArcs(
    scale: PitchScaleCalculator,
    style: PitchStyle
) {
    val lineWidth = style.effectiveLineWidth(scale)

    val cornerRadiusPx = scale.toPx(scale.dimensions.cornerArcRadius)
    val cornerDiameter = cornerRadiusPx * 2f

    // Four corners
    val corners = listOf(
        Triple(0f, 0f, 0f),           // top-left
        Triple(90f, scale.lengthPx, 0f), // top-right
        Triple(270f, 0f, scale.widthPx), // bottom-left
        Triple(180f, scale.lengthPx, scale.widthPx) // bottom-right
    )

    corners.forEach { (startAngle, x, y) ->
        drawArc(
            color = style.lineColor,
            startAngle = startAngle,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(x = x - cornerRadiusPx, y = y - cornerRadiusPx),
            size = Size(width = cornerDiameter, height = cornerDiameter),
            style = Stroke(width = lineWidth)
        )
    }
}
