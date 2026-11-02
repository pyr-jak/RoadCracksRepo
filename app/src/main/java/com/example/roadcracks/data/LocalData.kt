package com.example.roadcracks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal class LocalData(context: Context) {
    private val preferences = context.getSharedPreferences("roadcracks_ui", Context.MODE_PRIVATE)

    fun loadProfile() = UserProfile(
        name = preferences.getString("name", "Użytkownik").orEmpty(),
        email = preferences.getString("email", "").orEmpty(),
        gender = preferences.getString("gender", "").orEmpty(),
        photoUri = preferences.getString("photo_uri", "").orEmpty()
    )

    fun saveProfile(profile: UserProfile) {
        preferences.edit()
            .putString("name", profile.name)
            .putString("email", profile.email)
            .putString("gender", profile.gender)
            .putString("photo_uri", profile.photoUri)
            .apply()
    }

    var place: String
        get() = preferences.getString("observed_place", "").orEmpty()
        set(value) { preferences.edit().putString("observed_place", value).apply() }

    fun loadAlerts(): List<RoadAlert> = runCatching {
        val array = JSONArray(preferences.getString("alerts", "[]") ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                RoadAlert(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    message = item.getString("message"),
                    place = item.getString("place"),
                    createdAt = item.getLong("createdAt"),
                    isRead = item.optBoolean("isRead", false)
                )
            }.getOrNull()
        }.take(50)
    }.getOrDefault(emptyList())

    fun saveAlerts(alerts: List<RoadAlert>) {
        val array = JSONArray()
        alerts.take(50).forEach { alert ->
            array.put(JSONObject().apply {
                put("id", alert.id)
                put("title", alert.title)
                put("message", alert.message)
                put("place", alert.place)
                put("createdAt", alert.createdAt)
                put("isRead", alert.isRead)
            })
        }
        preferences.edit().putString("alerts", array.toString()).apply()
    }
}
