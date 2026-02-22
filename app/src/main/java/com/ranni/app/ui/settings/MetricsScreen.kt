package com.ranni.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val timelineOptions = listOf(3, 6, 12, 24)

@Composable
fun MetricsScreen(viewModel: MetricsViewModel) {
    val config by viewModel.config.collectAsState()

    var monthsText by remember(config) { mutableStateOf(config.months.toString()) }
    var topKText by remember(config) { mutableStateOf(config.topK.toString()) }

    // Validate months: must be an integer in 1..12
    val monthsValue = monthsText.toIntOrNull()
    val monthsError = monthsValue != null && monthsValue !in 1..12

    // Validate topK: must be an integer in 10..20
    val topKValue = topKText.toIntOrNull()
    val topKError = topKValue != null && topKValue !in 10..20

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "These values control the progress graph on the History tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = monthsText,
            onValueChange = { value ->
                monthsText = value
                val n = value.toIntOrNull()
                // Only save if within valid range
                if (n != null && n in 1..12) viewModel.updateMonths(n)
            },
            label = { Text("Months (n)") },
            supportingText = {
                if (monthsError) {
                    Text("Must be between 1 and 12", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Rolling window size in months")
                }
            },
            isError = monthsError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = topKText,
            onValueChange = { value ->
                topKText = value
                val k = value.toIntOrNull()
                // Only save if within valid range
                if (k != null && k in 10..20) viewModel.updateTopK(k)
            },
            label = { Text("Top climbs (k)") },
            supportingText = {
                if (topKError) {
                    Text("Must be between 10 and 20", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Number of top scoring climbs to sum")
                }
            },
            isError = topKError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Toggle to show/hide REPEAT climbs from dots and stats; detail/list views still show them.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show repeat climbs", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = !config.filterRepeats,
                onCheckedChange = { viewModel.updateFilterRepeats(!it) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("History timeline", style = MaterialTheme.typography.bodyLarge)
            Text(
                "How far back the progress graph displays",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                timelineOptions.forEachIndexed { index, months ->
                    SegmentedButton(
                        selected = config.timelineMonths == months,
                        onClick = { viewModel.updateTimeline(months) },
                        shape = SegmentedButtonDefaults.itemShape(index, timelineOptions.size)
                    ) {
                        Text("${months}m")
                    }
                }
            }
            // Warning when activity dots are hidden on longer timelines
            if (config.timelineMonths > 6) {
                Text(
                    "Activity dots are hidden on 12m and 24m timelines",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Visualisations", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Default visibility of activity dots on the progress graph",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show climb dots", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = config.showClimbDots,
                    onCheckedChange = { viewModel.updateShowClimbDots(it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show exercise dots", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = config.showExerciseDots,
                    onCheckedChange = { viewModel.updateShowExerciseDots(it) }
                )
            }

        }
    }
}
