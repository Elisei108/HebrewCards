package com.hebrewcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hebrewcards.domain.model.StudyMode
import com.hebrewcards.ui.screen.dashboard.DashboardScreen
import com.hebrewcards.ui.screen.deck.AddDeckScreen
import com.hebrewcards.ui.screen.deck.DeckScreen
import com.hebrewcards.ui.screen.settings.SettingsScreen
import com.hebrewcards.ui.screen.stats.StatsScreen
import com.hebrewcards.ui.screen.study.StudyScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_DECK  = "add_deck"
    const val DECK      = "deck/{deckId}"
    const val STUDY     = "study/{deckId}/{mode}"
    const val SESSION   = "session/{deckId}"
    const val SETTINGS  = "settings"
    const val STATS     = "stats"

    fun deck(deckId: Long)                = "deck/$deckId"
    fun study(deckId: Long, mode: String) = "study/$deckId/$mode"
    fun session(deckId: Long)             = "session/$deckId"
}

@Composable
fun HebrewCardsNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }

        composable(Routes.ADD_DECK) {
            AddDeckScreen(navController = navController)
        }

        composable(
            route     = Routes.DECK,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            DeckScreen(deckId = deckId, navController = navController)
        }

        composable(
            route     = Routes.STUDY,
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType },
                navArgument("mode")   { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
            val mode   = runCatching {
                StudyMode.valueOf(backStackEntry.arguments?.getString("mode") ?: "")
            }.getOrDefault(StudyMode.FLASHCARD)
            StudyScreen(deckId = deckId, mode = mode, navController = navController)
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }

        composable(Routes.STATS) {
            StatsScreen(navController = navController)
        }
    }
}
