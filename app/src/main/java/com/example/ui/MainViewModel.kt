package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeofenceConfig
import com.example.data.GeofenceRepository
import com.example.data.LogEntry
import com.example.geofence.GeofenceHelper
import com.example.util.DndManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionsState(
    val dndPolicyGranted: Boolean = false,
    val fineLocationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val notificationGranted: Boolean = false
) {
    val allRequiredGranted: Boolean
        get() = dndPolicyGranted && fineLocationGranted && backgroundLocationGranted
}

data class MainUiState(
    val config: GeofenceConfig = GeofenceConfig(),
    val permissions: PermissionsState = PermissionsState(),
    val currentDndFilter: Int = 0,
    val isDndActive: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val lastTransition: String = "NONE",
    val lastTransitionTime: Long = 0L,
    val isLocating: Boolean = false,
    val isRegistering: Boolean = false,
    val snackbarMessage: String? = null
)

private data class DataHolder(
    val logs: List<LogEntry>,
    val locating: Boolean,
    val registering: Boolean,
    val snackbar: String?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository.getInstance(application)
    private val geofenceHelper = GeofenceHelper(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _permissionsState = MutableStateFlow(checkPermissions())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    private val _currentDndFilter = MutableStateFlow(DndManager.getCurrentInterruptionFilter(application))
    val currentDndFilter: StateFlow<Int> = _currentDndFilter.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        combine(repository.config, _permissionsState, _currentDndFilter) { config, permissions, dndFilter ->
            Triple(config, permissions, dndFilter)
        },
        combine(repository.logs, _isLocating, _isRegistering, _snackbarMessage) { logs, locating, registering, snackbar ->
            DataHolder(logs, locating, registering, snackbar)
        }
    ) { core, extra ->
        val (config, permissions, dndFilter) = core
        MainUiState(
            config = config,
            permissions = permissions,
            currentDndFilter = dndFilter,
            isDndActive = DndManager.isCurrentlyDnd(getApplication()),
            logs = extra.logs,
            lastTransition = repository.lastTransition.value,
            lastTransitionTime = repository.lastTransitionTime.value,
            isLocating = extra.locating,
            isRegistering = extra.registering,
            snackbarMessage = extra.snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        refreshState()
    }

    fun refreshState() {
        _permissionsState.value = checkPermissions()
        _currentDndFilter.value = DndManager.getCurrentInterruptionFilter(getApplication())
    }

    private fun checkPermissions(): PermissionsState {
        val context = getApplication<Application>()
        val dndGranted = DndManager.isNotificationPolicyAccessGranted(context)
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val bgGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            fineGranted
        }

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        return PermissionsState(
            dndPolicyGranted = dndGranted,
            fineLocationGranted = fineGranted,
            backgroundLocationGranted = bgGranted,
            batteryOptimizationIgnored = batteryIgnored,
            notificationGranted = notifGranted
        )
    }

    fun updateCoordinates(latitude: Double, longitude: Double) {
        val current = repository.config.value
        val updated = current.copy(latitude = latitude, longitude = longitude)
        repository.saveConfig(updated)
        if (updated.isEnabled) {
            reRegisterActiveGeofence(updated)
        }
    }

    fun updateRadius(radiusMeters: Float) {
        val current = repository.config.value
        val updated = current.copy(radiusMeters = radiusMeters)
        repository.saveConfig(updated)
        if (updated.isEnabled) {
            reRegisterActiveGeofence(updated)
        }
    }

    fun updateLocationName(name: String) {
        val current = repository.config.value
        val updated = current.copy(locationName = name)
        repository.saveConfig(updated)
    }

    fun toggleGeofence(enable: Boolean) {
        refreshState()
        val permissions = _permissionsState.value

        if (enable) {
            if (!permissions.dndPolicyGranted) {
                _snackbarMessage.value = "DND Access is required. Grant permission in settings first."
                return
            }
            if (!permissions.fineLocationGranted) {
                _snackbarMessage.value = "Precise Location permission is required."
                return
            }
            if (!permissions.backgroundLocationGranted) {
                _snackbarMessage.value = "Background Location ('Allow all the time') is required."
                return
            }

            _isRegistering.value = true
            val config = repository.config.value
            geofenceHelper.registerGeofence(
                latitude = config.latitude,
                longitude = config.longitude,
                radiusMeters = config.radiusMeters
            ) { result ->
                _isRegistering.value = false
                result.fold(
                    onSuccess = {
                        repository.setEnabled(true)
                        repository.addLog(
                            LogEntry(
                                eventType = "REGISTERED",
                                description = "Geofence registered: ${config.locationName} (${config.radiusMeters.toInt()}m radius)",
                                dndFilterName = "Active Monitoring",
                                isSuccess = true
                            )
                        )
                        _snackbarMessage.value = "Geofence monitoring activated!"
                    },
                    onFailure = { error ->
                        repository.setEnabled(false)
                        repository.addLog(
                            LogEntry(
                                eventType = "ERROR",
                                description = "Registration failed: ${error.localizedMessage}",
                                dndFilterName = "Failed",
                                isSuccess = false
                            )
                        )
                        _snackbarMessage.value = "Failed to register geofence: ${error.localizedMessage}"
                    }
                )
            }
        } else {
            _isRegistering.value = true
            geofenceHelper.unregisterGeofence { result ->
                _isRegistering.value = false
                repository.setEnabled(false)
                repository.addLog(
                    LogEntry(
                        eventType = "DEACTIVATED",
                        description = "Geofence monitoring stopped by user",
                        dndFilterName = "Disabled",
                        isSuccess = true
                    )
                )
                _snackbarMessage.value = "Geofence monitoring deactivated."
            }
        }
    }

    private fun reRegisterActiveGeofence(config: GeofenceConfig) {
        if (!geofenceHelper.hasAllLocationPermissions()) return
        geofenceHelper.registerGeofence(config.latitude, config.longitude, config.radiusMeters) { result ->
            result.fold(
                onSuccess = {
                    repository.addLog(
                        LogEntry(
                            eventType = "UPDATED",
                            description = "Geofence parameters updated: ${config.latitude}, ${config.longitude} (${config.radiusMeters.toInt()}m)",
                            dndFilterName = "Active",
                            isSuccess = true
                        )
                    )
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Failed to update geofence: ${err.localizedMessage}"
                }
            )
        }
    }

    fun detectCurrentLocation() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            _snackbarMessage.value = "Location permission is required to detect current coordinates."
            return
        }

        _isLocating.value = true
        val cts = CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    _isLocating.value = false
                    if (location != null) {
                        updateCoordinates(location.latitude, location.longitude)
                        _snackbarMessage.value = "Campus coordinates updated to your current location!"
                    } else {
                        // Fallback to last location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                updateCoordinates(lastLoc.latitude, lastLoc.longitude)
                                _snackbarMessage.value = "Updated with latest known location."
                            } else {
                                _snackbarMessage.value = "Could not fetch GPS fix. Ensure GPS is enabled."
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    _isLocating.value = false
                    _snackbarMessage.value = "Failed to obtain location: ${e.localizedMessage}"
                }
        } catch (e: SecurityException) {
            _isLocating.value = false
            _snackbarMessage.value = "Location permission error: ${e.localizedMessage}"
        }
    }

    fun simulateTransition(isEnter: Boolean) {
        val context = getApplication<Application>()
        val (success, message) = DndManager.setDndMode(context, enableDnd = isEnter)
        val eventType = if (isEnter) "ENTER (SIM)" else "EXIT (SIM)"
        val dndMode = if (isEnter) "Priority DND" else "All (Normal)"

        repository.recordTransition(
            transitionType = eventType,
            dndMode = dndMode,
            description = "Simulated ${if (isEnter) "ENTER" else "EXIT"}: $message",
            isSuccess = success
        )

        refreshState()

        if (success) {
            DndManager.showStatusNotification(
                context,
                title = if (isEnter) "Auto DND: College Enter Test" else "Auto DND: College Exit Test",
                message = "Simulation test: DND mode switched to $dndMode."
            )
            _snackbarMessage.value = "Simulation executed: $message"
        } else {
            _snackbarMessage.value = "Simulation failed: $message"
        }
    }

    fun clearLogs() {
        repository.clearLogs()
        _snackbarMessage.value = "Activity logs cleared."
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
