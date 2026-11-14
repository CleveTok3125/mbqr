package vn.io.elsutm.mbqr.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import vn.io.elsutm.mbqr.BuildConfig
import vn.io.elsutm.mbqr.R
import vn.io.elsutm.mbqr.camera.CameraCapabilities
import vn.io.elsutm.mbqr.camera.PhotoQrDecoder
import vn.io.elsutm.mbqr.camera.QrPipelineSource
import vn.io.elsutm.mbqr.camera.QrPipelineStage
import vn.io.elsutm.mbqr.camera.QrPipelineStatus
import vn.io.elsutm.mbqr.camera.ScanCrop
import vn.io.elsutm.mbqr.handoff.ClipboardHandoff
import vn.io.elsutm.mbqr.handoff.ClipboardSession
import vn.io.elsutm.mbqr.handoff.MbLauncher
import vn.io.elsutm.mbqr.handoff.QrActions
import vn.io.elsutm.mbqr.handoff.UrlDetector
import vn.io.elsutm.mbqr.qr.QrContent
import vn.io.elsutm.mbqr.qr.QrContentParser
import vn.io.elsutm.mbqr.qr.QrField
import vn.io.elsutm.mbqr.qr.PayloadEncoder
import vn.io.elsutm.mbqr.qr.QrFieldSensitivity
import vn.io.elsutm.mbqr.service.ClipboardExpiryService
import vn.io.elsutm.mbqr.settings.AnalysisFpsPreset
import vn.io.elsutm.mbqr.settings.AnalysisResolutionPreset
import vn.io.elsutm.mbqr.settings.AnalysisSize
import vn.io.elsutm.mbqr.settings.AppSettings
import vn.io.elsutm.mbqr.settings.QrDecodePreset
import vn.io.elsutm.mbqr.settings.ScanBoundaryStyle
import vn.io.elsutm.mbqr.settings.ScanFrameStyle
import vn.io.elsutm.mbqr.vietqr.BankDirectory
import vn.io.elsutm.mbqr.vietqr.TransferBuilder
import vn.io.elsutm.mbqr.vietqr.VietQrData
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

private const val MB_REDIRECT_TIMEOUT_MS = 5_000L
private const val TRANSFER_CONTENT_TITLE = "Lệnh chuyển khoản"
private const val RAW_CONTENT_TITLE = "Nội dung gốc"

private enum class HandoffPhase { Idle, Awaiting, Retry, Final }

