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
    val forwards: Int,
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
 * "Left" and "Right" refer to the default horizontal orientation before any
 * [footballpitch.model.PitchOrientation] rotation is applied; vertical
 * orientations rotate these positions so goals end up at the top/bottom.
 */
enum class AttackDirection {
    /** Team attacks from the left goal towards the right goal. */
    LeftToRight,

    /** Team attacks from the right goal towards the left goal (mirrored). */
    RightToLeft,
}

/**
 * Return the opposite direction to ensure two teams face each other.
 */
fun AttackDirection.opposite(): AttackDirection =
    when (this) {
        AttackDirection.LeftToRight -> AttackDirection.RightToLeft
        AttackDirection.RightToLeft -> AttackDirection.LeftToRight
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
 *
 * Squad numbers are generated automatically and kept in the 1–11 range so
 * that callers do not need to provide them manually.
 */
fun Formation.toPlayers(
    includeGoalkeeper: Boolean = true,
    startingNumber: Int = 1,
    attackDirection: AttackDirection = AttackDirection.LeftToRight,
): List<Player> {
    val players = mutableListOf<Player>()
    var nextNumber = startingNumber

    fun nextSquadNumber(): Int {
        val normalized = ((nextNumber - 1) % 11) + 1
        nextNumber++
        return normalized
    }

    fun mirrorX(x: Float): Float =
        when (attackDirection) {
            AttackDirection.LeftToRight -> x
            AttackDirection.RightToLeft -> 1f - x
        }

    // Base X positions for a left-to-right attacking team. Anchored to typical
    // zones (six-yard box, penalty area, own half, final third).
    val gkX = mirrorX(0.07f)
    val defendersX = mirrorX(0.22f)
    val midfieldersX = mirrorX(0.46f)
    val forwardsX = mirrorX(0.76f)

    if (includeGoalkeeper) {
        players +=
            Player(
                position = PitchPosition(x = gkX, y = 0.5f),
                number = nextSquadNumber(),
                isGoalkeeper = true,
            )
    }

    fun addLine(
        count: Int,
        x: Float,
    ) {
        if (count <= 0) return
        val ys = evenlySpacedY(count)
        ys.forEach { y ->
            players +=
                Player(
                    position = PitchPosition(x = x, y = y),
                    number = nextSquadNumber(),
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
 * override or fine-tune the resulting [TeamLineup.players] list. By default
 * it numbers the squad 1-11 in order of placement and attacks left-to-right;
 * use [MatchTeams.toLineups] to auto-mirror the away side.
 */
fun Formation.toTeamLineup(
    teamName: String,
    colorArgb: Long,
    goalkeeperColorArgb: Long? = null,
    kitStyle: TeamKitStyle = TeamKitStyle(),
    attackDirection: AttackDirection = AttackDirection.LeftToRight,
): TeamLineup {
    val players =
        toPlayers(
            includeGoalkeeper = true,
            startingNumber = 1,
            attackDirection = attackDirection,
        )
    return TeamLineup(
        teamName = teamName,
        colorArgb = colorArgb,
        goalkeeperColorArgb = goalkeeperColorArgb,
        players = players,
        kitStyle = kitStyle,
    )
}

/**
 * Evenly distribute [count] players from bottom to top while keeping narrow lines
 * closer to the centre. This avoids wide players hugging the touchlines when only
 * a few are present (e.g. two centre-backs).
 */
private fun evenlySpacedY(count: Int): List<Float> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(0.5f)

    // Expand the spread gradually as the line gets busier to keep pairs/triangles compact.
    val baseSpan = 0.34f // span used for a pair
    val maxAdditionalSpan = 0.46f // extra spread added for larger lines
    val normalizedCount = (count - 1).coerceAtLeast(1)
    val span = (baseSpan + maxAdditionalSpan * (normalizedCount / 6f)).coerceAtMost(0.8f)

    val start = (0.5f - span / 2f).coerceAtLeast(0.08f)
    val end = (0.5f + span / 2f).coerceAtMost(0.92f)
    val step = (end - start) / (count - 1)

    return List(count) { index -> start + step * index }
}
