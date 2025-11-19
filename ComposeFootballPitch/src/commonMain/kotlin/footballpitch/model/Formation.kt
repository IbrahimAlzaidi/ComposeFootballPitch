package footballpitch.model

import androidx.compose.runtime.Immutable

/**
 * Simple representation of a three-line formation (defence / midfield / attack).
 *
 * Examples:
 * - 4-4-2  -> defenders = 4, midfielders = 4, forwards = 2
 * - 4-3-3  -> defenders = 4, midfielders = 3, forwards = 3
 */
@Immutable
data class Formation(
    val defenders: Int,
    val midfielders: Int,
    val forwards: Int
) {
    init {
        require(defenders >= 0) { "defenders must be >= 0" }
        require(midfielders >= 0) { "midfielders must be >= 0" }
        require(forwards >= 0) { "forwards must be >= 0" }
    }
}

/**
 * Direction in which a team is attacking on the pitch.
 *
 * This is used when generating player positions from a [Formation] so that
 * home and away teams can be mirrored horizontally and do not overlap.
 */
enum class AttackDirection {
    /** Team attacks from the left goal towards the right goal. */
    LeftToRight,

    /** Team attacks from the right goal towards the left goal (mirrored). */
    RightToLeft
}

/**
 * Convenience helpers for generating basic formations and lineups.
 */
object Formations {

    /** Classic 4-4-2 formation. */
    fun fourFourTwo(): Formation = Formation(defenders = 4, midfielders = 4, forwards = 2)

    /** Modern 4-3-3 formation. */
    fun fourThreeThree(): Formation = Formation(defenders = 4, midfielders = 3, forwards = 3)

    /** Compact 4-5-1 formation. */
    fun fourFiveOne(): Formation = Formation(defenders = 4, midfielders = 5, forwards = 1)

    /** Defensive 5-3-2 formation. */
    fun fiveThreeTwo(): Formation = Formation(defenders = 5, midfielders = 3, forwards = 2)

    /** Defensive 5-4-1 formation. */
    fun fiveFourOne(): Formation = Formation(defenders = 5, midfielders = 4, forwards = 1)

    /** Attacking 3-4-3 formation. */
    fun threeFourThree(): Formation = Formation(defenders = 3, midfielders = 4, forwards = 3)

    /** Balanced 3-5-2 formation. */
    fun threeFiveTwo(): Formation = Formation(defenders = 3, midfielders = 5, forwards = 2)

    /** Very attacking 4-2-4 formation. */
    fun fourTwoFour(): Formation = Formation(defenders = 4, midfielders = 2, forwards = 4)

    /** Midfield-heavy 3-6-1 formation. */
    fun threeSixOne(): Formation = Formation(defenders = 3, midfielders = 6, forwards = 1)

    /** Classic 2-3-5 "WM" style formation. */
    fun twoThreeFive(): Formation = Formation(defenders = 2, midfielders = 3, forwards = 5)
}

/**
 * Create a list of [Player] instances laid out in a simple, symmetric way
 * for this [Formation]. A goalkeeper is always added by default.
 *
 * The resulting positions are normalized ([PitchPosition]) and are designed
 * to work with the default [footballpitch.FootballPitch] orientation; the
 * orientation parameter on [footballpitch.FootballPitch] will rotate them
 * as needed. [attackDirection] controls whether the team is laid out from
 * left-to-right or mirrored right-to-left.
 */
fun Formation.toPlayers(
    includeGoalkeeper: Boolean = true,
    startingNumber: Int = 1,
    attackDirection: AttackDirection = AttackDirection.LeftToRight
): List<Player> {
    val players = mutableListOf<Player>()
    var nextNumber = startingNumber

    fun mirrorX(x: Float): Float =
        when (attackDirection) {
            AttackDirection.LeftToRight -> x
            AttackDirection.RightToLeft -> 1f - x
        }

    // Base X positions for a left-to-right attacking team.
    val gkX = mirrorX(0.06f)
    val defendersX = mirrorX(0.18f)
    val midfieldersX = mirrorX(0.5f)
    val forwardsX = mirrorX(0.9f)

    if (includeGoalkeeper) {
        players += Player(
            position = PitchPosition(x = gkX, y = 0.5f),
            number = nextNumber++,
            isGoalkeeper = true
        )
    }

    fun addLine(count: Int, x: Float) {
        if (count <= 0) return
        val ys = evenlySpacedY(count)
        ys.forEach { y ->
            players += Player(
                position = PitchPosition(x = x, y = y),
                number = nextNumber++
            )
        }
    }

    // Very simple horizontal layout: defenders near own box, midfield central, forwards near opponent box.
    addLine(defenders, x = defendersX)
    addLine(midfielders, x = midfieldersX)
    addLine(forwards, x = forwardsX)

    return players
}

/**
 * Convenience function to create a [TeamLineup] from a [Formation].
 *
 * This is intentionally simple: it generates evenly spaced defenders,
 * midfielders and forwards, plus a single goalkeeper. You can always
 * override or fine-tune the resulting [TeamLineup.players] list.
 */
fun Formation.toTeamLineup(
    teamName: String,
    colorArgb: Long,
    goalkeeperColorArgb: Long? = null,
    kitStyle: TeamKitStyle = TeamKitStyle(),
    attackDirection: AttackDirection = AttackDirection.LeftToRight
): TeamLineup {
    val players = toPlayers(
        includeGoalkeeper = true,
        startingNumber = 1,
        attackDirection = attackDirection
    )
    return TeamLineup(
        teamName = teamName,
        colorArgb = colorArgb,
        goalkeeperColorArgb = goalkeeperColorArgb,
        players = players,
        kitStyle = kitStyle
    )
}

/**
 * Evenly distribute [count] players between the bottom and top of the pitch.
 */
private fun evenlySpacedY(count: Int): List<Float> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(0.5f)

    val minY = 0.12f
    val maxY = 0.88f
    val step = (maxY - minY) / (count - 1)

    return List(count) { index ->
        (minY + step * index).coerceIn(0f, 1f)
    }
}
