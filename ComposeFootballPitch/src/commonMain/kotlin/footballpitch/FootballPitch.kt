// FootballPitch.kt
package footballpitch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import footballpitch.model.PitchDimensions
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchStyle
import footballpitch.model.TeamLineup
import footballpitch.rendering.drawBoundaryLines
import footballpitch.rendering.drawCenterArea
import footballpitch.rendering.drawCornerArcs
import footballpitch.rendering.drawGoalAreas
import footballpitch.rendering.drawPenaltyArcs
import footballpitch.rendering.drawPenaltyAreas
import footballpitch.rendering.drawPenaltySpots
import footballpitch.rendering.drawPitchBackground
import footballpitch.rendering.drawTeam
import kotlin.math.max

/**
 * High-level composable that renders a football pitch and optional team lineups.
 *
 * This is the main entry point for consumers of the library. It takes
 * dimension, styling and orientation configuration, and renders the pitch
 * consistently across platforms using Compose Multiplatform.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun FootballPitch(
    modifier: Modifier = Modifier.fillMaxWidth().padding(4.dp),
    homeTeam: TeamLineup? = null,
    awayTeam: TeamLineup? = null,
    showCenterCircle: Boolean = true,
    dimensions: PitchDimensions = FIFA_DIMENSIONS,
    orientation: PitchOrientation = PitchOrientation.Horizontal,
    style: PitchStyle = PitchStyle(),
) {
    val textMeasurer = rememberTextMeasurer()

    val baseRatio = dimensions.length / dimensions.width
    val aspectRatio =
        when (orientation) {
            PitchOrientation.Horizontal,
            PitchOrientation.HorizontalReversed,
            -> baseRatio
            PitchOrientation.Vertical,
            PitchOrientation.VerticalReversed,
            -> 1f / baseRatio
        }

    Canvas(
        modifier = modifier.aspectRatio(aspectRatio),
    ) {
        val scaleCalculator = PitchScaleCalculator(dimensions, size, orientation)

        drawPitchBackground(style)
        drawPitchLines(scaleCalculator, style, showCenterCircle)

        homeTeam?.let { drawTeam(it, textMeasurer, scaleCalculator) }
        awayTeam?.let { drawTeam(it, textMeasurer, scaleCalculator) }
    }
}

// Default dimensions according to FIFA standards
private val FIFA_DIMENSIONS =
    PitchDimensions(
        length = 105f,
        width = 68f,
        penaltyAreaDepth = 16.5f,
        penaltyAreaWidth = 40.32f,
        goalAreaDepth = 5.5f,
        goalAreaWidth = 18.32f,
        penaltyMarkDistance = 11f,
        circleRadius = 9.15f,
        cornerArcRadius = 1f,
    )

/**
 * Internal helper that converts real-world dimensions into canvas coordinates.
 *
 * The scaling logic lives here so that both line drawing and player placement
 * use the same coordinate system.
 */
class PitchScaleCalculator(
    val dimensions: PitchDimensions,
    val canvasSize: Size,
    val orientation: PitchOrientation,
) {
    private val isVertical = orientation == PitchOrientation.Vertical || orientation == PitchOrientation.VerticalReversed
    private val isReversed = orientation == PitchOrientation.HorizontalReversed || orientation == PitchOrientation.VerticalReversed

    private val primaryExtentPx: Float get() = if (isVertical) canvasSize.height else canvasSize.width
    private val secondaryExtentPx: Float get() = if (isVertical) canvasSize.width else canvasSize.height

    private val scale: Float get() = primaryExtentPx / dimensions.length

    val lineWidth: Float get() = max(scale * 0.16f, canvasSize.minDimension * 0.004f)
    private val pxPerMeter: Float get() = scale

    // Convert metres along the primary (goal-to-goal) axis to pixels.
    fun primaryToPx(meters: Float): Float = meters * pxPerMeter

    // Convert metres along the secondary (touchline) axis to pixels.
    fun secondaryToPx(meters: Float): Float = meters * pxPerMeter

    // Map a point expressed in metres from the top/left of the pitch to canvas coordinates.
    fun pitchOffset(
        primaryMeters: Float,
        secondaryMetersFromTop: Float,
    ): Offset {
        val primaryPx = primaryToPx(primaryMeters)
        val secondaryPx = secondaryToPx(secondaryMetersFromTop)

        val orientedPrimary = if (isReversed) primaryExtentPx - primaryPx else primaryPx
        val orientedSecondary = if (isReversed) secondaryExtentPx - secondaryPx else secondaryPx

        return if (isVertical) {
            Offset(x = orientedSecondary, y = orientedPrimary)
        } else {
            Offset(x = orientedPrimary, y = orientedSecondary)
        }
    }

    fun pitchSize(
        primaryMeters: Float,
        secondaryMeters: Float,
    ): Size {
        val primaryPx = primaryToPx(primaryMeters)
        val secondaryPx = secondaryToPx(secondaryMeters)
        return if (isVertical) {
            Size(width = secondaryPx, height = primaryPx)
        } else {
            Size(width = primaryPx, height = secondaryPx)
        }
    }

    fun forwardAngleDegrees(): Float =
        when (orientation) {
            PitchOrientation.Horizontal -> 0f
            PitchOrientation.HorizontalReversed -> 180f
            PitchOrientation.Vertical -> 90f
            PitchOrientation.VerticalReversed -> 270f
        }

    val center: Offset get() = Offset(x = canvasSize.width / 2f, y = canvasSize.height / 2f)

    val lengthPx: Float get() = primaryExtentPx
    val widthPx: Float get() = secondaryExtentPx

    fun positionToCanvas(position: footballpitch.model.PitchPosition): Offset {
        val clampedX = position.x.coerceIn(0f, 1f)
        val clampedY = position.y.coerceIn(0f, 1f)

        val metersAlongLength = clampedX * dimensions.length
        val metersFromTop = (1f - clampedY) * dimensions.width

        return pitchOffset(primaryMeters = metersAlongLength, secondaryMetersFromTop = metersFromTop)
    }
}

/**
 * Main function to draw all pitch lines
 */
private fun DrawScope.drawPitchLines(
    scale: PitchScaleCalculator,
    style: PitchStyle,
    showCenterCircle: Boolean,
) {
    drawBoundaryLines(scale, style)
    drawCenterArea(scale, style, showCenterCircle)
    drawPenaltyAreas(scale, style)
    drawGoalAreas(scale, style)
    drawPenaltySpots(scale, style)
    drawPenaltyArcs(scale, style)
    drawCornerArcs(scale, style)
}
