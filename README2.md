# Compose Football Pitch (Compose Multiplatform)

Compose Football Pitch is a **Compose Multiplatform** library for rendering customizable football (soccer) pitches and team lineups across Android, desktop, and iOS.

It focuses on:

- Accurate pitch lines based on real dimensions.
- Flexible pitch orientation and backgrounds.
- Configurable line thickness and colours.
- Simple but extensible player/kit rendering.
- Formation-aware lineups (4‑4‑2, 4‑3‑3, 3‑5‑2, etc.).

---

## Modules

The library is published as a Kotlin Multiplatform module:

- Module: `ComposeFootballPitch`
- Suggested Maven coordinates:
  - Group: `io.github.ibrahimalzaidi`
  - Artifact: `compose-football-pitch`
  - Version: `0.1.0`

> Note: Before publishing to Maven Central, ensure you control the `io.github.ibrahimalzaidi` group on Sonatype and adjust coordinates if needed.

---

## Installation

Once you publish the artifact (to Maven Central or your internal repository), add the dependency to your shared Gradle module:

```kotlin
dependencies {
    implementation("io.github.ibrahimalzaidi:compose-football-pitch:0.1.0")
}
```

For now, if you are using this project as a template, you can depend on the `ComposeFootballPitch` module directly from your app module:

```kotlin
dependencies {
    implementation(project(":ComposeFootballPitch"))
}
```

---

## Quick Start

The main entry point is the `FootballPitch` composable:

```kotlin
FootballPitch(
    teamA = homeTeam,
    teamB = awayTeam
)
```

Where `homeTeam` and `awayTeam` are instances of `TeamLineup`.

To make this easier, the library provides higher‑level models to describe teams, kit styles, and formations.

### Basic example with `TeamSetup` and `MatchTeams`

```kotlin
import footballpitch.FootballPitch
import footballpitch.model.*
import androidx.compose.ui.graphics.Color

@Composable
fun MatchScreen() {
    val matchTeams = MatchTeams(
        home = TeamSetup(
            name = "Home",
            colorArgb = 0xFF1E88E5,          // blue
            goalkeeperColorArgb = 0xFFFFC107,
            formation = Formations.fourFourTwo(),
            kitStyle = TeamKitStyle(
                fieldPlayerShirtStyle = ShirtStyle.STRIPED,
                goalkeeperShirtStyle = ShirtStyle.GOALKEEPER
            ),
            attackDirection = AttackDirection.LeftToRight
        ),
        away = TeamSetup(
            name = "Away",
            colorArgb = 0xFFEF5350,          // red
            goalkeeperColorArgb = 0xFF8D6E63,
            formation = Formations.threeFourThree(),
            kitStyle = TeamKitStyle(
                fieldPlayerShirtStyle = ShirtStyle.COLLAR,
                goalkeeperShirtStyle = ShirtStyle.GOALKEEPER
            ),
            attackDirection = AttackDirection.RightToLeft
        )
    )

    val (homeLineup, awayLineup) = matchTeams.toLineups()

    FootballPitch(
        style = PitchStyle(
            background = PitchBackground.Gradient(
                colors = listOf(Color(0xFF166C31), Color(0xFF0E5A26)),
            )
        ),
        teamA = homeLineup,
        teamB = awayLineup
    )
}
```

This gives you:

- Non‑overlapping home/away teams (mirrored with `AttackDirection`).
- A configurable formation for each team.
- Different shirt styles per team via `TeamKitStyle`.

---

## Core Concepts

### Pitch configuration

**Dimensions**

`PitchDimensions` describes the pitch in real‑world meters:

```kotlin
val customDimensions = PitchDimensions(
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
```

`FootballPitch` uses FIFA‑style defaults, but you can override them via the `dimensions` parameter.

**Orientation**

`PitchOrientation` controls where the goals are placed:

```kotlin
FootballPitch(
    orientation = PitchOrientation.Vertical, // goals at top/bottom
    // ...
)
```

Supported values:

- `Horizontal`, `HorizontalReversed`
- `Vertical`, `VerticalReversed`

### Pitch style & background

`PitchStyle` configures how the pitch looks:

```kotlin
val style = PitchStyle(
    background = PitchBackground.Stripes(
        colors = listOf(Color(0xFF166C31), Color(0xFF0E5A26)),
        stripeCount = 10,
        orientation = StripeOrientation.Vertical
    ),
    lineColor = Color.White,
    lineThicknessFactor = 1.2f
)
```

Available backgrounds:

- `PitchBackground.Solid` – single grass colour.
- `PitchBackground.Stripes` – alternating stripes (vertical/horizontal).
- `PitchBackground.Checkerboard` – grid pattern.
- `PitchBackground.Gradient` – linear gradient (vertical/horizontal/diagonal).

`lineThicknessFactor` scales the default line width (based on real dimensions) up or down.

---

## Teams, kits, and formations

### Positions and players

`PitchPosition` uses normalized coordinates:

