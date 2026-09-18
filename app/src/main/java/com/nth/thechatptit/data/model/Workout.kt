package com.nth.thechatptit.data.model

/** Loai hinh van dong duoc ung dung ho tro. */
enum class WorkoutType {
    RUNNING,
    CYCLING
}

/** Trang thai duoc luu ben vung cua mot buoi tap. */
enum class WorkoutStatus {
    RECORDING,
    PAUSED,
    COMPLETED
}

/** Pham vi nguoi co the xem buoi tap hoac bai chia se. */
enum class WorkoutVisibility {
    PRIVATE,
    FOLLOWERS,
    PUBLIC
}

/**
 * Mot buoi chay bo hoac dap xe.
 *
 * Cac dai luong duoc luu theo don vi goc de tranh sai so khi doi don vi tren UI.
 */
data class Workout(
    /** ID duy nhat, nen tao bang UUID ngay khi bat dau record. */
    val id: String,

    /** ID chu so huu; null khi record o che do chua dang nhap. */
    val ownerUserId: String? = null,

    /** Loai hinh van dong cua buoi tap. */
    val type: WorkoutType,

    /** Trang thai hien tai cua buoi tap. */
    val status: WorkoutStatus,

    /** Ten tuy chon de nguoi dung de nhan biet buoi tap. */
    val title: String? = null,

    /** Thoi diem bat dau, tinh bang epoch millisecond. */
    val startedAtEpochMillis: Long,

    /** Thoi diem ket thuc; null neu buoi tap van dang dien ra. */
    val finishedAtEpochMillis: Long? = null,

    /** Tong thoi gian thuc su van dong, khong tinh luc pause. */
    val activeDurationMillis: Long = 0L,

    /** Tong thoi gian tam dung cua buoi tap. */
    val pausedDurationMillis: Long = 0L,

    /** Tong quang duong hop le, luu bang met. */
    val distanceMeters: Double = 0.0,

    /** Quyen rieng tu cua buoi tap. */
    val visibility: WorkoutVisibility = WorkoutVisibility.PRIVATE,

    /** Trang thai dong bo cua buoi tap voi server. */
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,

    /** Thoi diem model duoc cap nhat gan nhat, dung cho luu tru va dong bo. */
    val updatedAtEpochMillis: Long
) {
    /** Tong thoi gian tu start den stop, bao gom ca khoang pause. */
    val elapsedDurationMillis: Long
        get() = activeDurationMillis + pausedDurationMillis

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

/**
 * Mot doan di chuyen lien tuc cua workout.
 * Moi lan resume tao mot segment moi de khong noi sai tuyen duong qua khoang pause.
 */
data class WorkoutSegment(
    /** ID duy nhat cua segment. */
    val id: String,

    /** ID workout chua segment nay. */
    val workoutId: String,

    /** Thu tu segment trong workout, bat dau tu 0. */
    val sequence: Int,

    /** Thoi diem segment bat dau, tinh bang epoch millisecond. */
    val startedAtEpochMillis: Long,

    /** Thoi diem segment ket thuc; null neu segment dang record. */
    val finishedAtEpochMillis: Long? = null,

    /** Thoi gian van dong cua rieng segment nay. */
    val durationMillis: Long = 0L,

    /** Quang duong cua rieng segment nay, tinh bang met. */
    val distanceMeters: Double = 0.0
)
