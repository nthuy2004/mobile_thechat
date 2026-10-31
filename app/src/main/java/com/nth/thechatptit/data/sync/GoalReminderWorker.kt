package com.nth.thechatptit.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nth.thechatptit.R
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.model.GoalMetric
import com.nth.thechatptit.data.repo.GoalRepository
import com.nth.thechatptit.data.settings.SettingsRepository
import com.nth.thechatptit.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Locale

@HiltWorker
class GoalReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val goalRepository: GoalRepository,
    private val reminderScheduler: GoalReminderScheduler,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val goalId = inputData.getString(GOAL_ID) ?: return Result.failure()
        val goal = goalRepository.getGoal(goalId) ?: return Result.success()
        if (!goal.isActive || goal.reminder == null) return Result.success()

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        createNotificationChannel(notificationManager)
        val permissionGranted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted && notificationManager.areNotificationsEnabled()) {
            val progress = goalRepository.getProgress(goal)
            val distanceUnit = settingsRepository.settings.first().distanceUnit
            val current = when (goal.metric) {
                GoalMetric.DISTANCE -> formatDistance(progress.currentValue, distanceUnit)
                GoalMetric.ACTIVE_DURATION -> "${(progress.currentValue / 60_000.0).toInt()} phút"
                GoalMetric.WORKOUT_COUNT -> "${progress.currentValue.toInt()} buổi"
            }
            val target = when (goal.metric) {
                GoalMetric.DISTANCE -> formatDistance(goal.targetValue, distanceUnit)
                GoalMetric.ACTIVE_DURATION -> "${(goal.targetValue / 60_000.0).toInt()} phút"
                GoalMetric.WORKOUT_COUNT -> "${goal.targetValue.toInt()} buổi"
            }
            val openApp = PendingIntent.getActivity(
                applicationContext,
                goal.id.hashCode(),
                Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_feed)
                .setContentTitle(goal.title)
                .setContentText("Tiến độ: $current / $target. Đến giờ vận động rồi!")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            runCatching { notificationManager.notify(goal.id.hashCode(), notification) }
        }
        reminderScheduler.scheduleNext(goal)
        return Result.success()
    }

    private fun formatDistance(meters: Double, unit: DistanceUnit): String {
        val value = if (unit == DistanceUnit.KILOMETERS) meters / 1_000.0 else meters / 1_609.344
        val suffix = if (unit == DistanceUnit.KILOMETERS) "km" else "mi"
        return String.format(Locale.getDefault(), "%.1f %s", value, suffix)
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Nhắc mục tiêu", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    companion object {
        const val GOAL_ID = "goal_id"
        private const val CHANNEL_ID = "goal_reminders"
    }
}
