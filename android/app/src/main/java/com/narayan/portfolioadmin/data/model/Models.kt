package com.narayan.portfolioadmin.data.model

data class Profile(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val bio: String = "",
    val about_text: String = "",
    val email: String = "",
    val phone: String = "",
    val avatar_url: String = "",
    val resume_url: String = "",
    val github_url: String = "",
    val linkedin_url: String = "",
    val instagram_url: String = "",
    val location: String = "",
    val available_for_hire: Boolean = true,
    val updated_at: String = ""
)

data class Project(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val long_description: String = "",
    val image_url: String = "",
    val tech_stack: List<String> = emptyList(),
    val live_url: String = "",
    val github_url: String = "",
    val featured: Boolean = false,
    val display_order: Int = 0,
    val created_at: String = ""
)

data class Skill(
    val id: String = "",
    val name: String = "",
    val category: String = "Frontend",
    val proficiency: Int = 50,
    val icon_name: String = "",
    val display_order: Int = 0,
    val created_at: String = ""
)

data class ContactMessage(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val is_read: Boolean = false,
    val created_at: String = ""
)

data class DashboardStats(
    val projectsCount: Int = 0,
    val skillsCount: Int = 0,
    val messagesCount: Int = 0,
    val unreadCount: Int = 0
)

data class ErrorReport(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val error_type: String = "USER_REPORT", // CRASH, NON_FATAL, USER_REPORT
    val stack_trace: String = "",
    val device_info: String = "",
    val app_version: String = "1.0",
    val screen_name: String = "",
    val created_at: String = "",
    val status: String = "new"
)
