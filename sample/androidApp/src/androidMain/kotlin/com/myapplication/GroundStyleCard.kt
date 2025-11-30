package com.myapplication

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import footballpitch.model.StripeOrientation
import kotlin.math.roundToInt

/**
 * Ground styling controls for the pitch background (stripes, solid, gradient, checkerboard).
 */
@Composable
fun GroundStyleCard(
    backgroundType: String,
    groundOptions: List<String>,
    stripeOrientation: StripeOrientation,
    stripeCount: Int,
    solidColor: Color,
    gradientStartColor: Color,
    gradientEndColor: Color,
    checkerLightColor: Color,
    checkerDarkColor: Color,
    grassPalette: List<Color>,
    onBackgroundTypeChange: (String) -> Unit,
    onStripeOrientationChange: (StripeOrientation) -> Unit,
    onStripeCountChange: (Int) -> Unit,
    onSolidColorChange: (Color) -> Unit,
    onGradientStartChange: (Color) -> Unit,
    onGradientEndChange: (Color) -> Unit,
    onCheckerLightChange: (Color) -> Unit,
    onCheckerDarkChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                // Smoothly animate height changes when Stripes controls appear/disappear
                .animateContentSize(),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ground styling",
                style = MaterialTheme.typography.titleMedium,
            )

            val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

            // 1) Background type (Solid, Stripes, Gradient, ...)
            BackgroundDropdown(
                label = "Background",
                selected = backgroundType,
                options = groundOptions,
                onSelect = onBackgroundTypeChange,
            )

            when (backgroundType.lowercase()) {
                "stripes" -> {
                    Divider(
                        modifier = Modifier.padding(top = 4.dp),
                        color = dividerColor,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val orientationLabel =
                            when (stripeOrientation) {
                                StripeOrientation.Vertical -> "Vertical"
                                StripeOrientation.Horizontal -> "Horizontal"
                            }

                        BackgroundDropdown(
                            label = "Stripe orientation",
                            selected = orientationLabel,
                            options = listOf("Vertical", "Horizontal"),
                            onSelect = { selected ->
                                onStripeOrientationChange(
                                    if (selected == "Vertical") {
                                        StripeOrientation.Vertical
                                    } else {
                                        StripeOrientation.Horizontal
                                    },
                                )
                            },
                        )

                        StripeCountSlider(
                            stripeCount = stripeCount,
                            onStripeCountChange = onStripeCountChange,
                        )
                    }
                }

                "solid" -> {
                    Divider(
                        modifier = Modifier.padding(top = 4.dp),
                        color = dividerColor,
                    )

                    ColorSwatchRow(
                        label = "Solid color",
                        colors = grassPalette,
                        selected = solidColor,
                        onSelect = onSolidColorChange,
                    )
                }

                "gradient" -> {
                    Divider(
                        modifier = Modifier.padding(top = 4.dp),
                        color = dividerColor,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ColorSwatchRow(
                            label = "Gradient start",
                            colors = grassPalette,
                            selected = gradientStartColor,
                            onSelect = onGradientStartChange,
                        )
                        ColorSwatchRow(
                            label = "Gradient end",
                            colors = grassPalette,
                            selected = gradientEndColor,
                            onSelect = onGradientEndChange,
                        )
                    }
                }

                "checkerboard" -> {
                    Divider(
                        modifier = Modifier.padding(top = 4.dp),
                        color = dividerColor,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ColorSwatchRow(
                            label = "Light squares",
                            colors = grassPalette,
                            selected = checkerLightColor,
                            onSelect = onCheckerLightChange,
                        )
                        ColorSwatchRow(
                            label = "Dark squares",
                            colors = grassPalette,
                            selected = checkerDarkColor,
                            onSelect = onCheckerDarkChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        )
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = selected,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Choose $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    // Small checkmark for the current selection (optional but nice)
                    trailingIcon = {
                        if (option == selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    label: String,
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            colors.forEach { color ->
                val isSelected = color == selected
                Box(
                    modifier =
                        Modifier
                            .size(if (isSelected) 36.dp else 32.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                shape = CircleShape,
                            )
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onSelect(color) },
                )
            }
        }
    }
}

@Composable
private fun StripeCountSlider(
    stripeCount: Int,
    onStripeCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Stripe count",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stripeCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Slider(
            value = stripeCount.toFloat(),
            onValueChange = { value ->
                onStripeCountChange(
                    value.roundToInt().coerceIn(2, 16),
                )
            },
            valueRange = 2f..16f,
            // max-min-1 => 16 - 2 - 1 = 13 steps, same as before
            steps = 13,
        )
    }
}
