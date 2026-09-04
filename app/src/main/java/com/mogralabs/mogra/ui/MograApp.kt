package com.mogralabs.mogra.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val HOME = "home"
    const val IDENTIFIER = "identifier"
    const val BY_NOTES = "by_notes"
    const val BY_NAME = "by_name"
}

@Composable
fun MograApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onOpenTool = { route -> nav.navigate(route) })
        }
        composable(Routes.IDENTIFIER) {
            IdentifierFlow(onLeave = { nav.popBackStack() })
        }
        composable(Routes.BY_NOTES) {
            NotBuiltYetScreen(title = "Raagfinder by Notes", onBack = { nav.popBackStack() })
        }
        composable(Routes.BY_NAME) {
            NotBuiltYetScreen(title = "Raagfinder by Name", onBack = { nav.popBackStack() })
        }
    }
}
