package footballpitch

import androidx.compose.ui.geometry.Size
import footballpitch.model.PitchDimensions
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchStyle
import footballpitch.rendering.boundarySpec
import footballpitch.rendering.effectiveLineWidth
import footballpitch.rendering.goalBoxSpecs
import footballpitch.rendering.penaltyBoxSpecs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 0.01f

class PitchDrawingTest {
    private val dimensions: PitchDimensions =
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

    @Test
    fun boundaryLinesAreInsetWithinCanvas() {
        val canvasSize = Size(width = 1050f, height = 680f)
        val scale = PitchScaleCalculator(dimensions, canvasSize, PitchOrientation.Horizontal)
        val style = PitchStyle()

        val lineWidth = style.effectiveLineWidth(scale)
        val boundary = boundarySpec(lineWidth, canvasSize)
        val inset = lineWidth / 2f

        assertEquals(inset, boundary.topLeft.x, TOLERANCE)
        assertEquals(inset, boundary.topLeft.y, TOLERANCE)
        assertEquals(canvasSize.width - lineWidth, boundary.size.width, TOLERANCE)
        assertEquals(canvasSize.height - lineWidth, boundary.size.height, TOLERANCE)
    }

    @Test
    fun penaltyBoxesStayInsideCanvas() {
        val canvasSize = Size(width = 1050f, height = 680f)
        val scale = PitchScaleCalculator(dimensions, canvasSize, PitchOrientation.Horizontal)
        val style = PitchStyle()

        val lineWidth = style.effectiveLineWidth(scale)
        val inset = lineWidth / 2f
        val boxes = penaltyBoxSpecs(scale, lineWidth)

        boxes.forEach { box ->
            assertTrue(box.topLeft.x >= inset - TOLERANCE)
            assertTrue(box.topLeft.y >= inset - TOLERANCE)
            assertTrue(box.topLeft.x + box.size.width <= canvasSize.width - inset + TOLERANCE)
            assertTrue(box.topLeft.y + box.size.height <= canvasSize.height - inset + TOLERANCE)
        }
    }

    @Test
    fun penaltyBoxesAreMirroredWhenReversed() {
        val canvasSize = Size(width = 1050f, height = 680f)
        val forwardScale = PitchScaleCalculator(dimensions, canvasSize, PitchOrientation.Horizontal)
        val reverseScale = PitchScaleCalculator(dimensions, canvasSize, PitchOrientation.HorizontalReversed)
        val style = PitchStyle()

        val lineWidth = style.effectiveLineWidth(forwardScale)
        val forwardBoxes = penaltyBoxSpecs(forwardScale, lineWidth)
        val reverseBoxes = penaltyBoxSpecs(reverseScale, lineWidth)

        // Sort by x so we know which is left/right on the canvas
        val forwardSorted = forwardBoxes.sortedBy { it.topLeft.x }
        val reverseSorted = reverseBoxes.sortedBy { it.topLeft.x }

        assertEquals(forwardSorted.map { it.size }, reverseSorted.map { it.size })
    }

    @Test
    fun goalAreasStayInsideCanvasInVerticalOrientation() {
        val canvasSize = Size(width = 680f, height = 1050f)
        val scale = PitchScaleCalculator(dimensions, canvasSize, PitchOrientation.Vertical)
        val style = PitchStyle()

        val lineWidth = style.effectiveLineWidth(scale)
        val inset = lineWidth / 2f
        val boxes = goalBoxSpecs(scale, lineWidth)

        boxes.forEach { box ->
            assertTrue(box.topLeft.x >= inset - TOLERANCE)
            assertTrue(box.topLeft.y >= inset - TOLERANCE)
            assertTrue(box.topLeft.x + box.size.width <= canvasSize.width - inset + TOLERANCE)
            assertTrue(box.topLeft.y + box.size.height <= canvasSize.height - inset + TOLERANCE)
        }
    }

    @Test
    fun boundaryHandlesThickLines() {
        val canvasSize = Size(width = 200f, height = 100f)
        val scale = PitchScaleCalculator(dimensions, canvasSize, PitchOrientation.Horizontal)
        val style = PitchStyle(lineThicknessFactor = 5f)

        val lineWidth = style.effectiveLineWidth(scale)
        val boundary = boundarySpec(lineWidth, canvasSize)

        assertTrue(boundary.size.width >= 0f)
        assertTrue(boundary.size.height >= 0f)
        assertTrue(boundary.topLeft.x >= 0f)
        assertTrue(boundary.topLeft.y >= 0f)
    }
}
