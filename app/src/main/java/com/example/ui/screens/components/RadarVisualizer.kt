package com.example.ui.screens.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SuccessGreen

@Composable
fun RadarVisualizer(
    isGeofenceActive: Boolean,
    isDndActive: Boolean,
    radiusMeters: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val activeColor = if (isDndActive) PrimaryIndigo else SecondaryCyan
    val statusRingColor = if (isGeofenceActive) SuccessGreen else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(160.dp)
            .testTag("radar_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 - 8.dp.toPx()

            // Outer reference rings
            drawCircle(
                color = statusRingColor.copy(alpha = 0.2f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = statusRingColor.copy(alpha = 0.15f),
                radius = maxRadius * 0.66f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = statusRingColor.copy(alpha = 0.1f),
                radius = maxRadius * 0.33f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Animated pulse wave when geofence is active
            if (isGeofenceActive) {
                drawCircle(
                    color = activeColor.copy(alpha = pulseAlpha),
                    radius = maxRadius * pulseScale,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Fill glowing geofence zone
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.25f),
                            activeColor.copy(alpha = 0.05f)
                        ),
                        center = center,
                        radius = maxRadius * 0.85f
                    ),
                    radius = maxRadius * 0.85f,
                    center = center
                )
            } else {
                // Inactive dashed or muted circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.15f),
                    radius = maxRadius * 0.8f,
                    center = center
                )
            }

            // Center pin background glow
            drawCircle(
                color = activeColor.copy(alpha = if (isGeofenceActive) 0.3f else 0.1f),
                radius = 24.dp.toPx(),
                center = center
            )
        }

        // Center status icon
        val icon = when {
            isDndActive -> Icons.Filled.DoNotDisturbOn
            isGeofenceActive -> Icons.Filled.LocationOn
            else -> Icons.Filled.NotificationsActive
        }

        val iconTint = when {
            isDndActive -> PrimaryIndigo
            isGeofenceActive -> SuccessGreen
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Icon(
            imageVector = icon,
            contentDescription = "Geofence Status Icon",
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
    }
}
