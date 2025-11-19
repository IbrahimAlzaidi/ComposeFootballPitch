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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import footballpitch.rendering.drawBoundaryLines
import footballpitch.rendering.drawCenterArea
import footballpitch.rendering.drawCornerArcs
import footballpitch.rendering.drawGoalAreas
import footballpitch.rendering.drawPenaltyAreas
import footballpitch.rendering.drawPenaltyArcs
import footballpitch.rendering.drawPenaltySpots
import footballpitch.rendering.drawPitchBackground
import footballpitch.rendering.drawTeam
import footballpitch.model.PitchDimensions
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchStyle
import footballpitch.model.TeamLineup
import kotlin.math.max

/**
 * High-level composable that renders a football pitch and optional team lineups.
 *
 * This is the main entry point for consumers of the library. It takes
 * dimension, styling and orientation configuration, and renders the pitch
 * consistently across platforms using Compose Multiplatform.
 */
@Composable
fun FootballPitch(
    modifier: Modifier = Modifier.fillMaxWidth().padding(4.dp),
    homeTeam: TeamLineup? = null,
    awayTeam: TeamLineup? = null,
    showCenterCircle: Boolean = true,
    dimensions: PitchDimensions = FIFA_DIMENSIONS,
    orientation: PitchOrientation = PitchOrientation.Horizontal,
    style: PitchStyle = PitchStyle()
) {
    val aspectRatio = when (orientation) {
        PitchOrientation.Horizontal,
        PitchOrientation.HorizontalReversed -> FIFA_RATIO
        PitchOrientation.Vertical,
        PitchOrientation.VerticalReversed -> 1f / FIFA_RATIO
    }

    Canvas(
        modifier = modifier.aspectRatio(aspectRatio)
    ) {
        val scaleCalculator = PitchScaleCalculator(dimensions, size)

        val rotation = when (orientation) {
            PitchOrientation.Horizontal -> 0f
            PitchOrientation.HorizontalReversed -> 180f
            PitchOrientation.Vertical -> 90f
            PitchOrientation.VerticalReversed -> 270f
        }

        rotate(degrees = rotation, pivot = center) {
            drawPitchBackground(style)
            drawPitchLines(scaleCalculator, style, showCenterCircle)

            homeTeam?.let { drawTeam(it) }
            awayTeam?.let { drawTeam(it) }
        }
    }
}

// Default dimensions according to FIFA standards
private val FIFA_DIMENSIONS = PitchDimensions(
    length = 105f,
    width = 68f,
    penaltyAreaDepth = 16.5f,
    penaltyAreaWidth = 40.32f,
    goalAreaDepth = 5.5f,
    goalAreaWidth = 18.32f,
    penaltyMarkDistance = 11f,
    circleRadius = 9.15f,
    cornerArcRadius = 1f
)

private const val FIFA_RATIO = 105f / 68f

/**
 * Internal helper that converts real-world dimensions into canvas coordinates.
 *
 * The scaling logic lives here so that both line drawing and player placement
 * use the same coordinate system.
 */
class PitchScaleCalculator(
    val dimensions: PitchDimensions,
    val canvasSize: Size
) {
    private val scale: Float get() = canvasSize.width / dimensions.length

    val lengthPx: Float get() = canvasSize.width
    val widthPx: Float get() = canvasSize.height
    val lineWidth: Float get() = max(scale * 0.16f, canvasSize.minDimension * 0.004f)

    // Convert meters to pixels
    fun toPx(meters: Float): Float = meters * scale

    val center: Offset get() = Offset(x = lengthPx / 2f, y = widthPx / 2f)
}


/**
 * Main function to draw all pitch lines
 */
private fun DrawScope.drawPitchLines(
    scale: PitchScaleCalculator,
    style: PitchStyle,
    showCenterCircle: Boolean
) {
    drawBoundaryLines(scale, style)
    drawCenterArea(scale, style, showCenterCircle)
    drawPenaltyAreas(scale, style)
    drawGoalAreas(scale, style)
    drawPenaltySpots(scale, style)
    drawPenaltyArcs(scale, style)
    drawCornerArcs(scale, style)
}
