package com.example.media

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.StreamSourceMode
import com.example.ui.theme.FameMint
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.abs

private const val TAG = "CameraStage"

@Composable
fun LiveMediaStage(
    sourceMode: StreamSourceMode,
    hasCameraPermission: Boolean,
    isCameraPaused: Boolean,
    visualUnderstandingEnabled: Boolean,
    screenShareActive: Boolean,
    onRequestCameraPermission: () -> Unit,
    onSceneFrameSampled: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    recordLiveEnabled: Boolean = false,
    recordingSessionId: String = ""
) {
    when {
        sourceMode == StreamSourceMode.AUDIO_ONLY -> {
            AudioOnlyStage(modifier = modifier)
        }
        sourceMode == StreamSourceMode.SCREEN_SHARE -> {
            ScreenShareStage(
                screenShareActive = screenShareActive,
                modifier = modifier
            )
        }
        isCameraPaused -> {
            CameraPausedStage(modifier = modifier)
        }
        !hasCameraPermission -> {
            CameraPermissionDeniedStage(
                onGrantClick = onRequestCameraPermission,
                modifier = modifier
            )
        }
        else -> {
            RealCameraXStage(
                useFrontCamera = sourceMode == StreamSourceMode.FRONT_CAMERA,
                visualUnderstandingEnabled = visualUnderstandingEnabled,
                onSceneFrameSampled = onSceneFrameSampled,
                recordLiveEnabled = recordLiveEnabled,
                recordingSessionId = recordingSessionId,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun RealCameraXStage(
    useFrontCamera: Boolean,
    visualUnderstandingEnabled: Boolean,
    onSceneFrameSampled: (Bitmap) -> Unit,
    recordLiveEnabled: Boolean,
    recordingSessionId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, useFrontCamera, visualUnderstandingEnabled, recordLiveEnabled, recordingSessionId) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var activeRecording: Recording? = null
        val listener = Runnable {
            try {
                val cameraProvider = providerFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val analyzer: ImageAnalysis? = if (visualUnderstandingEnabled) {
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().apply {
                            setAnalyzer(
                                analysisExecutor,
                                AdaptiveSceneAnalyzer(onSceneFrameSampled)
                            )
                        }
                } else null

                val videoCapture: VideoCapture<Recorder>? = if (recordLiveEnabled && recordingSessionId.isNotBlank()) {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.SD,
                                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                            )
                        )
                        .build()
                    VideoCapture.withOutput(recorder)
                } else null

                val finalSelector = if (cameraProvider.hasCamera(selector)) {
                    selector
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                val primaryUseCases = mutableListOf<UseCase>(preview).apply {
                    analyzer?.let(::add)
                    videoCapture?.let(::add)
                }

                var boundVideo = false
                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        finalSelector,
                        *primaryUseCases.toTypedArray()
                    )
                    boundVideo = videoCapture != null
                } catch (_: Exception) {
                    cameraProvider.unbindAll()
                    if (videoCapture != null) {
                        try {
                            cameraProvider.bindToLifecycle(lifecycleOwner, finalSelector, preview, videoCapture)
                            boundVideo = true
                        } catch (_: Exception) {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, finalSelector, preview)
                        }
                    } else {
                        cameraProvider.bindToLifecycle(lifecycleOwner, finalSelector, preview)
                    }
                }

                if (boundVideo && videoCapture != null) {
                    val outFile = LiveRecordingCoordinator.prepareCameraSegmentFile(context, recordingSessionId)
                    val outputOptions = FileOutputOptions.Builder(outFile).build()
                    val hasMicPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    val pending = videoCapture.output.prepareRecording(context, outputOptions).apply {
                        if (hasMicPerm) {
                            runCatching { withAudioEnabled() }
                        }
                    }
                    activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            LiveRecordingCoordinator.onCameraRecordingFinalized(
                                file = outFile,
                                hasError = event.hasError()
                            )
                        }
                    }
                    LiveRecordingCoordinator.registerCameraRecording(activeRecording!!, outFile)
                }
                cameraError = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind CameraX use cases", e)
                cameraError = e.localizedMessage ?: "Camera hardware unavailable on this device."
            }
        }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { activeRecording?.stop() }
            activeRecording = null
            try {
                if (providerFuture.isDone) {
                    providerFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error unbinding camera", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = modifier.background(StudioObsidian)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        if (cameraError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioObsidian.copy(alpha = 0.88f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "Camera unavailable",
                        tint = FameMint,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Camera Feed Unavailable",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = cameraError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionDeniedStage(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(StudioSurface, StudioObsidian))
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = "Camera permission needed",
                tint = FameMint,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Camera Permission Not Granted",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "FameLife never fakes a camera feed. Grant camera access to preview yourself live, or switch to Audio Only.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGrantClick) {
                Text("Enable Camera")
            }
        }
    }
}

@Composable
private fun CameraPausedStage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(StudioSurfaceElevated, StudioObsidian))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = "Camera turned off",
                tint = TextSecondary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("Camera Off", style = MaterialTheme.typography.titleMedium)
            Text(
                "Viewers see your stream pause card",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AudioOnlyStage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(listOf(StudioSurfaceElevated, StudioObsidian))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Audio only broadcast",
                tint = FameMint,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Audio-Only Studio", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Camera capture is off • Microphone & chat active",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ScreenShareStage(
    screenShareActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(StudioSurface, StudioObsidian)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (screenShareActive) "Screen Share Capturing" else "Screen Share Ready",
                style = MaterialTheme.typography.titleLarge,
                color = if (screenShareActive) FameMint else TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (screenShareActive) {
                    "MediaProjection active. Sparse frames are sampled for audience reactions only while live."
                } else {
                    "Tap the Screen Share control to request Android MediaProjection permission."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Sparse scene-change detector: computes average Y-plane luminance delta and emits a
 * downscaled Bitmap only when a meaningful scene change occurs or after a cooldown.
 */
private class AdaptiveSceneAnalyzer(
    private val onFrameSampled: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {
    private var lastSampledAtMs = 0L
    private var lastAvgLuma = -1f

    override fun analyze(image: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastSampledAtMs < 4500L) return

            val yBuffer = image.planes.firstOrNull()?.buffer ?: return
            val step = 16
            var sum = 0L
            var count = 0
            val limit = yBuffer.remaining()
            var i = 0
            while (i < limit) {
                sum += (yBuffer.get(i).toInt() and 0xFF)
                count++
                i += step
            }
            val avgLuma = if (count > 0) sum.toFloat() / count else 0f
            val lumaDelta = if (lastAvgLuma < 0f) 999f else abs(avgLuma - lastAvgLuma)

            if (lumaDelta >= 10f || now - lastSampledAtMs >= 12_000L) {
                lastAvgLuma = avgLuma
                lastSampledAtMs = now
                imageProxyToSmallBitmap(image)?.let(onFrameSampled)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Frame sampling failed", e)
        } finally {
            image.close()
        }
    }

    private fun imageProxyToSmallBitmap(image: ImageProxy): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888 || image.planes.size < 3) return null
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 55, out)
        val bytes = out.toByteArray()
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val maxDim = 480f
        val scale = (maxDim / maxOf(raw.width, raw.height)).coerceAtMost(1f)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also {
            if (it != raw) raw.recycle()
        }
    }
}
