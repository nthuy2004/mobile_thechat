package com.nth.thechatptit.data.sync

import com.nth.thechatptit.data.model.GoalReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GoalReminderSchedulerTest {
    @Test
    fun nextOccurrenceUsesSelectedDayAndLocalTime() {
        val now = utcTime(2026, Calendar.JANUARY, 5, 17, 30)
        val reminder = GoalReminder(
            minutesOfDay = 18 * 60 + 15,
            timeZoneId = "UTC",
            daysOfWeekMask = 1 shl 0
        )

        val expected = utcTime(2026, Calendar.JANUARY, 5, 18, 15)
        assertEquals(expected, GoalReminderScheduler.nextOccurrence(reminder, now))
    }

    @Test
    fun passedTimeMovesReminderToNextSelectedWeekday() {
        val now = utcTime(2026, Calendar.JANUARY, 5, 19, 0)
        val reminder = GoalReminder(
            minutesOfDay = 18 * 60,
            timeZoneId = "UTC",
            daysOfWeekMask = 1 shl 0
        )

        val expected = utcTime(2026, Calendar.JANUARY, 12, 18, 0)
        assertEquals(expected, GoalReminderScheduler.nextOccurrence(reminder, now))
    }

    @Test
    fun noSelectedDaysDoesNotScheduleWork() {
        val reminder = GoalReminder(minutesOfDay = 18 * 60, timeZoneId = "UTC", daysOfWeekMask = 0)
        assertNull(GoalReminderScheduler.nextOccurrence(reminder, utcTime(2026, Calendar.JANUARY, 5, 12, 0)))
    }

    private fun utcTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
