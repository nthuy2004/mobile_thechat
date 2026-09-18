package com.nth.thechatptit.data.model

/** Don vi quang duong duoc hien thi tren giao dien. */
enum class DistanceUnit {
    KILOMETERS,
    MILES
}

/** Che do mau cua ung dung. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** Cac tuy chon nho duoc luu trong DataStore. */
data class AppSettings(
    /** Don vi dung de hien thi quang duong va toc do. */
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,

    /** Theme sang, toi hoac theo cai dat he thong. */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    /** Loai workout duoc chon san khi mo man hinh record. */
    val defaultWorkoutType: WorkoutType = WorkoutType.RUNNING,

    /** Cho phep tu dong pause khi phat hien nguoi dung dung yen. */
    val autoPauseEnabled: Boolean = false,

    /** Cho phep gui notification nhac muc tieu. */
    val goalNotificationsEnabled: Boolean = true
)
