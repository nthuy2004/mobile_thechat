package com.nth.thechatptit.ui.profile

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.model.Goal
import com.nth.thechatptit.data.model.GoalMetric
import com.nth.thechatptit.data.model.GoalProgress
import com.nth.thechatptit.data.model.GoalReminder
import com.nth.thechatptit.ui.common.distanceInUserUnits
import com.nth.thechatptit.ui.common.distanceUnitLabel
import java.util.Locale
import java.util.TimeZone

@Composable
internal fun GoalsSection(
    goals: List<Goal>,
    progressByGoalId: Map<String, GoalProgress>,
    distanceUnit: DistanceUnit,
    onSaveGoal: (String?, String, GoalMetric, Double, GoalReminder?) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var isCreatingGoal by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mục tiêu tuần", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { isCreatingGoal = true }) {
                Icon(Icons.Default.Add, contentDescription = "Thêm mục tiêu")
            }
        }
        if (goals.isEmpty()) {
            Text("Chưa có mục tiêu. Đặt mục tiêu đầu tiên cho tuần này.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                goals.forEach { goal ->
                    GoalCard(
                        goal = goal,
                        progress = progressByGoalId[goal.id],
                        distanceUnit = distanceUnit,
                        onEdit = { editingGoal = goal },
                        onDelete = { onDeleteGoal(goal.id) }
                    )
                }
            }
        }
    }

    if (isCreatingGoal || editingGoal != null) {
        GoalEditorDialog(
            goal = editingGoal,
            distanceUnit = distanceUnit,
            onDismiss = {
                isCreatingGoal = false
                editingGoal = null
            },
            onSave = { goalId, title, metric, target, reminder ->
                onSaveGoal(goalId, title, metric, target, reminder)
                isCreatingGoal = false
                editingGoal = null
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    progress: GoalProgress?,
    distanceUnit: DistanceUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        text = "${formatGoalValue(goal.metric, progress?.currentValue ?: 0.0, distanceUnit)} / " +
                            formatGoalValue(goal.metric, goal.targetValue, distanceUnit),
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa mục tiêu")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa mục tiêu")
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (progress?.percentage ?: 0f) / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${(progress?.percentage ?: 0f).toInt()}% hoàn thành${goal.reminder?.let { " · ${reminderSummary(it)}" }.orEmpty()}",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalEditorDialog(
    goal: Goal?,
    distanceUnit: DistanceUnit,
    onDismiss: () -> Unit,
    onSave: (String?, String, GoalMetric, Double, GoalReminder?) -> Unit
) {
    val context = LocalContext.current
    var title by remember(goal?.id) { mutableStateOf(goal?.title ?: "Mục tiêu tuần") }
    var metric by remember(goal?.id) { mutableStateOf(goal?.metric ?: GoalMetric.DISTANCE) }
    var targetInput by remember(goal?.id, distanceUnit) {
        mutableStateOf(goal?.let { goalTargetInput(it, distanceUnit) } ?: "10")
    }
    var reminderEnabled by remember(goal?.id) { mutableStateOf(goal?.reminder != null) }
    var daysMask by remember(goal?.id) { mutableStateOf(goal?.reminder?.daysOfWeekMask ?: 0b0010101) }
    var hour by remember(goal?.id) { mutableStateOf(goal?.reminder?.minutesOfDay?.div(60) ?: 18) }
    var minute by remember(goal?.id) { mutableStateOf(goal?.reminder?.minutesOfDay?.rem(60) ?: 0) }
    var permissionError by remember(goal?.id) { mutableStateOf(false) }
    var targetTouched by remember(goal?.id) { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminderEnabled = granted
        permissionError = !granted
    }

    val targetNumber = targetInput.replace(',', '.').toDoubleOrNull()
    val targetValue = targetNumber?.let { input ->
        when (metric) {
            GoalMetric.DISTANCE -> if (distanceUnit == DistanceUnit.KILOMETERS) input * 1_000.0 else input * 1_609.344
            GoalMetric.ACTIVE_DURATION -> input * 60_000.0
            GoalMetric.WORKOUT_COUNT -> input
        }
    }
    val validTarget = targetNumber != null && targetNumber > 0.0 &&
        (metric != GoalMetric.WORKOUT_COUNT || targetNumber % 1.0 == 0.0)
    val canSave = !title.isBlank() && targetNumber != null && targetNumber > 0.0 &&
        (metric != GoalMetric.WORKOUT_COUNT || targetNumber % 1.0 == 0.0) &&
        (!reminderEnabled || daysMask != 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Tạo mục tiêu tuần" else "Sửa mục tiêu tuần") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tên mục tiêu") },
                    singleLine = true
                )
                Text("Đo lường bằng", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        GoalMetric.DISTANCE to "Quãng đường",
                        GoalMetric.ACTIVE_DURATION to "Thời lượng",
                        GoalMetric.WORKOUT_COUNT to "Số buổi"
                    ).forEach { (option, label) ->
                        FilterChip(
                            selected = metric == option,
                            onClick = {
                                metric = option
                                targetInput = when (option) {
                                    GoalMetric.DISTANCE -> "10"
                                    GoalMetric.ACTIVE_DURATION -> "120"
                                    GoalMetric.WORKOUT_COUNT -> "3"
                                }
                                targetTouched = false
                            },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = {
                        targetInput = it
                        targetTouched = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(targetUnitLabel(metric, distanceUnit)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (metric == GoalMetric.WORKOUT_COUNT) KeyboardType.Number else KeyboardType.Decimal
                    ),
                    singleLine = true,
                    isError = targetTouched && !validTarget,
                    supportingText = if (targetTouched && !validTarget) {
                        { Text("Nhập giá trị mục tiêu hợp lệ.") }
                    } else null
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nhắc tập", fontWeight = FontWeight.Medium)
                        Text("Chọn các ngày và giờ muốn được nhắc.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                reminderEnabled = false
                                permissionError = false
                            } else if (
                                Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                reminderEnabled = true
                            }
                        }
                    )
                }
                if (reminderEnabled) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DAY_OPTIONS.forEach { (dayIndex, label) ->
                            val selected = daysMask and (1 shl dayIndex) != 0
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    daysMask = if (selected) daysMask and (1 shl dayIndex).inv()
                                    else daysMask or (1 shl dayIndex)
                                },
                                label = { Text(label, fontSize = 12.sp, maxLines = 1) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    hour = selectedHour
                                    minute = selectedMinute
                                },
                                hour,
                                minute,
                                DateFormat.is24HourFormat(context)
                            ).show()
                        }
                    ) {
                        Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
                    }
                }
                if (permissionError) {
                    Text(
                        "Chưa được cấp quyền thông báo, mục tiêu sẽ không có nhắc tập.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!canSave || targetValue == null) return@TextButton
                val reminder = if (reminderEnabled) {
                    GoalReminder(
                        minutesOfDay = hour * 60 + minute,
                        timeZoneId = TimeZone.getDefault().id,
                        daysOfWeekMask = daysMask
                    )
                } else null
                onSave(goal?.id, title, metric, targetValue, reminder)
            }, enabled = canSave) {
                Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

private val DAY_OPTIONS = listOf(
    0 to "T2",
    1 to "T3",
    2 to "T4",
    3 to "T5",
    4 to "T6",
    5 to "T7",
    6 to "CN"
)

private fun goalTargetInput(goal: Goal, distanceUnit: DistanceUnit): String {
    val input = when (goal.metric) {
        GoalMetric.DISTANCE -> distanceInUserUnits(goal.targetValue, distanceUnit)
        GoalMetric.ACTIVE_DURATION -> goal.targetValue / 60_000.0
        GoalMetric.WORKOUT_COUNT -> goal.targetValue
    }
    return if (goal.metric == GoalMetric.WORKOUT_COUNT) input.toInt().toString()
    else String.format(Locale.US, "%.1f", input)
}

private fun targetUnitLabel(metric: GoalMetric, distanceUnit: DistanceUnit): String = when (metric) {
    GoalMetric.DISTANCE -> distanceUnitLabel(distanceUnit)
    GoalMetric.ACTIVE_DURATION -> "phút vận động mỗi tuần"
    GoalMetric.WORKOUT_COUNT -> "buổi mỗi tuần"
}

private fun formatGoalValue(metric: GoalMetric, value: Double, distanceUnit: DistanceUnit): String = when (metric) {
    GoalMetric.DISTANCE -> String.format(
        Locale.US,
        "%.1f %s",
        distanceInUserUnits(value, distanceUnit),
        distanceUnitLabel(distanceUnit)
    )
    GoalMetric.ACTIVE_DURATION -> {
        val minutes = (value / 60_000.0).toInt()
        if (minutes >= 60) "${minutes / 60} giờ ${minutes % 60} phút" else "$minutes phút"
    }
    GoalMetric.WORKOUT_COUNT -> "${value.toInt()} buổi"
}

private fun reminderSummary(reminder: GoalReminder): String {
    val days = DAY_OPTIONS.filter { (index, _) -> reminder.daysOfWeekMask and (1 shl index) != 0 }
        .joinToString(", ") { it.second }
    return "nhắc $days lúc ${String.format(Locale.getDefault(), "%02d:%02d", reminder.minutesOfDay / 60, reminder.minutesOfDay % 60)}"
}
