package com.narayan.portfolioadmin.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Login : Screen("login", "Login")
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Projects : Screen("projects", "Work", Icons.Default.BusinessCenter)
    data object Skills : Screen("skills", "Stats", Icons.Default.BarChart)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    data object Messages : Screen("messages", "Inquiries", Icons.Default.Mail)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Projects, Skills, Profile)
    }
}
