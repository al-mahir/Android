package com.iti.meeting.presentation.call.session

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.iti.meeting.presentation.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject


@Suppress("InlinedApi")
class CallForegroundService : Service() {

    private val registry: OngoingCallRegistry by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(registry.display.value), type)

        observeJob = registry.display
            .onEach { display ->
                if (display != null) {
                    notifySafely(buildNotification(display))
                } else {
                    stopSelfGracefully()
                }
            }
            .launchIn(scope)
    }

       @SuppressLint("MissingPermission")
    private fun notifySafely(notification: Notification) {
              if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "notifySafely: notify() rejected by the system", e)
            }
        } else {
            Log.w(TAG, "notifySafely: POST_NOTIFICATIONS not granted, skipping notification update")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_MIC -> registry.toggleMic()
            ACTION_END_CALL -> {
                registry.hangUp()
                stopSelfGracefully()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopSelfGracefully() {
        observeJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.meeting_call_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { setSound(null, null) }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(display: OngoingCallDisplay?): Notification {
        val isMuted = display?.isMicEnabled == false
        val title = display?.title
            ?.let { getString(R.string.meeting_call_notification_title_named, it) }
            ?: getString(R.string.meeting_call_notification_title_generic)

        val person = Person.Builder().setName(title).build()

        val muteAction = NotificationCompat.Action.Builder(
            if (isMuted) R.drawable.ic_mic_off_notification else R.drawable.ic_mic_notification,
            getString(if (isMuted) R.string.meeting_call_notification_unmute else R.string.meeting_call_notification_mute),
            servicePendingIntent(ACTION_TOGGLE_MIC, requestCode = 1),
        ).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(openAppPendingIntent())
            .setContentTitle(title)
            .setContentText(
                if (display?.isConnecting == true) getString(R.string.meeting_call_notification_connecting) else null
            )
            .addAction(muteAction)
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    person,
                    servicePendingIntent(ACTION_END_CALL, requestCode = 2),
                )
            )
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, CallForegroundService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Reopens the host app straight into the call — same
     * `packageManager.getLaunchIntentForPackage` trick `AudioPlaybackService` already uses to
     * deep-link without `:meeting:presentation` depending on either host app's `MainActivity`. */
    private fun openAppPendingIntent(): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            action = ACTION_OPEN_ACTIVE_CALL
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: return null
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        const val ACTION_OPEN_ACTIVE_CALL = "ACTION_OPEN_ACTIVE_CALL"

        private const val TAG = "CallForegroundService"
        private const val ACTION_TOGGLE_MIC = "com.iti.meeting.presentation.call.session.ACTION_TOGGLE_MIC"
        private const val ACTION_END_CALL = "com.iti.meeting.presentation.call.session.ACTION_END_CALL"
        private const val CHANNEL_ID = "active_call_v2"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, CallForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallForegroundService::class.java))
        }
    }
}
