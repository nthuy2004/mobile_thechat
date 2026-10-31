package com.nth.thechatptit.data.model

/** Dai luong ma nguoi dung muon dat duoc. */
enum class GoalMetric {
    DISTANCE,
    ACTIVE_DURATION,
    WORKOUT_COUNT
}

/** Chu ky lap lai cua muc tieu. */
enum class GoalPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

/** Cau hinh thong bao nhac thuc hien muc tieu. */
data class GoalReminder(
    /** So phut tinh tu 00:00; vi du 390 tuong ung 06:30. */
    val minutesOfDay: Int,

    /** Mui gio IANA, vi du Asia/Bangkok, dung de dat lich dung gio dia phuong. */
    val timeZoneId: String,

    /** Bit 0..6 lan luot dai dien thu Hai den Chu Nhat. */
    val daysOfWeekMask: Int = 0b0010101
)

/** Muc tieu van dong cua nguoi dung. */
data class Goal(
    /** ID duy nhat cua muc tieu. */
    val id: String,

    /** ID nguoi so huu; null neu muc tieu duoc tao o che do chua dang nhap. */
    val ownerUserId: String? = null,

    /** Ten ngan gon do nguoi dung dat cho muc tieu. */
    val title: String,

    /** Dai luong can dat: quang duong, thoi gian hoac so buoi tap. */
    val metric: GoalMetric,

    /** Loai workout ap dung; null nghia la tinh tat ca loai van dong. */
    val workoutType: WorkoutType? = null,

    /** Chu ky tinh lai tien do cua muc tieu. */
    val period: GoalPeriod,

    /** Gia tri dich theo don vi goc: met, millisecond hoac so buoi tuy metric. */
    val targetValue: Double,

    /** Ngay bat dau duoc luu bang epoch day. */
    val startDateEpochDay: Long,

    /** Ngay ket thuc bang epoch day; null neu muc tieu khong co han. */
    val endDateEpochDay: Long? = null,

    /** Cau hinh nhac; null neu nguoi dung tat notification cho muc tieu nay. */
    val reminder: GoalReminder? = null,

    /** Cho biet muc tieu con duoc su dung de tinh tien do hay khong. */
    val isActive: Boolean = true,

    /** Thoi diem muc tieu duoc tao, tinh bang epoch millisecond. */
    val createdAtEpochMillis: Long,

    /** Thoi diem muc tieu duoc chinh sua gan nhat. */
    val updatedAtEpochMillis: Long
)

/** Tien do duoc tinh tu cac workout da hoan thanh, khong phai du lieu goc. */
data class GoalProgress(
    /** ID muc tieu dang duoc thong ke. */
    val goalId: String,

    /** Gia tri da dat trong chu ky hien tai, cung don vi voi targetValue. */
    val currentValue: Double,

    /** Gia tri can dat cua muc tieu. */
    val targetValue: Double
) {
    /** Phan tram hoan thanh, duoc gioi han trong khoang 0 den 100. */
    val percentage: Float
        get() = if (targetValue > 0.0) {
            ((currentValue / targetValue) * 100.0)
                .coerceIn(0.0, 100.0)
                .toFloat()
        } else {
            0f
        }

    /** True khi gia tri hien tai da dat hoac vuot muc tieu. */
    val isCompleted: Boolean
        get() = targetValue > 0.0 && currentValue >= targetValue
}