private data class PendingHandoff(val raw: String, val transferText: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    sharedImageUri: Uri? = null,
    sharedImageNonce: Int = 0,
    onSharedImageConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }

    var cameraGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    var cameraError by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraAttempt by rememberSaveable { mutableStateOf(0) }
    var scanningEnabled by rememberSaveable { mutableStateOf(true) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var sheetRaw by rememberSaveable { mutableStateOf<String?>(null) }
    // Payload exactly as decoded, before normalization or sanitizing.
    var sheetRawExact by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetReason by rememberSaveable { mutableStateOf("") }
    var clipboardExpiresAt by rememberSaveable { mutableStateOf(0L) }
    var clipboardRemaining by rememberSaveable { mutableStateOf(0) }
    var clipboardPersistent by rememberSaveable {
        mutableStateOf(
            ClipboardSession.text != null &&
                AppSettings.clipboardExpirySeconds(context) == AppSettings.CLIPBOARD_TIMER_OFF_SECONDS,
        )
    }
    var lastClipboardText by remember { mutableStateOf(ClipboardSession.text) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showCredits by rememberSaveable { mutableStateOf(false) }
    var confirmBeforeOpeningMb by remember {
        mutableStateOf(AppSettings.confirmBeforeOpeningMb(context))
    }
    var clipboardExpirySeconds by remember {
        mutableStateOf(AppSettings.clipboardExpirySeconds(context))
    }
    var scanInsideFrame by remember { mutableStateOf(AppSettings.scanInsideFrame(context)) }
    var sensitiveMaskingLevel by remember {
        mutableStateOf(AppSettings.sensitiveMaskingLevel(context))
    }
    var scanFrameMarginPercent by remember {
        mutableStateOf(AppSettings.scanFrameMarginPercent(context))
    }
    var liveDecodePreset by remember {
        mutableStateOf(AppSettings.liveDecodePreset(context))
    }
    var stillDecodePreset by remember {
        mutableStateOf(AppSettings.stillDecodePreset(context))
    }
    var analysisResolutionPreset by remember {
        mutableStateOf(AppSettings.analysisResolutionPreset(context))
    }
    var customAnalysisSize by remember {
        mutableStateOf(AppSettings.customAnalysisSize(context))
    }
    var pipelineStatusEnabled by remember {
        mutableStateOf(AppSettings.pipelineStatusEnabled(context))
    }
    var advancedCopyEnabled by remember { mutableStateOf(AppSettings.advancedCopyEnabled(context)) }
    var sanitizeResults by remember { mutableStateOf(AppSettings.sanitizeResults(context)) }
    var hapticsEnabled by remember { mutableStateOf(AppSettings.hapticsEnabled(context)) }
    var scanFrameStyle by remember { mutableStateOf(AppSettings.scanFrameStyle(context)) }
    var scanBoundaryStyle by remember { mutableStateOf(AppSettings.scanBoundaryStyle(context)) }
    var analysisFpsPreset by remember {
        mutableStateOf(AppSettings.analysisFpsPreset(context))
    }
    var customAnalysisFps by remember {
        mutableStateOf(AppSettings.customAnalysisFps(context))
    }
    val analysisFps = analysisFpsPreset.fps ?: customAnalysisFps
    var pipelineStatus by remember { mutableStateOf<QrPipelineStatus?>(null) }
    var showCustomResolutionDialog by rememberSaveable { mutableStateOf(false) }
    var customResolutionWidth by rememberSaveable { mutableStateOf("") }
    var customResolutionHeight by rememberSaveable { mutableStateOf("") }
    var customResolutionError by rememberSaveable { mutableStateOf<String?>(null) }
    var showCustomFpsDialog by rememberSaveable { mutableStateOf(false) }
    var customFpsInput by rememberSaveable { mutableStateOf("") }
    var customFpsError by rememberSaveable { mutableStateOf<String?>(null) }
    val supportedAnalysisSizes = remember(cameraGranted) {
        CameraCapabilities.supportedSizes(context)
    }
    val analysisSize = remember(analysisResolutionPreset, customAnalysisSize) {
        CameraCapabilities.resolve(analysisResolutionPreset, customAnalysisSize)
    }
    val fpsSupport = remember(cameraGranted, analysisSize) {
        CameraCapabilities.fpsSupport(context, analysisSize)
    }
    val effectiveAnalysisFps = if (fpsSupport.known) {
        analysisFps.coerceAtMost(fpsSupport.maxFps)
    } else {
        analysisFps
    }
    val targetFpsRange = remember(fpsSupport, effectiveAnalysisFps) {
        CameraCapabilities.targetFpsRange(effectiveAnalysisFps, fpsSupport)
    }

    var demoMode by remember { mutableStateOf(false) }
    var demoCountdown by remember { mutableStateOf(60) }
    var demoCaseIndex by remember { mutableStateOf(0) }
    var demoCaseMbInstalled by remember { mutableStateOf(true) }
    var versionTaps by remember { mutableStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var zoomMin by remember { mutableStateOf(1f) }
    var zoomMax by remember { mutableStateOf(1f) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var isCapturing by remember { mutableStateOf(false) }
    var handoffPhaseName by rememberSaveable { mutableStateOf(HandoffPhase.Idle.name) }
    var handoffAttempt by rememberSaveable { mutableStateOf(0) }
    var pendingHandoffRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingHandoffText by rememberSaveable { mutableStateOf<String?>(null) }
    val handoffPhase = HandoffPhase.valueOf(handoffPhaseName)
    val pendingHandoff = pendingHandoffRaw?.let { raw ->
        pendingHandoffText?.let { text -> PendingHandoff(raw, text) }
    }

    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val pipelineReporter: ((QrPipelineStatus) -> Unit)? = if (pipelineStatusEnabled) {
        { status -> mainExecutor.execute { pipelineStatus = status } }
    } else {
        null
    }

    DisposableEffect(Unit) {
        onDispose { captureExecutor.shutdown() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(clipboardExpiresAt, clipboardPersistent) {
        if (clipboardPersistent) {
            clipboardRemaining = 0
            return@LaunchedEffect
        }
        while (true) {
            val remaining = clipboardExpiresAt - SystemClock.elapsedRealtime()
            clipboardRemaining = ((remaining + 999) / 1000).coerceAtLeast(0).toInt()
            if (remaining <= 0) break
            delay(500)
        }
    }

    LaunchedEffect(demoMode, clipboardExpirySeconds) {
        if (!demoMode) return@LaunchedEffect
        if (clipboardExpirySeconds == AppSettings.CLIPBOARD_TIMER_OFF_SECONDS) {
            demoCountdown = 0
            return@LaunchedEffect
        }
        demoCountdown = clipboardExpirySeconds
        while (true) {
            delay(1_000)
            demoCountdown = if (demoCountdown <= 1) clipboardExpirySeconds else demoCountdown - 1
        }
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(demoMode, activity) {
        val window = activity?.window
        val controller = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        if (demoMode) {
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.statusBars()) }
    }

    fun copyToClipboard(text: String) {
        val expiryMillis = AppSettings.clipboardExpiryMillis(context)
        if (expiryMillis <= 0L) {
            // Stop an older timer without clearing the text we are about to copy.
            ClipboardExpiryService.cancel(context)
        }
        ClipboardHandoff.copy(context, text)
        if (expiryMillis > 0L) {
            ClipboardExpiryService.start(context, text, expiryMillis)
        }
        ClipboardSession.text = text
        lastClipboardText = text
        clipboardPersistent = expiryMillis <= 0L
        clipboardExpiresAt = if (expiryMillis > 0L) {
            SystemClock.elapsedRealtime() + expiryMillis
        } else {
            0L
        }
        clipboardRemaining = if (expiryMillis > 0L) {
            ((expiryMillis + 999) / 1_000L).toInt()
        } else {
            0
        }
    }

    fun haptic(type: Int = HapticFeedbackConstants.CLOCK_TICK) {
        if (hapticsEnabled) view.performHapticFeedback(type)
    }

    fun copyField(text: String) {
        if (demoMode) {
            Toast.makeText(context, "Đang trong chế độ demo", Toast.LENGTH_SHORT).show()
            return
        }
        copyToClipboard(text)
        haptic()
        Toast.makeText(context, "Đã copy", Toast.LENGTH_SHORT).show()
    }

    fun restoreClipboard() {
        val text = ClipboardSession.text ?: lastClipboardText ?: return
        copyToClipboard(text)
        haptic()
        Toast.makeText(context, "Đã copy lại", Toast.LENGTH_SHORT).show()
    }

    fun commitClipboardExpiry(seconds: Int) {
        val boundedSeconds = AppSettings.boundedExpirySeconds(seconds)
        AppSettings.setClipboardExpirySeconds(context, boundedSeconds)
        if (boundedSeconds == AppSettings.CLIPBOARD_TIMER_OFF_SECONDS) {
            ClipboardExpiryService.cancel(context)
            clipboardPersistent = lastClipboardText != null
            clipboardExpiresAt = 0L
            clipboardRemaining = 0
        } else if (lastClipboardText != null && (clipboardPersistent || clipboardRemaining > 0)) {
            val text = lastClipboardText ?: return
            val expiryMillis = boundedSeconds * 1_000L
            ClipboardExpiryService.start(context, text, expiryMillis)
            clipboardPersistent = false
            clipboardExpiresAt = SystemClock.elapsedRealtime() + expiryMillis
            clipboardRemaining = boundedSeconds
        }
    }

    fun clearClipboardNow() {
        val text = ClipboardSession.text ?: lastClipboardText ?: return
        ClipboardHandoff.clearIfMatches(context, text)
        ClipboardSession.text = null
        lastClipboardText = null
        clipboardPersistent = false
        clipboardExpiresAt = 0L
        clipboardRemaining = 0
        context.stopService(Intent(context, ClipboardExpiryService::class.java))
        haptic()
        Toast.makeText(context, "Đã xoá clipboard", Toast.LENGTH_SHORT).show()
    }

    fun applySettingsChange(change: SettingsChange) {
        when (change) {
            is SettingsChange.ConfirmBeforeOpeningMb -> {
                confirmBeforeOpeningMb = change.value
                AppSettings.setConfirmBeforeOpeningMb(context, change.value)
            }
            is SettingsChange.ClipboardExpirySeconds -> clipboardExpirySeconds = change.value
            SettingsChange.CommitClipboardExpiry -> commitClipboardExpiry(clipboardExpirySeconds)
            is SettingsChange.ScanInsideFrame -> {
                scanInsideFrame = change.value
                AppSettings.setScanInsideFrame(context, change.value)
            }
            is SettingsChange.ScanFrameMarginPercent -> scanFrameMarginPercent = change.value
            SettingsChange.CommitScanFrameMargin -> {
                AppSettings.setScanFrameMarginPercent(context, scanFrameMarginPercent)
            }
            is SettingsChange.SensitiveMaskingLevel -> {
                sensitiveMaskingLevel = change.value
                AppSettings.setSensitiveMaskingLevel(context, change.value)
            }
            is SettingsChange.LiveDecodePreset -> {
                liveDecodePreset = change.value
                AppSettings.setLiveDecodePreset(context, change.value)
            }
            is SettingsChange.StillDecodePreset -> {
                stillDecodePreset = change.value
                AppSettings.setStillDecodePreset(context, change.value)
            }
            is SettingsChange.PipelineStatusEnabled -> {
                pipelineStatusEnabled = change.value
                if (!change.value) pipelineStatus = null
                AppSettings.setPipelineStatusEnabled(context, change.value)
            }
            is SettingsChange.AdvancedCopyEnabled -> {
                advancedCopyEnabled = change.value
                AppSettings.setAdvancedCopyEnabled(context, change.value)
            }
            is SettingsChange.SanitizeResults -> {
                sanitizeResults = change.value
                AppSettings.setSanitizeResults(context, change.value)
            }
            is SettingsChange.HapticsEnabled -> {
                hapticsEnabled = change.value
                AppSettings.setHapticsEnabled(context, change.value)
            }
            is SettingsChange.FrameStyle -> {
                scanFrameStyle = change.value
                AppSettings.setScanFrameStyle(context, change.value)
            }
            is SettingsChange.BoundaryStyle -> {
                scanBoundaryStyle = change.value
                AppSettings.setScanBoundaryStyle(context, change.value)
            }
            is SettingsChange.ResolutionPreset -> {
                analysisResolutionPreset = change.value
                AppSettings.setAnalysisResolutionPreset(context, change.value)
            }
            is SettingsChange.FpsPreset -> {
                analysisFpsPreset = change.value
                AppSettings.setAnalysisFpsPreset(context, change.value)
            }
        }
    }

    fun restoreDefaultSettings() {
        val defaults = AppSettings.defaults()
        listOf(
            SettingsChange.ConfirmBeforeOpeningMb(defaults.confirmBeforeOpeningMb),
            SettingsChange.ClipboardExpirySeconds(defaults.clipboardExpirySeconds),
            SettingsChange.CommitClipboardExpiry,
            SettingsChange.ScanInsideFrame(defaults.scanInsideFrame),
            SettingsChange.ScanFrameMarginPercent(defaults.scanFrameMarginPercent),
            SettingsChange.CommitScanFrameMargin,
            SettingsChange.SensitiveMaskingLevel(defaults.sensitiveMaskingLevel),
            SettingsChange.LiveDecodePreset(defaults.liveDecodePreset),
            SettingsChange.StillDecodePreset(defaults.stillDecodePreset),
            SettingsChange.PipelineStatusEnabled(defaults.pipelineStatusEnabled),
            SettingsChange.AdvancedCopyEnabled(defaults.advancedCopyEnabled),
            SettingsChange.SanitizeResults(defaults.sanitizeResults),
            SettingsChange.HapticsEnabled(defaults.hapticsEnabled),
            SettingsChange.FrameStyle(defaults.scanFrameStyle),
            SettingsChange.BoundaryStyle(defaults.scanBoundaryStyle),
            SettingsChange.ResolutionPreset(defaults.analysisResolutionPreset),
            SettingsChange.FpsPreset(defaults.analysisFpsPreset),
        ).forEach { change -> applySettingsChange(change) }
    }

    fun showSheet(raw: String, reason: String) {
        scanningEnabled = false
        sheetRaw = QrContentParser.normalized(raw)
        sheetRawExact = raw
        sheetReason = reason
    }

    fun showNextDemoCase() {
        val case = DEMO_SCAN_CASES[demoCaseIndex % DEMO_SCAN_CASES.size]
        demoCaseIndex = (demoCaseIndex + 1) % DEMO_SCAN_CASES.size
        demoCaseMbInstalled = case.mbInstalled
        if (!case.shouldShowDemoSheet(confirmBeforeOpeningMb)) {
            Toast.makeText(context, "Đang trong chế độ demo", Toast.LENGTH_SHORT).show()
            return
        }
        showSheet(case.raw, case.reason)
    }

    fun resetHandoff() {
        handoffPhaseName = HandoffPhase.Idle.name
        handoffAttempt = 0
        pendingHandoffRaw = null
        pendingHandoffText = null
    }

    fun closeSheet() {
        resetHandoff()
        sheetRaw = null
        sheetRawExact = null
        scanningEnabled = true
    }

    fun closeSheetAndCopy(text: String) {
        copyToClipboard(text)
        closeSheet()
    }

    fun shareOrToast(text: String) {
        if (!QrActions.shareText(context, text)) {
            Toast.makeText(
                context,
                "Không có ứng dụng để chia sẻ",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun openLinkOrToast(url: String) {
        if (!QrActions.openHttpUrl(context, url)) {
            Toast.makeText(
                context,
                "Không có ứng dụng để mở liên kết",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun beginHandoff(raw: String, transferText: String) {
        if (handoffPhase == HandoffPhase.Awaiting) return

        // The clipboard and timer must be ready before MB reads the paste buffer.
        scanningEnabled = false
        pendingHandoffRaw = raw
        pendingHandoffText = transferText
        copyToClipboard(transferText)

        handoffAttempt += 1
        handoffPhaseName = HandoffPhase.Awaiting.name
        val opened = MbLauncher.open(context)
        sheetRaw = null
        sheetRawExact = null

        if (!opened) {
            handoffPhaseName = if (handoffAttempt >= 2) {
                HandoffPhase.Final.name
            } else {
                HandoffPhase.Retry.name
            }
        }
    }

    fun retryHandoff() {
        val pending = pendingHandoff ?: return
        beginHandoff(pending.raw, pending.transferText)
    }

    fun showFinalConfirmation() {
        val pending = pendingHandoff ?: return
        resetHandoff()
        showSheet(pending.raw, "Xác nhận chuyển khoản")
    }

    fun handleDecoded(raw: String) {
        if (!scanningEnabled) return
        resetHandoff()

        // Any decoded QR is a scan success: either a sheet or the MB handoff follows.
        haptic()

        when (val content = QrContentParser.parse(raw, sanitizeResults)) {
            is QrContent.VietQr -> {
                val data = content.data
                if (!content.crcValid) {
                    showSheet(raw, "CRC không hợp lệ")
                    return
                }

                val transferText = BankDirectory.find(data.bankBin)
                    ?.prefix
                    ?.takeIf { it.isNotBlank() }
                    ?.let { TransferBuilder.build(data, it) }

                if (!MbLauncher.isInstalled(context)) {
                    showSheet(raw, "Chưa cài MB Bank")
                    return
                }

                if (transferText == null) {
                    showSheet(raw, "Chưa hỗ trợ ngân hàng")
                    return
                }

                scanningEnabled = false
                if (AppSettings.confirmBeforeOpeningMb(context)) {
                    showSheet(raw, "Xác nhận chuyển khoản")
                } else {
                    beginHandoff(raw, transferText)
                }
            }

            is QrContent.Details -> showSheet(raw, content.title)
            is QrContent.Text -> showSheet(raw, "Nội dung QR")
        }
    }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return

        isCapturing = true
        capture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val text = try {
                        decodeCapturedImage(
                            image,
                            stillDecodePreset,
                            QrPipelineSource.SHUTTER,
                            pipelineReporter,
                        )
                    } finally {
                        image.close()
                    }
                    mainExecutor.execute {
                        isCapturing = false
                        if (text != null) {
                            handleDecoded(text)
                        } else {
                            Toast.makeText(
                                context,
                                "Không tìm thấy mã trong ảnh",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    mainExecutor.execute {
                        isCapturing = false
                        Toast.makeText(
                            context,
                            "Chụp ảnh thất bại",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
        )
    }

    fun decodePickedImage(
        uri: Uri,
        source: QrPipelineSource = QrPipelineSource.IMPORT,
    ) {
        captureExecutor.execute {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                }
            } catch (_: Exception) {
                null
            }
            val text = bytes?.let {
                PhotoQrDecoder.decodeImage(
                    it,
                    stillDecodePreset,
                    source,
                    pipelineReporter,
                )
            }
            mainExecutor.execute {
                if (text != null) {
                    handleDecoded(text)
                } else {
                    Toast.makeText(
                        context,
                        if (bytes == null) "Không đọc được ảnh" else "Không tìm thấy mã trong ảnh",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    LaunchedEffect(sharedImageUri, sharedImageNonce) {
        val uri = sharedImageUri ?: return@LaunchedEffect
        onSharedImageConsumed()
        resetHandoff()
        sheetRaw = null
        sheetRawExact = null
        scanningEnabled = true
        decodePickedImage(uri, QrPipelineSource.SHARED)
    }

    DisposableEffect(lifecycleOwner, handoffPhase) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && handoffPhase == HandoffPhase.Awaiting) {
                resetHandoff()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(handoffPhase, handoffAttempt) {
        if (handoffPhase != HandoffPhase.Awaiting) return@LaunchedEffect
        delay(MB_REDIRECT_TIMEOUT_MS)
        if (handoffPhase != HandoffPhase.Awaiting) return@LaunchedEffect
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            resetHandoff()
            return@LaunchedEffect
        }
        handoffPhaseName = if (handoffAttempt >= 2) {
            HandoffPhase.Final.name
        } else {
            HandoffPhase.Retry.name
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) decodePickedImage(uri) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                !cameraGranted -> PermissionContent(
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                )

                cameraError != null -> CameraErrorContent(
                    message = cameraError.orEmpty(),
                    onRetry = {
                        cameraError = null
                        cameraAttempt += 1
                    },
                )

                !scanningEnabled && sheetRaw == null -> {
                    val pending = pendingHandoff
                    PausedContent(
                        clipboardRemaining = clipboardRemaining,
                        clipboardPersistent = clipboardPersistent,
                        reminder = when (handoffPhase) {
                            HandoffPhase.Retry -> "MB Bank chưa mở?"
                            HandoffPhase.Final -> "Không mở được MB Bank. Thao tác thủ công?"
                            else -> null
                        },
                        openLabel = if (handoffPhase == HandoffPhase.Final) {
                            "Mở xác nhận chuyển khoản"
                        } else {
                            "Mở MB Bank"
                        },
                        onOpen = when {
                            pending == null -> null
                            handoffPhase == HandoffPhase.Retry -> ({ retryHandoff() })
                            handoffPhase == HandoffPhase.Final -> ({ showFinalConfirmation() })
                            else -> null
                        },
                        onScanAgain = {
                            resetHandoff()
                            scanningEnabled = true
                        },
                    )
                }

                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val statusTop = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding()
                        val navigationBottom = WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                        val topBarHeight = statusTop + 64.dp
                        val controlsHeight = if (camera != null || demoMode) 190.dp else 0.dp
                        val bottom = maxHeight - navigationBottom
                        val availableHeight = (bottom - topBarHeight - controlsHeight)
                            .coerceAtLeast(120.dp)
                        val frameSize = minOf(
                            maxWidth * 0.72f,
                            260.dp,
                            availableHeight * 0.55f,
                        ).coerceAtLeast(140.dp)
                        val frameCenterY = maxHeight / 2f - maxHeight * 0.05f
                        val frameBottom = frameCenterY + frameSize / 2f
                        val availableBelowFrame = (bottom - frameBottom).coerceAtLeast(0.dp)
                        val controlsMargin = ((availableBelowFrame - 190.dp) / 2f)
                            .coerceAtLeast(16.dp)
                        val scanCrop = if (scanInsideFrame) {
                            val horizontalSpace = ((maxWidth - frameSize) / 2f)
                                .coerceAtLeast(0.dp)
                            val topSpace = (frameCenterY - frameSize / 2f)
                                .coerceAtLeast(0.dp)
                            val bottomSpace = (maxHeight - frameCenterY - frameSize / 2f)
                                .coerceAtLeast(0.dp)
                            // Scale the tightest gap so the boundary stays centered
                            // and never leaves the viewport.
                            val minGap = minOf(horizontalSpace, topSpace, bottomSpace)
                            val margin = scanMargin(minGap, scanFrameMarginPercent)
                            val left = ((maxWidth - frameSize) / 2f - margin)
                                .coerceAtLeast(0.dp)
                            val top = (frameCenterY - frameSize / 2f - margin)
                                .coerceAtLeast(0.dp)
                            val right = (left + frameSize + margin * 2)
                                .coerceAtMost(maxWidth)
                            val bottom = (top + frameSize + margin * 2)
                                .coerceAtMost(maxHeight)
                            ScanCrop(
                                left = left.value / maxWidth.value,
                                top = top.value / maxHeight.value,
                                right = right.value / maxWidth.value,
                                bottom = bottom.value / maxHeight.value,
                            )
                        } else {
                            null
                        }

                        if (demoMode) {
                            Image(
                                painter = painterResource(R.drawable.demo_bg),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            key(cameraAttempt) {
                                CameraPreview(
                                    modifier = Modifier.fillMaxSize(),
                                    torchOn = torchOn,
                                    // Pause decoding so a QR cannot open a sheet under the
                                    // settings screen.
                                    scanningEnabled = scanningEnabled && !showSettings,
                                    onDecoded = ::handleDecoded,
                                    onCameraError = { message -> cameraError = message },
                                    onCameraBound = { camera = it },
                                    onImageCaptureBound = { imageCapture = it },
                                    onZoomStateChanged = { state ->
                                        zoomMin = state.minZoomRatio
                                        zoomMax = state.maxZoomRatio
                                        zoomRatio = state.zoomRatio
                                    },
                                    scanCrop = scanCrop,
                                    decodePreset = liveDecodePreset,
                                    analysisSize = android.util.Size(
                                        analysisSize.width,
                                        analysisSize.height,
                                    ),
                                    analysisFps = effectiveAnalysisFps,
                                    targetFpsRange = targetFpsRange,
                                    onPipelineStatus = pipelineReporter,
                                )
                            }
                        }

                        ScannerOverlay(
                            torchOn = torchOn,
                            onToggleTorch = { torchOn = !torchOn },
                            clipboardRemaining = if (demoMode) demoCountdown else clipboardRemaining,
                            clipboardPersistent = if (demoMode) {
                                clipboardExpirySeconds == AppSettings.CLIPBOARD_TIMER_OFF_SECONDS
                            } else {
                                clipboardPersistent
                            },
                            onRestoreClipboard = when {
                                demoMode -> {
                                    {
                                        demoCountdown = if (clipboardExpirySeconds > 0) {
                                            clipboardExpirySeconds
                                        } else {
                                            0
                                        }
                                        haptic()
                                    }
                                }
                                lastClipboardText != null -> {
                                    { restoreClipboard() }
                                }
                                else -> null
                            },
                            onClearClipboard = if (!demoMode && lastClipboardText != null) {
                                { clearClipboardNow() }
                            } else {
                                null
                            },
                            frameSize = frameSize,
                            frameOffsetY = -maxHeight * 0.05f,
                            frameStyle = scanFrameStyle,
                            boundaryStyle = scanBoundaryStyle,
                            scanCrop = scanCrop,
                            showTorch = true,
                            showImage = true,
                            onPickImage = {
                                if (demoMode) {
                                    showNextDemoCase()
                                } else {
                                    imagePicker.launch("image/*")
                                }
                            },
                            onShowSettings = { showSettings = true },
                            pipelineStatus = pipelineStatus,
                        )

                        if (camera != null || demoMode) {
                            val dialMin = if (demoMode) {
                                1f
                            } else {
                                maxOf(1f, zoomMin).coerceAtMost(zoomMax)
                            }
                            val dialMax = if (demoMode) 25f else zoomMax.coerceAtLeast(dialMin)
                            val dialRatio = zoomRatio.coerceIn(dialMin, dialMax)

                            CameraControls(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = controlsMargin),
                                zoomRatio = dialRatio,
                                minZoom = dialMin,
                                maxZoom = dialMax,
                                isCapturing = isCapturing,
                                showCapture = if (demoMode) true else imageCapture != null,
                                hapticsEnabled = hapticsEnabled,
                                onZoomChange = { ratio ->
                                    zoomRatio = ratio
                                    if (!demoMode) {
                                        camera?.cameraControl?.setZoomRatio(ratio)
                                    }
                                },
                                onCapture = {
                                    if (demoMode) {
                                        showNextDemoCase()
                                    } else {
                                        capturePhoto()
                                    }
                                },
                            )
                        }
                    }
                }

            }
            if (showSettings) {
                SettingsScreen(
                    values = SettingsSnapshot(
                        confirmBeforeOpeningMb = confirmBeforeOpeningMb,
                        clipboardExpirySeconds = clipboardExpirySeconds,
                        scanInsideFrame = scanInsideFrame,
                        scanFrameMarginPercent = scanFrameMarginPercent,
                        sensitiveMaskingLevel = sensitiveMaskingLevel,
                        liveDecodePreset = liveDecodePreset,
                        stillDecodePreset = stillDecodePreset,
                        pipelineStatusEnabled = pipelineStatusEnabled,
                        advancedCopyEnabled = advancedCopyEnabled,
                        sanitizeResults = sanitizeResults,
                        hapticsEnabled = hapticsEnabled,
                        scanFrameStyle = scanFrameStyle,
                        scanBoundaryStyle = scanBoundaryStyle,
                        analysisResolutionPreset = analysisResolutionPreset,
                        customAnalysisSize = customAnalysisSize,
                        analysisFpsPreset = analysisFpsPreset,
                        customAnalysisFps = customAnalysisFps,
                    ),
                    defaults = AppSettings.defaults(),
                    supportedAnalysisSizes = supportedAnalysisSizes,
                    maxFps = fpsSupport.maxFps,
                    fpsKnown = fpsSupport.known,
                    onValueChange = ::applySettingsChange,
                    onEditCustomResolution = {
                        customResolutionWidth = customAnalysisSize.width.toString()
                        customResolutionHeight = customAnalysisSize.height.toString()
                        customResolutionError = null
                        showCustomResolutionDialog = true
                    },
                    onEditCustomFps = {
                        customFpsInput = customAnalysisFps.toString()
                        customFpsError = null
                        showCustomFpsDialog = true
                    },
                    onOpenCredits = { showCredits = true },
                    onRestoreDefaults = ::restoreDefaultSettings,
                    onClose = { showSettings = false },
                )
            }
        }
    }

    val raw = sheetRaw
    if (raw != null) {
        val content = remember(raw, sanitizeResults) {
            QrContentParser.parse(raw, sanitizeResults)
        }
        val mbInstalled = remember(raw, demoMode, demoCaseMbInstalled) {
            if (demoMode) demoCaseMbInstalled else MbLauncher.isInstalled(context)
        }
        // Advanced copy encodes the payload exactly as decoded: no
        // normalization, sanitizing, or masking applies.
        val exactRaw = sheetRawExact ?: content.raw
        val onCopyHex: (() -> Unit)? = if (advancedCopyEnabled && !demoMode) {
            ({
                copyToClipboard(PayloadEncoder.toHex(exactRaw))
                Toast.makeText(context, "Đã copy Hex", Toast.LENGTH_SHORT).show()
            })
        } else {
            null
        }
        val onCopyBase64: (() -> Unit)? = if (advancedCopyEnabled && !demoMode) {
            ({
                copyToClipboard(PayloadEncoder.toBase64(exactRaw))
                Toast.makeText(context, "Đã copy Base64", Toast.LENGTH_SHORT).show()
            })
        } else {
            null
        }
        val maxSheetHeight = (LocalConfiguration.current.screenHeightDp.dp - 48.dp)
            .coerceAtLeast(0.dp)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                resetHandoff()
                sheetRaw = null
                sheetRawExact = null
                scanningEnabled = true
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                HideDialogStatusBar(demoMode)
                when (content) {
                    is QrContent.VietQr -> {
                        if (!content.crcValid) {
                            RawQrSheetContent(
                                reason = sheetReason,
                                raw = content.raw,
                                maskingActive = rawMaskingActive(
                                    sensitiveMaskingLevel,
                                    hasHiddenVietQrFields(content.data, sensitiveMaskingLevel),
                                ),
                                sensitiveMaskingLevel = sensitiveMaskingLevel,
                                onCopy = if (demoMode) {
                                    ({})
                                } else {
                                    ({ closeSheetAndCopy(content.raw) })
                                },
                                onShare = if (demoMode) {
                                    ({})
                                } else {
                                    ({ shareOrToast(content.raw) })
                                },
                                onCopyHex = onCopyHex,
                                onCopyBase64 = onCopyBase64,
                                onScanAgain = { closeSheet() },
                            )
                        } else {
                            val data = content.data
                            val transferText = remember(data) {
                                BankDirectory.find(data.bankBin)
                                    ?.prefix
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { TransferBuilder.build(data, it) }
                            }
                            ParsedQrSheetContent(
                                reason = sheetReason,
                                data = data,
                                bankName = BankDirectory.find(data.bankBin)?.shortName,
                                copyText = transferText ?: content.raw,
                                copyTitle = if (transferText != null) {
                                    TRANSFER_CONTENT_TITLE
                                } else {
                                    RAW_CONTENT_TITLE
                                },
                                copyMaskingActive = if (transferText != null) {
                                    isMaskingActive(
                                        QrFieldSensitivity.Direct,
                                        sensitiveMaskingLevel,
                                    )
                                } else {
                                    rawMaskingActive(
                                        sensitiveMaskingLevel,
                                        hasHiddenVietQrFields(data, sensitiveMaskingLevel),
                                    )
                                },
                                sensitiveMaskingLevel = sensitiveMaskingLevel,
                                onOpenMb = if (transferText == null || !mbInstalled) {
                                    null
                                } else if (demoMode) {
                                    ({})
                                } else {
                                    ({ beginHandoff(exactRaw, transferText) })
                                },
                                onCopy = if (demoMode) {
                                    ({})
                                } else {
                                    ({ closeSheetAndCopy(transferText ?: content.raw) })
                                },
                                onShare = if (demoMode) {
                                    ({})
                                } else {
                                    ({ shareOrToast(transferText ?: content.raw) })
                                },
                                onCopyHex = onCopyHex,
                                onCopyBase64 = onCopyBase64,
                                onFieldLongClick = { value -> copyField(value) },
                                onScanAgain = { closeSheet() },
                            )
                        }
                    }

                    is QrContent.Details -> {
                        DetailsQrSheetContent(
                            details = content,
                            sensitiveMaskingLevel = sensitiveMaskingLevel,
                            onCopy = if (demoMode) {
                                ({})
                            } else {
                                ({ closeSheetAndCopy(content.raw) })
                            },
                            onShare = if (demoMode) {
                                ({})
                            } else {
                                ({ shareOrToast(content.raw) })
                            },
                            onCopyHex = onCopyHex,
                            onCopyBase64 = onCopyBase64,
                            onOpenLink = if (demoMode) {
                                ({})
                            } else {
                                ({ openLinkOrToast(content.raw) })
                            },
                            onFieldLongClick = { field -> copyField(field.value) },
                            onScanAgain = { closeSheet() },
                        )
                    }

                    is QrContent.Text -> {
                        RawQrSheetContent(
                            reason = sheetReason.ifBlank { "Nội dung QR" },
                            raw = content.raw,
                            maskingActive = rawMaskingActive(
                                sensitiveMaskingLevel,
                                hasHiddenParsedFields = false,
                            ),
                            sensitiveMaskingLevel = sensitiveMaskingLevel,
                            onCopy = if (demoMode) {
                                ({})
                            } else {
                                ({ closeSheetAndCopy(content.raw) })
                            },
                            onShare = if (demoMode) {
                                ({})
                            } else {
                                ({ shareOrToast(content.raw) })
                            },
                            onCopyHex = onCopyHex,
                            onCopyBase64 = onCopyBase64,
                            onScanAgain = { closeSheet() },
                        )
                    }
                }
            }
        }
    }


    if (showCustomResolutionDialog) {
        AlertDialog(
            onDismissRequest = { showCustomResolutionDialog = false },
            title = { Text("Độ phân giải tùy chỉnh") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customResolutionWidth,
                        onValueChange = { customResolutionWidth = it.filter(Char::isDigit) },
                        label = { Text("Chiều rộng") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = customResolutionHeight,
                        onValueChange = { customResolutionHeight = it.filter(Char::isDigit) },
                        label = { Text("Chiều cao") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    customResolutionError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomResolutionDialog = false }) {
                    Text("Hủy")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val width = customResolutionWidth.toIntOrNull()
                        val height = customResolutionHeight.toIntOrNull()
                        val requested = if (width != null && height != null) {
                            AnalysisSize(width, height)
                        } else {
                            null
                        }
                        customResolutionError = when {
                            requested == null || requested.width <= 0 || requested.height <= 0 ->
                                "Kích thước không hợp lệ"
                            !CameraCapabilities.contains(supportedAnalysisSizes, requested) ->
                                "Camera không hỗ trợ độ phân giải này"
                            else -> null
                        }
                        if (customResolutionError == null && requested != null) {
                            customAnalysisSize = requested
                            analysisResolutionPreset = AnalysisResolutionPreset.CUSTOM
                            AppSettings.setCustomAnalysisSize(context, requested)
                            AppSettings.setAnalysisResolutionPreset(
                                context,
                                AnalysisResolutionPreset.CUSTOM,
                            )
                            showCustomResolutionDialog = false
                        }
                    },
                ) {
                    Text("Lưu")
                }
            },
        )
    }

    if (showCustomFpsDialog) {
        AlertDialog(
            onDismissRequest = { showCustomFpsDialog = false },
            title = { Text("FPS tùy chỉnh") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customFpsInput,
                        onValueChange = {
                            customFpsInput = it.filter(Char::isDigit).take(4)
                        },
                        label = { Text("FPS") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    customFpsError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFpsDialog = false }) {
                    Text("Hủy")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val fps = customFpsInput.toIntOrNull()
                        customFpsError = when {
                            fps == null ->
                                "FPS không hợp lệ"
                            fps < 1 ->
                                "FPS phải lớn hơn 0"
                            fps > fpsSupport.maxFps ->
                                if (fpsSupport.known) {
                                    "Camera chỉ hỗ trợ tối đa ${fpsSupport.maxFps} FPS"
                                } else {
                                    "Không xác định được giới hạn FPS của camera"
                                }
                            else -> null
                        }
                        if (customFpsError == null && fps != null) {
                            customAnalysisFps = fps
                            analysisFpsPreset = AnalysisFpsPreset.CUSTOM
                            AppSettings.setCustomAnalysisFps(context, fps)
                            AppSettings.setAnalysisFpsPreset(
                                context,
                                AnalysisFpsPreset.CUSTOM,
                            )
                            showCustomFpsDialog = false
                        }
                    },
                ) {
                    Text("Lưu")
                }
            },
        )
    }

    if (showCredits) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showCredits = false },
            text = {
                Column {
                    HideDialogStatusBar(demoMode)
                    CreditsContent(
                        version = BuildConfig.VERSION_NAME,
                        onOpenSource = { uriHandler.openUri(SOURCE_URL) },
                        onVersionTap = {
                            versionTaps += 1
                            if (versionTaps >= 5) {
                                versionTaps = 0
                                demoMode = !demoMode
                                if (demoMode) {
                                    zoomRatio = 1f
                                    demoCaseIndex = 0
                                    demoCaseMbInstalled = true
                                }
                                showCredits = false
                                Toast.makeText(
                                    context,
                                    if (demoMode) "Đã bật chế độ demo" else "Đã tắt chế độ demo",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCredits = false }) {
                    Text("Đóng")
                }
            },
        )
    }
}

@Composable
private fun SheetCopyShareActions(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCopyHex: (() -> Unit)? = null,
    onCopyBase64: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onCopy, modifier = Modifier.weight(1f)) {
            Icon(
                painter = painterResource(R.drawable.ic_copy),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sao chép")
        }
        FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Chia sẻ")
        }
    }

    if (onCopyHex != null && onCopyBase64 != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onCopyHex, modifier = Modifier.weight(1f)) {
                Text("Hex")
            }
            OutlinedButton(onClick = onCopyBase64, modifier = Modifier.weight(1f)) {
                Text("Base64")
            }
        }
    }
}

@Composable
private fun SheetScanAgainButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Quét tiếp")
    }
}

@Composable
private fun ParsedQrSheetContent(
    reason: String,
    data: VietQrData,
    bankName: String?,
    copyText: String,
    copyTitle: String,
    copyMaskingActive: Boolean,
    sensitiveMaskingLevel: Int,
    onOpenMb: (() -> Unit)?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCopyHex: (() -> Unit)?,
    onCopyBase64: (() -> Unit)?,
    onFieldLongClick: (String) -> Unit,
    onScanAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = reason, style = MaterialTheme.typography.titleLarge)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val bankLabel = bankName ?: "<BIN ${data.bankBin}>"
            val bankCopy = bankName ?: data.bankBin
            ParsedField(
                label = "Ngân hàng",
                value = bankLabel,
                sensitivity = QrFieldSensitivity.None,
                sensitiveMaskingLevel = sensitiveMaskingLevel,
                onLongClick = { onFieldLongClick(bankCopy) },
            )
            ParsedField(
                label = "Tài khoản",
                value = data.account,
                sensitivity = QrFieldSensitivity.Direct,
                sensitiveMaskingLevel = sensitiveMaskingLevel,
                onLongClick = { onFieldLongClick(data.account) },
            )
            data.amountVnd?.let { amount ->
                ParsedField(
                    label = "Số tiền",
                    value = formatVnd(amount),
                    sensitivity = QrFieldSensitivity.Direct,
                    sensitiveMaskingLevel = sensitiveMaskingLevel,
                    onLongClick = { onFieldLongClick(amount.toString()) },
                )
            }
            data.merchantName?.let { name ->
                ParsedField(
                    label = "Tên đơn vị",
                    value = name,
                    sensitivity = QrFieldSensitivity.Personal,
                    sensitiveMaskingLevel = sensitiveMaskingLevel,
                    onLongClick = { onFieldLongClick(name) },
                )
            }
            data.content?.let { content ->
                ParsedField(
                    label = "Nội dung",
                    value = content,
                    sensitivity = QrFieldSensitivity.Direct,
                    sensitiveMaskingLevel = sensitiveMaskingLevel,
                    onLongClick = { onFieldLongClick(content) },
                )
            }
        }

        SensitiveCopyableBlock(
            title = copyTitle,
            value = copyText,
            maskingActive = copyMaskingActive,
            sensitiveMaskingLevel = sensitiveMaskingLevel,
            maxHeight = 120.dp,
        )

        onOpenMb?.let { open ->
            Button(onClick = open, modifier = Modifier.fillMaxWidth()) {
                Text("Mở MB Bank")
            }
        }

        SheetCopyShareActions(
            onCopy = onCopy,
            onShare = onShare,
            onCopyHex = onCopyHex,
            onCopyBase64 = onCopyBase64,
        )

        SheetScanAgainButton(onClick = onScanAgain)
    }
}

