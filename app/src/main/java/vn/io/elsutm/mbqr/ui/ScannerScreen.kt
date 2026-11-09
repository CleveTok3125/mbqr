package vn.io.elsutm.mbqr.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.io.elsutm.mbqr.R
import vn.io.elsutm.mbqr.camera.PhotoQrDecoder
import vn.io.elsutm.mbqr.handoff.ClipboardHandoff
import vn.io.elsutm.mbqr.handoff.MbLauncher
import vn.io.elsutm.mbqr.handoff.QrActions
import vn.io.elsutm.mbqr.handoff.UrlDetector
import vn.io.elsutm.mbqr.service.ClipboardExpiryService
import vn.io.elsutm.mbqr.vietqr.BankBin
import vn.io.elsutm.mbqr.vietqr.TransferBuilder
import vn.io.elsutm.mbqr.vietqr.VietQrParseResult
import vn.io.elsutm.mbqr.vietqr.VietQrParser
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var cameraGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    var cameraError by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraAttempt by rememberSaveable { mutableStateOf(0) }
    var scanningEnabled by rememberSaveable { mutableStateOf(true) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var sheetRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetReason by rememberSaveable { mutableStateOf("") }
    var clipboardExpiresAt by rememberSaveable { mutableStateOf(0L) }
    var clipboardRemaining by rememberSaveable { mutableStateOf(0) }
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

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    fun copyTransfer(text: String) {
        ClipboardHandoff.copy(context, text)
        ClipboardExpiryService.start(context, text)
        clipboardExpiresAt = SystemClock.elapsedRealtime() + ClipboardExpiryService.EXPIRY_MILLIS
    }

    fun showRaw(raw: String, reason: String) {
        scanningEnabled = false
        sheetRaw = VietQrParser.normalized(raw)
        sheetReason = reason
    }

    fun handleDecoded(raw: String) {
        if (!scanningEnabled) return

        when (val result = VietQrParser.parse(raw)) {
            is VietQrParseResult.VietQr -> {
                val data = result.data
                val bankPrefix = BankBin.transferPrefix(data.bankBin)

                when {
                    !result.crcValid -> showRaw(raw, "CRC không hợp lệ")
                    bankPrefix == null -> showRaw(raw, "Chưa hỗ trợ ngân hàng BIN ${data.bankBin}")
                    else -> {
                        scanningEnabled = false
                        val transferText = TransferBuilder.build(data, bankPrefix)
                        copyTransfer(transferText)
                        if (!MbLauncher.open(context)) {
                            scanningEnabled = true
                            scope.launch { snackbarHostState.showSnackbar("Không mở được MB Bank") }
                        }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
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

                    ScannerOverlay(
                        torchOn = torchOn,
                        onToggleTorch = { torchOn = !torchOn },
                        clipboardRemaining = clipboardRemaining,
                        showBottomInfo = camera == null,
                    )

                    if (camera != null) {
                        CameraControls(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 24.dp),
                            zoomRatio = zoomRatio,
                            minZoom = zoomMin,
                            maxZoom = zoomMax,
                            isCapturing = isCapturing,
                            showCapture = imageCapture != null,
                            onZoomChange = { ratio ->
                                zoomRatio = ratio
                                camera?.cameraControl?.setZoomRatio(ratio)
                            },
                            onCapture = ::capturePhoto,
                        )
                    }
                }
            }
        }
    }

    val raw = sheetRaw
    if (raw != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheetRaw = null
                scanningEnabled = true
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = sheetReason, style = MaterialTheme.typography.titleLarge)
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
                    Button(
                        onClick = {
                            copyTransfer(raw)
                            sheetRaw = null
                            scanningEnabled = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sao chép")
                    }
                    FilledTonalButton(
                        onClick = {
                            if (!QrActions.shareText(context, raw)) {
                                Toast.makeText(
                                    context,
                                    "Không có ứng dụng để chia sẻ",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
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
                            onClick = {
                                if (!QrActions.openHttpUrl(context, raw)) {
                                    Toast.makeText(
                                        context,
                                        "Không có ứng dụng để mở liên kết",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
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
                        OutlinedButton(
                            onClick = {
                                sheetRaw = null
                                scanningEnabled = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
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
                    OutlinedButton(
                        onClick = {
                            sheetRaw = null
                            scanningEnabled = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
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
    }
}

@Composable
private fun ScannerOverlay(
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    clipboardRemaining: Int,
    showBottomInfo: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Quét VietQR",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = onToggleTorch,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (torchOn) TorchOnContainer else Color.Black.copy(alpha = 0.45f),
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

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(28.dp),
                ),
        )

        if (showBottomInfo) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (clipboardRemaining > 0) {
                    Text(
                        text = "Clipboard tự xoá sau ${clipboardRemaining}s",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = "Đưa mã vào khung",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
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

private val TorchOnContainer = Color(0xFFFFC107)
private val TorchOnContent = Color(0xFF1B1B1B)

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
