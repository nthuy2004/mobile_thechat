package com.nth.thechatptit.data.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nth.thechatptit.R
import com.nth.thechatptit.data.model.GpsStatus
import com.nth.thechatptit.data.model.TrackingStatus
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {
    @Inject lateinit var repository: TrackingRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var timerJob: Job? = null
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                serviceScope.launch {
                    repository.acceptLocation(location)
                    updateNotification()
                }
            }
        }
    }
    private val locationProviderReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != LocationManager.PROVIDERS_CHANGED_ACTION) return
            val enabled = isLocationEnabled()
            if (!enabled) {
                stopLocationUpdates()
                repository.setGpsStatus(GpsStatus.DISABLED)
            } else if (repository.state.value.status == TrackingStatus.RECORDING) {
                requestUpdates()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ContextCompat.registerReceiver(
            this,
            locationProviderReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val type = intent.getStringExtra(EXTRA_TYPE)
                    ?.let { runCatching { WorkoutType.valueOf(it) }.getOrNull() }
                    ?: WorkoutType.RUNNING
                startAsForeground()
                serviceScope.launch {
                    repository.start(type)
                    withContext(Dispatchers.Main.immediate) { requestUpdates() }
                    startTimer()
                    updateNotification()
                }
            }
            ACTION_PAUSE -> serviceScope.launch {
                repository.restore()
                repository.pause()
                withContext(Dispatchers.Main.immediate) { stopLocationUpdates() }
                timerJob?.cancel()
                updateNotification()
            }
            ACTION_RESUME -> serviceScope.launch {
                repository.restore()
                repository.resume()
                withContext(Dispatchers.Main.immediate) { requestUpdates() }
                startTimer()
                updateNotification()
            }
            ACTION_RETRY_LOCATION -> serviceScope.launch {
                repository.restore()
                if (repository.state.value.status == TrackingStatus.RECORDING) {
                    withContext(Dispatchers.Main.immediate) { requestUpdates() }
                    updateNotification()
                }
            }
            ACTION_STOP -> serviceScope.launch {
                repository.restore()
                withContext(Dispatchers.Main.immediate) { stopLocationUpdates() }
                timerJob?.cancel()
                repository.finish()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> serviceScope.launch {
                repository.restore()
                if (repository.state.value.status != TrackingStatus.IDLE) {
                    withContext(Dispatchers.Main.immediate) { startAsForeground() }
                    if (repository.state.value.status == TrackingStatus.RECORDING) {
                        withContext(Dispatchers.Main.immediate) { requestUpdates() }
                        startTimer()
                    }
                    updateNotification()
                }
            }
        }
        return START_STICKY
    }

    private fun requestUpdates() {
        repository.setGpsStatus(GpsStatus.SEARCHING)
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            repository.setGpsStatus(GpsStatus.PERMISSION_MISSING)
            return
        }
        val enabled = isLocationEnabled()
        if (!enabled) {
            repository.setGpsStatus(GpsStatus.DISABLED)
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(3f)
            .build()
        locationClient.requestLocationUpdates(request, callback, mainLooper)
            .addOnFailureListener { error ->
                repository.setGpsStatus(
                    if (error is SecurityException) GpsStatus.PERMISSION_MISSING else GpsStatus.DISABLED
                )
            }
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(callback)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LocationManager::class.java)
        return runCatching {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }.getOrDefault(false)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                repository.updateDuration()
                if (repository.state.value.status == TrackingStatus.PAUSED) {
                    withContext(Dispatchers.Main.immediate) { stopLocationUpdates() }
                    updateNotification()
                    break
                }
                updateNotification()
                delay(1_000L)
            }
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val state = repository.state.value
        val distance = String.format("%.2f km", state.distanceMeters / 1_000.0)
        val action = when (state.status) {
            TrackingStatus.PAUSED -> ACTION_RESUME
            TrackingStatus.RECORDING -> ACTION_PAUSE
            else -> null
        }
        val label = if (state.status == TrackingStatus.PAUSED) "Tiếp tục" else "Tạm dừng"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_feed)
            .setContentTitle("KaiWalker đang ghi hoạt động")
            .setContentText("${state.status.name} · $distance")
            .setContentIntent(openAppIntent())
            .setOngoing(state.status != TrackingStatus.IDLE)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .apply {
                if (action != null) addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_feed,
                        label,
                        serviceAction(action)
                    ).build()
                )
                if (state.status != TrackingStatus.IDLE) addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_feed,
                        "Kết thúc",
                        serviceAction(ACTION_STOP)
                    ).build()
                )
            }
            .build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        2001,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun serviceAction(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, TrackingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Tracking", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        unregisterReceiver(locationProviderReceiver)
        stopLocationUpdates()
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (repository.state.value.status != TrackingStatus.IDLE) {
            startAsForeground()
        }
    }

    companion object {
        const val ACTION_START = "com.nth.thechatptit.tracking.START"
        const val ACTION_PAUSE = "com.nth.thechatptit.tracking.PAUSE"
        const val ACTION_RESUME = "com.nth.thechatptit.tracking.RESUME"
        const val ACTION_RETRY_LOCATION = "com.nth.thechatptit.tracking.RETRY_LOCATION"
        const val ACTION_STOP = "com.nth.thechatptit.tracking.STOP"
        const val EXTRA_TYPE = "workout_type"
        private const val CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 1001
    }
}