private fun hasHiddenFields(
    fields: List<QrField>,
    maskingLevel: Int,
): Boolean = fields.any { isMaskingActive(it.sensitivity, maskingLevel) }

// Account is present in every parsed VietQR and is Direct.
private fun hasHiddenVietQrFields(
    data: VietQrData,
    maskingLevel: Int,
): Boolean = data.account.isNotBlank() &&
    isMaskingActive(QrFieldSensitivity.Direct, maskingLevel)

private fun rawMaskingActive(
    maskingLevel: Int,
    hasHiddenParsedFields: Boolean,
): Boolean = when (maskingLevel) {
    AppSettings.MASKING_OFF -> false
    AppSettings.MASKING_FULL -> true
    AppSettings.MASKING_LIGHT -> hasHiddenParsedFields
    else -> false
}

@Composable
private fun ParsedField(
    label: String,
    value: String,
    sensitivity: QrFieldSensitivity,
    sensitiveMaskingLevel: Int,
    onLongClick: () -> Unit,
) {
    var revealed by remember(label, value, sensitivity, sensitiveMaskingLevel) {
        mutableStateOf(false)
    }
    val maskingActive = isMaskingActive(sensitivity, sensitiveMaskingLevel)
    val hideValue = maskingActive && !revealed
    val copyAction = onLongClick
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (maskingActive) {
                VisibilityToggleIcon(
                    hidden = hideValue,
                    onToggle = { revealed = !revealed },
                )
            }
        }
        Text(
            text = if (hideValue) MASKED_SENSITIVE_VALUE else value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClickLabel = if (maskingActive) {
                        if (hideValue) "Hiện dữ liệu" else "Ẩn dữ liệu"
                    } else {
                        null
                    },
                    onLongClickLabel = "Sao chép",
                    onClick = {
                        if (maskingActive) {
                            revealed = !revealed
                        }
                    },
                    onLongClick = { copyAction() },
                ),
        )
    }
}

