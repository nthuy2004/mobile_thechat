package com.nth.thechatptit.ui.common

import com.nth.thechatptit.data.model.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitFormattingTest {
    @Test
    fun milesUseMetersToMilesConversion() {
        assertEquals("1.00 mi", formatDistance(1_609.344, DistanceUnit.MILES))
        assertEquals("1.00 km", formatDistance(1_000.0, DistanceUnit.KILOMETERS))
    }

    @Test
    fun speedAndPaceUseSelectedUnit() {
        assertEquals("10.0 mph", formatSpeed(4.4704, DistanceUnit.MILES))
        assertEquals("96:33/mi", formatPace(3_600_000L, DistanceUnit.MILES))
    }
}
