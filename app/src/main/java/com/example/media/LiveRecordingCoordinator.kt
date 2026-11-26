package com.example.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.camera.video.Recording
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

private const val TAG = "LiveRecordingCoord"

/**
 * Manages optional silent local recording of a simulated livestream into app-private storage.
 * Zero UI indicators are displayed while live; upon stream completion the finalized recording
 * path is attached to the saved StreamSessionEntity in History.
 */
object LiveRecordingCoordinator {
    private val lock = Any()
    private var activeSessionId: String? = null
    private var segmentCounter: Int = 0
    private var activeCameraRecording: Recording? = null
    private var activeCameraFile: File? = null
    private var finalizeDeferred: CompletableDeferred<String?>? = null
    private var audioRecorder: MediaRecorder? = null
    private var audioOutputFile: File? = null
    private val completedFiles = mutableListOf<File>()

    fun beginSession(sessionId: String) {
        synchronized(lock) {
            releaseAllLocked()
            activeSessionId = sessionId
            segmentCounter = 0
            completedFiles.clear()
        }
    }

    fun prepareCameraSegmentFile(context: Context, sessionId: String): File {
        synchronized(lock) {
            if (activeSessionId != sessionId) {
                activeSessionId = sessionId
                segmentCounter = 0
                completedFiles.clear()
            }
            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
            segmentCounter++
            val suffix = if (segmentCounter == 1) "" else "_$segmentCounter"
            return File(dir, "famelife_${sessionId}${suffix}.mp4")
        }
    }

    fun registerCameraRecording(recording: Recording, file: File) {
        synchronized(lock) {
            activeCameraRecording = recording
            activeCameraFile = file
            finalizeDeferred = CompletableDeferred()
        }
    }

    fun onCameraRecordingFinalized(file: File, hasError: Boolean) {
        val deferredToComplete: CompletableDeferred<String?>?
        synchronized(lock) {
            if (file.exists() && file.length() > 0L && (!hasError || file.length() > 4096L)) {
                completedFiles.add(file)
            }
            if (activeCameraFile == file) {
                activeCameraRecording = null
            }
            deferredToComplete = finalizeDeferred
        }
        val validPath = file.takeIf { it.exists() && it.length() > 0L }?.absolutePath
        deferredToComplete?.complete(validPath)
    }

    fun startAudioOnlyRecording(context: Context, sessionId: String) {
        synchronized(lock) {
            if (activeCameraRecording != null || audioRecorder != null) return
            try {
                val dir = File(context.filesDir, "recordings").apply { mkdirs() }
                val outFile = File(dir, "famelife_${sessionId}_audio.mp4")
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(96_000)
                    setAudioSamplingRate(44_100)
                    setOutputFile(outFile.absolutePath)
                    prepare()
                    start()
                }
                audioRecorder = recorder
                audioOutputFile = outFile
            } catch (e: Exception) {
                Log.w(TAG, "Silent audio-only recording fallback failed", e)
                runCatching { audioRecorder?.release() }
                audioRecorder = null
                audioOutputFile = null
            }
        }
    }

    suspend fun stopAndAwaitFinalPath(timeoutMs: Long = 2_200L): String? {
        val cameraRec: Recording?
        val camDeferred: CompletableDeferred<String?>?
        synchronized(lock) {
            cameraRec = activeCameraRecording
            camDeferred = finalizeDeferred
            stopAudioRecorderLocked()
        }

        if (cameraRec != null) {
            runCatching { cameraRec.stop() }
        }
        if (camDeferred != null && !camDeferred.isCompleted) {
            withTimeoutOrNull(timeoutMs) {
                camDeferred.await()
            }
        }

        return synchronized(lock) {
            val best = completedFiles
                .filter { it.exists() && it.length() > 0L }
                .maxByOrNull { it.length() }
            val result = best?.absolutePath
            activeCameraRecording = null
            activeCameraFile = null
            finalizeDeferred = null
            activeSessionId = null
            completedFiles.clear()
            result
        }
    }

    fun deleteRecordingFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }

    fun deleteAllRecordings(context: Context) {
        synchronized(lock) {
            releaseAllLocked()
        }
        runCatching {
            val dir = File(context.filesDir, "recordings")
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun stopAudioRecorderLocked() {
        val recorder = audioRecorder
        val file = audioOutputFile
        audioRecorder = null
        audioOutputFile = null
        if (recorder != null) {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
            if (file != null && file.exists() && file.length() > 0L) {
                completedFiles.add(file)
            }
        }
    }

    private fun releaseAllLocked() {
        runCatching { activeCameraRecording?.stop() }
        activeCameraRecording = null
        activeCameraFile = null
        finalizeDeferred?.complete(null)
        finalizeDeferred = null
        stopAudioRecorderLocked()
    }
}