@Composable
private fun VisibilityToggleIcon(
    hidden: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                if (hidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
            ),
            contentDescription = if (hidden) "Hiện dữ liệu" else "Ẩn dữ liệu",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SensitiveCopyableBlock(
    title: String,
    value: String,
    maskingActive: Boolean,
    sensitiveMaskingLevel: Int,
    maxHeight: Dp,
) {
    var revealed by remember(title, value, maskingActive, sensitiveMaskingLevel) {
        mutableStateOf(false)
    }
    val hideValue = maskingActive && !revealed

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        if (maskingActive) {
            VisibilityToggleIcon(
                hidden = hideValue,
                onToggle = { revealed = !revealed },
            )
        }
    }

    if (maskingActive) {
        Text(
            text = if (hideValue) MASKED_SENSITIVE_VALUE else value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .clickable { revealed = !revealed },
        )
    } else {
        SelectionContainer {
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

@Composable
private fun DetailsQrSheetContent(
    details: QrContent.Details,
    sensitiveMaskingLevel: Int,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCopyHex: (() -> Unit)?,
    onCopyBase64: (() -> Unit)?,
    onOpenLink: () -> Unit,
    onFieldLongClick: (QrField) -> Unit,
    onScanAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = details.title, style = MaterialTheme.typography.titleLarge)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            details.fields.forEach { field ->
                ParsedField(
                    label = field.label,
                    value = field.value,
                    sensitivity = field.sensitivity,
                    sensitiveMaskingLevel = sensitiveMaskingLevel,
                    onLongClick = { onFieldLongClick(field) },
                )
            }
        }

        if (!UrlDetector.isHttpUrl(details.raw)) {
            SensitiveCopyableBlock(
                title = RAW_CONTENT_TITLE,
                value = details.raw,
                maskingActive = rawMaskingActive(
                    sensitiveMaskingLevel,
                    hasHiddenFields(details.fields, sensitiveMaskingLevel),
                ),
                sensitiveMaskingLevel = sensitiveMaskingLevel,
                maxHeight = 120.dp,
            )
        }

        SheetCopyShareActions(
            onCopy = onCopy,
            onShare = onShare,
            onCopyHex = onCopyHex,
            onCopyBase64 = onCopyBase64,
        )

        if (UrlDetector.isHttpUrl(details.raw)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onOpenLink,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mở liên kết")
                }
                SheetScanAgainButton(
                    onClick = onScanAgain,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            SheetScanAgainButton(onClick = onScanAgain)
        }
    }
}

