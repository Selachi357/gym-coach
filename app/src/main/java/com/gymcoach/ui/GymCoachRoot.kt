package com.gymcoach.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymcoach.data.GymRepository
import com.gymcoach.ui.exercises.ExerciseDetailScreen
import com.gymcoach.ui.exercises.ExercisesScreen
import com.gymcoach.ui.goals.GoalsScreen
import com.gymcoach.ui.history.HistoryScreen
import com.gymcoach.ui.history.SessionDetailScreen
import com.gymcoach.ui.home.HomeScreen
import com.gymcoach.ui.workout.ExercisePickerScreen
import com.gymcoach.ui.workout.WorkoutScreen

val LocalRepository = staticCompositionLocalOf<GymRepository> { error("No repository provided") }

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("exercises", "Exercises", Icons.Filled.FitnessCenter),
    Tab("goals", "Goals", Icons.Filled.EmojiEvents),
    Tab("history", "History", Icons.Filled.History),
)

object Routes {
    fun workout(sessionId: Long) = "workout/$sessionId"
    fun picker(sessionId: Long) = "pick/$sessionId"
    fun exercise(id: Long) = "exercise/$id"
    fun session(id: Long) = "session/$id"
}

@Composable
fun GymCoachRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.navigateTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    openWorkout = { nav.navigate(Routes.workout(it)) },
                    openGoals = { nav.navigateTab("goals") },
                )
            }
            composable("exercises") {
                ExercisesScreen(openExercise = { nav.navigate(Routes.exercise(it)) })
            }
            composable("goals") { GoalsScreen() }
            composable("history") {
                HistoryScreen(openSession = { nav.navigate(Routes.session(it)) })
            }
            composable("workout/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                WorkoutScreen(
                    sessionId = id,
                    addExercise = { nav.navigate(Routes.picker(id)) },
                    openExercise = { nav.navigate(Routes.exercise(it)) },
                    close = { nav.popBackStack("home", inclusive = false) },
                )
            }
            composable("pick/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ExercisePickerScreen(sessionId = id, done = { nav.popBackStack() })
            }
            composable("exercise/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ExerciseDetailScreen(
                    exerciseId = entry.arguments?.getLong("id") ?: 0L,
                    openExercise = { nav.navigate(Routes.exercise(it)) },
                    back = { nav.popBackStack() },
                )
            }
            composable("session/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                SessionDetailScreen(sessionId = entry.arguments?.getLong("id") ?: 0L, back = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
