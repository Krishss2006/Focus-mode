package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.GeofenceRepository
import com.example.geofence.GeofenceHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Device rebooted or package updated ($action). Re-registering geofence if enabled.")
            val repository = GeofenceRepository.getInstance(context)
            val config = repository.config.value

            if (config.isEnabled) {
                val geofenceHelper = GeofenceHelper(context)
                if (geofenceHelper.hasAllLocationPermissions()) {
                    geofenceHelper.registerGeofence(
                        latitude = config.latitude,
                        longitude = config.longitude,
                        radiusMeters = config.radiusMeters
                    ) { result ->
                        result.fold(
                            onSuccess = {
                                Log.i(TAG, "Geofence successfully restored after boot.")
                                repository.addLog(
                                    com.example.data.LogEntry(
                                        eventType = "BOOT_RESTORE",
                                        description = "Geofence restored after device restart for '${config.locationName}' (${config.radiusMeters.toInt()}m)",
                                        dndFilterName = "Active",
                                        isSuccess = true
                                    )
                                )
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Failed to restore geofence after boot: ${error.localizedMessage}")
                                repository.addLog(
                                    com.example.data.LogEntry(
                                        eventType = "BOOT_ERROR",
                                        description = "Failed to restore geofence after reboot: ${error.localizedMessage}",
                                        dndFilterName = "Inactive",
                                        isSuccess = false
                                    )
                                )
                            }
                        )
                    }
                } else {
                    Log.w(TAG, "Cannot restore geofence: location permissions not granted.")
                }
            }
        }
    }

    companion object {
        private const val TAG = "AutoDndBootReceiver"
    }
}
