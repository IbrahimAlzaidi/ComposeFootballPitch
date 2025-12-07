package footballpitch

import footballpitch.model.PitchPosition
import footballpitch.model.Player
import footballpitch.model.TeamLineup
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeamPlacementTest {
    @Test
    fun teamsSnapToEightAnchorLinesWhenBothProvided() {
        val home =
            TeamLineup(
                teamName = "Home",
                colorArgb = 0xFF0000FFL,
                players =
                    listOf(
                        Player(PitchPosition(0.05f, 0.4f), number = 1, isGoalkeeper = true),
                        Player(PitchPosition(0.15f, 0.3f), number = 2),
                        Player(PitchPosition(0.2f, 0.7f), number = 3),
                        Player(PitchPosition(0.34f, 0.5f), number = 6),
                        Player(PitchPosition(0.36f, 0.6f), number = 7),
                        Player(PitchPosition(0.55f, 0.4f), number = 9),
                        Player(PitchPosition(0.57f, 0.5f), number = 10),
                    ),
            )
        val away =
            TeamLineup(
                teamName = "Away",
                colorArgb = 0xFF00FF00L,
                players =
                    listOf(
                        Player(PitchPosition(0.98f, 0.6f), number = 1, isGoalkeeper = true),
                        Player(PitchPosition(0.82f, 0.5f), number = 2),
                        Player(PitchPosition(0.8f, 0.4f), number = 3),
                        Player(PitchPosition(0.68f, 0.3f), number = 6),
                        Player(PitchPosition(0.66f, 0.7f), number = 7),
                        Player(PitchPosition(0.44f, 0.5f), number = 9),
                        Player(PitchPosition(0.45f, 0.6f), number = 10),
                    ),
            )

        val (resolvedHome, resolvedAway) =
            distributeTeamsAcrossPitchHalves(home, away, stripeCount = 8)

        val homePlayers = requireNotNull(resolvedHome).players
        val awayPlayers = requireNotNull(resolvedAway).players

        val leftAnchors = setOf(0.0625f, 0.1875f, 0.3125f, 0.4375f)
        val rightAnchors = setOf(0.5625f, 0.6875f, 0.8125f, 0.9375f)

        assertEquals(0.0625f, homePlayers.first { it.isGoalkeeper }.position.x)
        assertEquals(0.9375f, awayPlayers.first { it.isGoalkeeper }.position.x)

        assertTrue(homePlayers.all { it.position.x in leftAnchors })
        assertTrue(awayPlayers.all { it.position.x in rightAnchors })
    }

    @Test
    fun singleTeamIsLeftUntouched() {
        val home =
            TeamLineup(
                teamName = "Solo",
                colorArgb = 0xFFFF0000L,
                players =
                    listOf(
                        Player(PitchPosition(0.6f, 0.5f), number = 4),
                    ),
            )

        val (resolvedHome, resolvedAway) = distributeTeamsAcrossPitchHalves(homeTeam = home, awayTeam = null)

        assertEquals(home.players.map { it.position }, resolvedHome?.players?.map { it.position })
        assertEquals(null, resolvedAway)
    }

    @Test
    fun outOfBoundsPlayersAreClampedAndAnchored() {
        val home =
            TeamLineup(
                teamName = "ClampHome",
                colorArgb = 0xFFFF00FFL,
                players =
                    listOf(
                        Player(PitchPosition(-0.5f, 1.2f), number = 1, isGoalkeeper = true),
                        Player(PitchPosition(-0.3f, -0.2f), number = 2),
                    ),
            )
        val away =
            TeamLineup(
                teamName = "ClampAway",
                colorArgb = 0xFFFFFF00L,
                players =
                    listOf(
                        Player(PitchPosition(1.5f, 0.5f), number = 1, isGoalkeeper = true),
                        Player(PitchPosition(2f, 0.1f), number = 9),
                    ),
            )

        val (resolvedHome, resolvedAway) =
            distributeTeamsAcrossPitchHalves(home, away, stripeCount = 8)

        val homePlayers = requireNotNull(resolvedHome).players
        val awayPlayers = requireNotNull(resolvedAway).players

        assertEquals(0.0625f, homePlayers.first { it.isGoalkeeper }.position.x)
        assertTrue(homePlayers.all { it.position.y in 0f..1f })
        assertEquals(0.9375f, awayPlayers.first { it.isGoalkeeper }.position.x)
        assertTrue(awayPlayers.all { it.position.y in 0f..1f })
    }
}
