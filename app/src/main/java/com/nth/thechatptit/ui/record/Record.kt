package com.nth.thechatptit.ui.record

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.nth.thechatptit.data.model.GpsStatus
import com.nth.thechatptit.data.model.TrackingState
import com.nth.thechatptit.data.model.TrackingStatus
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.ui.common.distanceInUserUnits
import com.nth.thechatptit.ui.common.distanceUnitLabel
import com.nth.thechatptit.ui.common.formatPace
import com.nth.thechatptit.ui.common.formatSpeed
import com.nth.thechatptit.ui.theme.warning

@Composable
fun Record(
    modifier: Modifier,
    viewModel: RecordViewModel,
    onBack: () -> Unit = {},
    onWorkoutFinished: (String?) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val route by viewModel.route.collectAsStateWithLifecycle()
    val defaultWorkoutType by viewModel.defaultWorkoutType.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val isStopping by viewModel.isStopping.collectAsStateWithLifecycle()
    val stopError by viewModel.stopError.collectAsStateWithLifecycle()
    var selectedType by remember(defaultWorkoutType) { mutableStateOf(defaultWorkoutType) }
    var showStopDialog by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var permissionError by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf(false) }
    var isPanelExpanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasPermission = ContextCompat.checkSelfPermission(
            viewModel.getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            viewModel.getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.start(selectedType)
        } else {
            permissionError = true
        }
    }
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshLocation()
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            viewModel.getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            viewModel.getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(state.gpsStatus, state.status) {
        if (state.gpsStatus == GpsStatus.DISABLED && state.status != TrackingStatus.IDLE) {
            showLocationSettings = true
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.surface)) {
        RecordMap(route, state.currentPoint?.let { LatLng(it.latitude, it.longitude) })

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Quay lại",
                tint = colors.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            MetricsPanel(
                state = state,
                distanceUnit = distanceUnit,
                isExpanded = isPanelExpanded,
                onToggleExpand = { isPanelExpanded = !isPanelExpanded }
            )

            BottomControlSheet(
                status = state.status,
                selectedType = selectedType,
                onSelectType = { selectedType = it },
                onStart = {
                    val notificationPermissionMissing = android.os.Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            viewModel.getApplication(), Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    if (hasPermission && !notificationPermissionMissing) {
                        viewModel.start(selectedType)
                    } else {
                        permissionLauncher.launch(
                        buildList {
                            if (!hasPermission) {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                            if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                        }.toTypedArray()
                        )
                    }
                },
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = { showStopDialog = true }
            )
        }
    }

    if (showLocationSettings) {
        AlertDialog(
            onDismissRequest = { showLocationSettings = false },
            containerColor = colors.surfaceVariant,
            title = { Text("Định vị đang tắt", color = colors.onSurface) },
            text = { Text("Bật vị trí để KaiWalker có thể ghi lại tuyến đường.", color = colors.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationSettings = false
                    locationSettingsLauncher.launch(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }) { Text("Mở cài đặt", color = colors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationSettings = false }) {
                    Text("Để sau", color = colors.onSurfaceVariant)
                }
            }
        )
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            containerColor = colors.surfaceVariant,
            title = { Text("Kết thúc hoạt động?", color = colors.onSurface) },
            text = { Text("Hoạt động sẽ được lưu với số liệu hiện tại.", color = colors.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    viewModel.stop { stoppedId ->
                        onWorkoutFinished(stoppedId)
                    }
                }) {
                    Text("Kết thúc", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Tiếp tục", color = colors.onSurfaceVariant)
                }
            }
        )
    }

    if (isStopping) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = colors.surfaceVariant,
            title = { Text("Đang hoàn tất hoạt động", color = colors.onSurface) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(
                        "Đang lưu dữ liệu tuyến đường...",
                        modifier = Modifier.padding(start = 16.dp),
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    stopError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissStopError,
            containerColor = colors.surfaceVariant,
            title = { Text("Không thể hoàn tất", color = colors.onSurface) },
            text = { Text(error, color = colors.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissStopError) {
                    Text("Đóng", color = colors.primary)
                }
            }
        )
    }

    if (permissionError) {
        AlertDialog(
            onDismissRequest = { permissionError = false },
            containerColor = colors.surfaceVariant,
            title = { Text("Cần quyền vị trí", color = colors.onSurface) },
            text = {
                Text(
                    "Cho phép quyền vị trí để ghi lại tuyến đường trong lúc tập. Bản ghi vẫn tiếp tục khi bạn tắt màn hình.",
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    permissionError = false
                    viewModel.getApplication<Application>().startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${viewModel.getApplication<Application>().packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) {
                    Text("Mở cài đặt", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { permissionError = false }) {
                    Text("Để sau", color = colors.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun RecordMap(route: List<com.nth.thechatptit.data.model.TrackPoint>, current: LatLng?) {
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(current ?: LatLng(21.0285, 105.8542), 15f)
    }
    LaunchedEffect(current) {
        current?.let { camera.animate(CameraUpdateFactory.newLatLngZoom(it, 17f)) }
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
    ) {
        val points = route.map { LatLng(it.latitude, it.longitude) }
        if (points.size > 1) Polyline(points = points, color = MaterialTheme.colorScheme.primary, width = 12f)
        current?.let { Marker(state = MarkerState(position = it), title = "Vị trí hiện tại") }
    }
}

@Composable
private fun MetricsPanel(
    state: TrackingState,
    distanceUnit: DistanceUnit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .animateContentSize(animationSpec = tween(300)),
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GpsStatusBadge(state.gpsStatus)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                        contentDescription = if (isExpanded) "Thu nhỏ" else "Phóng to",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactMetric("Thời gian", formatDuration(state.activeDurationMillis))
                    CompactMetricDivider()
                    CompactMetric(
                        if (state.workoutType == WorkoutType.CYCLING) "Tốc độ" else "Nhịp độ TB (/${distanceUnitLabel(distanceUnit)})",
                        if (state.workoutType == WorkoutType.CYCLING)
                            formatSpeed(state.currentSpeedMetersPerSecond ?: state.averageSpeedMetersPerSecond, distanceUnit)
                        else
                            formatPace(state.averagePaceMillisPerKilometer, distanceUnit)
                    )
                    CompactMetricDivider()
                    CompactMetric(
                        "Quãng đường (${distanceUnitLabel(distanceUnit)})",
                        String.format("%.2f", distanceInUserUnits(state.distanceMeters, distanceUnit))
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatDuration(state.activeDurationMillis),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ExpandedMetric(
                        label = if (state.workoutType == WorkoutType.CYCLING) "Tốc độ tức thời" else "Nhịp độ trung bình (/${distanceUnitLabel(distanceUnit)})",
                        value = if (state.workoutType == WorkoutType.CYCLING)
                            formatSpeed(state.currentSpeedMetersPerSecond ?: state.averageSpeedMetersPerSecond, distanceUnit)
                        else
                            formatPace(state.averagePaceMillisPerKilometer, distanceUnit)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ExpandedMetric(
                        label = "Quãng đường (${distanceUnitLabel(distanceUnit)})",
                        value = String.format("%.2f", distanceInUserUnits(state.distanceMeters, distanceUnit))
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surfaceVariant)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Các chặng (${distanceUnitLabel(distanceUnit)})",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun GpsStatusBadge(status: GpsStatus) {
    val label = gpsLabel(status)
    val colors = MaterialTheme.colorScheme
    val color = gpsColor(status, colors)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RowScope.CompactMetric(label: String, value: String) {
    val colors = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompactMetricDivider() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(colors.outlineVariant)
    )
}

@Composable
private fun ExpandedMetric(label: String, value: String) {
    val colors = MaterialTheme.colorScheme

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun BottomControlSheet(
    status: TrackingStatus,
    selectedType: WorkoutType,
    onSelectType: (WorkoutType) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (status) {
                TrackingStatus.IDLE -> IdleControls(selectedType, onSelectType, onStart)
                TrackingStatus.RECORDING -> RecordingControls(onPause)
                TrackingStatus.PAUSED -> PausedControls(onResume, onStop)
                else -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Đang xử lý...", color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun IdleControls(
    selectedType: WorkoutType,
    onSelectType: (WorkoutType) -> Unit,
    onStart: () -> Unit
) {
    var showActivityPicker by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivitySelectorButton(
                selectedType = selectedType,
                onClick = { showActivityPicker = !showActivityPicker }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onStart
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Bắt đầu",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bắt đầu",
                    fontSize = 11.sp,
                    color = colors.primary
                )
            }

            Spacer(modifier = Modifier.size(56.dp))
        }

        AnimatedVisibility(
            visible = showActivityPicker,
            enter = fadeIn(tween(150)) + expandVertically(tween(150), expandFrom = Alignment.Bottom),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150), shrinkTowards = Alignment.Bottom),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp)
        ) {
            ActivityPickerPopup(
                selectedType = selectedType,
                onSelect = {
                    onSelectType(it)
                    showActivityPicker = false
                }
            )
        }
    }
}

@Composable
private fun ActivitySelectorButton(
    selectedType: WorkoutType,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val icon = when (selectedType) {
        WorkoutType.RUNNING -> Icons.Default.DirectionsRun
        WorkoutType.CYCLING -> Icons.Default.DirectionsBike
    }
    val label = when (selectedType) {
        WorkoutType.RUNNING -> "Chạy bộ"
        WorkoutType.CYCLING -> "Đạp xe"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.outlineVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.primary
        )
    }
}

@Composable
private fun ActivityPickerPopup(
    selectedType: WorkoutType,
    onSelect: (WorkoutType) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val activities = listOf(
        Triple(WorkoutType.RUNNING, Icons.Default.DirectionsRun, "Chạy bộ"),
        Triple(WorkoutType.CYCLING, Icons.Default.DirectionsBike, "Đạp xe"),
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            activities.forEach { (type, icon, label) ->
                val isSelected = selectedType == type
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(type) }
                        .background(
                            if (isSelected) colors.outlineVariant else colors.surfaceVariant
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                        modifier = Modifier.width(80.dp)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingControls(onPause: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPause
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Tạm dừng",
                tint = colors.onPrimary,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun PausedControls(onResume: () -> Unit, onStop: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onStop
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Hoàn thành",
                    tint = colors.onSurface,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Hoàn thành", fontSize = 11.sp, color = colors.onSurfaceVariant)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onResume
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Tiếp tục",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tiếp tục", fontSize = 11.sp, color = colors.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(64.dp))
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000
    return "%02d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
}

private fun gpsLabel(status: GpsStatus) = when (status) {
    GpsStatus.GOOD -> "Đã thu được tín hiệu GPS"
    GpsStatus.WEAK -> "GPS yếu"
    GpsStatus.SEARCHING -> "Đang tìm tín hiệu GPS..."
    GpsStatus.PERMISSION_MISSING -> "Thiếu quyền vị trí"
    GpsStatus.DISABLED -> "GPS bị tắt"
    GpsStatus.UNKNOWN -> "Đang kiểm tra GPS"
}

private fun gpsColor(status: GpsStatus, colors: ColorScheme) = when (status) {
    GpsStatus.GOOD -> colors.tertiary
    GpsStatus.WEAK -> colors.warning
    GpsStatus.PERMISSION_MISSING, GpsStatus.DISABLED -> colors.error
    else -> colors.onSurfaceVariant
}
