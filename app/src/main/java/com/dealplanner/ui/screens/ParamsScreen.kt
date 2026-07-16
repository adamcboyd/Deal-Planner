package com.dealplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dealplanner.BuildConfig
import com.dealplanner.data.model.Params
import com.dealplanner.ui.viewmodel.AppViewModel
import com.dealplanner.util.toFlexibleDoubleOrNull

@Composable
fun ParamsScreen(viewModel: AppViewModel) {
    val params by viewModel.params.collectAsState()
    val aiVisionConnectionStatus by viewModel.aiVisionConnectionStatus.collectAsState()
    val settingsStatus by viewModel.settingsStatus.collectAsState()

    var gerdFriendly by remember { mutableStateOf(params?.gerdFriendly ?: false) }
    var avoidPeppers by remember { mutableStateOf(params?.avoidPeppers ?: false) }
    var breakfastAnchor by remember { mutableStateOf(params?.breakfastAnchor ?: true) }
    var proteinPerMeal by remember { mutableStateOf(params?.proteinPerMealLb?.toString() ?: "0.5") }
    var settingsEdited by remember { mutableStateOf(false) }
    val parsedProteinPerMeal = proteinPerMeal.toFlexibleDoubleOrNull()
    val isProteinPerMealValid = parsedProteinPerMeal != null && parsedProteinPerMeal >= 0.0

    LaunchedEffect(params) {
        params?.let {
            gerdFriendly = it.gerdFriendly
            avoidPeppers = it.avoidPeppers
            breakfastAnchor = it.breakfastAnchor
            proteinPerMeal = it.proteinPerMealLb.toString()
            settingsEdited = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Dietary preferences
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Dietary Preferences",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GERD-Friendly", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = gerdFriendly,
                            onCheckedChange = {
                                gerdFriendly = it
                                settingsEdited = true
                            }
                        )
                    }

                    Text(
                        "Avoids acidic vegetables (tomatoes, peppers, onions)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Avoid Peppers", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = avoidPeppers,
                            onCheckedChange = {
                                avoidPeppers = it
                                settingsEdited = true
                            }
                        )
                    }

                    Text(
                        "Excludes all types of peppers from meal plans",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Meal planning
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Meal Planning",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Breakfast Anchor", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = breakfastAnchor,
                            onCheckedChange = {
                                breakfastAnchor = it
                                settingsEdited = true
                            }
                        )
                    }

                    Text(
                        "Use pantry staples (oats, cereal) for breakfast",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Protein per Meal (lb)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = proteinPerMeal,
                        onValueChange = {
                            proteinPerMeal = it
                            settingsEdited = true
                        },
                        label = { Text("Pounds") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isProteinPerMealValid
                    )
                    Text(
                        if (isProteinPerMealValid) {
                            "Default: 0.5 lb per meal"
                        } else {
                            "Use a non-negative number like 0.5 or 0,5."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isProteinPerMealValid) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        // Save button
        item {
            Button(
                onClick = {
                    val proteinValue = parsedProteinPerMeal ?: return@Button
                    val updatedParams = Params(
                        gerdFriendly = gerdFriendly,
                        avoidPeppers = avoidPeppers,
                        breakfastAnchor = breakfastAnchor,
                        proteinPerMealLb = proteinValue
                    )
                    viewModel.updateParams(updatedParams)
                    settingsEdited = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isProteinPerMealValid
            ) {
                Text("Save Settings")
            }
            if (settingsStatus != null && !settingsEdited) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    settingsStatus.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // AI status
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI Pantry Photo Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (viewModel.aiVisionConfigured) {
                            "Gemini Vision is configured."
                        } else {
                            "Gemini Vision is not configured. Pantry photos will use on-device OCR fallback."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (viewModel.aiVisionConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Model: ${viewModel.aiVisionModel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Add a real key in local.properties to enable AI item recognition. Keys are never shown in the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.testAiVisionConnection() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test AI Connection")
                    }
                    if (aiVisionConnectionStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            aiVisionConnectionStatus.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (aiVisionConnectionStatus?.contains("OK") == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        // About section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "About Deal Planner",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Package: ${BuildConfig.APPLICATION_ID}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Build: ${if (BuildConfig.DEBUG) "Debug" else "Release"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Source: ${BuildConfig.GIT_BRANCH} @ ${BuildConfig.GIT_SHA}${if (BuildConfig.GIT_DIRTY) " (dirty)" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Core principle: Deals drive the meals.\nOffline-first grocery optimization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