@Composable
private fun RawQrSheetContent(
    reason: String,
    raw: String,
    maskingActive: Boolean,
    sensitiveMaskingLevel: Int,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCopyHex: (() -> Unit)?,
    onCopyBase64: (() -> Unit)?,
    onScanAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = reason, style = MaterialTheme.typography.titleLarge)
        SensitiveCopyableBlock(
            title = RAW_CONTENT_TITLE,
            value = raw,
            maskingActive = maskingActive,
            sensitiveMaskingLevel = sensitiveMaskingLevel,
            maxHeight = 220.dp,
        )
        SheetCopyShareActions(
            onCopy = onCopy,
            onShare = onShare,
            onCopyHex = onCopyHex,
            onCopyBase64 = onCopyBase64,
        )

        SheetScanAgainButton(onClick = onScanAgain)
    }
}

private fun formatVnd(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫"

@Composable
private fun CreditsContent(
    version: String,
    onOpenSource: () -> Unit,
    onVersionTap: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "MBQR", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Phiên bản $version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onVersionTap),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        CreditItem(label = "Tác giả", value = "CleveTok3125")
        CreditItem(
            label = "Mã nguồn",
            value = "github.com/CleveTok3125/mbqr",
            onClick = onOpenSource,
        )
        CreditItem(label = "Giấy phép", value = "GPL-3.0 · ZXing-C++ Apache-2.0")
    }
}

