package vn.io.elsutm.mbqr.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import vn.io.elsutm.mbqr.BuildConfig
import vn.io.elsutm.mbqr.R
import vn.io.elsutm.mbqr.camera.PhotoQrDecoder
import vn.io.elsutm.mbqr.handoff.ClipboardHandoff
import vn.io.elsutm.mbqr.handoff.ClipboardSession
import vn.io.elsutm.mbqr.handoff.MbLauncher
import vn.io.elsutm.mbqr.handoff.QrActions
import vn.io.elsutm.mbqr.handoff.UrlDetector
import vn.io.elsutm.mbqr.service.ClipboardExpiryService
import vn.io.elsutm.mbqr.settings.AppSettings
import vn.io.elsutm.mbqr.vietqr.BankDirectory
import vn.io.elsutm.mbqr.vietqr.TransferBuilder
import vn.io.elsutm.mbqr.vietqr.VietQrData
import vn.io.elsutm.mbqr.vietqr.VietQrParseResult
import vn.io.elsutm.mbqr.vietqr.VietQrParser
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }

    var cameraGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    var cameraError by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraAttempt by rememberSaveable { mutableStateOf(0) }
    var scanningEnabled by rememberSaveable { mutableStateOf(true) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var sheetRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetReason by rememberSaveable { mutableStateOf("") }
    var sheetParsed by rememberSaveable { mutableStateOf(false) }
    var clipboardExpiresAt by rememberSaveable { mutableStateOf(0L) }
    var clipboardRemaining by rememberSaveable { mutableStateOf(0) }
    var lastClipboardText by remember { mutableStateOf(ClipboardSession.text) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showCredits by rememberSaveable { mutableStateOf(false) }
    var confirmBeforeOpeningMb by remember {
        mutableStateOf(AppSettings.confirmBeforeOpeningMb(context))
    }
    var clipboardExpirySeconds by remember {
        mutableStateOf(AppSettings.clipboardExpirySeconds(context))
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

    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose { captureExecutor.shutdown() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(clipboardExpiresAt) {
        while (true) {
            val remaining = clipboardExpiresAt - SystemClock.elapsedRealtime()
            clipboardRemaining = ((remaining + 999) / 1000).coerceAtLeast(0).toInt()
            if (remaining <= 0) break
            delay(500)
        }
    }

    LaunchedEffect(demoMode) {
        if (!demoMode) return@LaunchedEffect
        demoCountdown = 60
        while (true) {
            delay(1_000)
            demoCountdown = if (demoCountdown <= 1) 60 else demoCountdown - 1
        }
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val activity = context.findActivity()
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

    fun copyTransfer(text: String) {
        val expiryMillis = AppSettings.clipboardExpiryMillis(context)
        ClipboardHandoff.copy(context, text)
        ClipboardExpiryService.start(context, text, expiryMillis)
        ClipboardSession.text = text
        lastClipboardText = text
        clipboardExpiresAt = SystemClock.elapsedRealtime() + expiryMillis
    }

    fun restoreClipboard() {
        val text = ClipboardSession.text ?: lastClipboardText ?: return
        copyTransfer(text)
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        Toast.makeText(context, "Đã copy lại", Toast.LENGTH_SHORT).show()
    }

    fun clearClipboardNow() {
        val text = ClipboardSession.text ?: lastClipboardText ?: return
        ClipboardHandoff.clearIfMatches(context, text)
        ClipboardSession.text = null
        lastClipboardText = null
        clipboardExpiresAt = 0L
        clipboardRemaining = 0
        context.stopService(Intent(context, ClipboardExpiryService::class.java))
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        Toast.makeText(context, "Đã xoá clipboard", Toast.LENGTH_SHORT).show()
    }

    fun showRaw(raw: String, reason: String) {
        scanningEnabled = false
        sheetRaw = VietQrParser.normalized(raw)
        sheetParsed = false
        sheetReason = reason
    }

    fun showParsed(raw: String, reason: String) {
        scanningEnabled = false
        sheetRaw = VietQrParser.normalized(raw)
        sheetParsed = true
        sheetReason = reason
    }

    fun showNextDemoCase() {
        val case = DEMO_SCAN_CASES[demoCaseIndex % DEMO_SCAN_CASES.size]
        demoCaseIndex = (demoCaseIndex + 1) % DEMO_SCAN_CASES.size
        demoCaseMbInstalled = case.mbInstalled
        if (case.parsed) {
            showParsed(case.raw, case.reason)
        } else {
            showRaw(case.raw, case.reason)
        }
    }

    fun copyAndOpenTransfer(text: String) {
        // The clipboard and timer must be ready before MB reads the paste buffer.
        copyTransfer(text)
        if (MbLauncher.open(context)) {
            sheetRaw = null
        } else {
            sheetReason = "Không mở được MB Bank"
            Toast.makeText(context, "Không mở được MB Bank", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleDecoded(raw: String) {
        if (!scanningEnabled) return

        // Any decoded QR is a scan success: either a sheet or the MB handoff follows.
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        when (val result = VietQrParser.parse(raw)) {
            is VietQrParseResult.VietQr -> {
                val data = result.data
                if (!result.crcValid) {
                    showRaw(raw, "CRC không hợp lệ")
                    return
                }

                val transferText = BankDirectory.find(data.bankBin)
                    ?.prefix
                    ?.takeIf { it.isNotBlank() }
                    ?.let { TransferBuilder.build(data, it) }

                if (!MbLauncher.isInstalled(context)) {
                    showParsed(raw, "Chưa cài MB Bank")
                    return
                }

                if (transferText == null) {
                    showParsed(raw, "Chưa hỗ trợ ngân hàng BIN ${data.bankBin}")
                    return
                }

                scanningEnabled = false
                if (AppSettings.confirmBeforeOpeningMb(context)) {
                    showParsed(raw, "Xác nhận chuyển khoản")
                } else {
                    copyTransfer(transferText)
                    if (!MbLauncher.open(context)) {
                        showParsed(raw, "Không mở được MB Bank")
                    }
                }
            }

            VietQrParseResult.NotVietQr -> showRaw(raw, "Không phải VietQR")
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
                        decodeCapturedImage(image)
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

    fun decodePickedImage(uri: Uri) {
        captureExecutor.execute {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    PhotoQrDecoder.decodeJpeg(stream.readBytes())
                }
            } catch (_: Exception) {
                null
            }
            mainExecutor.execute {
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

                !scanningEnabled && sheetRaw == null -> PausedContent(
                    clipboardRemaining = clipboardRemaining,
                    onScanAgain = { scanningEnabled = true },
                )

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
                                    scanningEnabled = scanningEnabled,
                                    onDecoded = ::handleDecoded,
                                    onCameraError = { message -> cameraError = message },
                                    onCameraBound = { camera = it },
                                    onImageCaptureBound = { imageCapture = it },
                                    onZoomStateChanged = { state ->
                                        zoomMin = state.minZoomRatio
                                        zoomMax = state.maxZoomRatio
                                        zoomRatio = state.zoomRatio
                                    },
                                )
                            }
                        }

                        ScannerOverlay(
                            torchOn = torchOn,
                            onToggleTorch = { torchOn = !torchOn },
                            clipboardRemaining = if (demoMode) demoCountdown else clipboardRemaining,
                            onRestoreClipboard = when {
                                demoMode -> {
                                    {
                                        demoCountdown = 60
                                        view.performHapticFeedback(
                                            HapticFeedbackConstants.CLOCK_TICK,
                                        )
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
        }
    }

    val raw = sheetRaw
    if (raw != null) {
        val parsed = remember(raw) {
            (VietQrParser.parse(raw) as? VietQrParseResult.VietQr)?.data
        }
        val transferText = remember(parsed) {
            parsed?.let { data ->
                BankDirectory.find(data.bankBin)
                    ?.prefix
                    ?.takeIf { it.isNotBlank() }
                    ?.let { TransferBuilder.build(data, it) }
            }
        }
        val mbInstalled = remember(raw, demoMode, demoCaseMbInstalled) {
            if (demoMode) demoCaseMbInstalled else MbLauncher.isInstalled(context)
        }
        ModalBottomSheet(
            onDismissRequest = {
                sheetRaw = null
                scanningEnabled = true
            },
        ) {
            HideDialogStatusBar(demoMode)
            if (sheetParsed && parsed != null) {
                ParsedQrSheetContent(
                    reason = sheetReason,
                    data = parsed,
                    bankName = BankDirectory.find(parsed.bankBin)?.shortName,
                    transferText = transferText,
                    onOpenMb = if (transferText == null || !mbInstalled) {
                        null
                    } else if (demoMode) {
                        ({})
                    } else {
                        ({ copyAndOpenTransfer(transferText) })
                    },
                    onCopy = if (demoMode) {
                        ({})
                    } else {
                        {
                            copyTransfer(transferText ?: raw)
                            sheetRaw = null
                            scanningEnabled = true
                        }
                    },
                    onShare = if (demoMode) {
                        ({})
                    } else {
                        {
                            val text = transferText ?: raw
                            if (!QrActions.shareText(context, text)) {
                                Toast.makeText(
                                    context,
                                    "Không có ứng dụng để chia sẻ",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    onScanAgain = {
                        sheetRaw = null
                        scanningEnabled = true
                    },
                )
            } else {
                RawQrSheetContent(
                    reason = sheetReason,
                    raw = raw,
                    onCopy = if (demoMode) {
                        ({})
                    } else {
                        {
                            copyTransfer(raw)
                            sheetRaw = null
                            scanningEnabled = true
                        }
                    },
                    onShare = if (demoMode) {
                        ({})
                    } else {
                        {
                            if (!QrActions.shareText(context, raw)) {
                                Toast.makeText(
                                    context,
                                    "Không có ứng dụng để chia sẻ",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    onOpenLink = if (demoMode) {
                        ({})
                    } else {
                        {
                            if (!QrActions.openHttpUrl(context, raw)) {
                                Toast.makeText(
                                    context,
                                    "Không có ứng dụng để mở liên kết",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                    onScanAgain = {
                        sheetRaw = null
                        scanningEnabled = true
                    },
                )
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Cài đặt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HideDialogStatusBar(demoMode)
                    SettingsToggle(
                        label = "Hiện bảng xác nhận trước khi mở MB Bank",
                        checked = confirmBeforeOpeningMb,
                        onCheckedChange = { enabled ->
                            confirmBeforeOpeningMb = enabled
                            AppSettings.setConfirmBeforeOpeningMb(context, enabled)
                        },
                    )
                    ClipboardExpirySetting(
                        seconds = clipboardExpirySeconds,
                        onSecondsChange = { seconds -> clipboardExpirySeconds = seconds },
                        onSecondsChangeFinished = {
                            AppSettings.setClipboardExpirySeconds(context, clipboardExpirySeconds)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettings = false
                        showCredits = true
                    },
                ) {
                    Text("Thông tin")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Đóng")
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
private fun ParsedQrSheetContent(
    reason: String,
    data: VietQrData,
    bankName: String?,
    transferText: String?,
    onOpenMb: (() -> Unit)?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
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
            ParsedField(label = "Ngân hàng", value = bankName ?: "BIN ${data.bankBin}")
            ParsedField(label = "Tài khoản", value = data.account)
            data.amountVnd?.let { ParsedField(label = "Số tiền", value = formatVnd(it)) }
            data.merchantName?.let { ParsedField(label = "Tên đơn vị", value = it) }
            data.content?.let { ParsedField(label = "Nội dung", value = it) }
        }

        transferText?.let { text ->
            SelectionContainer {
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }

        onOpenMb?.let { open ->
            Button(onClick = open, modifier = Modifier.fillMaxWidth()) {
                Text("Mở MB Bank")
            }
        }

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

        OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quét tiếp")
        }
    }
}

@Composable
private fun ParsedField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RawQrSheetContent(
    reason: String,
    raw: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenLink: () -> Unit,
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
        SelectionContainer {
            Text(
                text = raw,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
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

        if (UrlDetector.isHttpUrl(raw)) {
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
                OutlinedButton(onClick = onScanAgain, modifier = Modifier.weight(1f)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quét tiếp")
                }
            }
        } else {
            OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quét tiếp")
            }
        }
    }
}

private fun formatVnd(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫"

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ClipboardExpirySetting(
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    onSecondsChangeFinished: () -> Unit,
) {
    val minSeconds = AppSettings.MIN_CLIPBOARD_EXPIRY_SECONDS
    val maxSeconds = AppSettings.MAX_CLIPBOARD_EXPIRY_SECONDS
    val stepSeconds = AppSettings.CLIPBOARD_EXPIRY_STEP_SECONDS

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Thời gian clipboard",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "$seconds giây",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = seconds.toFloat(),
            onValueChange = { onSecondsChange(it.roundToInt()) },
            onValueChangeFinished = onSecondsChangeFinished,
            valueRange = minSeconds.toFloat()..maxSeconds.toFloat(),
            steps = (maxSeconds - minSeconds) / stepSeconds - 1,
        )
    }
}

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
        CreditItem(label = "Giấy phép", value = "GPL-3.0")
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
    onRestoreClipboard: (() -> Unit)?,
    onClearClipboard: (() -> Unit)?,
    frameSize: Dp,
    frameOffsetY: Dp,
    showTorch: Boolean,
    showImage: Boolean,
    onPickImage: () -> Unit,
    onShowSettings: () -> Unit,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
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

            if (showImage || clipboardRemaining > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (clipboardRemaining > 0) {
                        Text(
                            text = "Clipboard còn ${clipboardRemaining}s",
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
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = frameOffsetY)
                .size(frameSize)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(28.dp),
                ),
        )
    }
}

@Composable
private fun PausedContent(
    clipboardRemaining: Int,
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
            text = if (clipboardRemaining > 0) {
                "Clipboard tự xoá sau ${clipboardRemaining}s"
            } else {
                "Clipboard đã được xoá"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
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

private fun decodeCapturedImage(image: ImageProxy): String? {
    if (image.format == ImageFormat.JPEG) {
        val buffer = image.planes.firstOrNull()?.buffer ?: return null
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return PhotoQrDecoder.decodeJpeg(bytes)
    }
    return PhotoQrDecoder.decodeBitmap(image.toBitmap())
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private val TorchOnContainer = Color(0xFFFFC107)
private val TorchOnContent = Color(0xFF1B1B1B)

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
