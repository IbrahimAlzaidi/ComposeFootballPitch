package com.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import footballpitch.model.ShirtStyle

/**
 * Card that controls attacking direction, visibility of teams, and kit colors.
 */
@Composable
fun ControlCard(
    homeStartsOnLeft: Boolean,
    onHomeStartsOnLeftChange: (Boolean) -> Unit,
    homeColor: Color,
    awayColor: Color,
    palette: List<Pair<String, Color>>,
    showHomeTeam: Boolean,
    showAwayTeam: Boolean,
    homeKeeperColor: Color,
    awayKeeperColor: Color,
    homeKeeperColorMenu: Boolean,
    awayKeeperColorMenu: Boolean,
    onHomeColorChange: (Color) -> Unit,
    onAwayColorChange: (Color) -> Unit,
    homeColorMenu: Boolean,
    awayColorMenu: Boolean,
    onHomeColorMenuChange: (Boolean) -> Unit,
    onAwayColorMenuChange: (Boolean) -> Unit,
    onShowHomeTeamChange: (Boolean) -> Unit,
    onShowAwayTeamChange: (Boolean) -> Unit,
    onHomeKeeperColorMenuChange: (Boolean) -> Unit,
    onAwayKeeperColorMenuChange: (Boolean) -> Unit,
    onHomeKeeperColorChange: (Color) -> Unit,
    onAwayKeeperColorChange: (Color) -> Unit,
    homeShirtStyle: ShirtStyle,
    awayShirtStyle: ShirtStyle,
    onHomeShirtStyleChange: (ShirtStyle) -> Unit,
    onAwayShirtStyleChange: (ShirtStyle) -> Unit,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Attacking direction",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (homeStartsOnLeft) "Home attacking right" else "Home attacking left",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    IconButton(onClick = { onHomeStartsOnLeftChange(true) }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Home attacks left",
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = { onHomeStartsOnLeftChange(false) }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "Home attacks right",
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Show teams",
                    style = MaterialTheme.typography.titleSmall,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Switch(
                            checked = showHomeTeam,
                            onCheckedChange = onShowHomeTeamChange,
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Switch(
                            checked = showAwayTeam,
                            onCheckedChange = onShowAwayTeamChange,
                        )
                        Text(
                            text = "Away",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            ShirtAndColorRow(
                homeColor = homeColor,
                awayColor = awayColor,
                palette = palette,
                homeColorMenu = homeColorMenu,
                awayColorMenu = awayColorMenu,
                onHomeColorMenuChange = onHomeColorMenuChange,
                onAwayColorMenuChange = onAwayColorMenuChange,
                onHomeColorChange = onHomeColorChange,
                onAwayColorChange = onAwayColorChange,
                homeShirtStyle = homeShirtStyle,
                awayShirtStyle = awayShirtStyle,
                onHomeShirtStyleChange = onHomeShirtStyleChange,
                onAwayShirtStyleChange = onAwayShirtStyleChange,
            )

            KeeperColorRow(
                palette = palette,
                homeKeeperColor = homeKeeperColor,
                awayKeeperColor = awayKeeperColor,
                homeKeeperColorMenu = homeKeeperColorMenu,
                awayKeeperColorMenu = awayKeeperColorMenu,
                onHomeKeeperColorMenuChange = onHomeKeeperColorMenuChange,
                onAwayKeeperColorMenuChange = onAwayKeeperColorMenuChange,
                onHomeKeeperColorChange = onHomeKeeperColorChange,
                onAwayKeeperColorChange = onAwayKeeperColorChange,
            )
        }
    }
}

