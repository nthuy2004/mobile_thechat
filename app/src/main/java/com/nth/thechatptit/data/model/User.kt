package com.nth.thechatptit.data.model

/** Thong tin tai khoan can dung trong ung dung. */
data class User(
    /** ID duy nhat cua nguoi dung, do server tao. */
    val id: String,

    /** Email dung de dang nhap va quan ly tai khoan. */
    val email: String,

    /** Ten hien thi tren trang ca nhan va bai dang. */
    val displayName: String,

    /** Dia chi anh dai dien; null khi nguoi dung chua dat anh. */
    val avatarUrl: String? = null,

    /** Gioi thieu ngan tren trang ca nhan. */
    val bio: String? = null,

    /** Thoi diem tai khoan duoc tao, tinh bang epoch millisecond. */
    val createdAtEpochMillis: Long,

    /** Thoi diem thong tin tai khoan duoc cap nhat gan nhat. */
    val updatedAtEpochMillis: Long
)
