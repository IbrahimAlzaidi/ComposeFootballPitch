package footballpitch

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import footballpitch.model.PitchDimensions
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchPosition
import footballpitch.model.Player
import footballpitch.model.ShirtStyle
import footballpitch.model.TeamKitStyle
import footballpitch.model.TeamLineup
import footballpitch.rendering.resolvePlayers
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerRenderingTest {
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
    fun resolvePlayersKeepsPositionsWithinCanvas() {
        val scale =
            PitchScaleCalculator(dims, Size(width = 1000f, height = 500f), PitchOrientation.Horizontal)
        val lineup =
            TeamLineup(
                teamName = "Mixed",
                colorArgb = Color.Blue.value.toLong(),
                goalkeeperColorArgb = null,
                players =
                    listOf(
                        Player(PitchPosition(0f, 0f), number = 1, isGoalkeeper = true),
                        Player(PitchPosition(1f, 1f), number = 9),
                        Player(PitchPosition(0.5f, 0.5f), number = 10),
                        // deliberately out of bounds to test clamping
                        Player(PitchPosition(-0.2f, 1.2f), number = 11),
                    ),
                kitStyle =
                    TeamKitStyle(
                        fieldPlayerShirtStyle = ShirtStyle.STRIPED,
                        goalkeeperShirtStyle = ShirtStyle.GOALKEEPER,
                    ),
            )

        val resolved = resolvePlayers(lineup, scale, baseSize = 10f)

        resolved.forEach {
            assertTrue(it.center.x in 0f..1000f)
            assertTrue(it.center.y in 0f..500f)
        }
    }

    @Test
    fun goalkeeperUsesOverrideOrFallbackColor() {
        val scale =
            PitchScaleCalculator(dims, Size(width = 1000f, height = 500f), PitchOrientation.Horizontal)
        val kit = TeamKitStyle(fieldPlayerShirtStyle = ShirtStyle.CLASSIC, goalkeeperShirtStyle = ShirtStyle.GOALKEEPER)

        val withOverride =
            TeamLineup(
                teamName = "Override",
                colorArgb = Color.Red.value.toLong(),
                goalkeeperColorArgb = Color.Yellow.value.toLong(),
                players =
                    listOf(
                        Player(PitchPosition(0.2f, 0.5f), isGoalkeeper = true),
                    ),
                kitStyle = kit,
            )
        val withoutOverride =
            TeamLineup(
                teamName = "Fallback",
                colorArgb = Color.Red.value.toLong(),
                goalkeeperColorArgb = null,
                players =
                    listOf(
                        Player(PitchPosition(0.2f, 0.5f), isGoalkeeper = true),
                    ),
                kitStyle = kit,
            )

        resolvePlayers(withOverride, scale, baseSize = 10f)
        resolvePlayers(withoutOverride, scale, baseSize = 10f)
    }

    @Test
    fun stylesAreAppliedPerRole() {
        val scale =
            PitchScaleCalculator(dims, Size(width = 1000f, height = 500f), PitchOrientation.Vertical)
        val kit = TeamKitStyle(fieldPlayerShirtStyle = ShirtStyle.COLLAR, goalkeeperShirtStyle = ShirtStyle.CIRCLE)
        val lineup =
            TeamLineup(
                teamName = "Styled",
                colorArgb = Color.Green.value.toLong(),
                goalkeeperColorArgb = Color.Magenta.value.toLong(),
                players =
                    listOf(
                        Player(PitchPosition(0.2f, 0.5f), isGoalkeeper = true, number = 1),
                        Player(PitchPosition(0.8f, 0.5f), isGoalkeeper = false, number = 9),
                    ),
                kitStyle = kit,
            )

        val resolved = resolvePlayers(lineup, scale, baseSize = 10f)
        val gk = resolved.first { it.appearance.number == "1" }
        val outfield = resolved.first { it.appearance.number == "9" }

        assertEquals(ShirtStyle.CIRCLE, gk.appearance.style)
        assertEquals(ShirtStyle.COLLAR, outfield.appearance.style)
    }
}
