package com.example.data

data class GeofenceConfig(
    val latitude: Double = 37.4220,
    val longitude: Double = -122.0841,
    val radiusMeters: Float = 150f,
    val locationName: String = "Campus / College",
    val isEnabled: Boolean = false
)

data class LogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "ENTER", "EXIT", "BOOT_RESTORE", "MANUAL_REGISTER", "SIMULATION"
    val description: String,
    val dndFilterName: String,
    val isSuccess: Boolean = true
)

enum class GeofenceStatus {
    IDLE,
    ACTIVE,
    REGISTERING,
    ERROR
}
