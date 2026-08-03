package com.iti.sheikh.presentation.availability.service

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
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.availability.AvailabilityUiState
import com.iti.sheikh.presentation.availability.SheikhAvailabilityController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

/**
 * Keeps the sheikh's availability listening alive regardless of screen/background state — the
 * Uber-style "you're online" counterpart to [com.iti.meeting.presentation.call.session
 * .CallForegroundService], which does the same for an active call. Shows a persistent, silent
 * "You're Online" notification (deactivatable via its own action) plus, whenever
 * [SheikhAvailabilityController.state] is [AvailabilityUiState.IncomingRequest], a separate loud
 * ringing notification with Accept/Decline actions.
 */
@Suppress("InlinedApi")
class SheikhAvailabilityForegroundService : Service() {

    private val controller: SheikhAvailabilityController by inject()
    private val ringer: IncomingRequestRinger by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var ringingRequestId: String? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannels()
        ServiceCompat.startForeground(
            this,
            ONLINE_NOTIFICATION_ID,
            buildOnlineNotification(busy = false),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        observeJob = controller.state
            .onEach { state ->
                when (state) {
                    AvailabilityUiState.Offline -> {
                        dismissRinging()
                        stopSelfGracefully()
                    }
                    AvailabilityUiState.Available -> {
                        dismissRinging()
                        notifySafely(ONLINE_NOTIFICATION_ID, buildOnlineNotification(busy = false))
                    }
                    is AvailabilityUiState.IncomingRequest -> showRingingNotification(state)
                    is AvailabilityUiState.Busy -> {
                        dismissRinging()
                        notifySafely(ONLINE_NOTIFICATION_ID, buildOnlineNotification(busy = true))
                    }
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_GO_OFFLINE -> controller.goOffline()
            ACTION_DECLINE -> intent.getStringExtra(EXTRA_REQUEST_ID)?.let { controller.decline(it) }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        ringer.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopSelfGracefully() {
        observeJob?.cancel()
        ringer.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(id: Int, notification: Notification) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try {
                NotificationManagerCompat.from(this).notify(id, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "notifySafely: notify() rejected by the system", e)
            }
        } else {
            Log.w(TAG, "notifySafely: POST_NOTIFICATIONS not granted, skipping notification update")
        }
    }

    private fun showRingingNotification(request: AvailabilityUiState.IncomingRequest) {
        if (ringingRequestId != request.requestId) {
            ringingRequestId = request.requestId
            ringer.start()
        }
        notifySafely(RINGING_NOTIFICATION_ID, buildRingingNotification(request))
    }

    private fun dismissRinging() {
        if (ringingRequestId == null) return
        ringingRequestId = null
        ringer.stop()
        NotificationManagerCompat.from(this).cancel(RINGING_NOTIFICATION_ID)
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        if (manager.getNotificationChannel(ONLINE_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ONLINE_CHANNEL_ID,
                    getString(R.string.sheikh_availability_notification_channel_online_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setSound(null, null) },
            )
        }

        if (manager.getNotificationChannel(RINGING_CHANNEL_ID) == null) {
            val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            manager.createNotificationChannel(
                NotificationChannel(
                    RINGING_CHANNEL_ID,
                    getString(R.string.sheikh_availability_notification_channel_ringing_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    enableVibration(true)
                    if (ringtoneUri != null) {
                        setSound(
                            ringtoneUri,
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                    }
                },
            )
        }
    }

    private fun buildOnlineNotification(busy: Boolean): Notification {
        val goOfflineAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification_online,
            getString(R.string.sheikh_availability_notification_go_offline),
            servicePendingIntent(ACTION_GO_OFFLINE, requestCode = 1),
        ).build()

        return NotificationCompat.Builder(this, ONLINE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_online)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent())
            .setContentTitle(getString(R.string.sheikh_availability_notification_online_title))
            .setContentText(
                getString(
                    if (busy) R.string.sheikh_availability_notification_online_busy_subtitle
                    else R.string.sheikh_availability_notification_online_subtitle,
                ),
            )
            .addAction(goOfflineAction)
            .build()
    }

    private fun buildRingingNotification(request: AvailabilityUiState.IncomingRequest): Notification {
        val person = Person.Builder().setName(request.studentName).build()
        val acceptIntent = acceptActivityPendingIntent()
        val declineIntent = declineServicePendingIntent(request.requestId)

        val openAppIntent = openAppPendingIntent()
        return NotificationCompat.Builder(this, RINGING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_ringing)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            // Without this, re-posting/updating this notification (e.g. the state collector
            // re-emitting for the same pending request) re-triggers the channel's alert sound and
            // heads-up presentation on every single `notify()` call, not just the first — the
            // "ringing plays twice" symptom.
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setFullScreenIntent(openAppIntent, true)
            .setContentTitle(getString(R.string.sheikh_availability_notification_incoming_title, request.studentName))
            .setContentText(request.note.ifBlank { getString(R.string.sheikh_availability_notification_incoming_text_generic) })
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, declineIntent, acceptIntent))
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int, requestId: String? = null): PendingIntent {
        val intent = Intent(this, SheikhAvailabilityForegroundService::class.java).setAction(action)
        if (requestId != null) intent.putExtra(EXTRA_REQUEST_ID, requestId)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun declineServicePendingIntent(requestId: String): PendingIntent =
        servicePendingIntent(ACTION_DECLINE, requestCode = 3, requestId = requestId)

    /** Trampoline into the host app so an accept tapped while backgrounded/killed still lands in
     * `CallScreen` — same `packageManager.getLaunchIntentForPackage` trick `CallForegroundService`
     * already uses, since `:sheikh:presentation` can't reference `sheikh-app`'s `MainActivity`
     * directly. Carries only the action string, not the requestId — the nav host reads the current
     * request off [SheikhAvailabilityController.state], the same "intent is just a trigger, the
     * real data comes from live singleton state" idiom `ACTION_OPEN_ACTIVE_CALL` already uses. */
    private fun acceptActivityPendingIntent(): PendingIntent {
        val intent = launchIntent().apply {
            action = ACTION_ACCEPT_INCOMING_REQUEST
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = launchIntent().apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Reuses the OS launcher-resolution `packageManager` already does, with an equivalent
     * explicit fallback for the (practically unreachable, since our own manifest always declares
     * a LAUNCHER activity) case where the lookup itself returns null. */
    private fun launchIntent(): Intent =
        packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)

    companion object {
        const val ACTION_ACCEPT_INCOMING_REQUEST = "ACTION_ACCEPT_INCOMING_REQUEST"

        private const val EXTRA_REQUEST_ID = "com.iti.sheikh.presentation.availability.service.EXTRA_REQUEST_ID"
        private const val ACTION_GO_OFFLINE = "com.iti.sheikh.presentation.availability.service.ACTION_GO_OFFLINE"
        private const val ACTION_DECLINE = "com.iti.sheikh.presentation.availability.service.ACTION_DECLINE"
        private const val ONLINE_CHANNEL_ID = "sheikh_availability_online"
        private const val RINGING_CHANNEL_ID = "sheikh_availability_ringing"
        private const val ONLINE_NOTIFICATION_ID = 4301
        private const val RINGING_NOTIFICATION_ID = 4302
        private const val TAG = "SheikhAvailabilityForegroundService"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, SheikhAvailabilityForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SheikhAvailabilityForegroundService::class.java))
        }
    }
}
