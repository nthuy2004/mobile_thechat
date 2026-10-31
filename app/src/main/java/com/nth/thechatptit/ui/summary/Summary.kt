package com.nth.thechatptit.ui.summary

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.data.model.WorkoutVisibility
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.ui.common.formatDistance
import com.nth.thechatptit.ui.common.formatPace
import com.nth.thechatptit.ui.common.formatSpeed
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Summary(
    workoutId: String,
    onBack: () -> Unit,
    viewModel: SummaryViewModel
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val imagePath by viewModel.imagePath.collectAsStateWithLifecycle()
    val selectedImage by viewModel.selectedImage.collectAsStateWithLifecycle()
    val visibility by viewModel.visibility.collectAsStateWithLifecycle()
    val locationName by viewModel.locationName.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val cancelError by viewModel.cancelError.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var title by remember(workoutId) { mutableStateOf("") }
    var message by remember(workoutId) { mutableStateOf("") }
    var showCancelConfirmation by remember(workoutId) { mutableStateOf(false) }

    val requestExit = {
        when {
            workout == null || actionState.result != null -> onBack()
            !actionState.isSaving && !actionState.isCancelling -> showCancelConfirmation = true
        }
    }

    BackHandler(enabled = actionState.result == null) {
        requestExit()
    }

    LaunchedEffect(workoutId) {
        viewModel.load(workoutId)
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.selectImage(uri)
    }

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        SummaryHeader(onBack = requestExit)

        if (workout == null) {
            Loading()
            return@Column
        }

        Column(modifier = Modifier.padding(20.dp)) {
            WorkoutInfo(workout!!, locationName, distanceUnit)
            Spacer(modifier = Modifier.height(20.dp))
            RoutePreview(
                imagePath = imagePath,
                selectedImage = selectedImage,
                onPickImage = { imagePicker.launch("image/*") },
                onResetImage = { viewModel.selectImage(null) }
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tiêu đề") },
                placeholder = { Text("Buổi chạy buổi sáng") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ghi chú") },
                placeholder = { Text("Cảm nhận về hoạt động") },
                minLines = 3
            )
            Spacer(modifier = Modifier.height(20.dp))
            VisibilityPicker(
                selected = visibility,
                onSelect = viewModel::setVisibility
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = requestExit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Huỷ")
                }
                Button(
                    onClick = { viewModel.save(title, message) },
                    modifier = Modifier.weight(2f),
                    enabled = !actionState.isSaving && !actionState.isCancelling && actionState.result == null
                ) {
                    Text(if (actionState.isSaving) "Đang lưu..." else "Lưu")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            containerColor = colors.surfaceVariant,
            title = { Text("Huỷ hoạt động?", color = colors.onSurface) },
            text = {
                Text("Hoạt động và tuyến đường đã ghi sẽ bị xoá khỏi thiết bị.", color = colors.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirmation = false
                    viewModel.cancel(onBack)
                }) { Text("Huỷ hoạt động", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text("Tiếp tục chỉnh sửa", color = colors.onSurfaceVariant)
                }
            }
        )
    }

    if (actionState.isSaving || actionState.isCancelling) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = colors.surfaceVariant,
            title = {
                Text(
                    if (actionState.isCancelling) "Đang huỷ hoạt động" else "Đang lưu hoạt động",
                    color = colors.onSurface
                )
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(
                        if (actionState.isCancelling) "Đang xoá dữ liệu trên thiết bị..."
                        else "Đang tải tuyến đường và ảnh lên...",
                        modifier = Modifier.padding(start = 16.dp),
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    actionState.result?.let { result ->
        val (heading, detail) = when (result) {
            SummarySaveResult.Synced -> "Đồng bộ thành công" to "Hoạt động đã được lưu và tải lên tài khoản của bạn."
            SummarySaveResult.LocalOnly -> "Đã lưu trên thiết bị" to "Đăng nhập để đồng bộ hoạt động lên tài khoản."
            is SummarySaveResult.Failed -> "Tải lên thất bại" to "Hoạt động vẫn được lưu trên thiết bị. ${result.reason}"
        }
        AlertDialog(
            onDismissRequest = onBack,
            containerColor = colors.surfaceVariant,
            title = { Text(heading, color = colors.onSurface) },
            text = { Text(detail, color = colors.onSurfaceVariant) },
            confirmButton = {
                if (result is SummarySaveResult.Failed) {
                    Row {
                        TextButton(onClick = onBack) { Text("Để sau", color = colors.onSurfaceVariant) }
                        TextButton(onClick = { viewModel.save(title, message) }) {
                            Text("Thử lại", color = colors.primary)
                        }
                    }
                } else {
                    TextButton(onClick = onBack) { Text("Xong", color = colors.primary) }
                }
            }
        )
    }

    cancelError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissCancelError,
            containerColor = colors.surfaceVariant,
            title = { Text("Không thể huỷ hoạt động", color = colors.onSurface) },
            text = { Text(error, color = colors.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCancelError) {
                    Text("Đóng", color = colors.primary)
                }
            }
        )
    }
}

@Composable
private fun SummaryHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
        }
        Text(
            text = "Hoàn thành",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun WorkoutInfo(workout: Workout, locationName: String, distanceUnit: DistanceUnit) {
    val colors = MaterialTheme.colorScheme

    val type = if (workout.type == WorkoutType.RUNNING) "Chạy bộ" else "Đạp xe"
    val date = Instant.ofEpochMilli(workout.startedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy · HH:mm", Locale.forLanguageTag("vi")))

    Column {
        Text(type, fontSize = 16.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(date, fontSize = 14.sp, color = colors.onSurface)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryMetric(
                label = "Thời gian",
                value = formatDuration(workout.activeDurationMillis)
            )
            SummaryMetric(
                label = "Quãng đường",
                value = formatDistance(workout.distanceMeters, distanceUnit)
            )
            SummaryMetric(
                label = if (workout.type == WorkoutType.CYCLING) "Tốc độ TB" else "Nhịp độ TB",
                value = if (workout.type == WorkoutType.CYCLING) {
                    formatSpeed(workout.averageSpeedMetersPerSecond, distanceUnit)
                } else {
                    formatPace(workout.averagePaceMillisPerKilometer, distanceUnit)
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (locationName.isNotEmpty()) locationName else "Không có dữ liệu vị trí",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    val colors = MaterialTheme.colorScheme

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun RoutePreview(
    imagePath: String?,
    selectedImage: String?,
    onPickImage: () -> Unit,
    onResetImage: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val currentImage = selectedImage ?: imagePath

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant)
    ) {
        if (currentImage != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(
                        when {
                            currentImage.startsWith("http") -> currentImage
                            currentImage.startsWith("/") -> File(currentImage)
                            else -> Uri.parse(currentImage)
                        }
                    )
                    .build(),
                contentDescription = "Ảnh hoạt động",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Không có ảnh bản đồ", color = colors.onSurface)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onResetImage,
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.scrim.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Dùng ảnh bản đồ", tint = colors.onSurface)
            }
            IconButton(
                onClick = onPickImage,
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.scrim.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Chọn ảnh", tint = colors.onSurface)
            }
        }
    }
}

@Composable
private fun VisibilityPicker(
    selected: WorkoutVisibility,
    onSelect: (WorkoutVisibility) -> Unit
) {
    Column {
        Text("Chế độ hiển thị", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selected == WorkoutVisibility.PRIVATE,
                onClick = { onSelect(WorkoutVisibility.PRIVATE) },
                label = { Text("Riêng tư", fontSize = 12.sp, maxLines = 1) }
            )
            FilterChip(
                selected = selected == WorkoutVisibility.FOLLOWERS,
                onClick = { onSelect(WorkoutVisibility.FOLLOWERS) },
                label = { Text("Người theo dõi", fontSize = 12.sp, maxLines = 1) }
            )
            FilterChip(
                selected = selected == WorkoutVisibility.PUBLIC,
                onClick = { onSelect(WorkoutVisibility.PUBLIC) },
                label = { Text("Công khai", fontSize = 12.sp, maxLines = 1) }
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    return String.format(
        Locale.US,
        "%02d:%02d:%02d",
        totalSeconds / 3600,
        totalSeconds % 3600 / 60,
        totalSeconds % 60
    )
}

