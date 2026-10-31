package com.nth.thechatptit.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteImageGenerator @Inject constructor() {

    private companion object {
        const val IMAGE_BACKGROUND = 0xFF111111.toInt()
        const val ROUTE_GLOW = 0x46FC4C02
        const val ROUTE_LINE = 0xFFFC4C02.toInt()
        const val START_POINT = 0xFFF5F5F5.toInt()
        const val END_POINT = 0xFF35D07F.toInt()
    }

    fun generate(
        context: Context,
        points: List<Pair<Double, Double>>
    ): String? {
        if (points.size < 2) return null

        val width = 1080
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(IMAGE_BACKGROUND)

        val latitudes = points.map { it.first }
        val longitudes = points.map { it.second }
        val minLat = latitudes.min()
        val maxLat = latitudes.max()
        val minLng = longitudes.min()
        val maxLng = longitudes.max()
        val latRange = ((maxLat - minLat).coerceAtLeast(0.00001)).toFloat()
        val lngRange = ((maxLng - minLng).coerceAtLeast(0.00001)).toFloat()
        val availableWidth = width - 96f
        val availableHeight = height - 96f
        val latScale = availableHeight / latRange.toFloat()
        val lngScale = availableWidth / lngRange.toFloat()
        val scale = minOf(latScale, lngScale)
        val offsetX = (availableWidth - lngRange * scale) / 2f
        val offsetY = (availableHeight - latRange * scale) / 2f

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = 48f + offsetX + ((point.second - minLng).toFloat() / lngRange) * lngRange * scale
            val y = height - 48f - offsetY - ((point.first - minLat).toFloat() / latRange) * latRange * scale
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ROUTE_GLOW
            style = Paint.Style.STROKE
            strokeWidth = 20f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, glowPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ROUTE_LINE
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.BUTT
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, linePaint)

        val startPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = START_POINT }
        val endPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = END_POINT }
        val first = points.first()
        val last = points.last()
        val firstX = 48f + offsetX
        val firstY = height - 48f - offsetY
        val lastX = 48f + offsetX + ((last.second - minLng).toFloat() / lngRange) * lngRange * scale
        val lastY = height - 48f - offsetY - ((last.first - minLat).toFloat() / latRange) * latRange * scale
        canvas.drawCircle(firstX, firstY, 10f, startPaint)
        canvas.drawCircle(lastX, lastY, 10f, endPaint)

        val file = File(context.filesDir, "routes/${UUID.randomUUID()}.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        return file.absolutePath
    }
}
