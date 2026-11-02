package com.example.roadcracks.data

internal data class UserProfile(
    val name: String = "Użytkownik",
    val email: String = "",
    val gender: String = "",
    val photoUri: String = ""
)

internal data class RoadAlert(
    val id: String,
    val title: String,
    val message: String,
    val place: String,
    val createdAt: Long,
    val isRead: Boolean = false
)
