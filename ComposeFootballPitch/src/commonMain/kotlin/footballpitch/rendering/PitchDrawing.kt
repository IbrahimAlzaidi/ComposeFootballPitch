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
import kotlin.math.acos

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
 * Draw outer boundary and halfway line.
 * Updated to ensure the halfway line connects perfectly to the outer boundary without gaps.
 */
internal fun DrawScope.drawBoundaryLines(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)

    // The outer boundary is inset by half the line width so the stroke sits fully inside the canvas area.
    val boundary = boundarySpec(lineWidth, size)

    drawRect(
        color = style.lineColor,
        topLeft = boundary.topLeft,
        size = boundary.size,
        style = Stroke(width = lineWidth),
    )

    // Draw the halfway line.
    // It should extend exactly from one side of the boundary rectangle to the other.
    val halfPrimary = scale.dimensions.length / 2f

    // We do NOT apply an inset here, because we want the line endpoints to touch the outer boundary.
    val start = scale.pitchOffset(primaryMeters = halfPrimary, secondaryMetersFromTop = 0f)
    val end = scale.pitchOffset(primaryMeters = halfPrimary, secondaryMetersFromTop = scale.dimensions.width)

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
 * Draw penalty arcs (the "D").
 * Updated to dynamically calculate angles based on dimensions, rather than using hardcoded FIFA values.
 */
internal fun DrawScope.drawPenaltyArcs(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)
    val dims = scale.dimensions

    // 1. Calculate geometric parameters in meters.
    val radiusMeters = dims.circleRadius
    // The distance from the penalty spot back to the edge of the penalty box line.
    // (e.g., Standard FIFA: 16.5m box depth - 11m spot distance = 5.5m adjacent side).
    val adjacentMeters = dims.penaltyAreaDepth - dims.penaltyMarkDistance

    // Safety check: Ensure dimensions form a valid triangle for arccos calculation.
    // If the spot is outside the box or the radius is too small to reach the box line, don't draw.
    if (adjacentMeters >= radiusMeters || adjacentMeters <= 0f) return

    // 2. Calculate the angle using trigonometry (arccosine: cos(angle) = adjacent / hypotenuse).
    // This gives the angle from the center line to the point where the arc meets the box.
    val angleRad = acos(adjacentMeters / radiusMeters)
    val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

    // Determine angles for drawing.
    val forwardAngle = scale.forwardAngleDegrees()
    val sweepAngle = angleDeg * 2f
    val nearStartAngle = forwardAngle - angleDeg
    val farStartAngle = forwardAngle + 180f - angleDeg

    // 3. Prepare pixel coordinates.
    val penaltyArcRadiusPx = scale.primaryToPx(radiusMeters)
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

    // 4. Draw the arcs.
    drawArc(
        color = style.lineColor,
        startAngle = nearStartAngle,
        sweepAngle = sweepAngle,
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
        startAngle = farStartAngle,
        sweepAngle = sweepAngle,
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
 * Draw corner arcs.
 * Updated to inset the arc centers by half the line width, preventing clipping at the canvas edges.
 */
internal fun DrawScope.drawCornerArcs(
    scale: PitchScaleCalculator,
    style: PitchStyle,
) {
    val lineWidth = style.effectiveLineWidth(scale)
    // Inset needed so the center of the stroke sits exactly on the corner boundary.
    val inset = lineWidth / 2f

    val cornerRadiusPx = scale.primaryToPx(scale.dimensions.cornerArcRadius)
    val cornerDiameter = cornerRadiusPx * 2f

    // Four corner centers, adjusted by the inset.
    val corners =
        listOf(
            // top-left center
            Triple(0f, inset, inset),
            // top-right center
            Triple(90f, size.width - inset, inset),
            // bottom-left center
            Triple(270f, inset, size.height - inset),
            // bottom-right center
            Triple(180f, size.width - inset, size.height - inset),
        )

    corners.forEach { (startAngle, centerX, centerY) ->
        drawArc(
            color = style.lineColor,
            startAngle = startAngle,
            sweepAngle = 90f,
            useCenter = false,
            // The topLeft of the bounding box is relative to the adjusted center point.
            topLeft = Offset(x = centerX - cornerRadiusPx, y = centerY - cornerRadiusPx),
            size = Size(width = cornerDiameter, height = cornerDiameter),
            style = Stroke(width = lineWidth),
        )
    }
}
