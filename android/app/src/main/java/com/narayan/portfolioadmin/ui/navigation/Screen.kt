package com.narayan.portfolioadmin.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Login : Screen("login", "Login")
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.SpaceDashboard)
    data object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    data object Skills : Screen("skills", "Skills", Icons.Default.Star)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    data object Messages : Screen("messages", "Messages", Icons.Default.Mail)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Projects, Skills, Messages, Profile)
    }
}