- `x` and `y` in `[0f, 1f]`
- `(0f, 0f)` is bottom‑left, `(1f, 1f)` is top‑right in the default orientation.

```kotlin
val player = Player(
    position = PitchPosition(x = 0.7f, y = 0.4f),
    number = 9,
    isGoalkeeper = false
)
```

`TeamLineup` is the structure passed to `FootballPitch`:

```kotlin
val team = TeamLineup(
    teamName = "Home",
    colorArgb = 0xFF1E88E5,
    goalkeeperColorArgb = 0xFFFFC107,
    players = listOf(player),
    kitStyle = TeamKitStyle(
        fieldPlayerShirtStyle = ShirtStyle.CLASSIC,
        goalkeeperShirtStyle = ShirtStyle.GOALKEEPER
    )
)
```

### Shirt styles and kits

`ShirtStyle` controls how player icons are drawn:

- `CLASSIC` – V‑neck shirt with sleeves.
- `GOALKEEPER` – keeper variant (ready for future customization).
- `CIRCLE` – simple circular marker (good for tactical views).
- `STRIPED` – classic shirt with simple vertical stripes.
- `COLLAR` – classic shirt with a highlighted collar.

`TeamKitStyle` selects the default style per role:

```kotlin
val kit = TeamKitStyle(
    fieldPlayerShirtStyle = ShirtStyle.STRIPED,
    goalkeeperShirtStyle = ShirtStyle.GOALKEEPER
)
```

The renderer automatically applies these to each player based on `isGoalkeeper`.

### Formations

Formations are modeled as a simple three‑line system:

```kotlin
data class Formation(
    val defenders: Int,
    val midfielders: Int,
    val forwards: Int
)
```

Predefined formations (via `Formations`):

- `Formations.fourFourTwo()` – 4‑4‑2
- `Formations.fourThreeThree()` – 4‑3‑3
- `Formations.fourFiveOne()` – 4‑5‑1
- `Formations.fiveThreeTwo()` – 5‑3‑2
- `Formations.fiveFourOne()` – 5‑4‑1
- `Formations.threeFourThree()` – 3‑4‑3
- `Formations.threeFiveTwo()` – 3‑5‑2
- `Formations.fourTwoFour()` – 4‑2‑4
- `Formations.threeSixOne()` – 3‑6‑1
- `Formations.twoThreeFive()` – 2‑3‑5

Player positions are generated as:

- Goalkeeper near own goal.
- Defenders closer to own box.
- Midfield line around the centre.
- Forwards near the opponent’s box.

### Attack direction (home vs away)

`AttackDirection` ensures home and away teams do not overlap:

```kotlin
enum class AttackDirection {
    LeftToRight,
    RightToLeft
}
```

It is used by formation helpers and by `TeamSetup`:

```kotlin
val home = TeamSetup(
    // ...
    formation = Formations.fourFourTwo(),
    attackDirection = AttackDirection.LeftToRight
)

val away = TeamSetup(
    // ...
    formation = Formations.fourThreeThree(),
    attackDirection = AttackDirection.RightToLeft
)
```

Internally, the X coordinates are mirrored when `RightToLeft` is selected, so both teams always have a goalkeeper at their own end and matching formations do not overlap.

---

## Architecture overview

- **Public API (`footballpitch` and `footballpitch.model`)**
  - `FootballPitch` composable.
  - Data models: `PitchDimensions`, `PitchOrientation`, `PitchStyle`, `PitchBackground`, `PitchPosition`, `Formation`, `TeamLineup`, `Player`, `TeamSetup`, `MatchTeams`, `TeamKitStyle`, `PlayerAppearance`, `ShirtStyle`, `AttackDirection`.
- **Rendering internals (`footballpitch.rendering`)**
  - Pitch drawing functions (lines, background, circles, boxes).
  - Player rendering (shirts, stripes, collars, markers, numbers).
  - These are kept internal to keep the public API stable and easy to reason about.

This separation makes it easier to extend the visuals without breaking consumers.

---

## Publishing notes

The Gradle configuration for `ComposeFootballPitch` already includes a basic Maven publishing setup using `com.vanniktech.maven.publish`. Important fields:

- Group ID: `io.github.ibrahimalzaidi`
- Artifact ID: `compose-football-pitch`
- Version: `0.1.0`
- POM metadata:
  - URL / SCM / developer info pointing to `https://github.com/IbrahimAlzaidi/ComposeFootballPitch`

Before publishing:

1. Make sure you have a Sonatype account and have requested access to the `io.github.ibrahimalzaidi` group.
2. Update coordinates if you decide on a different group or artifact ID.
3. Verify the license and POM fields match your repository setup.

---

## Roadmap ideas

- Text rendering for player numbers and names.
- Bench / substitutes visualisation.
- More advanced pitch themes (TV broadcast, training mode, dark mode).
- Animation utilities (player movement, ball path).

---

## License

This project is based on the [compose-multiplatform-library-template](https://github.com/KevinnZou/compose-multiplatform-library-template).

See `LICENSE.txt` in this repository for license details.

