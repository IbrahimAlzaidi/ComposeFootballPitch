package footballpitch

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import footballpitch.model.PitchBackground
import footballpitch.model.PitchDimensions
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchStyle
import footballpitch.rendering.drawPitchBackground
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val DEFAULT_DIMENSIONS =
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

private data class RenderResult(
    val bitmap: ImageBitmap,
    val scale: PitchScaleCalculator,
)

class PitchRenderingSnapshotTest {
    @Test
    fun centerSpotIsPainted() {
        val result =
            renderPitch(
                style =
                    PitchStyle(
                        background = PitchBackground.Solid(Color(0xFF0F6D2B)),
                        lineColor = Color.White,
                    ),
            )

        val pixels = result.bitmap.toPixelMap()
        val centerColor = pixels[pixels.width / 2, pixels.height / 2]

        assertColorClose(Color.White, centerColor)
    }

    @Test
    fun penaltySpotsAppearAtExpectedPositions() {
        val style =
            PitchStyle(
                background = PitchBackground.Solid(Color(0xFF0F6D2B)),
                lineColor = Color.White,
            )
        val result = renderPitch(style = style)
        val pixels = result.bitmap.toPixelMap()

        val near =
            result.scale.pitchOffset(
                primaryMeters = DEFAULT_DIMENSIONS.penaltyMarkDistance,
                secondaryMetersFromTop = DEFAULT_DIMENSIONS.width / 2f,
            )
        val far =
            result.scale.pitchOffset(
                primaryMeters = DEFAULT_DIMENSIONS.length - DEFAULT_DIMENSIONS.penaltyMarkDistance,
                secondaryMetersFromTop = DEFAULT_DIMENSIONS.width / 2f,
            )

        val nearColor = pixels[near.x.roundToInt(), near.y.roundToInt()]
        val farColor = pixels[far.x.roundToInt(), far.y.roundToInt()]

        assertColorClose(style.lineColor, nearColor)
        assertColorClose(style.lineColor, farColor)
    }

    @Test
    fun boundaryLinesRenderInVerticalOrientation() {
        val result =
            renderPitch(
                orientation = PitchOrientation.Vertical,
                style =
                    PitchStyle(
                        background = PitchBackground.Solid(Color(0xFF103F1C)),
                        lineColor = Color.White,
                    ),
            )
        val pixels = result.bitmap.toPixelMap()

        val leftEdge = pixels[1, pixels.height / 2]
        val topEdge = pixels[pixels.width / 2, 1]

        assertColorClose(Color.White, leftEdge)
        assertColorClose(Color.White, topEdge)
    }

    @Test
    fun stripedBackgroundAlternatesColors() {
        val stripeColors = listOf(Color.Red, Color.Blue)
        val result =
            renderPitch(
                style =
                    PitchStyle(
                        background = PitchBackground.Stripes(colors = stripeColors, stripeCount = 4),
                        lineColor = Color.White,
                    ),
            )

        val pixels = result.bitmap.toPixelMap()
        val stripeWidth = pixels.width / 4
        val y = pixels.height / 2

        val firstStripe = pixels[stripeWidth / 2, y]
        val secondStripe = pixels[stripeWidth + stripeWidth / 2, y]

        assertNotEquals(firstStripe, secondStripe, "Adjacent stripes should differ in color")
    }

    @Test
    fun gradientBackgroundFillsCanvas() {
        val result =
            renderPitch(
                style =
                    PitchStyle(
                        background = PitchBackground.Gradient(colors = listOf(Color.Red, Color.Blue)),
                        lineColor = Color.White,
                    ),
            )
        val pixels = result.bitmap.toPixelMap()

        val nonTransparent =
            (0 until pixels.width).sumOf { x ->
                (0 until pixels.height).count { y -> pixels[x, y].alpha > 0f }
            }

        assertEquals(pixels.width * pixels.height, nonTransparent, "Gradient should cover the entire canvas")
    }

    private fun renderPitch(
        // preserves 105x68 ratio with generous line width
        size: Size = Size(1050f, 680f),
        orientation: PitchOrientation = PitchOrientation.Horizontal,
        style: PitchStyle = PitchStyle(),
    ): RenderResult {
        val image = ImageBitmap(size.width.toInt(), size.height.toInt())
        val canvas = Canvas(image)
        val scale = PitchScaleCalculator(DEFAULT_DIMENSIONS, size, orientation)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = size,
        ) {
            drawPitchBackground(style)
            drawPitchLines(scale, style, showCenterCircle = true)
        }

        return RenderResult(bitmap = image, scale = scale)
    }

    private fun assertColorClose(
        expected: Color,
        actual: Color,
        tolerance: Float = 0.05f,
    ) {
        assertTrue(
            actual.red in (expected.red - tolerance)..(expected.red + tolerance) &&
                actual.green in (expected.green - tolerance)..(expected.green + tolerance) &&
                actual.blue in (expected.blue - tolerance)..(expected.blue + tolerance) &&
                actual.alpha in (expected.alpha - tolerance)..(expected.alpha + tolerance),
            "Expected $expected but was $actual",
        )
    }
}
