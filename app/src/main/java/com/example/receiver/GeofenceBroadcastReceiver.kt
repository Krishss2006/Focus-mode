package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.GeofenceRepository
import com.example.util.DndManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = GeofenceRepository.getInstance(context)
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.w(TAG, "GeofencingEvent was null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error code: ${geofencingEvent.errorCode} ($errorMessage)")
            repository.recordTransition(
                transitionType = "ERROR",
                dndMode = "Unchanged",
                description = "Geofence error: $errorMessage (code ${geofencingEvent.errorCode})",
                isSuccess = false
            )
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.i(TAG, "GEOFENCE_TRANSITION_ENTER triggered")
                val (success, message) = DndManager.setDndMode(context, enableDnd = true)
                repository.recordTransition(
                    transitionType = "ENTER",
                    dndMode = "Priority DND",
                    description = if (success) {
                        "Entered College Geofence: Priority DND enabled automatically"
                    } else {
                        "Entered College Geofence: Failed to enable DND ($message)"
                    },
                    isSuccess = success
                )
                DndManager.showStatusNotification(
                    context,
                    title = "College Zone Detected 🔕",
                    message = "Auto DND: Do Not Disturb (Priority Only) has been enabled for your classes."
                )
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.i(TAG, "GEOFENCE_TRANSITION_EXIT triggered")
                val (success, message) = DndManager.setDndMode(context, enableDnd = false)
                repository.recordTransition(
                    transitionType = "EXIT",
                    dndMode = "All (Normal)",
                    description = if (success) {
                        "Exited College Geofence: Normal interruptions mode restored"
                    } else {
                        "Exited College Geofence: Failed to restore Normal mode ($message)"
                    },
                    isSuccess = success
                )
                DndManager.showStatusNotification(
                    context,
                    title = "Leaving College 🔔",
                    message = "Auto DND: Normal sound and notifications restored."
                )
            }

            else -> {
                Log.w(TAG, "Unknown geofence transition: $transitionType")
            }
        }
    }

    companion object {
        const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_EVENT = "com.example.action.GEOFENCE_EVENT"
    }
}
