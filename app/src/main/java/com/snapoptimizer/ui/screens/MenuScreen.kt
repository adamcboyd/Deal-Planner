package com.snapoptimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snapoptimizer.data.model.MealPlan
import com.snapoptimizer.data.model.MealSlot
import com.snapoptimizer.ui.viewmodel.AppViewModel
import java.time.format.DateTimeFormatter

@Composable
fun MenuScreen(viewModel: AppViewModel) {
    val mealPlans by viewModel.mealPlans.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with generate button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("7-Day Meal Plan", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Deals drive the meals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(onClick = { viewModel.generateMealPlan() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate")
                }
            }
        }

        // Meal plan list
        if (mealPlans.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No meal plan yet.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Click Generate to create a meal plan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(mealPlans) { plan ->
                    MealPlanCard(plan = plan)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun MealPlanCard(plan: MealPlan) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plan.date.format(formatter),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            plan.slots.forEach { slot ->
                MealSlotItem(slot = slot)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (plan.notes != null) {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${plan.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MealSlotItem(slot: MealSlot) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = slot.mealType.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (slot.protein != null) {
                Text(
                    text = "Protein: ${slot.protein} (${slot.proteinQty} ${slot.proteinUnit})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (slot.veg != null) {
                Text(
                    text = "Veg: ${slot.veg} (${slot.vegQty} ${slot.vegUnit})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (slot.starch != null) {
                Text(
                    text = "Starch: ${slot.starch} (${slot.starchQty} ${slot.starchUnit})",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (slot.freezerDirective != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "Freezer: ${slot.freezerDirective}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Est. Cost: ${"$%.2f".format(slot.estimatedCost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