@Composable
private fun ShirtAndColorRow(
    homeColor: Color,
    awayColor: Color,
    palette: List<Pair<String, Color>>,
    homeColorMenu: Boolean,
    awayColorMenu: Boolean,
    onHomeColorMenuChange: (Boolean) -> Unit,
    onAwayColorMenuChange: (Boolean) -> Unit,
    onHomeColorChange: (Color) -> Unit,
    onAwayColorChange: (Color) -> Unit,
    homeShirtStyle: ShirtStyle,
    awayShirtStyle: ShirtStyle,
    onHomeShirtStyleChange: (ShirtStyle) -> Unit,
    onAwayShirtStyleChange: (ShirtStyle) -> Unit,
) {
    val styleOptions =
        listOf(
            ShirtStyle.CLASSIC to "Classic",
            ShirtStyle.STRIPED to "Striped",
            ShirtStyle.COLLAR to "Collar",
            ShirtStyle.CIRCLE to "Circle marker",
        )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Kits",
            style = MaterialTheme.typography.titleSmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamBadge(
                modifier = Modifier.weight(1f),
                name = "Home color",
                color = homeColor,
                palette = palette.filter { it.second != awayColor },
                expanded = homeColorMenu,
                onExpandedChange = onHomeColorMenuChange,
                onColorChange = onHomeColorChange,
            )

            TeamBadge(
                modifier = Modifier.weight(1f),
                name = "Away color",
                color = awayColor,
                palette = palette.filter { it.second != homeColor },
                expanded = awayColorMenu,
                onExpandedChange = onAwayColorMenuChange,
                onColorChange = onAwayColorChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShirtStylePicker(
                label = "Player shirt (Home)",
                selected = homeShirtStyle,
                options = styleOptions,
                onSelect = onHomeShirtStyleChange,
                modifier = Modifier.weight(1f),
            )
            ShirtStylePicker(
                label = "Player shirt (Away)",
                selected = awayShirtStyle,
                options = styleOptions,
                onSelect = onAwayShirtStyleChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KeeperColorRow(
    palette: List<Pair<String, Color>>,
    homeKeeperColor: Color,
    awayKeeperColor: Color,
    homeKeeperColorMenu: Boolean,
    awayKeeperColorMenu: Boolean,
    onHomeKeeperColorMenuChange: (Boolean) -> Unit,
    onAwayKeeperColorMenuChange: (Boolean) -> Unit,
    onHomeKeeperColorChange: (Color) -> Unit,
    onAwayKeeperColorChange: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Text(
            text = "Goalkeeper look",
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamBadge(
                modifier = Modifier.weight(1f),
                name = "Home GK color",
                color = homeKeeperColor,
                palette = palette,
                expanded = homeKeeperColorMenu,
                onExpandedChange = onHomeKeeperColorMenuChange,
                onColorChange = onHomeKeeperColorChange,
            )
            TeamBadge(
                modifier = Modifier.weight(1f),
                name = "Away GK color",
                color = awayKeeperColor,
                palette = palette,
                expanded = awayKeeperColorMenu,
                onExpandedChange = onAwayKeeperColorMenuChange,
                onColorChange = onAwayKeeperColorChange,
            )
        }
    }
}

@Composable
private fun TeamBadge(
    modifier: Modifier = Modifier,
    name: String,
    color: Color,
    palette: List<Pair<String, Color>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onColorChange: (Color) -> Unit,
) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    )
                    .clickable { onExpandedChange(true) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color),
            )

            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )

            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Change $name color",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            palette.forEach { (label, swatch) ->
                DropdownMenuItem(
                    leadingIcon = {
                        Box(
                            modifier =
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(swatch),
                        )
                    },
                    text = { Text(text = label) },
                    onClick = {
                        onExpandedChange(false)
                        onColorChange(swatch)
                    },
                )
            }
        }
    }
}

@Composable
fun ShirtStylePicker(
    label: String,
    selected: ShirtStyle,
    options: List<Pair<ShirtStyle, String>>,
    onSelect: (ShirtStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable { expanded.value = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = label, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = options.first { it.first == selected }.second,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Change $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            options.forEach { (style, labelText) ->
                DropdownMenuItem(
                    text = { Text(labelText) },
                    onClick = {
                        expanded.value = false
                        onSelect(style)
                    },
                )
            }
        }
    }
}
