package com.nth.thechatptit.data.model

/** Trang thai runtime cua bo may tracking. */
enum class TrackingStatus {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
    STOPPING,
    ERROR
}

/** Chat luong hoac kha nang su dung GPS hien tai. */
enum class GpsStatus {
    UNKNOWN,
    SEARCHING,
    GOOD,
    WEAK,
    DISABLED,
    PERMISSION_MISSING
}

/**
 * Snapshot realtime do TrackingService cap nhat va UI quan sat qua StateFlow.
 * Model nay chi ton tai trong RAM; Workout va TrackPoint moi la du lieu luu ben vung.
 */
data class TrackingState(
    /** Trang thai hien tai cua bo may tracking. */
    val status: TrackingStatus = TrackingStatus.IDLE,

    /** ID workout dang record; null khi dang IDLE. */
    val workoutId: String? = null,

    /** ID segment dang record; null khi chua co segment hoat dong. */
    val activeSegmentId: String? = null,

    /** Loai workout dang record; null khi dang IDLE. */
    val workoutType: WorkoutType? = null,

    /** Thoi gian van dong hien tai, khong tinh luc pause. */
    val activeDurationMillis: Long = 0L,

    /** Quang duong da chap nhan, tinh bang met. */
    val distanceMeters: Double = 0.0,

    /** Diem GPS hop le gan nhat; null khi chua bat duoc vi tri. */
    val currentPoint: TrackPoint? = null,

    /** Trang thai tin hieu GPS de UI hien thi cho nguoi dung. */
    val gpsStatus: GpsStatus = GpsStatus.UNKNOWN,

    /** Noi dung loi gan nhat; chi co gia tri khi status la ERROR. */
    val errorMessage: String? = null
) {
    /** Toc do trung binh theo met/giay. */
    val averageSpeedMetersPerSecond: Double
        get() = if (activeDurationMillis > 0L) {
            distanceMeters / (activeDurationMillis / 1_000.0)
        } else {
            0.0
        }

    /** Nhip do trung binh theo millisecond/km; null khi chua co quang duong. */
    val averagePaceMillisPerKilometer: Long?
        get() = if (distanceMeters > 0.0) {
            (activeDurationMillis / (distanceMeters / 1_000.0)).toLong()
        } else {
            null
        }
}
