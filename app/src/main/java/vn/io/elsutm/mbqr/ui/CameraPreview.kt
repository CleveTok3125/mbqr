package vn.io.elsutm.mbqr.ui

import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import vn.io.elsutm.mbqr.camera.QrAnalyzer
import vn.io.elsutm.mbqr.camera.QrPipelineStatus
import vn.io.elsutm.mbqr.camera.ScanCrop
import vn.io.elsutm.mbqr.settings.QrDecodePreset
import java.util.concurrent.Executors

@OptIn(markerClass = [ExperimentalCamera2Interop::class])
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    torchOn: Boolean,
    scanningEnabled: Boolean,
    onDecoded: (String) -> Unit,
    onCameraError: (String) -> Unit,
    onCameraBound: (Camera?) -> Unit = {},
    onImageCaptureBound: (ImageCapture?) -> Unit = {},
    onZoomStateChanged: (ZoomState) -> Unit = {},
    scanCrop: ScanCrop? = null,
    decodePreset: QrDecodePreset = QrDecodePreset.FAST,
    analysisSize: Size = Size(1280, 720),
    analysisFps: Int = 24,
    targetFpsRange: IntRange? = null,
    onPipelineStatus: ((QrPipelineStatus) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzer = remember { QrAnalyzer() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }
    val currentOnDecoded by rememberUpdatedState(onDecoded)
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val currentOnCameraBound by rememberUpdatedState(onCameraBound)
    val currentOnImageCaptureBound by rememberUpdatedState(onImageCaptureBound)
    val currentOnZoomStateChanged by rememberUpdatedState(onZoomStateChanged)
    val currentOnPipelineStatus by rememberUpdatedState(onPipelineStatus)

    var previewWidth by remember { mutableStateOf(0) }
    var previewHeight by remember { mutableStateOf(0) }

    SideEffect {
        analyzer.onDecoded = { text -> mainExecutor.execute { currentOnDecoded(text) } }
        analyzer.decodePreset = decodePreset
        analyzer.targetFps = analysisFps
        analyzer.onPipelineStatus = currentOnPipelineStatus
        analyzer.setScanCrop(scanCrop, previewWidth, previewHeight)
    }

    LaunchedEffect(scanningEnabled) {
        val wasEnabled = analyzer.enabled
        analyzer.enabled = scanningEnabled
        if (scanningEnabled) {
            analyzer.resetDuplicateGuard(requireFreshFrame = !wasEnabled)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lifecycleOwner, analysisSize, targetFpsRange) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false

        providerFuture.addListener(
            {
                if (disposed) return@addListener
                try {
                    val resolvedProvider = providerFuture.get()
                    provider = resolvedProvider

                    val preview = Preview.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(
                                    AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                                )
                                .build(),
                        )
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    fun buildAnalysis(applyTargetFps: Boolean): ImageAnalysis {
                        val builder = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .setResolutionSelector(
                                ResolutionSelector.Builder()
                                    .setAspectRatioStrategy(
                                        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                                    )
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            analysisSize,
                                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                                        ),
                                    )
                                    .build(),
                            )
                        if (applyTargetFps && targetFpsRange != null) {
                            Camera2Interop.Extender(builder).setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                Range(targetFpsRange.first, targetFpsRange.last),
                            )
                        }
                        return builder.build().also {
                            it.setAnalyzer(analysisExecutor, analyzer)
                        }
                    }
                    fun bindWithCapture(analysis: ImageAnalysis): Camera =
                        resolvedProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                            imageCapture,
                        ).also { currentOnImageCaptureBound(imageCapture) }

                    fun bindWithoutCapture(analysis: ImageAnalysis): Camera =
                        resolvedProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        ).also { currentOnImageCaptureBound(null) }

                    val analysis = buildAnalysis(applyTargetFps = true)

                    resolvedProvider.unbindAll()
                    camera = try {
                        bindWithCapture(analysis)
                    } catch (_: Exception) {
                        // Some devices cannot bind three use cases. Keep live scanning
                        // and hide the photo capture control.
                        resolvedProvider.unbindAll()
                        currentOnImageCaptureBound(null)
                        try {
                            bindWithoutCapture(analysis)
                        } catch (_: Exception) {
                            // Some devices reject the AE target range. Fall back
                            // to throttling only instead of losing the camera.
                            resolvedProvider.unbindAll()
                            val fallbackAnalysis = buildAnalysis(applyTargetFps = false)
                            try {
                                bindWithCapture(fallbackAnalysis)
                            } catch (_: Exception) {
                                resolvedProvider.unbindAll()
                                currentOnImageCaptureBound(null)
                                bindWithoutCapture(fallbackAnalysis)
                            }
                        }
                    }

                    currentOnCameraBound(camera)
                } catch (error: Exception) {
                    currentOnCameraError(error.message ?: "Camera unavailable")
                }
            },
            mainExecutor,
        )

        onDispose {
            disposed = true
            provider?.unbindAll()
            camera = null
            currentOnCameraBound(null)
            currentOnImageCaptureBound(null)
        }
    }

    LaunchedEffect(torchOn, camera) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    DisposableEffect(camera, lifecycleOwner) {
        val activeCamera = camera
        if (activeCamera == null) {
            onDispose {}
        } else {
            val observer = Observer<ZoomState> { state -> currentOnZoomStateChanged(state) }
            activeCamera.cameraInfo.zoomState.observe(lifecycleOwner, observer)
            onDispose { activeCamera.cameraInfo.zoomState.removeObserver(observer) }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
            .onSizeChanged { size ->
                previewWidth = size.width
                previewHeight = size.height
            }
            .pointerInput(camera) {
                detectTapGestures { offset ->
                    val activeCamera = camera ?: return@detectTapGestures
                    val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                    activeCamera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(point).build(),
                    )
                }
            },
    )
}
