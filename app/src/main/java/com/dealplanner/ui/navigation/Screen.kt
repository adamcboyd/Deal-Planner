package com.dealplanner.ui.navigation

sealed class Screen(val route: String) {
    object Pantry : Screen("pantry")
    object Deals : Screen("deals")
    object Receipts : Screen("receipts")
    object ShoppingList : Screen("shopping_list")
    object Menu : Screen("menu")
    object Budget : Screen("budget")
    object Params : Screen("params")
}
