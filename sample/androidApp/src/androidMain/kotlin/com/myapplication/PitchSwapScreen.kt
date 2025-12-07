package com.myapplication

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import footballpitch.FootballPitch
import footballpitch.model.AttackDirection
import footballpitch.model.Formation
import footballpitch.model.Formations
import footballpitch.model.MatchTeams
import footballpitch.model.PitchBackground
import footballpitch.model.PitchOrientation
import footballpitch.model.PitchStyle
import footballpitch.model.PlayerNameStyle
import footballpitch.model.ShirtStyle
import footballpitch.model.StripeOrientation
import footballpitch.model.TeamKitStyle
import footballpitch.model.TeamLineup
import footballpitch.model.TeamSetup
import footballpitch.model.toLineups

/**
 * High-level screen for interacting with the pitch:
 * - Swap home/away attacking sides
 * - Choose formations per team
 * - Pick kit colors per team and pitch background
 * - Toggle teams on/off
 */
@Composable
fun PitchSwapScreen() {
    val colorSaver =
        Saver<Color, Long>(
            save = { it.value.toLong() },
            restore = { Color(it.toULong()) },
        )

    val baseTeams =
        remember {
            MatchTeams(
                home =
                    TeamSetup(
                        name = "Home",
                        // blue
                        colorArgb = 0xFF1E88E5,
                        goalkeeperColorArgb = 0xFFFFC107,
                        formation = Formations.fourFourTwo(),
                        kitStyle =
                            TeamKitStyle(
                                fieldPlayerShirtStyle = ShirtStyle.STRIPED,
                                goalkeeperShirtStyle = ShirtStyle.GOALKEEPER,
                            ),
                    ),
                away =
                    TeamSetup(
                        name = "Away",
                        // red
                        colorArgb = 0xFFEF5350,
                        goalkeeperColorArgb = 0xFF8D6E63,
                        formation = Formations.threeFourThree(),
                        kitStyle =
                            TeamKitStyle(
                                fieldPlayerShirtStyle = ShirtStyle.COLLAR,
                                goalkeeperShirtStyle = ShirtStyle.GOALKEEPER,
                            ),
                    ),
            )
        }

    val formationOptions: List<Pair<String, Formation>> =
        remember {
            listOf(
                "4-4-2" to Formations.fourFourTwo(),
                "4-3-3" to Formations.fourThreeThree(),
                "4-5-1" to Formations.fourFiveOne(),
                "3-4-3" to Formations.threeFourThree(),
                "3-5-2" to Formations.threeFiveTwo(),
                "5-3-2" to Formations.fiveThreeTwo(),
                "5-4-1" to Formations.fiveFourOne(),
                "4-2-4" to Formations.fourTwoFour(),
                "3-6-1" to Formations.threeSixOne(),
                "2-3-5" to Formations.twoThreeFive(),
            )
        }

    // Interaction state
    var homeStartsOnLeft by rememberSaveable { mutableStateOf(true) }
    var homeFormationKey by rememberSaveable { mutableStateOf("4-4-2") }
    var awayFormationKey by rememberSaveable { mutableStateOf("3-4-3") }
    var homeMenuExpanded by remember { mutableStateOf(false) }
    var awayMenuExpanded by remember { mutableStateOf(false) }
    var homeColor by remember { mutableStateOf(Color(0xFF1E88E5)) }
    var awayColor by remember { mutableStateOf(Color(0xFFEF5350)) }
    var homeKeeperColor by remember { mutableStateOf(Color(0xFFFFC107)) }
    var awayKeeperColor by remember { mutableStateOf(Color(0xFF8D6E63)) }
    var homeShirtStyleKey by rememberSaveable { mutableStateOf(ShirtStyle.STRIPED.name) }
    var awayShirtStyleKey by rememberSaveable { mutableStateOf(ShirtStyle.COLLAR.name) }
    val palette =
        remember {
            listOf(
                "Blue" to Color(0xFF1E88E5),
                "Red" to Color(0xFFEF5350),
                "Green" to Color(0xFF43A047),
                "Orange" to Color(0xFFFB8C00),
                "Purple" to Color(0xFF8E24AA),
                "Teal" to Color(0xFF0097A7),
            )
        }
    val grassPalette =
        remember {
            listOf(
                Color(0xFF166C31),
                Color(0xFF0E5A26),
                Color(0xFF2E7D32),
                Color(0xFF1B5E20),
                Color(0xFF4CAF50),
                Color(0xFF6FBF73),
                Color(0xFF3B7A57),
            )
        }
    var homeColorMenu by remember { mutableStateOf(false) }
    var awayColorMenu by remember { mutableStateOf(false) }
    var backgroundType by rememberSaveable { mutableStateOf("Stripes") }
    var stripeOrientation by rememberSaveable { mutableStateOf(StripeOrientation.Vertical) }
    var stripeCount by rememberSaveable { mutableStateOf(8) }
    var solidColor by rememberSaveable(stateSaver = colorSaver) { mutableStateOf(grassPalette[0]) }
    var gradientStartColor by rememberSaveable(stateSaver = colorSaver) { mutableStateOf(grassPalette[0]) }
    var gradientEndColor by rememberSaveable(stateSaver = colorSaver) { mutableStateOf(grassPalette[1]) }
    var checkerLightColor by rememberSaveable(stateSaver = colorSaver) { mutableStateOf(grassPalette[0]) }
    var checkerDarkColor by rememberSaveable(stateSaver = colorSaver) { mutableStateOf(grassPalette[1]) }
    var showHomeTeam by rememberSaveable { mutableStateOf(true) }
    var showAwayTeam by rememberSaveable { mutableStateOf(true) }
    var homeKeeperColorMenu by remember { mutableStateOf(false) }
    var awayKeeperColorMenu by remember { mutableStateOf(false) }
    val groundOptions = listOf("Stripes", "Checkerboard", "Solid", "Gradient")

    // Derived formations
    val homeFormation = formationOptions.first { it.first == homeFormationKey }.second
    val awayFormation = formationOptions.first { it.first == awayFormationKey }.second
    val homeShirtStyle = ShirtStyle.valueOf(homeShirtStyleKey)
    val awayShirtStyle = ShirtStyle.valueOf(awayShirtStyleKey)

    // Build match view model based on side + formation choices
    val matchForLayout =
        remember(
            homeStartsOnLeft,
            homeFormationKey,
            awayFormationKey,
            homeColor,
            awayColor,
            homeKeeperColor,
            awayKeeperColor,
            homeShirtStyleKey,
            awayShirtStyleKey,
        ) {
            if (homeStartsOnLeft) {
                baseTeams.copy(
                    home =
                        baseTeams.home.copy(
                            attackDirection = AttackDirection.LeftToRight,
                            formation = homeFormation,
                            colorArgb = homeColor.toArgb().toLong(),
                            goalkeeperColorArgb = homeKeeperColor.toArgb().toLong(),
                            kitStyle =
                                TeamKitStyle(
                                    fieldPlayerShirtStyle = homeShirtStyle,
                                    goalkeeperShirtStyle = ShirtStyle.GOALKEEPER,
                                ),
                        ),
                    away =
                        baseTeams.away.copy(
                            attackDirection = AttackDirection.RightToLeft,
                            formation = awayFormation,
                            colorArgb = awayColor.toArgb().toLong(),
                            goalkeeperColorArgb = awayKeeperColor.toArgb().toLong(),
                            kitStyle =
                                TeamKitStyle(
                                    fieldPlayerShirtStyle = awayShirtStyle,
                                    goalkeeperShirtStyle = ShirtStyle.GOALKEEPER,
                                ),
                        ),
                )
            } else {
                baseTeams.copy(
                    home =
                        baseTeams.away.copy(
                            attackDirection = AttackDirection.LeftToRight,
                            formation = awayFormation,
                            colorArgb = awayColor.toArgb().toLong(),
                            goalkeeperColorArgb = awayKeeperColor.toArgb().toLong(),
                            kitStyle =
                                TeamKitStyle(
                                    fieldPlayerShirtStyle = awayShirtStyle,
                                    goalkeeperShirtStyle = ShirtStyle.GOALKEEPER,
                                ),
                        ),
                    away =
                        baseTeams.home.copy(
                            attackDirection = AttackDirection.RightToLeft,
                            formation = homeFormation,
                            colorArgb = homeColor.toArgb().toLong(),
                            goalkeeperColorArgb = homeKeeperColor.toArgb().toLong(),
                            kitStyle =
                                TeamKitStyle(
                                    fieldPlayerShirtStyle = homeShirtStyle,
                                    goalkeeperShirtStyle = ShirtStyle.GOALKEEPER,
                                ),
                        ),
                )
            }
        }

    val realMadridNames =
        listOf(
            "T. Courtois",
            "D. Carvajal",
            "A. Rudiger",
            "D. Alaba",
            "F. Mendy",
            "F. Valverde",
            "T. Kroos",
            "J. Bellingham",
            "Vini Jr.",
            "K. Mbappe",
            "Rodrygo",
        )
    val barcelonaNames =
        listOf(
            "M. ter Stegen",
            "J. Kounde",
            "R. Araujo",
            "A. Christensen",
            "A. Balde",
            "I. Gundogan",
            "F. de Jong",
            "Pedri",
            "Raphinha",
            "L. Yamal",
            "R. Lewandowski",
        )

    val (rawHomeLineup, rawAwayLineup) = matchForLayout.toLineups()
    val homeLineup = rawHomeLineup.withTemplateNames(realMadridNames)
    val awayLineup = rawAwayLineup.withTemplateNames(barcelonaNames)
    val orientation = PitchOrientation.Horizontal

    val scrollState = rememberScrollState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "Lineup Playground", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Swap sides, tweak formations, and preview the pitch.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PitchPreviewCard(
            homeStartsOnLeft = homeStartsOnLeft,
            homeFormationKey = homeFormationKey,
            awayFormationKey = awayFormationKey,
            homeColor = homeColor,
            awayColor = awayColor,
            backgroundType = backgroundType,
            stripeOrientation = stripeOrientation,
            stripeCount = stripeCount,
            solidColor = solidColor,
            gradientStartColor = gradientStartColor,
            gradientEndColor = gradientEndColor,
            checkerLightColor = checkerLightColor,
            checkerDarkColor = checkerDarkColor,
            orientation = orientation,
            showHomeTeam = showHomeTeam,
            showAwayTeam = showAwayTeam,
            homeLineup = homeLineup,
            awayLineup = awayLineup,
        )

        FormationCard(
            homeFormationKey = homeFormationKey,
            awayFormationKey = awayFormationKey,
            homeMenuExpanded = homeMenuExpanded,
            awayMenuExpanded = awayMenuExpanded,
            formationOptions = formationOptions,
            homeColor = homeColor,
            awayColor = awayColor,
            onHomeFormationChange = { homeFormationKey = it },
            onAwayFormationChange = { awayFormationKey = it },
            onHomeMenuExpandedChange = { homeMenuExpanded = it },
            onAwayMenuExpandedChange = { awayMenuExpanded = it },
        )

        ControlCard(
            homeStartsOnLeft = homeStartsOnLeft,
            onHomeStartsOnLeftChange = { homeStartsOnLeft = it },
            homeColor = homeColor,
            awayColor = awayColor,
            palette = palette,
            showHomeTeam = showHomeTeam,
            showAwayTeam = showAwayTeam,
            homeKeeperColor = homeKeeperColor,
            awayKeeperColor = awayKeeperColor,
            homeKeeperColorMenu = homeKeeperColorMenu,
            awayKeeperColorMenu = awayKeeperColorMenu,
            onHomeKeeperColorMenuChange = { homeKeeperColorMenu = it },
            onAwayKeeperColorMenuChange = { awayKeeperColorMenu = it },
            onHomeKeeperColorChange = { homeKeeperColor = it },
            onAwayKeeperColorChange = { awayKeeperColor = it },
            onHomeColorChange = {
                homeColor = it
                if (awayColor == it) {
                    awayColor = palette.first { option -> option.second != it }.second
                }
            },
            onAwayColorChange = {
                awayColor = it
                if (homeColor == it) {
                    homeColor = palette.first { option -> option.second != it }.second
                }
            },
            homeColorMenu = homeColorMenu,
            awayColorMenu = awayColorMenu,
            onHomeColorMenuChange = { homeColorMenu = it },
            onAwayColorMenuChange = { awayColorMenu = it },
            onShowHomeTeamChange = { showHomeTeam = it },
            onShowAwayTeamChange = { showAwayTeam = it },
            homeShirtStyle = homeShirtStyle,
            awayShirtStyle = awayShirtStyle,
            onHomeShirtStyleChange = { homeShirtStyleKey = it.name },
            onAwayShirtStyleChange = { awayShirtStyleKey = it.name },
        )

        GroundStyleCard(
            backgroundType = backgroundType,
            groundOptions = groundOptions,
            stripeOrientation = stripeOrientation,
            stripeCount = stripeCount,
            solidColor = solidColor,
            gradientStartColor = gradientStartColor,
            gradientEndColor = gradientEndColor,
            checkerLightColor = checkerLightColor,
            checkerDarkColor = checkerDarkColor,
            grassPalette = grassPalette,
            onBackgroundTypeChange = { backgroundType = it },
            onStripeOrientationChange = { stripeOrientation = it },
            onStripeCountChange = { stripeCount = it },
            onSolidColorChange = { solidColor = it },
            onGradientStartChange = { gradientStartColor = it },
            onGradientEndChange = { gradientEndColor = it },
            onCheckerLightChange = { checkerLightColor = it },
            onCheckerDarkChange = { checkerDarkColor = it },
        )
    }
}

