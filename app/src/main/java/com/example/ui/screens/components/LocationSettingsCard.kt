package com.example.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeofenceConfig
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SuccessGreen

@Composable
fun LocationSettingsCard(
    config: GeofenceConfig,
    isLocating: Boolean,
    isRegistering: Boolean,
    onUpdateCoordinates: (Double, Double) -> Unit,
    onUpdateRadius: (Float) -> Unit,
    onUpdateName: (String) -> Unit,
    onToggleGeofence: (Boolean) -> Unit,
    onDetectCurrentLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingManual by remember { mutableStateOf(false) }
    var latInput by remember(config.latitude) { mutableStateOf(config.latitude.toString()) }
    var lngInput by remember(config.longitude) { mutableStateOf(config.longitude.toString()) }
    var nameInput by remember(config.locationName) { mutableStateOf(config.locationName) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("location_settings_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "College Geofence Zone",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (config.isEnabled) "Armed • Auto-DND Active" else "Inactive • Geofence paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (config.isEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isRegistering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = PrimaryIndigo
                    )
                } else {
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { onToggleGeofence(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = PrimaryIndigo
                        ),
                        modifier = Modifier.testTag("master_geofence_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Campus Name Field
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    onUpdateName(it)
                },
                label = { Text("Campus / College Location Name") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.School, contentDescription = null, tint = PrimaryIndigo)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("college_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Coordinates Display / Editor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Target Coordinates",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${String.format("%.5f", config.latitude)}, ${String.format("%.5f", config.longitude)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    OutlinedButton(
                        onClick = { isEditingManual = !isEditingManual },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("manual_coords_toggle")
                    ) {
                        Icon(
                            imageVector = if (isEditingManual) Icons.Filled.Check else Icons.Filled.EditLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditingManual) "Done" else "Manual", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onDetectCurrentLocation,
                        enabled = !isLocating,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan),
                        modifier = Modifier.testTag("detect_location_button")
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.surface
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Current GPS", fontSize = 12.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isEditingManual) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = {
                                latInput = it
                                val lat = it.toDoubleOrNull()
                                if (lat != null && lat in -90.0..90.0) {
                                    onUpdateCoordinates(lat, config.longitude)
                                }
                            },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("latitude_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = lngInput,
                            onValueChange = {
                                lngInput = it
                                val lng = it.toDoubleOrNull()
                                if (lng != null && lng in -180.0..180.0) {
                                    onUpdateCoordinates(config.latitude, lng)
                                }
                            },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("longitude_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Radius Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Radar,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Geofence Radius",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${config.radiusMeters.toInt()} meters",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = config.radiusMeters,
                onValueChange = { onUpdateRadius(it) },
                valueRange = 50f..1000f,
                steps = 18,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryIndigo,
                    activeTrackColor = PrimaryIndigo
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radius_slider")
            )

            // Radius Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(100f, 150f, 250f, 500f)
                presets.forEach { radius ->
                    val isSelected = config.radiusMeters.toInt() == radius.toInt()
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdateRadius(radius) },
                        label = {
                            Text(
                                text = if (radius == 150f) "150m (Default)" else "${radius.toInt()}m",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryIndigo
                        ),
                        modifier = Modifier.testTag("preset_radius_${radius.toInt()}")
                    )
                }
            }
        }
    }
}
