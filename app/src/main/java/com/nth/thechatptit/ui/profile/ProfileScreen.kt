package com.nth.thechatptit.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nth.thechatptit.data.model.AppSettings
import com.nth.thechatptit.data.model.AuthState
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.model.ThemeMode
import com.nth.thechatptit.data.model.Workout
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.ui.common.distanceInUserUnits
import com.nth.thechatptit.ui.common.distanceUnitLabel
import com.nth.thechatptit.ui.common.formatDistance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File

@Composable
fun Profile(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    onWorkoutClick: (String) -> Unit = {},
    onLogin: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var showProfileEditor by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::updateAvatar)
    }
    val currentUser = (state.authState as? AuthState.LoggedIn)?.user

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Cá nhân", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
            }
        }
        Spacer(Modifier.height(16.dp))
        ProfileHeader(
            authState = state.authState,
            avatarImagePath = state.avatarImagePath,
            isUpdatingAvatar = state.isSavingProfile,
            onLogin = onLogin,
            onEdit = { showProfileEditor = true },
            onPickAvatar = { avatarPicker.launch("image/*") }
        )
        if (state.accountError != null && !showProfileEditor) {
            Text(state.accountError.orEmpty(), color = colors.error)
        }
        if (showProfileEditor && currentUser != null) {
            ProfileEditDialog(
                initialName = currentUser.displayName,
                error = state.accountError,
                isSaving = state.isSavingProfile,
                onDismiss = {
                    viewModel.clearAccountError()
                    showProfileEditor = false
                },
                onSave = { name ->
                    viewModel.updateDisplayName(name) { success ->
                        if (success) showProfileEditor = false
                    }
                },
                onNameChanged = viewModel::clearAccountError
            )
        }
        Spacer(Modifier.height(22.dp))
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            ProfileStats(state, state.settings.distanceUnit)
            Spacer(Modifier.height(26.dp))
            GoalsSection(
                goals = state.goals,
                progressByGoalId = state.goalProgress,
                distanceUnit = state.settings.distanceUnit,
                onSaveGoal = viewModel::saveGoal,
                onDeleteGoal = viewModel::deleteGoal
            )
            Spacer(Modifier.height(26.dp))
            HistorySection(state.workouts, state.settings.distanceUnit, onWorkoutClick)
        }
        Spacer(Modifier.height(26.dp))
        SettingsSection(
            settings = state.settings,
            onAutoPauseChanged = viewModel::setAutoPauseEnabled,
            onThemeClick = viewModel::cycleTheme,
            onUnitClick = viewModel::toggleDistanceUnit,
            onDefaultWorkoutClick = viewModel::toggleDefaultWorkoutType,
            onLogout = if (state.authState is AuthState.LoggedIn) {
                {
                    viewModel.logout()
                    onLogin()
                }
            } else null
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileHeader(
    authState: AuthState,
    avatarImagePath: String?,
    isUpdatingAvatar: Boolean,
    onLogin: () -> Unit,
    onEdit: () -> Unit,
    onPickAvatar: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(68.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (avatarImagePath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(avatarImagePath)).build(),
                        contentDescription = "Ảnh đại diện",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = colors.onPrimaryContainer
                    )
                }
                if (authState is AuthState.LoggedIn) {
                    IconButton(
                        onClick = onPickAvatar,
                        enabled = !isUpdatingAvatar,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(colors.surface, CircleShape)
                    ) {
                        if (isUpdatingAvatar) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Thay ảnh đại diện",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                when (authState) {
                    is AuthState.LoggedIn -> {
                        Text(
                            text = authState.user.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = authState.user.email,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Chỉnh sửa hồ sơ")
                        }
                        Text("Đã đăng nhập", fontSize = 12.sp, color = colors.primary)
                    }
                    else -> {
                        Text("Walker", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Tài khoản cục bộ", color = colors.onSurfaceVariant)
                        TextButton(onClick = onLogin) { Text("Đăng nhập") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditDialog(
    initialName: String,
    error: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onNameChanged: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa hồ sơ") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; onNameChanged() },
                    label = { Text("Tên hiển thị") },
                    singleLine = true
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank() && !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Hủy") }
        }
    )
}

@Composable
private fun ProfileStats(state: ProfileUiState, distanceUnit: DistanceUnit) {
    val colors = MaterialTheme.colorScheme
    val statistics = state.statistics
    Column {
        Text("Thống kê tổng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    StatCell("${statistics?.workoutCount ?: 0}", "hoạt động", Modifier.weight(1f))
                    StatCell(
                        String.format(
                            Locale.US,
                            "%.1f",
                            distanceInUserUnits(statistics?.totalDistanceMeters ?: 0.0, distanceUnit)
                        ),
                        distanceUnitLabel(distanceUnit),
                        Modifier.weight(1f)
                    )
                    StatCell(
                        formatDuration(statistics?.totalActiveDurationMillis ?: 0L),
                        "thời gian",
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = colors.outlineVariant)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Hoạt động được lưu trên thiết bị để có thể xem lại.",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun HistorySection(
    workouts: List<Workout>,
    distanceUnit: DistanceUnit,
    onWorkoutClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text("Lịch sử hoạt động", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (workouts.isEmpty()) {
            Text("Chưa có hoạt động hoàn thành.", color = colors.onSurfaceVariant)
        } else {
            workouts.take(8).forEach { workout ->
                WorkoutHistoryRow(workout, distanceUnit, onWorkoutClick)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(
    workout: Workout,
    distanceUnit: DistanceUnit,
    onWorkoutClick: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onWorkoutClick(workout.id) },
        shape = RoundedCornerShape(12.dp),
        color = colors.surface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (workout.type == WorkoutType.CYCLING) Icons.Default.DirectionsBike else Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer
                )
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    workout.title ?: if (workout.type == WorkoutType.CYCLING) "Đạp xe" else "Chạy bộ",
                    fontWeight = FontWeight.SemiBold
                )
                Text(formatDate(workout.startedAtEpochMillis), fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatDistance(workout.distanceMeters, distanceUnit),
                    fontWeight = FontWeight.SemiBold
                )
                Text(formatDuration(workout.activeDurationMillis), fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    settings: AppSettings,
    onAutoPauseChanged: (Boolean) -> Unit,
    onThemeClick: () -> Unit,
    onUnitClick: () -> Unit,
    onDefaultWorkoutClick: () -> Unit,
    onLogout: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text("Cài đặt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant
        ) {
            Column {
                SettingSwitchRow(
                    icon = Icons.Default.DirectionsRun,
                    title = "Tạm dừng tự động",
                    subtitle = "Tạm dừng khi phát hiện đứng yên",
                    checked = settings.autoPauseEnabled,
                    onCheckedChange = onAutoPauseChanged
                )
                HorizontalDivider(color = colors.outlineVariant)
                SettingInfoRow(Icons.Default.DarkMode, "Giao diện", themeLabel(settings.themeMode), onClick = onThemeClick)
                HorizontalDivider(color = colors.outlineVariant)
                SettingInfoRow(
                    Icons.Default.Settings,
                    "Đơn vị",
                    if (settings.distanceUnit == DistanceUnit.KILOMETERS) "Kilomet và km/h" else "Dặm và mph",
                    onClick = onUnitClick
                )
                HorizontalDivider(color = colors.outlineVariant)
                SettingInfoRow(
                    if (settings.defaultWorkoutType == WorkoutType.CYCLING) {
                        Icons.Default.DirectionsBike
                    } else {
                        Icons.Default.DirectionsRun
                    },
                    "Hoạt động mặc định",
                    if (settings.defaultWorkoutType == WorkoutType.CYCLING) "Đạp xe" else "Chạy bộ",
                    onClick = onDefaultWorkoutClick
                )
                if (onLogout != null) {
                    HorizontalDivider(color = colors.outlineVariant)
                    SettingInfoRow(
                        Icons.Default.Logout,
                        "Đăng xuất",
                        "Xóa phiên đăng nhập trên thiết bị",
                        colors.error,
                        onClick = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: colors.primary)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Theo hệ thống"
    ThemeMode.LIGHT -> "Sáng"
    ThemeMode.DARK -> "Tối"
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy · HH:mm", Locale.forLanguageTag("vi")).format(Date(epochMillis))

private fun formatDuration(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000
    return if (totalMinutes >= 60) {
        String.format(Locale.US, "%dh %02dm", totalMinutes / 60, totalMinutes % 60)
    } else {
        String.format(Locale.US, "%dm", totalMinutes)
    }
}
