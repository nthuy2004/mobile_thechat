package com.nth.thechatptit.data.repo

import com.nth.thechatptit.data.local.AppDatabase
import com.nth.thechatptit.data.local.GoalEntity
import com.nth.thechatptit.data.model.Goal
import com.nth.thechatptit.data.model.GoalMetric
import com.nth.thechatptit.data.model.GoalPeriod
import com.nth.thechatptit.data.model.GoalProgress
import com.nth.thechatptit.data.model.GoalReminder
import com.nth.thechatptit.data.model.WorkoutType
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val database: AppDatabase,
    private val workoutRepository: WorkoutRepository
) {
    suspend fun getActiveGoals(userId: String?): List<Goal> =
        database.goalDao().getActiveForUser(userId).map { it.toDomain() }

    suspend fun getGoal(id: String): Goal? = database.goalDao().getById(id)?.toDomain()

    suspend fun save(goal: Goal) = database.goalDao().upsert(goal.toEntity())

    suspend fun delete(id: String) = database.goalDao().deleteById(id)

    suspend fun claimUnownedForUser(userId: String) =
        database.goalDao().claimUnownedForUser(userId)

    suspend fun getProgress(goal: Goal, nowEpochMillis: Long = System.currentTimeMillis()): GoalProgress {
        val bounds = periodBounds(goal.period, nowEpochMillis)
        val startDate = localDayStart(goal.startDateEpochDay)
        val effectiveStart = maxOf(bounds.first, startDate)
        val endDate = goal.endDateEpochDay?.let { localDayStart(it + 1L) } ?: bounds.second
        val effectiveEnd = minOf(bounds.second, endDate, nowEpochMillis + 1L)
        val statistics = if (effectiveEnd > effectiveStart) {
            workoutRepository.getStatistics(
                fromEpochMillis = effectiveStart,
                toEpochMillis = effectiveEnd,
                workoutType = goal.workoutType,
                userId = goal.ownerUserId
            )
        } else null
        val currentValue = when (goal.metric) {
            GoalMetric.DISTANCE -> statistics?.totalDistanceMeters ?: 0.0
            GoalMetric.ACTIVE_DURATION -> statistics?.totalActiveDurationMillis?.toDouble() ?: 0.0
            GoalMetric.WORKOUT_COUNT -> statistics?.workoutCount?.toDouble() ?: 0.0
        }
        return GoalProgress(goal.id, currentValue, goal.targetValue)
    }

    private fun localDayStart(epochDay: Long): Long {
        val date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(1970, Calendar.JANUARY, 1, 0, 0, 0)
            add(Calendar.DAY_OF_YEAR, epochDay.toInt())
        }
        val localDate = Calendar.getInstance().apply {
            clear()
            set(
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }
        return localDate.timeInMillis
    }

    private fun periodBounds(period: GoalPeriod, nowEpochMillis: Long): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = nowEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (period) {
                GoalPeriod.DAILY -> Unit
                GoalPeriod.WEEKLY -> {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }
                GoalPeriod.MONTHLY -> set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val end = (start.clone() as Calendar).apply {
            when (period) {
                GoalPeriod.DAILY -> add(Calendar.DAY_OF_MONTH, 1)
                GoalPeriod.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                GoalPeriod.MONTHLY -> add(Calendar.MONTH, 1)
            }
        }
        return start.timeInMillis to end.timeInMillis
    }

    private fun GoalEntity.toDomain() = Goal(
        id = id,
        ownerUserId = ownerUserId,
        title = title,
        metric = enumValueOr(metric, GoalMetric.DISTANCE),
        workoutType = workoutType?.let { enumValueOr(it, WorkoutType.RUNNING) },
        period = enumValueOr(period, GoalPeriod.WEEKLY),
        targetValue = targetValue,
        startDateEpochDay = startDateEpochDay,
        endDateEpochDay = endDateEpochDay,
        reminder = if (reminderMinutesOfDay != null && reminderTimeZoneId != null) {
            GoalReminder(
                minutesOfDay = reminderMinutesOfDay,
                timeZoneId = reminderTimeZoneId,
                daysOfWeekMask = reminderDaysOfWeekMask ?: 0b0010101
            )
        } else null,
        isActive = isActive,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )

    private fun Goal.toEntity() = GoalEntity(
        id = id,
        ownerUserId = ownerUserId,
        title = title,
        metric = metric.name,
        workoutType = workoutType?.name,
        period = period.name,
        targetValue = targetValue,
        startDateEpochDay = startDateEpochDay,
        endDateEpochDay = endDateEpochDay,
        reminderMinutesOfDay = reminder?.minutesOfDay,
        reminderTimeZoneId = reminder?.timeZoneId,
        reminderDaysOfWeekMask = reminder?.daysOfWeekMask,
        isActive = isActive,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )

    private inline fun <reified T : Enum<T>> enumValueOr(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
}
