package com.narayan.portfolioadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.narayan.portfolioadmin.ui.theme.*

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()
    private val projectsRepository = ProjectsRepository()
    private val skillsRepository = SkillsRepository()
    private val messagesRepository = MessagesRepository()
    private val storageRepository = StorageRepository()
    private val errorReportRepository = ErrorReportRepository()
    private val analyticsRepository = AnalyticsRepository()

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
                    containerColor = BackgroundCanvas,
                    bottomBar = {
                        if (showBottomBar) {
                            FloatingBottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
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
                                analyticsRepository = analyticsRepository,
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

                        composable(Screen.Projects.route) {
                            ProjectsScreen(
                                projectsRepository = projectsRepository,
                                storageRepository = storageRepository
                            )
                        }

                        composable(Screen.Skills.route) {
                            SkillsScreen(
                                skillsRepository = skillsRepository
                            )
                        }

                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                profileRepository = profileRepository,
                                storageRepository = storageRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Messages.route) {
                            MessagesScreen(
                                messagesRepository = messagesRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(68.dp),
        shape = RoundedCornerShape(34.dp),
        color = Color(0xFF0F1E36),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFF223450))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFF223450) else Color.Transparent)
                        .clickable { onNavigate(screen.route) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        screen.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) Color.White else Color(0xFF8A99AD),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = screen.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF8A99AD)
                        )
                    }
                }
            }
        }
    }
}
