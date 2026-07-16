package com.dealplanner.ui.navigation

import androidx.annotation.StringRes
import com.dealplanner.R

sealed class Screen(val route: String, @StringRes val labelRes: Int) {
    object Pantry : Screen("pantry", R.string.pantry)
    object Deals : Screen("deals", R.string.deals)
    object Receipts : Screen("receipts", R.string.receipts)
    object ShoppingList : Screen("shopping_list", R.string.shopping)
    object Menu : Screen("menu", R.string.menu)
    object Budget : Screen("budget", R.string.budget)
    object Params : Screen("params", R.string.params)
}
