package com.example.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Prior to Android 10, fine location covers background
        }
    }

    fun hasAllLocationPermissions(): Boolean {
        return hasFineLocationPermission() && hasBackgroundLocationPermission()
    }

    fun buildGeofence(latitude: Double, longitude: Double, radiusMeters: Float): Geofence {
        return Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(30_000) // 30 seconds responsiveness
            .build()
    }

    fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, intent, flags)
    }

    fun registerGeofence(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (!hasFineLocationPermission()) {
            onResult(Result.failure(SecurityException("ACCESS_FINE_LOCATION permission is missing.")))
            return
        }

        if (!hasBackgroundLocationPermission()) {
            onResult(Result.failure(SecurityException("ACCESS_BACKGROUND_LOCATION ('Allow all the time') is required.")))
            return
        }

        try {
            val geofence = buildGeofence(latitude, longitude, radiusMeters)
            val request = getGeofencingRequest(geofence)
            val pendingIntent = getGeofencePendingIntent()

            geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener {
                    onResult(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    onResult(Result.failure(exception))
                }
        } catch (e: SecurityException) {
            onResult(Result.failure(e))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun unregisterGeofence(onResult: ((Result<Unit>) -> Unit)? = null) {
        val pendingIntent = getGeofencePendingIntent()
        geofencingClient.removeGeofences(pendingIntent)
            .addOnSuccessListener {
                onResult?.invoke(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult?.invoke(Result.failure(exception))
            }
    }

    companion object {
        const val GEOFENCE_ID = "COLLEGE_AUTO_DND_GEOFENCE"
        private const val PENDING_INTENT_REQUEST_CODE = 2002
    }
}
