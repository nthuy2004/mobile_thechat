package com.nth.thechatptit.data.model

/** Trang thai dang nhap dung chung trong toan ung dung. */
sealed interface AuthState {
    /** Ung dung dang doc va khoi phuc phien dang nhap da luu. */
    data object Restoring : AuthState

    /** Khong co phien dang nhap hop le. */
    data object LoggedOut : AuthState

    /** Da dang nhap va co thong tin nguoi dung hien tai. */
    data class LoggedIn(
        /** Tai khoan dang su dung ung dung. */
        val user: User
    ) : AuthState
}
