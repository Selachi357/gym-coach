package com.gymcoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.gymcoach.ui.guide.GuideScreen
import com.gymcoach.ui.hero.CityScreen
import com.gymcoach.ui.hero.DefenderScreen
import com.gymcoach.ui.hero.HeroScreen
import com.gymcoach.ui.hero.PixelIcon
import com.gymcoach.ui.hero.PixelIconImage
import com.gymcoach.ui.hero.Retro
import com.gymcoach.ui.home.HomeScreen
import com.gymcoach.ui.settings.SettingsScreen
import com.gymcoach.ui.program.ProgramScreen
import com.gymcoach.ui.workout.ExercisePickerScreen
import com.gymcoach.ui.workout.WorkoutScreen

val LocalRepository = staticCompositionLocalOf<GymRepository> { error("No repository provided") }

private data class Tab(val route: String, val label: String, val icon: PixelIcon)

private val tabs = listOf(
    Tab("home", "Home", PixelIcon.HOME),
    Tab("exercises", "Lifts", PixelIcon.DUMBBELL),
    Tab("hero", "Hero", PixelIcon.SHIELD),
    Tab("goals", "Goals", PixelIcon.CUP),
    Tab("history", "Log", PixelIcon.SCROLL),
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
    com.gymcoach.ui.timer.TimerAlarm()

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Retro.gold))
                    NavigationBar(containerColor = Retro.night) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.navigateTab(tab.route) },
                                icon = { PixelIconImage(tab.icon, if (selected) Retro.gold else Retro.dim, 24.dp) },
                                label = {
                                    Text(
                                        tab.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Retro.gold else Retro.dim,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Retro.panel),
                            )
                        }
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
                    openProgram = { nav.navigate("program") },
                    openHero = { nav.navigateTab("hero") },
                    openSettings = { nav.navigate("settings") },
                    openTimer = { nav.navigate("timer") },
                )
            }
            composable("program") {
                ProgramScreen(back = { nav.popBackStack() }, openGuide = { nav.navigate("guide") })
            }
            composable("timer") { com.gymcoach.ui.timer.TimerScreen(back = { nav.popBackStack() }) }
            composable("guide") { GuideScreen(back = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(back = { nav.popBackStack() }) }
            composable("hero") { HeroScreen(openDefender = { nav.navigate("defend") }, openCity = { nav.navigate("city") }) }
            composable("city") { CityScreen(back = { nav.popBackStack() }) }
            composable("defend") { DefenderScreen(back = { nav.popBackStack() }) }
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
                    openTimer = { nav.navigate("timer") },
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
