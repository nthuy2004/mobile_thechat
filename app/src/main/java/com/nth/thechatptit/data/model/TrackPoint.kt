package com.nth.thechatptit.data.model

/** Mot diem GPS da duoc chap nhan trong tuyen duong. */
data class TrackPoint(
    /** ID workout so huu diem GPS nay. */
    val workoutId: String,

    /** ID segment so huu diem, giup tach tuyen duong khi pause/resume. */
    val segmentId: String,

    /** Thu tu diem trong toan bo workout, dung de ve dung chieu tuyen duong. */
    val sequence: Long,

    /** Vi do theo he toa do WGS84. */
    val latitude: Double,

    /** Kinh do theo he toa do WGS84. */
    val longitude: Double,

    /** Do cao so voi muc nuoc bien, tinh bang met; null neu thiet bi khong cung cap. */
    val altitudeMeters: Double? = null,

    /** Ban kinh sai so uoc tinh cua vi tri, tinh bang met. */
    val accuracyMeters: Float,

    /** Toc do tuc thoi theo met/giay; null neu vi tri khong co du lieu toc do. */
    val speedMetersPerSecond: Float? = null,

    /** Thoi diem lay mau GPS, tinh bang epoch millisecond. */
    val capturedAtEpochMillis: Long
)
