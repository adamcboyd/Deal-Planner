package com.dealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dealplanner.ui.navigation.Screen
import com.dealplanner.ui.screens.*
import com.dealplanner.ui.theme.DealPlannerTheme
import com.dealplanner.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DealPlannerTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deal Planner") },
                actions = {
                    // Demo button
                    TextButton(onClick = { viewModel.loadDemoData() }) {
                        Text("Load Demo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    NavigationItem(
                        screen = Screen.Pantry,
                        icon = Icons.Default.Kitchen,
                        label = "Pantry"
                    ),
                    NavigationItem(
                        screen = Screen.Deals,
                        icon = Icons.Default.LocalOffer,
                        label = "Deals"
                    ),
                    NavigationItem(
                        screen = Screen.Receipts,
                        icon = Icons.Default.ReceiptLong,
                        label = "Receipts"
                    ),
                    NavigationItem(
                        screen = Screen.ShoppingList,
                        icon = Icons.Default.ShoppingCart,
                        label = "Shopping"
                    ),
                    NavigationItem(
                        screen = Screen.Menu,
                        icon = Icons.Default.Restaurant,
                        label = "Menu"
                    ),
                    NavigationItem(
                        screen = Screen.Budget,
                        icon = Icons.Default.AccountBalance,
                        label = "Budget"
                    ),
                    NavigationItem(
                        screen = Screen.Params,
                        icon = Icons.Default.Settings,
                        label = "Settings"
                    )
                )

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavigationHost(navController = navController, viewModel = viewModel)
        }
    }
}

@Composable
fun NavigationHost(navController: NavHostController, viewModel: AppViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Pantry.route
    ) {
        composable(Screen.Pantry.route) {
            PantryScreen(viewModel = viewModel)
        }
        composable(Screen.Deals.route) {
            DealsScreen(viewModel = viewModel)
        }
        composable(Screen.Receipts.route) {
            ReceiptsScreen(viewModel = viewModel)
        }
        composable(Screen.ShoppingList.route) {
            ShoppingListScreen(viewModel = viewModel)
        }
        composable(Screen.Menu.route) {
            MenuScreen(viewModel = viewModel)
        }
        composable(Screen.Budget.route) {
            BudgetScreen(viewModel = viewModel)
        }
        composable(Screen.Params.route) {
            ParamsScreen(viewModel = viewModel)
        }
    }
}

data class NavigationItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)
