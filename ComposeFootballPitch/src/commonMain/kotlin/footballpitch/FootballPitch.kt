package footballpitch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import footballpitch.model.PitchDimensions
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchPosition
import footballpitch.model.PitchStyle
import footballpitch.model.Player
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
 * Default dimensions according to FIFA standards (105m x 68m).
 * Useful as a default for standard professional matches.
 */
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
 * High-level composable that renders a football pitch and optional team lineups.
 *
 * @param modifier Modifier used to adjust the layout or semantics of the pitch.
 * @param homeTeam The home team lineup to render.
 * @param awayTeam The away team lineup to render.
 * @param showCenterCircle Whether to draw the center circle line.
 * @param dimensions Real-world dimensions of the pitch in meters. Defaults to [FIFA_DIMENSIONS].
 * @param orientation Controls the visual orientation of the pitch (e.g. horizontal TV view vs vertical mobile view).
 * @param style Styling configuration for the pitch markings, grass, and fonts.
 * @param contentDescription Optional semantics description. If null, a default description summarizing the visible teams is used.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun FootballPitch(
    modifier: Modifier = Modifier,
    homeTeam: TeamLineup? = null,
    awayTeam: TeamLineup? = null,
    showCenterCircle: Boolean = true,
    dimensions: PitchDimensions = FIFA_DIMENSIONS,
    orientation: PitchOrientation = PitchOrientation.Horizontal,
    style: PitchStyle = PitchStyle(),
    contentDescription: String? = null,
) {
    val textMeasurer = rememberTextMeasurer()

    // 1. Calculate Aspect Ratio based on orientation
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

    // 2. Memoize expensive team layout calculations.
    // This ensures the distribution logic only runs when the team objects actually change.
    val (resolvedHomeTeam, resolvedAwayTeam) =
        remember(homeTeam, awayTeam) {
            distributeTeamsAcrossPitchHalves(homeTeam, awayTeam)
        }

    // 3. Smart Semantics
    val semanticsDesc =
        contentDescription ?: remember(homeTeam?.teamName, awayTeam?.teamName) {
            buildString {
                append("Football Pitch.")
                if (homeTeam != null && awayTeam != null) {
                    append(" Showing ${homeTeam.teamName} versus ${awayTeam.teamName}.")
                }
            }
        }

    Canvas(
        modifier =
            modifier
                .semantics { this.contentDescription = semanticsDesc }
                .aspectRatio(aspectRatio),
    ) {
        // Scale calculator relies on canvas size, so it lives inside the draw scope.
        val scaleCalculator = PitchScaleCalculator(dimensions, size, orientation)

        // Layer 1: Background
        drawPitchBackground(style)

        // Layer 2: Markings
        drawPitchLines(scaleCalculator, style, showCenterCircle)

        // Layer 3: Players (Home then Away)
        // Pass style down for things like font customization
        resolvedHomeTeam?.let { drawTeam(it, textMeasurer, scaleCalculator, style) }
        resolvedAwayTeam?.let { drawTeam(it, textMeasurer, scaleCalculator, style) }
    }
}

/**
 * When both teams are visible, anchor their lines to evenly spaced vertical guides (mirroring
 * an 8-stripe layout) so they never overlap. Single-team views are left untouched.
 *
 * @param stripeCount number of stripes to derive anchor positions from (minimum 8)
 */
internal fun distributeTeamsAcrossPitchHalves(
    homeTeam: TeamLineup?,
    awayTeam: TeamLineup?,
    stripeCount: Int = 8,
): Pair<TeamLineup?, TeamLineup?> {
    // Only adjust when both teams are shown; a single team keeps its original layout.
    if (homeTeam == null || awayTeam == null) return homeTeam to awayTeam

    // Derive eight anchor lines (centre of stripes) across the pitch length.
    val stripes = stripeCount.coerceAtLeast(8)
    val stripeWidth = 1f / stripes
    val anchors = List(8) { index -> ((index + 0.5f) * stripeWidth).coerceIn(0f, 1f) }

    val homeAnchors =
        TeamAnchors(
            goalkeeper = anchors[0],
            defence = anchors[1],
            midfield = anchors[2],
            attack = anchors[3],
            goalOnRight = false,
        )
    val awayAnchors =
        TeamAnchors(
            goalkeeper = anchors[7],
            defence = anchors[6],
            midfield = anchors[5],
            attack = anchors[4],
            goalOnRight = true,
        )

    return projectToAnchors(homeTeam, homeAnchors) to projectToAnchors(awayTeam, awayAnchors)
}