@Composable
private fun PitchPreviewCard(
    homeStartsOnLeft: Boolean,
    homeFormationKey: String,
    awayFormationKey: String,
    homeColor: Color,
    awayColor: Color,
    backgroundType: String,
    stripeOrientation: StripeOrientation,
    stripeCount: Int,
    solidColor: Color,
    gradientStartColor: Color,
    gradientEndColor: Color,
    checkerLightColor: Color,
    checkerDarkColor: Color,
    orientation: PitchOrientation,
    showHomeTeam: Boolean,
    showAwayTeam: Boolean,
    homeLineup: TeamLineup,
    awayLineup: TeamLineup,
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(105f / 68f),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState =
                    listOf(
                        homeStartsOnLeft,
                        homeFormationKey,
                        awayFormationKey,
                        homeColor,
                        awayColor,
                        backgroundType,
                        stripeOrientation,
                        stripeCount,
                        solidColor,
                        gradientStartColor,
                        gradientEndColor,
                        checkerLightColor,
                        checkerDarkColor,
                    ),
                label = "sideAndFormationSwap",
            ) { _ ->
                val nameStyle = PlayerNameStyle()
                val pitchStyle =
                    when (backgroundType) {
                        "Stripes" ->
                            PitchStyle(
                                background =
                                    PitchBackground.Stripes(
                                        stripeCount = stripeCount,
                                        orientation = stripeOrientation,
                                    ),
                                playerNameStyle = nameStyle,
                            )
                        "Solid" ->
                            PitchStyle(
                                background = PitchBackground.Solid(color = solidColor),
                                playerNameStyle = nameStyle,
                            )
                        "Checkerboard" ->
                            PitchStyle(
                                background =
                                    PitchBackground.Checkerboard(
                                        colors = listOf(checkerLightColor, checkerDarkColor),
                                    ),
                                playerNameStyle = nameStyle,
                            )
                        "Gradient" ->
                            PitchStyle(
                                background =
                                    PitchBackground.Gradient(
                                        colors = listOf(gradientStartColor, gradientEndColor),
                                    ),
                                playerNameStyle = nameStyle,
                            )
                        else ->
                            PitchStyle(
                                background = PitchBackground.Stripes(),
                                playerNameStyle = nameStyle,
                            )
                    }
                FootballPitch(
                    orientation = orientation,
                    style = pitchStyle,
                    homeTeam = if (showHomeTeam) homeLineup else null,
                    awayTeam = if (showAwayTeam) awayLineup else null,
                    contentDescription = "Football pitch showing home and away lineups",
                )
            }
        }
    }
}

