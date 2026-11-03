package com.ondamic.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ondamic.app.MainActivity
import com.ondamic.app.R

/**
 * Android 14/15/16 compliant Foreground Service (`foregroundServiceType="microphone"`).
 * Keeps the OndaMic audio pipeline alive when the screen turns off during a speech or lecture,
 * and provides a one-tap "STOP MIC" action in the persistent notification.
 */
class OndaMicForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_AMPLIFICATION -> {
                onStopRequestedCallback?.invoke()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                createChannelIfNeeded()
                val notification = buildNotification()
                runCatching {
                    val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    } else {
                        0
                    }
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
                }
                acquirePartialWakeLock()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun acquirePartialWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "OndaMic::LiveMicrophoneWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(4 * 60 * 60 * 1000L) // Max 4 hours safety timeout
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun stopForegroundCompat() {
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun createChannelIfNeeded() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OndaMicForegroundService::class.java).apply {
            action = ACTION_STOP_AMPLIFICATION
        }
        val stopPending = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.notif_live_title))
            .setContentText(getString(R.string.notif_live_body))
            .setContentIntent(contentPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notif_action_stop),
                stopPending
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ondamic_live_mic_channel"
        private const val NOTIFICATION_ID = 2026
        const val ACTION_START_AMPLIFICATION = "com.ondamic.app.action.START"
        const val ACTION_STOP_AMPLIFICATION = "com.ondamic.app.action.STOP"

        var onStopRequestedCallback: (() -> Unit)? = null

        fun startService(context: Context) {
            val intent = Intent(context, OndaMicForegroundService::class.java).apply {
                action = ACTION_START_AMPLIFICATION
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, OndaMicForegroundService::class.java)
            runCatching {
                context.stopService(intent)
            }
        }
    }
}
