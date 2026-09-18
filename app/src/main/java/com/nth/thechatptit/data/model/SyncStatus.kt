package com.nth.thechatptit.data.model

/** Trang thai dong bo cua du lieu local voi server. */
enum class SyncStatus {
    /** Du lieu chi ton tai tren thiet bi, thuong dung khi chua dang nhap. */
    LOCAL_ONLY,

    /** Du lieu dang cho WorkManager gui len server. */
    PENDING,

    /** Du lieu local va server da dong bo thanh cong. */
    SYNCED,

    /** Lan dong bo gan nhat that bai va can thu lai. */
    FAILED
}
