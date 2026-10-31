package com.nth.thechatptit.data.model

/** Ket qua tong hop workout trong mot khoang thoi gian. */
data class WorkoutStatistics(
    /** Moc dau khoang thong ke, tinh bang epoch millisecond. */
    val fromEpochMillis: Long,

    /** Moc cuoi khoang thong ke, tinh bang epoch millisecond. */
    val toEpochMillis: Long,

    /** Loai workout dang loc; null nghia la tong hop tat ca. */
    val workoutType: WorkoutType? = null,

    /** So workout da hoan thanh trong khoang thoi gian. */
    val workoutCount: Int,

    /** Tong quang duong cua cac workout, tinh bang met. */
    val totalDistanceMeters: Double,

    /** Tong thoi gian van dong, khong tinh pause. */
    val totalActiveDurationMillis: Long
) {
    /** Toc do trung binh tong hop theo met/giay. */
    val averageSpeedMetersPerSecond: Double
        get() = if (totalActiveDurationMillis > 0L) {
            totalDistanceMeters / (totalActiveDurationMillis / 1_000.0)
        } else {
            0.0
        }

    /** Pace trung binh tong hop theo millisecond/km. */
    val averagePaceMillisPerKilometer: Long?
        get() = if (totalDistanceMeters > 0.0) {
            (totalActiveDurationMillis / (totalDistanceMeters / 1_000.0)).toLong()
        } else {
            null
        }
}

/** One weekday's totals for a compact weekly chart. */
data class DailyWorkoutStatistics(
    /** Calendar.DAY_OF_WEEK value for this bucket. */
    val dayOfWeek: Int,
    val distanceMeters: Double,
    val activeDurationMillis: Long,
    val workoutCount: Int
)