@Composable
private fun CreditItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        )
    }
}

private const val SOURCE_URL = "https://github.com/CleveTok3125/mbqr"

@Composable
private fun ScannerOverlay(
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    clipboardRemaining: Int,
    clipboardPersistent: Boolean,
    onRestoreClipboard: (() -> Unit)?,
    onClearClipboard: (() -> Unit)?,
    frameSize: Dp,
    frameOffsetY: Dp,
    frameStyle: ScanFrameStyle,
    boundaryStyle: ScanBoundaryStyle,
    scanCrop: ScanCrop?,
    showTorch: Boolean,
    showImage: Boolean,
    onPickImage: () -> Unit,
    onShowSettings: () -> Unit,
    pipelineStatus: QrPipelineStatus? = null,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val clipboardLabel = when {
        clipboardRemaining > 0 -> "Clipboard còn ${clipboardRemaining}s"
        clipboardPersistent -> "Clipboard không tự xoá"
        else -> null
    }
    val scanBoundaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        ScanFrameOutline(
            style = frameStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = frameOffsetY)
                .size(frameSize),
        )

        scanCrop?.let { crop ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val left = crop.left * size.width
                val top = crop.top * size.height
                val right = crop.right * size.width
                val bottom = crop.bottom * size.height
                if (right > left && bottom > top) {
                    drawRoundRect(
                        color = scanBoundaryColor,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                        cornerRadius = CornerRadius(scanFrameBoundaryRadius(boundaryStyle).toPx()),
                        style = Stroke(
                            width = ScanBoundaryStrokeWidth.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(
                                    ScanBoundaryDashOn.toPx(),
                                    ScanBoundaryDashOff.toPx(),
                                ),
                            ),
                        ),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusTop)
                .background(Color.Black.copy(alpha = 0.45f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onShowSettings,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cài đặt")
                }

                if (showTorch) {
                    FilledTonalButton(
                        onClick = onToggleTorch,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (torchOn) {
                                TorchOnContainer
                            } else {
                                Color.Black.copy(alpha = 0.45f)
                            },
                            contentColor = if (torchOn) TorchOnContent else Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (torchOn) R.drawable.ic_flash_off else R.drawable.ic_flash_on,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (torchOn) "Tắt đèn" else "Bật đèn")
                    }
                }
            }

            if (showImage || clipboardLabel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    clipboardLabel?.let { label ->
                        Text(
                            text = label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(50),
                                )
                                .then(
                                    if (onRestoreClipboard != null || onClearClipboard != null) {
                                        Modifier.combinedClickable(
                                            onClick = { onRestoreClipboard?.invoke() },
                                            onLongClick = { onClearClipboard?.invoke() },
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }

                    if (showImage) {
                        FilledTonalButton(
                            onClick = onPickImage,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(48.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.45f),
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_image),
                                contentDescription = "Quét ảnh",
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            pipelineStatus?.takeIf { it.active }?.let { status ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PipelineStatusPill(status)
                }
            }
        }

    }
}

@Composable
private fun PipelineStatusPill(status: QrPipelineStatus) {
    val result = when (status.success) {
        true -> "ok"
        false -> "fail"
        null -> "running"
    }
    Text(
        text = "${status.source.label()} · ${status.preset.label()}\n" +
            "${status.stage.label()} · $result · ${status.elapsedMs}ms · " +
            "brightness=${status.brightness ?: "-"}",
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun QrPipelineSource.label(): String = when (this) {
    QrPipelineSource.LIVE -> "Live"
    QrPipelineSource.IMPORT -> "Import"
    QrPipelineSource.SHUTTER -> "Shutter"
    QrPipelineSource.SHARED -> "Share"
}

private fun QrPipelineStage.label(): String = when (this) {
    QrPipelineStage.DIRECT -> "direct"
    QrPipelineStage.PAD -> "pad"
    QrPipelineStage.UPSCALE -> "upscale"
    QrPipelineStage.DONE -> "done"
}

@Composable
private fun PausedContent(
    clipboardRemaining: Int,
    clipboardPersistent: Boolean,
    reminder: String?,
    openLabel: String,
    onOpen: (() -> Unit)?,
    onScanAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Đã copy thông tin chuyển khoản",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                clipboardPersistent -> "Clipboard không tự xoá"
                clipboardRemaining > 0 -> "Clipboard tự xoá sau ${clipboardRemaining}s"
                else -> "Clipboard đã được xoá"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        reminder?.let { text ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        onOpen?.let { open ->
            FilledTonalButton(onClick = open) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(openLabel)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Button(onClick = onScanAgain) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quét mã khác")
        }
    }
}

@Composable
private fun CameraErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Không mở được camera",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thử lại")
        }
    }
}

@Composable
private fun PermissionContent(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Cần quyền camera",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "MBQR dùng camera để đọc mã VietQR.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("Cấp quyền")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text("Mở cài đặt", color = Color.White)
        }
    }
}

private fun decodeCapturedImage(
    image: ImageProxy,
    preset: QrDecodePreset,
    source: QrPipelineSource,
    onPipelineStatus: ((QrPipelineStatus) -> Unit)?,
): String? {
    if (image.format == ImageFormat.JPEG) {
        val buffer = image.planes.firstOrNull()?.buffer ?: return null
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return PhotoQrDecoder.decodeImage(bytes, preset, source, onPipelineStatus)
    }
    return PhotoQrDecoder.decodeBitmap(
        image.toBitmap(),
        preset,
        source,
        onPipelineStatus,
    )
}

private val TorchOnContainer = Color(0xFFFFC107)
private val TorchOnContent = Color(0xFF1B1B1B)

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
