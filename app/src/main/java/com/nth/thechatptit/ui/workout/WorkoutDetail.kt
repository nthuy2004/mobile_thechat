package com.nth.thechatptit.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutSegment
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.model.WorkoutVisibility
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.ui.common.formatDistance
import com.nth.thechatptit.ui.common.formatPace
import com.nth.thechatptit.ui.common.formatSpeed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutDetail(
    modifier: Modifier = Modifier,
    workoutId: String,
    viewModel: WorkoutDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(workoutId) {
        viewModel.load(workoutId)
    }

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary)
        }
        return
    }

    val workout = state.workout
    if (workout == null) {
        Column(
            modifier = modifier.fillMaxSize().background(colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Không tìm thấy hoạt động")
            Spacer(modifier = Modifier.height(12.dp))
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = workout.title ?: activityName(workout.type),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        WorkoutRouteMap(state.route)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ActivityHeading(workout)
            Spacer(modifier = Modifier.height(18.dp))
            DetailMetrics(workout, distanceUnit)
            if (state.segments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                SegmentList(state.segments, distanceUnit)
            }
            Spacer(modifier = Modifier.height(18.dp))
            DetailInfo(workout, state.post?.caption)
        }
    }
}

@Composable
private fun SegmentList(segments: List<WorkoutSegment>, distanceUnit: DistanceUnit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Đoạn hoạt động", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            segments.forEach { segment ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Đoạn ${segment.sequence + 1}", color = colors.onSurfaceVariant)
                    Text(formatDistance(segment.distanceMeters, distanceUnit), fontWeight = FontWeight.Medium)
                    Text(formatDuration(segment.durationMillis), color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WorkoutRouteMap(route: List<com.nth.thechatptit.data.model.TrackPoint>) {
    val colors = MaterialTheme.colorScheme
    val points = route.map { LatLng(it.latitude, it.longitude) }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(points.firstOrNull() ?: LatLng(21.0285, 105.8542), 15f)
    }
    LaunchedEffect(points) {
        points.firstOrNull()?.let { camera.animate(CameraUpdateFactory.newLatLngZoom(it, 16f)) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(0.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = true
            )
        ) {
            if (points.size > 1) {
                Polyline(points = points, color = colors.primary, width = 12f)
                Marker(state = MarkerState(position = points.first()), title = "Bắt đầu")
                Marker(state = MarkerState(position = points.last()), title = "Kết thúc")
            }
        }
    }
}

@Composable
private fun ActivityHeading(workout: Workout) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (workout.type == WorkoutType.CYCLING) Icons.Default.DirectionsBike else Icons.Default.DirectionsRun,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column {
            Text(activityName(workout.type), color = colors.primary, fontWeight = FontWeight.SemiBold)
            Text(formatDate(workout.startedAtEpochMillis), color = colors.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DetailMetrics(workout: Workout, distanceUnit: DistanceUnit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                DetailMetric("Quãng đường", formatDistance(workout.distanceMeters, distanceUnit), Modifier.weight(1f))
                DetailMetric("Thời gian", formatDuration(workout.activeDurationMillis), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                DetailMetric(
                    if (workout.type == WorkoutType.CYCLING) "Tốc độ TB" else "Pace TB",
                    if (workout.type == WorkoutType.CYCLING) {
                        formatSpeed(workout.averageSpeedMetersPerSecond, distanceUnit)
                    } else {
                        formatPace(workout.averagePaceMillisPerKilometer, distanceUnit)
                    },
                    Modifier.weight(1f)
                )
                DetailMetric("Tạm dừng", formatDuration(workout.pausedDurationMillis), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DetailInfo(workout: Workout, caption: String?) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = colors.primary)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Tuyến đường đã ghi", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Dữ liệu GPS được lưu trên thiết bị.", color = colors.onSurfaceVariant, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (workout.visibility == WorkoutVisibility.PRIVATE) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(visibilityName(workout.visibility), fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
            caption?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(it)
            }
        }
    }
}

private fun activityName(type: WorkoutType) = if (type == WorkoutType.CYCLING) "Đạp xe" else "Chạy bộ"

private fun visibilityName(visibility: WorkoutVisibility) = when (visibility) {
    WorkoutVisibility.PRIVATE -> "Riêng tư"
    WorkoutVisibility.FOLLOWERS -> "Người theo dõi"
    WorkoutVisibility.PUBLIC -> "Công khai"
}

private fun formatDate(epochMillis: Long): String = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.forLanguageTag("vi"))
    .format(Date(epochMillis))

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    return String.format(Locale.US, "%02d:%02d:%02d", totalSeconds / 3600, totalSeconds % 3600 / 60, totalSeconds % 60)
}

