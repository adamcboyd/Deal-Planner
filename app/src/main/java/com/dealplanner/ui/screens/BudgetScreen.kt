package com.dealplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dealplanner.data.model.BudgetState
import com.dealplanner.ui.viewmodel.AppViewModel
import com.dealplanner.util.toFlexibleDoubleOrNull

@Composable
fun BudgetScreen(viewModel: AppViewModel) {
    val budgetState by viewModel.budgetState.collectAsState()
    val budgetAnalysis by viewModel.budgetAnalysis.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()
    val startingBudget = budgetState?.startingBudget ?: 0.0
    val displayCurrentBalance = budgetAnalysis?.currentBalance
        ?: budgetState?.let { it.startingBudget - it.spentToDate }
        ?: 0.0
    val displayDailyEnvelope = budgetAnalysis?.dailyBudget ?: budgetState?.dailyEnvelope ?: 0.0
    val displaySpentToDate = (startingBudget - displayCurrentBalance).coerceAtLeast(0.0)
    val spendingProgress = if (startingBudget > 0.0) {
        (displaySpentToDate / startingBudget).coerceIn(0.0, 1.0).toFloat()
    } else {
        0.0f
    }
    var startingBudgetText by remember { mutableStateOf("") }
    var spentToDateText by remember { mutableStateOf("") }
    var breakfastAnchorCostText by remember { mutableStateOf("") }
    var budgetEdited by remember { mutableStateOf(false) }
    val parsedStartingBudget = startingBudgetText.toFlexibleDoubleOrNull()
    val parsedSpentToDate = spentToDateText.toFlexibleDoubleOrNull()
    val parsedBreakfastAnchorCost = breakfastAnchorCostText.toFlexibleDoubleOrNull()
    val isStartingBudgetValid = parsedStartingBudget != null && parsedStartingBudget >= 0.0
    val isSpentToDateValid = parsedSpentToDate != null && parsedSpentToDate >= 0.0
    val isBreakfastAnchorCostValid = parsedBreakfastAnchorCost != null && parsedBreakfastAnchorCost >= 0.0
    val isBudgetFormValid = isStartingBudgetValid && isSpentToDateValid && isBreakfastAnchorCostValid

    LaunchedEffect(budgetState) {
        budgetState?.let { budget ->
            startingBudgetText = budget.startingBudget.toString()
            spentToDateText = budget.spentToDate.toString()
            breakfastAnchorCostText = budget.breakfastAnchorCost.toString()
            budgetEdited = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Current balance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Current Balance",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${"$%.2f".format(displayCurrentBalance)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "of ${"$%.2f".format(startingBudget)} monthly food budget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Budget settings
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Budget Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = startingBudgetText,
                        onValueChange = {
                            startingBudgetText = it
                            budgetEdited = true
                        },
                        label = { Text("Monthly food budget") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isStartingBudgetValid,
                        singleLine = true
                    )
                    if (!isStartingBudgetValid) {
                        BudgetNumberError()
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = spentToDateText,
                        onValueChange = {
                            spentToDateText = it
                            budgetEdited = true
                        },
                        label = { Text("Spent to date baseline") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isSpentToDateValid,
                        singleLine = true
                    )
                    if (!isSpentToDateValid) {
                        BudgetNumberError()
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = breakfastAnchorCostText,
                        onValueChange = {
                            breakfastAnchorCostText = it
                            budgetEdited = true
                        },
                        label = { Text("Breakfast anchor cost") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = !isBreakfastAnchorCostValid,
                        singleLine = true
                    )
                    if (!isBreakfastAnchorCostValid) {
                        BudgetNumberError()
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val startingValue = parsedStartingBudget ?: return@Button
                            val spentValue = parsedSpentToDate ?: return@Button
                            val breakfastValue = parsedBreakfastAnchorCost ?: return@Button
                            val currentBudget = budgetState ?: BudgetState(startingBudget = startingValue)

                            viewModel.updateBudget(
                                currentBudget.copy(
                                    startingBudget = startingValue,
                                    spentToDate = spentValue,
                                    breakfastAnchorCost = breakfastValue
                                )
                            )
                            budgetEdited = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isBudgetFormValid
                    ) {
                        Text("Save Budget")
                    }

                    if (budgetStatus != null && !budgetEdited) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            budgetStatus.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Daily envelope
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Daily Envelope",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${"$%.2f".format(displayDailyEnvelope)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Breakfast Anchor",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${"$%.2f".format(budgetState?.breakfastAnchorCost ?: 0.0)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Budget analysis
        if (budgetAnalysis != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (budgetAnalysis!!.onTrack) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Budget Analysis",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Days Remaining", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${budgetAnalysis!!.daysRemaining}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Projected Spend", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${"$%.2f".format(budgetAnalysis!!.projectedSpend)}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            if (budgetAnalysis!!.surplus >= 0) {
                                "Surplus: ${"$%.2f".format(budgetAnalysis!!.surplus)}"
                            } else {
                                "Deficit: ${"$%.2f".format(-budgetAnalysis!!.surplus)}"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (budgetAnalysis!!.onTrack) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            // Suggestions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Suggestions",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        budgetAnalysis!!.suggestions.forEach { suggestion ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    suggestion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Spending breakdown
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Monthly Overview",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    budgetState?.let { budget ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Starting Balance:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${"$%.2f".format(budget.startingBudget)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Spent to Date:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${"$%.2f".format(displaySpentToDate)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = spendingProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetNumberError() {
    Text(
        "Use a non-negative number like 292 or 292,50.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}
