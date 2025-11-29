# Compose Football Pitch (Compose Multiplatform)

A Compose Multiplatform library for rendering customizable football (soccer) pitches and team lineups across Android, Desktop, and iOS. Includes a sample playground app with interactive controls for backgrounds, formations, and team visibility.

---

## What you get

- Accurate FIFA-style pitch lines with configurable thickness and colors.
- Multiple pitch orientations: horizontal/vertical + reversed.
- Grass backgrounds: stripes, solid, gradient, checkerboard (all color-configurable).
- Team lineups generated from formations; mirrored attack directions to avoid overlap.
- Simple kit rendering with shirt styles (classic, striped, collar, keeper, circle).
- Compose-first API that works across Android, Desktop, and iOS.

---

## Modules

- `ComposeFootballPitch` — the KMP library.
- `sample/shared` — shared Compose code for the sample apps.
- `sample/androidApp` — Android playground (fully wired with the latest UI).
- `sample/desktopApp` / `sample/iosApp` — platform shells from the template (may need wiring depending on your setup).

---

## Getting started

Add the dependency once published (or depend on the included module directly):

```kotlin
dependencies {
    implementation("io.github.ibrahimalzaidi:compose-football-pitch:0.1.0")
    // or, while working locally:
    // implementation(project(":ComposeFootballPitch"))
}
```

Render a pitch:

```kotlin
import footballpitch.FootballPitch
import footballpitch.model.*
import androidx.compose.ui.graphics.Color

@Composable
fun MatchScreen() {
    val matchTeams =
        MatchTeams(
            home =
                TeamSetup(
                    name = "Home",
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

    val (homeLineup, awayLineup) = matchTeams.toLineups()

    FootballPitch(
        orientation = PitchOrientation.Horizontal,
        style =
            PitchStyle(
                background =
                    PitchBackground.Gradient(
                        colors = listOf(Color(0xFF166C31), Color(0xFF0E5A26)),
                    ),
            ),
        homeTeam = homeLineup,
        awayTeam = awayLineup,
    )
}
```

---

## Core models

- `PitchDimensions` — real-world meters; defaults match FIFA guidance.
- `PitchOrientation` — `Horizontal`, `HorizontalReversed`, `Vertical`, `VerticalReversed`.
- `PitchBackground` — `Solid`, `Stripes` (vertical/horizontal), `Checkerboard`, `Gradient` (directional).
- `PitchStyle` — wraps background, line color, and line thickness factor.
- `TeamSetup` / `MatchTeams` — high-level team definitions that convert to `TeamLineup` via `toLineups()`.
- `Formation` / `Formations` — 4-4-2, 4-3-3, 3-5-2, etc.
- `ShirtStyle` / `TeamKitStyle` — pick shirt rendering styles per role.
- `AttackDirection` — left-to-right or right-to-left; mirrored automatically for away teams unless overridden.

Positions use normalized coordinates (`x`, `y` in `[0f, 1f]`, origin bottom-left in default orientation).

---

## Pitch backgrounds

```kotlin
PitchStyle(
    background =
        PitchBackground.Stripes(
            colors = listOf(Color(0xFF166C31), Color(0xFF0E5A26)),
            stripeCount = 10,
            orientation = StripeOrientation.Vertical,
        ),
    lineColor = Color.White,
    lineThicknessFactor = 1.0f,
)
```

Options:

- `Solid(color)`
- `Stripes(colors, stripeCount, orientation)`
- `Checkerboard(colors, rows, columns)`
- `Gradient(colors, direction)`

---

## Sample playground (Android)

Path: `sample/androidApp`

Key screen/components:

- `PitchSwapScreen` — holds state for formations, kit colors, pitch backgrounds, and team visibility toggles.
- `PitchPreviewCard` — renders the pitch with Crossfade when settings change.
- `FormationCard` + `FormationPicker` — pick formations for home/away.
- `ControlCard` — swap attacking direction, show/hide teams, pick kit colors.
- `GroundStyleCard` — choose background type (stripes/solid/gradient/checkerboard), adjust stripe orientation/count, and pick grass colors.

Run (Android):

```bash
./gradlew :sample:androidApp:assembleDebug
```

Run (Desktop sample shell, if configured):

```bash
./gradlew :sample:desktopApp:run
```

---

## Development

- Core library sources: `ComposeFootballPitch/src/commonMain/kotlin/footballpitch`
- Rendering internals: `ComposeFootballPitch/src/commonMain/kotlin/footballpitch/rendering`
- Tests: `ComposeFootballPitch/src/commonTest` (snapshot/geometry checks)

Useful tasks:

```bash
./gradlew :ComposeFootballPitch:check
./gradlew :sample:androidApp:assembleDebug
```

---

## Publishing notes

Coordinates in Gradle are set to `io.github.ibrahimalzaidi:compose-football-pitch:0.1.0` (update to match your Sonatype group before release).

---

## License

See `LICENSE.txt`. This project started from the compose-multiplatform-library-template.
