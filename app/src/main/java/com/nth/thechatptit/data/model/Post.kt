package com.nth.thechatptit.data.model

/** Bai chia se duoc tao tu mot workout da hoan thanh. */
data class Post(
    /** ID duy nhat cua bai dang. */
    val id: String,

    /** ID nguoi tao bai dang. */
    val authorUserId: String?,

    /** ID workout duoc chia se trong bai dang. */
    val workoutId: String,

    /** Noi dung mo ta tuy chon cua bai dang. */
    val caption: String? = null,

    /** URL anh dinh kem hoac anh preview tuyen duong; null neu khong co anh. */
    val imageUrl: String? = null,

    /** Pham vi nguoi co the xem bai dang. */
    val visibility: WorkoutVisibility = WorkoutVisibility.PUBLIC,

    /** Trang thai dong bo cua bai dang voi server. */
    val syncStatus: SyncStatus = SyncStatus.PENDING,

    /** Thoi diem bai dang duoc tao, tinh bang epoch millisecond. */
    val createdAtEpochMillis: Long,

    /** Thoi diem noi dung bai dang duoc cap nhat gan nhat. */
    val updatedAtEpochMillis: Long
)
