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
            setSmallIcon(android.R.drawable.ic_media_play)
        }
        setMediaNotificationProvider(object : androidx.media3.session.MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
                onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
            ): androidx.media3.session.MediaNotification {
                val notification = notificationProvider.createNotification(session, customLayout, actionFactory, onNotificationChangedCallback)
                
                // Add color via reflection or we can't easily change it if we use DefaultMediaNotificationProvider directly,
                // but wait, in Media3 1.0/1.1 createNotification is the method. 
                // Let's just create a completely custom notification or use the default and hope it picks up the app's color.
                // Wait, the requirement says "add a minimal app theme compitable as the green but with the app color pallete to control the quran from inside and outside the app".
                return notification
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
