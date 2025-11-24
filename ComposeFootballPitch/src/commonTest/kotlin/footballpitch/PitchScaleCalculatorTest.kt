package footballpitch

import androidx.compose.ui.geometry.Size
import footballpitch.model.PitchDimensions
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchPosition
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TOLERANCE = 0.001f

class PitchScaleCalculatorTest {
    private val dims: PitchDimensions =
        PitchDimensions(
            length = 100f,
            width = 50f,
            penaltyAreaDepth = 16.5f,
            penaltyAreaWidth = 40.32f,
            goalAreaDepth = 5.5f,
            goalAreaWidth = 18.32f,
            penaltyMarkDistance = 11f,
            circleRadius = 9.15f,
            cornerArcRadius = 1f,
        )

    @Test
    fun positionMapsCorrectlyHorizontal() {
        val scale = PitchScaleCalculator(dims, Size(width = 1000f, height = 500f), PitchOrientation.Horizontal)
        val center = scale.positionToCanvas(PitchPosition(0.5f, 0.5f))
        assertEquals(500f, center.x, TOLERANCE)
        assertEquals(250f, center.y, TOLERANCE)
    }

    @Test
    fun positionMapsCorrectlyHorizontalReversed() {
        val scale = PitchScaleCalculator(dims, Size(width = 1000f, height = 500f), PitchOrientation.HorizontalReversed)
        val origin = scale.positionToCanvas(PitchPosition(0f, 0f))
        val topRight = scale.positionToCanvas(PitchPosition(1f, 1f))
        assertEquals(1000f, origin.x, TOLERANCE)
        assertEquals(0f, origin.y, TOLERANCE)
        assertEquals(0f, topRight.x, TOLERANCE)
        assertEquals(500f, topRight.y, TOLERANCE)
    }

    @Test
    fun positionMapsCorrectlyVertical() {
        val scale = PitchScaleCalculator(dims, Size(width = 500f, height = 1000f), PitchOrientation.Vertical)
        val bottomLeft = scale.positionToCanvas(PitchPosition(0f, 0f))
        val topRight = scale.positionToCanvas(PitchPosition(1f, 1f))
        assertEquals(500f, bottomLeft.x, TOLERANCE)
        assertEquals(0f, bottomLeft.y, TOLERANCE)
        assertEquals(0f, topRight.x, TOLERANCE)
        assertEquals(1000f, topRight.y, TOLERANCE)
    }

    @Test
    fun forwardAngleMatchesOrientation() {
        val angles =
            mapOf(
                PitchOrientation.Horizontal to 0f,
                PitchOrientation.HorizontalReversed to 180f,
                PitchOrientation.Vertical to 90f,
                PitchOrientation.VerticalReversed to 270f,
            )
        angles.forEach { (orientation, expected) ->
            val scale = PitchScaleCalculator(dims, Size(width = 1000f, height = 500f), orientation)
            assertEquals(expected, scale.forwardAngleDegrees(), TOLERANCE)
        }
    }
}
