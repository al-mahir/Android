package com.example.mushaf.presentation.audio

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class AudioPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build()
        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(exoPlayer) {
            override fun seekToNext() {
                val command = androidx.media3.session.SessionCommand("ACTION_NEXT_SURAH", android.os.Bundle.EMPTY)
                mediaSession?.broadcastCustomCommand(command, android.os.Bundle.EMPTY)
            }
            override fun seekToPrevious() {
                val command = androidx.media3.session.SessionCommand("ACTION_PREV_SURAH", android.os.Bundle.EMPTY)
                mediaSession?.broadcastCustomCommand(command, android.os.Bundle.EMPTY)
            }
        }
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            action = "ACTION_OPEN_MUSHAF_LISTEN"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = intent?.let {
            android.app.PendingIntent.getActivity(this, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        }
        
        val builder = MediaSession.Builder(this, forwardingPlayer)
        if (pendingIntent != null) builder.setSessionActivity(pendingIntent)
        mediaSession = builder.build()

        val notificationProvider = androidx.media3.session.DefaultMediaNotificationProvider(this).apply {
            setSmallIcon(com.example.designsystem.R.drawable.ic_book_unselected)
        }
        setMediaNotificationProvider(object : androidx.media3.session.MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
                onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
            ): androidx.media3.session.MediaNotification {
                val notification = notificationProvider.createNotification(session, customLayout, actionFactory, onNotificationChangedCallback)
                
                val builder = androidx.core.app.NotificationCompat.Builder(this@AudioPlaybackService, notification.notification)
                builder.setColor(androidx.core.content.ContextCompat.getColor(this@AudioPlaybackService, com.example.designsystem.R.color.primary_brand))
                builder.setColorized(true)
                
                return androidx.media3.session.MediaNotification(notification.notificationId, builder.build())
            }
            override fun handleCustomCommand(session: MediaSession, action: String, extras: android.os.Bundle): Boolean {
                return notificationProvider.handleCustomCommand(session, action, extras)
            }
        })
    }

    /**
     * On Android 12+, `Context.startForegroundService()` requires `startForeground()` to be
     * called within ~5s or the system kills the process with a `RemoteServiceException`. Media3
     * calls `startForeground()` for us reactively once the player has something to actually play
     * — but the OS can start this service (e.g. a Bluetooth/media-button "play" event trying to
     * resume a session after the process was killed) with no media loaded at all, so that never
     * happens. Bail out immediately in that case instead of leaving the service started-but-not-
     * foregrounded until the OS's timeout fires.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (mediaSession?.player?.mediaItemCount == 0) {
            stopSelf()
        }
        return result
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}
