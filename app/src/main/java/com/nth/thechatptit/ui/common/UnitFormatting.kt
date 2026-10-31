package com.nth.thechatptit.ui.common

import com.nth.thechatptit.data.model.DistanceUnit
import java.util.Locale

fun distanceInUserUnits(meters: Double, unit: DistanceUnit): Double = when (unit) {
    DistanceUnit.KILOMETERS -> meters / 1_000.0
    DistanceUnit.MILES -> meters / 1_609.344
}

fun distanceUnitLabel(unit: DistanceUnit): String = when (unit) {
    DistanceUnit.KILOMETERS -> "km"
    DistanceUnit.MILES -> "mi"
}

fun formatDistance(meters: Double, unit: DistanceUnit, decimals: Int = 2): String =
    "${String.format(Locale.US, "%.${decimals}f", distanceInUserUnits(meters, unit))} ${distanceUnitLabel(unit)}"

fun formatSpeed(metersPerSecond: Double, unit: DistanceUnit): String {
    val speed = when (unit) {
        DistanceUnit.KILOMETERS -> metersPerSecond * 3.6
        DistanceUnit.MILES -> metersPerSecond * 2.2369362921
    }
    val label = if (unit == DistanceUnit.KILOMETERS) "km/h" else "mph"
    return String.format(Locale.US, "%.1f %s", speed, label)
}

fun formatPace(millisecondsPerKilometer: Long?, unit: DistanceUnit): String {
    val paceUnit = distanceUnitLabel(unit)
    if (millisecondsPerKilometer == null) return "--:--/$paceUnit"
    val millisPerUnit = when (unit) {
        DistanceUnit.KILOMETERS -> millisecondsPerKilometer.toDouble()
        DistanceUnit.MILES -> millisecondsPerKilometer * 1.609344
    }
    val totalSeconds = (millisPerUnit / 1_000.0).toLong()
    return String.format(Locale.US, "%02d:%02d/%s", totalSeconds / 60, totalSeconds % 60, paceUnit)
}
