package com.narayan.portfolioadmin.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Profile(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("bio") @set:PropertyName("bio") var bio: String = "",
    @get:PropertyName("about_text") @set:PropertyName("about_text") var about_text: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("phone") @set:PropertyName("phone") var phone: String = "",
    @get:PropertyName("avatar_url") @set:PropertyName("avatar_url") var avatar_url: String = "",
    @get:PropertyName("resume_url") @set:PropertyName("resume_url") var resume_url: String = "",
    @get:PropertyName("github_url") @set:PropertyName("github_url") var github_url: String = "",
    @get:PropertyName("linkedin_url") @set:PropertyName("linkedin_url") var linkedin_url: String = "",
    @get:PropertyName("instagram_url") @set:PropertyName("instagram_url") var instagram_url: String = "",
    @get:PropertyName("location") @set:PropertyName("location") var location: String = "",
    @get:PropertyName("available_for_hire") @set:PropertyName("available_for_hire") var available_for_hire: Boolean = true,
    @get:PropertyName("updated_at") @set:PropertyName("updated_at") var updated_at: String = ""
)

@IgnoreExtraProperties
data class Project(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("long_description") @set:PropertyName("long_description") var long_description: String = "",
    @get:PropertyName("image_url") @set:PropertyName("image_url") var image_url: String = "",
    @get:PropertyName("tech_stack") @set:PropertyName("tech_stack") var tech_stack: List<String> = emptyList(),
    @get:PropertyName("live_url") @set:PropertyName("live_url") var live_url: String = "",
    @get:PropertyName("github_url") @set:PropertyName("github_url") var github_url: String = "",
    @get:PropertyName("featured") @set:PropertyName("featured") var featured: Boolean = false,
    @get:PropertyName("display_order") @set:PropertyName("display_order") var display_order: Int = 0,
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: String = ""
)

@IgnoreExtraProperties
data class Skill(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("category") @set:PropertyName("category") var category: String = "Frontend",
    @get:PropertyName("proficiency") @set:PropertyName("proficiency") var proficiency: Int = 50,
    @get:PropertyName("icon_name") @set:PropertyName("icon_name") var icon_name: String = "",
    @get:PropertyName("display_order") @set:PropertyName("display_order") var display_order: Int = 0,
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: String = ""
)

@IgnoreExtraProperties
data class ContactMessage(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("subject") @set:PropertyName("subject") var subject: String = "",
    @get:PropertyName("message") @set:PropertyName("message") var message: String = "",
    @get:PropertyName("is_read") @set:PropertyName("is_read") var is_read: Boolean = false,
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: String = ""
)

data class DashboardStats(
    val projectsCount: Int = 0,
    val skillsCount: Int = 0,
    val messagesCount: Int = 0,
    val unreadCount: Int = 0
)

@IgnoreExtraProperties
data class ErrorReport(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("error_type") @set:PropertyName("error_type") var error_type: String = "USER_REPORT", // CRASH, NON_FATAL, USER_REPORT
    @get:PropertyName("stack_trace") @set:PropertyName("stack_trace") var stack_trace: String = "",
    @get:PropertyName("device_info") @set:PropertyName("device_info") var device_info: String = "",
    @get:PropertyName("app_version") @set:PropertyName("app_version") var app_version: String = "1.0",
    @get:PropertyName("screen_name") @set:PropertyName("screen_name") var screen_name: String = "",
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: String = "",
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "new"
)