private fun TeamLineup.withTemplateNames(names: List<String>): TeamLineup =
    copy(
        players =
            players.mapIndexed { index, player ->
                if (!player.name.isNullOrBlank()) {
                    player
                } else {
                    player.copy(name = names.getOrNull(index))
                }
            },
    )

@Composable
private fun FormationCard(
    homeFormationKey: String,
    awayFormationKey: String,
    homeMenuExpanded: Boolean,
    awayMenuExpanded: Boolean,
    formationOptions: List<Pair<String, Formation>>,
    homeColor: Color,
    awayColor: Color,
    onHomeFormationChange: (String) -> Unit,
    onAwayFormationChange: (String) -> Unit,
    onHomeMenuExpandedChange: (Boolean) -> Unit,
    onAwayMenuExpandedChange: (Boolean) -> Unit,
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Formations",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormationPicker(
                    label = "Home",
                    selected = homeFormationKey,
                    expanded = homeMenuExpanded,
                    options = formationOptions,
                    onExpandedChange = onHomeMenuExpandedChange,
                    onSelect = {
                        onHomeMenuExpandedChange(false)
                        onHomeFormationChange(it)
                    },
                    accent = homeColor,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                )

                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .heightIn(min = 40.dp)
                            .background(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            ),
                )

                FormationPicker(
                    label = "Away",
                    selected = awayFormationKey,
                    expanded = awayMenuExpanded,
                    options = formationOptions,
                    onExpandedChange = onAwayMenuExpandedChange,
                    onSelect = {
                        onAwayMenuExpandedChange(false)
                        onAwayFormationChange(it)
                    },
                    accent = awayColor,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                )
            }
        }
    }
}
