package com.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import footballpitch.model.Formation

/**
 * Dropdown-based formation selector with a colored accent chip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormationPicker(
    label: String,
    selected: String,
    expanded: Boolean,
    options: List<Pair<String, Formation>>,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { /* read-only */ },
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            leadingIcon = {
                Box(
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(accent),
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleMedium,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach { (name, _) ->
                DropdownMenuItem(
                    onClick = {
                        onSelect(name)
                        onExpandedChange(false)
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accent),
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    },
                    trailingIcon = {
                        if (name == selected) {
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
