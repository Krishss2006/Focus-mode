package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class GeofenceRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<GeofenceConfig> = _config.asStateFlow()

    private val _logs = MutableStateFlow(loadLogs())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _lastTransition = MutableStateFlow(prefs.getString(KEY_LAST_TRANSITION, "NONE") ?: "NONE")
    val lastTransition: StateFlow<String> = _lastTransition.asStateFlow()

    private val _lastTransitionTime = MutableStateFlow(prefs.getLong(KEY_LAST_TRANSITION_TIME, 0L))
    val lastTransitionTime: StateFlow<Long> = _lastTransitionTime.asStateFlow()

    private fun loadConfig(): GeofenceConfig {
        val lat = prefs.getString(KEY_LAT, "37.4220")?.toDoubleOrNull() ?: 37.4220
        val lng = prefs.getString(KEY_LNG, "-122.0841")?.toDoubleOrNull() ?: -122.0841
        val radius = prefs.getFloat(KEY_RADIUS, 150f)
        val name = prefs.getString(KEY_NAME, "Campus / College") ?: "Campus / College"
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        return GeofenceConfig(
            latitude = lat,
            longitude = lng,
            radiusMeters = radius,
            locationName = name,
            isEnabled = enabled
        )
    }

    fun saveConfig(config: GeofenceConfig) {
        prefs.edit()
            .putString(KEY_LAT, config.latitude.toString())
            .putString(KEY_LNG, config.longitude.toString())
            .putFloat(KEY_RADIUS, config.radiusMeters)
            .putString(KEY_NAME, config.locationName)
            .putBoolean(KEY_ENABLED, config.isEnabled)
            .apply()
        _config.value = config
    }

    fun setEnabled(enabled: Boolean) {
        val updated = _config.value.copy(isEnabled = enabled)
        saveConfig(updated)
    }

    fun recordTransition(transitionType: String, dndMode: String, description: String, isSuccess: Boolean = true) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_LAST_TRANSITION, transitionType)
            .putLong(KEY_LAST_TRANSITION_TIME, now)
            .apply()

        _lastTransition.value = transitionType
        _lastTransitionTime.value = now

        addLog(
            LogEntry(
                id = now,
                timestamp = now,
                eventType = transitionType,
                description = description,
                dndFilterName = dndMode,
                isSuccess = isSuccess
            )
        )
    }

    fun addLog(entry: LogEntry) {
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > MAX_LOGS) {
            current.removeAt(current.lastIndex)
        }
        _logs.value = current
        saveLogs(current)
    }

    fun clearLogs() {
        _logs.value = emptyList()
        prefs.edit().remove(KEY_LOGS).apply()
    }

    private fun loadLogs(): List<LogEntry> {
        val raw = prefs.getString(KEY_LOGS, null) ?: return emptyList()
        val list = mutableListOf<LogEntry>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LogEntry(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        eventType = obj.optString("eventType", "EVENT"),
                        description = obj.optString("description", ""),
                        dndFilterName = obj.optString("dndFilterName", ""),
                        isSuccess = obj.optBoolean("isSuccess", true)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveLogs(logs: List<LogEntry>) {
        val jsonArray = JSONArray()
        logs.take(MAX_LOGS).forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("timestamp", entry.timestamp)
            obj.put("eventType", entry.eventType)
            obj.put("description", entry.description)
            obj.put("dndFilterName", entry.dndFilterName)
            obj.put("isSuccess", entry.isSuccess)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_LOGS, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "auto_dnd_geofence_prefs"
        private const val KEY_LAT = "geofence_lat"
        private const val KEY_LNG = "geofence_lng"
        private const val KEY_RADIUS = "geofence_radius"
        private const val KEY_NAME = "geofence_name"
        private const val KEY_ENABLED = "geofence_enabled"
        private const val KEY_LAST_TRANSITION = "last_transition"
        private const val KEY_LAST_TRANSITION_TIME = "last_transition_time"
        private const val KEY_LOGS = "transition_logs"
        private const val MAX_LOGS = 40

        @Volatile
        private var instance: GeofenceRepository? = null

        fun getInstance(context: Context): GeofenceRepository {
            return instance ?: synchronized(this) {
                instance ?: GeofenceRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
