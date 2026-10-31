package com.nth.thechatptit.data.sync

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nth.thechatptit.data.model.Goal
import com.nth.thechatptit.data.model.GoalReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalReminderScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(goal: Goal) {
        val request = buildRequest(goal, System.currentTimeMillis())
        if (request == null) {
            cancel(goal.id)
            return
        }
        workManager.cancelAllWorkByTag(legacyTag(goal.id))
        workManager.enqueueUniqueWork(
            uniqueName(goal.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleNext(goal: Goal, nowEpochMillis: Long = System.currentTimeMillis()) {
        val request = buildRequest(goal, nowEpochMillis) ?: return
        workManager.enqueueUniqueWork(
            uniqueName(goal.id),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    private fun buildRequest(goal: Goal, nowEpochMillis: Long): OneTimeWorkRequest? {
        val reminder = goal.reminder ?: return null
        if (!goal.isActive) return null
        val runAt = nextOccurrence(reminder, nowEpochMillis) ?: return null
        return OneTimeWorkRequestBuilder<GoalReminderWorker>()
            .setInputData(Data.Builder().putString(GoalReminderWorker.GOAL_ID, goal.id).build())
            .setInitialDelay((runAt - nowEpochMillis).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .addTag(chainTag(goal.id))
            .build()
    }

    fun cancel(goalId: String) {
        workManager.cancelUniqueWork(uniqueName(goalId))
        workManager.cancelAllWorkByTag(legacyTag(goalId))
        workManager.cancelAllWorkByTag(chainTag(goalId))
    }

    companion object {
        internal fun nextOccurrence(reminder: GoalReminder, nowEpochMillis: Long): Long? {
            if (reminder.daysOfWeekMask and 0b1111111 == 0) return null
            val timeZone = TimeZone.getTimeZone(reminder.timeZoneId)
            val now = Calendar.getInstance(timeZone, Locale.getDefault()).apply {
                timeInMillis = nowEpochMillis
            }
            for (dayOffset in 0..7) {
                val candidate = (now.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, reminder.minutesOfDay.coerceIn(0, 1439) / 60)
                    set(Calendar.MINUTE, reminder.minutesOfDay.coerceIn(0, 1439) % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_MONTH, dayOffset)
                }
                val isoDayIndex = (candidate.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val selected = reminder.daysOfWeekMask and (1 shl isoDayIndex) != 0
                if (selected && candidate.timeInMillis > nowEpochMillis) return candidate.timeInMillis
            }
            return null
        }

        private fun uniqueName(goalId: String) = "goal-reminder-$goalId"
        private fun legacyTag(goalId: String) = "goal-reminder-$goalId"
        private fun chainTag(goalId: String) = "goal-reminder-chain-$goalId"
    }
}