private data class TeamAnchors(
    val goalkeeper: Float,
    val defence: Float,
    val midfield: Float,
    val attack: Float,
    val goalOnRight: Boolean,
)

/**
 * Clamp and re-map all player X positions to the provided anchor lines, preserving Y as-is.
 */
private fun projectToAnchors(
    team: TeamLineup,
    anchors: TeamAnchors,
): TeamLineup {
    val clampedPlayers =
        team.players.map { player ->
            val clampedX = player.position.x.coerceIn(0f, 1f)
            val clampedY = player.position.y.coerceIn(0f, 1f)
            player.copy(position = PitchPosition(x = clampedX, y = clampedY))
        }

    val goalkeeperIndices = clampedPlayers.withIndex().filter { it.value.isGoalkeeper }.map { it.index }
    val outfieldIndices = clampedPlayers.withIndex().filterNot { it.value.isGoalkeeper }.map { it.index }

    val (defence, midfield, attack) = bucketOutfieldPlayers(clampedPlayers, outfieldIndices, anchors.goalOnRight)

    val adjustedPlayers =
        clampedPlayers.mapIndexed { index, player ->
            val newX =
                when {
                    index in goalkeeperIndices -> anchors.goalkeeper
                    index in defence -> anchors.defence
                    index in midfield -> anchors.midfield
                    index in attack -> anchors.attack
                    else -> player.position.x // fallback; should not happen
                }
            player.copy(position = player.position.copy(x = newX))
        }

    return team.copy(players = adjustedPlayers)
}

private data class IndexedDistance(val index: Int, val distance: Float)

/**
 * Split outfield players into defence, midfield and attack lines based on their distance from own goal.
 *
 * This stays robust for custom lineups by detecting natural gaps between player lines instead of
 * relying on specific formations.
 */
private fun bucketOutfieldPlayers(
    players: List<Player>,
    outfieldIndices: List<Int>,
    goalOnRight: Boolean,
): Triple<List<Int>, List<Int>, List<Int>> {
    if (outfieldIndices.isEmpty()) return Triple(emptyList(), emptyList(), emptyList())

    fun distanceFromOwnGoal(x: Float): Float = if (goalOnRight) 1f - x else x

    val sortedByDistance =
        outfieldIndices
            .map { idx ->
                val x = players[idx].position.x
                IndexedDistance(idx, distanceFromOwnGoal(x))
            }
            .sortedBy { it.distance }

    when (sortedByDistance.size) {
        1 -> return Triple(emptyList(), listOf(sortedByDistance[0].index), emptyList()) // place lone outfielder as midfield
        2 ->
            return Triple(
                listOf(sortedByDistance[0].index),
                emptyList(),
                listOf(sortedByDistance[1].index),
            )
        3 ->
            return Triple(
                listOf(sortedByDistance[0].index),
                listOf(sortedByDistance[1].index),
                listOf(sortedByDistance[2].index),
            )
    }

    val distances = sortedByDistance.map { it.distance }
    val gaps =
        distances.zipWithNext().mapIndexed { idx, pair ->
            val gap = pair.second - pair.first
            idx to gap
        }

    val splitPositions =
        gaps
            .sortedByDescending { it.second }
            .take(2)
            .map { it.first + 1 }
            .sorted()

    val firstSplit = splitPositions.getOrElse(0) { 1 }.coerceIn(1, sortedByDistance.lastIndex)
    val secondSplit = splitPositions.getOrElse(1) { firstSplit + 1 }.coerceIn(firstSplit + 1, sortedByDistance.size)

    val defence = sortedByDistance.subList(0, firstSplit).map { it.index }
    val midfield = sortedByDistance.subList(firstSplit, secondSplit).map { it.index }
    val attack = sortedByDistance.subList(secondSplit, sortedByDistance.size).map { it.index }

    return Triple(defence, midfield, attack)
}

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

    fun positionToCanvas(position: PitchPosition): Offset {
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
fun DrawScope.drawPitchLines(
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
