package com.narayan.portfolioadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.narayan.portfolioadmin.data.repository.*
import com.narayan.portfolioadmin.ui.navigation.Screen
import com.narayan.portfolioadmin.ui.screens.dashboard.DashboardScreen
import com.narayan.portfolioadmin.ui.screens.login.LoginScreen
import com.narayan.portfolioadmin.ui.screens.messages.MessagesScreen
import com.narayan.portfolioadmin.ui.screens.profile.ProfileScreen
import com.narayan.portfolioadmin.ui.screens.projects.ProjectsScreen
import com.narayan.portfolioadmin.ui.screens.skills.SkillsScreen
import com.narayan.portfolioadmin.ui.theme.BackgroundDark
import com.narayan.portfolioadmin.ui.theme.CardDark
import com.narayan.portfolioadmin.ui.theme.PortfolioAdminTheme
import com.narayan.portfolioadmin.ui.theme.PrimaryIndigo
import com.narayan.portfolioadmin.ui.theme.SurfaceDark
import com.narayan.portfolioadmin.ui.theme.TextMuted
import com.narayan.portfolioadmin.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()
    private val projectsRepository = ProjectsRepository()
    private val skillsRepository = SkillsRepository()
    private val messagesRepository = MessagesRepository()
    private val storageRepository = StorageRepository()
    private val errorReportRepository = ErrorReportRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PortfolioAdminTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute != null && currentRoute != Screen.Login.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundDark,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = SurfaceDark,
                                contentColor = TextPrimary
                            ) {
                                Screen.bottomNavItems.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            screen.icon?.let { icon ->
                                                Icon(icon, contentDescription = screen.title)
                                            }
                                        },
                                        label = { Text(screen.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PrimaryIndigo,
                                            selectedTextColor = PrimaryIndigo,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted,
                                            indicatorColor = CardDark
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (authRepository.isLoggedIn) Screen.Dashboard.route else Screen.Login.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                authRepository = authRepository,
                                onLoginSuccess = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                authRepository = authRepository,
                                profileRepository = profileRepository,
                                projectsRepository = projectsRepository,
                                skillsRepository = skillsRepository,
                                messagesRepository = messagesRepository,
                                errorReportRepository = errorReportRepository,
                                onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                                onNavigateToSkills = { navController.navigate(Screen.Skills.route) },
                                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                                onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                                onLogout = {
                                    authRepository.logout()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        val handleBack: () -> Unit = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }

                        composable(Screen.Projects.route) {
                            ProjectsScreen(
                                projectsRepository = projectsRepository,
                                storageRepository = storageRepository,
                                onBack = handleBack
                            )
                        }

                        composable(Screen.Skills.route) {
                            SkillsScreen(
                                skillsRepository = skillsRepository,
                                onBack = handleBack
                            )
                        }

                        composable(Screen.Messages.route) {
                            MessagesScreen(
                                messagesRepository = messagesRepository,
                                onBack = handleBack
                            )
                        }

                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                profileRepository = profileRepository,
                                storageRepository = storageRepository,
                                onBack = handleBack
                            )
                        }
                    }
                }
            }
        }
    }
}
