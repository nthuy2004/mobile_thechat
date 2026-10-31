package com.nth.thechatptit.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressTest {
    @Test
    fun percentageIsClampedAndCompletionReflectsTarget() {
        val progress = GoalProgress(goalId = "weekly", currentValue = 12.0, targetValue = 10.0)

        assertEquals(100f, progress.percentage)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun nonPositiveTargetCannotBeCompleted() {
        val progress = GoalProgress(goalId = "invalid", currentValue = 5.0, targetValue = 0.0)

        assertEquals(0f, progress.percentage)
        assertFalse(progress.isCompleted)
    }
}
