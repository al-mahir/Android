package com.example.mushaf.data.recite.audio

import android.os.Process
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * The one thread `AudioRecord.read()` is allowed to block on.
 *
 * Capture used to run on [kotlinx.coroutines.Dispatchers.IO] - a shared, default-priority pool that
 * every other bit of network and disk work in the app also queues on. That is fine while the device
 * is idle and bad exactly when it is not: a late `read()` does not merely delay one frame, it
 * delays every frame behind it, and the server cannot detect a waqf in audio it has not received.
 * The reciter's pause then appears to the server's VAD to have happened later than it did, and the
 * highlight lands late.
 *
 * One thread, `THREAD_PRIORITY_URGENT_AUDIO`, nothing else on it. The priority is set from inside
 * the thread body because [Process.setThreadPriority] with no tid argument applies to the calling
 * thread, and it is the Linux nice value that actually matters here - `Thread.priority` alone is
 * advisory on Android and does not reach the scheduler.
 *
 * A single shared instance rather than one per session: recording is exclusive anyway (one
 * microphone), and rebuilding a thread on every mic tap would put thread creation on the critical
 * path this exists to shorten.
 */
internal object AudioCaptureDispatcher {

    val instance: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runnable.run()
        }, THREAD_NAME)
    }.asCoroutineDispatcher()

    private const val THREAD_NAME = "recitation-mic"
}
