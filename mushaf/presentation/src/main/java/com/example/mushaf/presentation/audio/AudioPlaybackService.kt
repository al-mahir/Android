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
        mediaSession = MediaSession.Builder(this, forwardingPlayer).build()

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
                builder.setColor(android.graphics.Color.parseColor("#014F39"))
                
                return androidx.media3.session.MediaNotification(notification.notificationId, builder.build())
            }
            override fun handleCustomCommand(session: MediaSession, action: String, extras: android.os.Bundle): Boolean {
                return notificationProvider.handleCustomCommand(session, action, extras)
            }
        })
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
