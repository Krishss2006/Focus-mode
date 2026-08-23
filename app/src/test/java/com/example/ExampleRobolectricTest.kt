package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GeofenceConfig
import com.example.data.GeofenceRepository
import com.example.geofence.GeofenceHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Auto DND", appName)
  }

  @Test
  fun `save and load geofence configuration in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = GeofenceRepository(context)

    val customConfig = GeofenceConfig(
      latitude = 34.0689,
      longitude = -118.4452,
      radiusMeters = 200f,
      locationName = "University Hall",
      isEnabled = true
    )

    repository.saveConfig(customConfig)
    val loaded = repository.config.value

    assertEquals(34.0689, loaded.latitude, 0.0001)
    assertEquals(-118.4452, loaded.longitude, 0.0001)
    assertEquals(200f, loaded.radiusMeters, 0.1f)
    assertEquals("University Hall", loaded.locationName)
    assertEquals(true, loaded.isEnabled)
  }

  @Test
  fun `build geofence object with correct parameters`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val helper = GeofenceHelper(context)
    val geofence = helper.buildGeofence(37.7749, -122.4194, 150f)
    assertNotNull(geofence)
    assertEquals(GeofenceHelper.GEOFENCE_ID, geofence.requestId)
  }
}
