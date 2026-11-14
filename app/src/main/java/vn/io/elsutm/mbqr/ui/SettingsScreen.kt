package vn.io.elsutm.mbqr.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import vn.io.elsutm.mbqr.R
import vn.io.elsutm.mbqr.camera.CameraCapabilities
import vn.io.elsutm.mbqr.qr.QrFieldSensitivity
import vn.io.elsutm.mbqr.settings.AnalysisFpsPreset
import vn.io.elsutm.mbqr.settings.AnalysisResolutionPreset
import vn.io.elsutm.mbqr.settings.AnalysisSize
import vn.io.elsutm.mbqr.settings.AppSettings
import vn.io.elsutm.mbqr.settings.QrDecodePreset
import vn.io.elsutm.mbqr.settings.ScanBoundaryStyle
import vn.io.elsutm.mbqr.settings.ScanFrameStyle
import vn.io.elsutm.mbqr.settings.SettingsDefaults

/** Current settings values rendered by [SettingsScreen]. */
internal data class SettingsSnapshot(
    val confirmBeforeOpeningMb: Boolean,
    val clipboardExpirySeconds: Int,
    val scanInsideFrame: Boolean,
    val scanFrameMarginPercent: Int,
    val sensitiveMaskingLevel: Int,
    val liveDecodePreset: QrDecodePreset,
    val stillDecodePreset: QrDecodePreset,
    val pipelineStatusEnabled: Boolean,
    val advancedCopyEnabled: Boolean,
    val sanitizeResults: Boolean,
    val hapticsEnabled: Boolean,
    val scanFrameStyle: ScanFrameStyle,
    val scanBoundaryStyle: ScanBoundaryStyle,
    val analysisResolutionPreset: AnalysisResolutionPreset,
    val customAnalysisSize: AnalysisSize,
    val analysisFpsPreset: AnalysisFpsPreset,
    val customAnalysisFps: Int,
) {
    val analysisFps: Int get() = analysisFpsPreset.fps ?: customAnalysisFps
}

/** User edits emitted by [SettingsScreen]. */
internal sealed interface SettingsChange {
    data class ConfirmBeforeOpeningMb(val value: Boolean) : SettingsChange
    data class ClipboardExpirySeconds(val value: Int) : SettingsChange
    data object CommitClipboardExpiry : SettingsChange
    data class ScanInsideFrame(val value: Boolean) : SettingsChange
    data class ScanFrameMarginPercent(val value: Int) : SettingsChange
    data object CommitScanFrameMargin : SettingsChange
    data class SensitiveMaskingLevel(val value: Int) : SettingsChange
    data class LiveDecodePreset(val value: QrDecodePreset) : SettingsChange
    data class StillDecodePreset(val value: QrDecodePreset) : SettingsChange
    data class PipelineStatusEnabled(val value: Boolean) : SettingsChange
    data class AdvancedCopyEnabled(val value: Boolean) : SettingsChange
    data class SanitizeResults(val value: Boolean) : SettingsChange
    data class HapticsEnabled(val value: Boolean) : SettingsChange
    data class FrameStyle(val value: ScanFrameStyle) : SettingsChange
    data class BoundaryStyle(val value: ScanBoundaryStyle) : SettingsChange
    data class ResolutionPreset(val value: AnalysisResolutionPreset) : SettingsChange
    data class FpsPreset(val value: AnalysisFpsPreset) : SettingsChange
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    values: SettingsSnapshot,
    defaults: SettingsDefaults,
    supportedAnalysisSizes: List<AnalysisSize>,
    maxFps: Int,
    fpsKnown: Boolean,
    onValueChange: (SettingsChange) -> Unit,
    onEditCustomResolution: () -> Unit,
    onEditCustomFps: () -> Unit,
    onOpenCredits: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onClose: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var liveExpanded by rememberSaveable { mutableStateOf(false) }
    var stillExpanded by rememberSaveable { mutableStateOf(false) }
    var scanSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var appearanceSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var interactionSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var integrationSectionExpanded by rememberSaveable { mutableStateOf(true) }
    var privacySectionExpanded by rememberSaveable { mutableStateOf(true) }
    var advancedSectionExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onClose)
    SettingsStatusBarAppearance()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text("Cài đặt") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Quay lại",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "Tùy chọn khác",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Thông tin") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenCredits()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Khôi phục mặc định") },
                                onClick = {
                                    menuExpanded = false
                                    confirmRestore = true
                                },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 32.dp),
            ) {
                SettingsSection(
                    title = "Giao diện",
                    icon = R.drawable.ic_palette,
                    showTopDivider = false,
                    expanded = appearanceSectionExpanded,
                    onExpandChange = { appearanceSectionExpanded = it },
                ) {
                    ScanAppearancePreview(
                        frameStyle = values.scanFrameStyle,
                        boundaryStyle = values.scanBoundaryStyle,
                        marginPercent = values.scanFrameMarginPercent,
                        marginEnabled = values.scanInsideFrame,
                    )
                    ChoiceSetting(
                        title = "Kiểu khung",
                        options = ScanFrameStyle.entries,
                        selected = values.scanFrameStyle,
                        label = { it.label() },
                        resetVisible = values.scanFrameStyle != defaults.scanFrameStyle,
                        onReset = {
                            onValueChange(
                                SettingsChange.FrameStyle(defaults.scanFrameStyle),
                            )
                        },
                        onSelect = {
                            onValueChange(SettingsChange.FrameStyle(it))
                        },
                    )
                    ScanFrameMarginSetting(
                        percent = values.scanFrameMarginPercent,
                        enabled = values.scanInsideFrame,
                        resetVisible = values.scanInsideFrame &&
                            values.scanFrameMarginPercent != defaults.scanFrameMarginPercent,
                        onReset = {
                            onValueChange(
                                SettingsChange.ScanFrameMarginPercent(
                                    defaults.scanFrameMarginPercent,
                                ),
                            )
                            onValueChange(SettingsChange.CommitScanFrameMargin)
                        },
                        onPercentChange = {
                            onValueChange(SettingsChange.ScanFrameMarginPercent(it))
                        },
                        onPercentChangeFinished = {
                            onValueChange(SettingsChange.CommitScanFrameMargin)
                        },
                    )
                    ChoiceSetting(
                        title = "Kiểu lề",
                        options = ScanBoundaryStyle.entries,
                        selected = values.scanBoundaryStyle,
                        label = { it.label() },
                        enabled = values.scanInsideFrame,
                        resetVisible = values.scanInsideFrame &&
                            values.scanBoundaryStyle != defaults.scanBoundaryStyle,
                        onReset = {
                            onValueChange(
                                SettingsChange.BoundaryStyle(defaults.scanBoundaryStyle),
                            )
                        },
                        onSelect = {
                            onValueChange(SettingsChange.BoundaryStyle(it))
                        },
                    )
                }

                SettingsSection(
                    title = "Quét",
                    icon = R.drawable.ic_qr_code_scanner,
                    expanded = scanSectionExpanded,
                    onExpandChange = { scanSectionExpanded = it },
                ) {
                    ToggleSetting(
                        title = "Chỉ quét trong khung",
                        checked = values.scanInsideFrame,
                        resetVisible = values.scanInsideFrame != defaults.scanInsideFrame,
                        onReset = {
                            onValueChange(
                                SettingsChange.ScanInsideFrame(defaults.scanInsideFrame),
                            )
                        },
                        onCheckedChange = {
                            onValueChange(SettingsChange.ScanInsideFrame(it))
                        },
                    )
                }

                SettingsSection(
                    title = "Tương tác",
                    icon = R.drawable.ic_touch_app,
                    expanded = interactionSectionExpanded,
                    onExpandChange = { interactionSectionExpanded = it },
                ) {
                    ToggleSetting(
                        title = "Rung",
                        checked = values.hapticsEnabled,
                        resetVisible = values.hapticsEnabled != defaults.hapticsEnabled,
                        onReset = {
                            onValueChange(
                                SettingsChange.HapticsEnabled(defaults.hapticsEnabled),
                            )
                        },
                        onCheckedChange = {
                            onValueChange(SettingsChange.HapticsEnabled(it))
                        },
                    )
                }

                SettingsSection(
                    title = "Tích hợp",
                    icon = R.drawable.ic_link,
                    expanded = integrationSectionExpanded,
                    onExpandChange = { integrationSectionExpanded = it },
                ) {
                    ToggleSetting(
                        title = "Hỏi trước khi mở MB Bank",
                        checked = values.confirmBeforeOpeningMb,
                        resetVisible = values.confirmBeforeOpeningMb !=
                            defaults.confirmBeforeOpeningMb,
                        onReset = {
                            onValueChange(
                                SettingsChange.ConfirmBeforeOpeningMb(
                                    defaults.confirmBeforeOpeningMb,
                                ),
                            )
                        },
                        onCheckedChange = {
                            onValueChange(SettingsChange.ConfirmBeforeOpeningMb(it))
                        },
                    )
                }

                SettingsSection(
                    title = "Riêng tư",
                    icon = R.drawable.ic_shield,
                    expanded = privacySectionExpanded,
                    onExpandChange = { privacySectionExpanded = it },
                ) {
                    ChoiceSetting(
                        title = "Ẩn dữ liệu nhạy cảm",
                        options = listOf(
                            AppSettings.MASKING_OFF,
                            AppSettings.MASKING_LIGHT,
                            AppSettings.MASKING_FULL,
                        ),
                        selected = values.sensitiveMaskingLevel,
                        label = { level ->
                            when (level) {
                                AppSettings.MASKING_LIGHT -> "Nhẹ"
                                AppSettings.MASKING_FULL -> "Toàn bộ"
                                else -> "Tắt"
                            }
                        },
                        resetVisible = values.sensitiveMaskingLevel !=
                            defaults.sensitiveMaskingLevel,
                        onReset = {
                            onValueChange(
                                SettingsChange.SensitiveMaskingLevel(
                                    defaults.sensitiveMaskingLevel,
                                ),
                            )
                        },
                        onSelect = {
                            onValueChange(SettingsChange.SensitiveMaskingLevel(it))
                        },
                    )
                    MaskingPreview(level = values.sensitiveMaskingLevel)
                    ClipboardExpirySetting(
                        seconds = values.clipboardExpirySeconds,
                        resetVisible = values.clipboardExpirySeconds !=
                            defaults.clipboardExpirySeconds,
                        onReset = {
                            onValueChange(
                                SettingsChange.ClipboardExpirySeconds(
                                    defaults.clipboardExpirySeconds,
                                ),
                            )
                            onValueChange(SettingsChange.CommitClipboardExpiry)
                        },
                        onSecondsChange = {
                            onValueChange(SettingsChange.ClipboardExpirySeconds(it))
                        },
                        onSecondsChangeFinished = {
                            onValueChange(SettingsChange.CommitClipboardExpiry)
                        },
                    )
                }

                SettingsSection(
                    title = "Nâng cao",
                    icon = R.drawable.ic_tune,
                    expanded = advancedSectionExpanded,
                    onExpandChange = { advancedSectionExpanded = it },
                ) {
                    ToggleSetting(
                        title = "Lọc ký tự không an toàn",
                        checked = values.sanitizeResults,
                        resetVisible = values.sanitizeResults != defaults.sanitizeResults,
                        onReset = {
                            onValueChange(
                                SettingsChange.SanitizeResults(defaults.sanitizeResults),
                            )
                        },
                        onCheckedChange = {
                            onValueChange(SettingsChange.SanitizeResults(it))
                        },
                    )
                    ToggleSetting(
                        title = "Bật copy nâng cao",
                        checked = values.advancedCopyEnabled,
                        resetVisible = values.advancedCopyEnabled !=
                            defaults.advancedCopyEnabled,
                        onReset = {
                            onValueChange(
                                SettingsChange.AdvancedCopyEnabled(
                                    defaults.advancedCopyEnabled,
                                ),
                            )
                        },
                        onCheckedChange = {
                            onValueChange(SettingsChange.AdvancedCopyEnabled(it))
                        },
                    )
                    ToggleSetting(
                        title = "Hiện thông tin xử lý",
                        checked = values.pipelineStatusEnabled,
                        resetVisible = values.pipelineStatusEnabled !=
                            defaults.pipelineStatusEnabled,
                        onReset = {
                            onValueChange(
                                SettingsChange.PipelineStatusEnabled(
                                    defaults.pipelineStatusEnabled,
                                ),
                            )
                        },
                        onCheckedChange = {
                            onValueChange(SettingsChange.PipelineStatusEnabled(it))
                        },
                    )
                    ExpandableSetting(
                        title = "Quét trực tiếp",
                        summary = liveSummary(
                            values.analysisResolutionPreset,
                            values.customAnalysisSize,
                            values.analysisFps,
                        ),
                        expanded = liveExpanded,
                        onExpandChange = { liveExpanded = it },
                    ) {
                        DecodePresetSetting(
                            title = "Giải mã",
                            selected = values.liveDecodePreset,
                            resetVisible = values.liveDecodePreset != defaults.liveDecodePreset,
                            onReset = {
                                onValueChange(
                                    SettingsChange.LiveDecodePreset(defaults.liveDecodePreset),
                                )
                            },
                            onSelect = {
                                onValueChange(SettingsChange.LiveDecodePreset(it))
                            },
                        )
                        ResolutionSetting(
                            selected = values.analysisResolutionPreset,
                            custom = values.customAnalysisSize,
                            supported = supportedAnalysisSizes,
                            resetVisible = values.analysisResolutionPreset !=
                                defaults.analysisResolutionPreset,
                            onReset = {
                                onValueChange(
                                    SettingsChange.ResolutionPreset(
                                        defaults.analysisResolutionPreset,
                                    ),
                                )
                            },
                            onSelect = {
                                onValueChange(SettingsChange.ResolutionPreset(it))
                            },
                            onCustomClick = onEditCustomResolution,
                        )
                        FpsSetting(
                            selected = values.analysisFpsPreset,
                            custom = values.customAnalysisFps,
                            maxFps = maxFps,
                            fpsKnown = fpsKnown,
                            resetVisible = values.analysisFpsPreset !=
                                defaults.analysisFpsPreset,
                            onReset = {
                                onValueChange(
                                    SettingsChange.FpsPreset(
                                        defaults.analysisFpsPreset,
                                    ),
                                )
                            },
                            onSelect = {
                                onValueChange(SettingsChange.FpsPreset(it))
                            },
                            onCustomClick = onEditCustomFps,
                        )
                    }
                    ExpandableSetting(
                        title = "Quét ảnh",
                        summary = values.stillDecodePreset.label(),
                        expanded = stillExpanded,
                        onExpandChange = { stillExpanded = it },
                    ) {
                        DecodePresetSetting(
                            title = "Giải mã",
                            selected = values.stillDecodePreset,
                            resetVisible = values.stillDecodePreset !=
                                defaults.stillDecodePreset,
                            onReset = {
                                onValueChange(
                                    SettingsChange.StillDecodePreset(
                                        defaults.stillDecodePreset,
                                    ),
                                )
                            },
                            onSelect = {
                                onValueChange(SettingsChange.StillDecodePreset(it))
                            },
                        )
                    }
                }
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Khôi phục mặc định") },
            text = { Text("Đưa toàn bộ cài đặt về giá trị mặc định?") },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) {
                    Text("Hủy")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        onRestoreDefaults()
                    },
                ) {
                    Text("Khôi phục")
                }
            },
        )
    }
}

/**
 * Keeps system bar icons readable while the light surface is on screen.
 *
 * The scanner stays on a black background, so icons revert to light on exit.
 */
@Composable
private fun SettingsStatusBarAppearance() {
    val lightSurface = !isSystemInDarkTheme()
    val view = LocalView.current
    DisposableEffect(lightSurface) {
        val controller = view.context.findActivity()?.window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        controller?.isAppearanceLightStatusBars = lightSurface
        controller?.isAppearanceLightNavigationBars = lightSurface
        onDispose {
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    @DrawableRes icon: Int,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    showTopDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        if (showTopDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Thu gọn" else "Mở rộng",
                    onClick = { onExpandChange(!expanded) },
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (expanded) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun SettingHeader(
    title: String,
    valueLabel: String? = null,
    resetVisible: Boolean = false,
    onReset: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (resetVisible && onReset != null) {
            ResetDefaultAction(onReset = onReset)
        }
        if (valueLabel != null) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResetDefaultAction(onReset: () -> Unit) {
    Text(
        text = "Mặc định",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Về giá trị mặc định",
                onClick = onReset,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun ToggleSetting(
    title: String,
    checked: Boolean,
    resetVisible: Boolean,
    onReset: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (resetVisible) {
                ResetDefaultAction(onReset = onReset)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun <T> ChoiceSetting(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    resetVisible: Boolean,
    onReset: () -> Unit,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingHeader(
            title = title,
            resetVisible = resetVisible,
            onReset = onReset,
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        enabled = enabled,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size,
                        ),
                        label = {
                            Text(text = label(option), maxLines = 1)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DecodePresetSetting(
    title: String,
    selected: QrDecodePreset,
    resetVisible: Boolean,
    onReset: () -> Unit,
    onSelect: (QrDecodePreset) -> Unit,
) {
    ChoiceSetting(
        title = title,
        options = QrDecodePreset.entries,
        selected = selected,
        label = { it.label() },
        resetVisible = resetVisible,
        onReset = onReset,
        onSelect = onSelect,
    )
}

@Composable
private fun ResolutionSetting(
    selected: AnalysisResolutionPreset,
    custom: AnalysisSize,
    supported: List<AnalysisSize>,
    resetVisible: Boolean,
    onReset: () -> Unit,
    onSelect: (AnalysisResolutionPreset) -> Unit,
    onCustomClick: () -> Unit,
) {
    val presets = CameraCapabilities.fixedPresets.filter { (preset, size) ->
        supported.isEmpty() ||
            CameraCapabilities.contains(supported, size) ||
            preset == selected
    }
    val optionCount = presets.size + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingHeader(
            title = "Độ phân giải",
            valueLabel = resolutionLabel(selected, custom),
            resetVisible = resetVisible,
            onReset = onReset,
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SingleChoiceSegmentedButtonRow {
                presets.forEachIndexed { index, (preset, size) ->
                    SegmentedButton(
                        selected = selected == preset,
                        onClick = { onSelect(preset) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = optionCount,
                        ),
                        enabled = supported.isEmpty() ||
                            CameraCapabilities.contains(supported, size),
                        label = { Text(text = size.shortLabel(), maxLines = 1) },
                    )
                }
                SegmentedButton(
                    selected = selected == AnalysisResolutionPreset.CUSTOM,
                    onClick = onCustomClick,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = optionCount - 1,
                        count = optionCount,
                    ),
                    label = { Text(text = "Khác", maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun FpsSetting(
    selected: AnalysisFpsPreset,
    custom: Int,
    maxFps: Int,
    fpsKnown: Boolean,
    resetVisible: Boolean,
    onReset: () -> Unit,
    onSelect: (AnalysisFpsPreset) -> Unit,
    onCustomClick: () -> Unit,
) {
    val fps = selected.fps ?: custom
    val presets = AnalysisFpsPreset.fixed.filter { preset ->
        val presetFps = preset.fps ?: return@filter false
        presetFps <= maxFps || preset == selected
    }
    val optionCount = presets.size + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingHeader(
            title = "Tốc độ",
            valueLabel = "$fps FPS",
            resetVisible = resetVisible,
            onReset = onReset,
        )
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SingleChoiceSegmentedButtonRow {
                presets.forEachIndexed { index, preset ->
                    val presetFps = preset.fps ?: return@forEachIndexed
                    SegmentedButton(
                        selected = selected == preset,
                        onClick = { onSelect(preset) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = optionCount,
                        ),
                        enabled = presetFps <= maxFps,
                        label = { Text(text = "$presetFps", maxLines = 1) },
                    )
                }
                SegmentedButton(
                    selected = selected == AnalysisFpsPreset.CUSTOM,
                    onClick = onCustomClick,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = optionCount - 1,
                        count = optionCount,
                    ),
                    label = { Text(text = "Khác", maxLines = 1) },
                )
            }
        }
        if (fpsKnown && fps > maxFps) {
            Text(
                text = "Camera chỉ chạy tối đa $maxFps FPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScanFrameMarginSetting(
    percent: Int,
    enabled: Boolean,
    resetVisible: Boolean,
    onReset: () -> Unit,
    onPercentChange: (Int) -> Unit,
    onPercentChangeFinished: () -> Unit,
) {
    val maxPercent = AppSettings.MAX_SCAN_FRAME_MARGIN_PERCENT
    val stepPercent = AppSettings.SCAN_FRAME_MARGIN_STEP_PERCENT

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingHeader(
            title = "Lề vùng quét",
            valueLabel = "$percent%",
            resetVisible = resetVisible,
            onReset = onReset,
        )
        Slider(
            value = percent.toFloat(),
            onValueChange = { value ->
                onPercentChange(AppSettings.snappedScanFrameMarginPercent(value))
            },
            onValueChangeFinished = onPercentChangeFinished,
            valueRange = 0f..maxPercent.toFloat(),
            steps = maxPercent / stepPercent - 1,
            enabled = enabled,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EndpointLabel("Sát khung")
            EndpointLabel("Rộng")
        }
        if (!enabled) {
            Text(
                text = "Bật “Chỉ quét trong khung” để chỉnh lề.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScanAppearancePreview(
    frameStyle: ScanFrameStyle,
    boundaryStyle: ScanBoundaryStyle,
    marginPercent: Int,
    marginEnabled: Boolean,
) {
    val color = MaterialTheme.colorScheme.primary
    val extra = scanMargin(minGap = 12.dp, percent = marginPercent)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScanFrameOutline(
                style = frameStyle,
                color = color,
                modifier = Modifier.size(40.dp),
            )
            if (marginEnabled) {
                val outerRadius = if (boundaryStyle == ScanBoundaryStyle.SQUARE) {
                    0.dp
                } else {
                    8.dp
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outer = (40.dp + extra * 2).toPx()
                        .coerceAtMost(size.minDimension - ScanBoundaryStrokeWidth.toPx())

                    drawRoundRect(
                        color = color.copy(alpha = 0.5f),
                        topLeft = Offset(center.x - outer / 2f, center.y - outer / 2f),
                        size = Size(outer, outer),
                        cornerRadius = CornerRadius(outerRadius.toPx()),
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
    }
}

@Composable
private fun ClipboardExpirySetting(
    seconds: Int,
    resetVisible: Boolean,
    onReset: () -> Unit,
    onSecondsChange: (Int) -> Unit,
    onSecondsChangeFinished: () -> Unit,
) {
    val maxSeconds = AppSettings.MAX_CLIPBOARD_EXPIRY_SECONDS
    val stepSeconds = AppSettings.CLIPBOARD_EXPIRY_STEP_SECONDS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingHeader(
            title = "Tự xoá clipboard",
            valueLabel = clipboardExpiryLabel(seconds),
            resetVisible = resetVisible,
            onReset = onReset,
        )
        Slider(
            value = seconds.toFloat(),
            onValueChange = { value ->
                onSecondsChange(AppSettings.snappedSliderExpirySeconds(value))
            },
            onValueChangeFinished = onSecondsChangeFinished,
            valueRange = 0f..maxSeconds.toFloat(),
            steps = maxSeconds / stepSeconds - 1,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EndpointLabel("Tắt")
            EndpointLabel("5 phút")
        }
    }
}

@Composable
private fun EndpointLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Header that reveals its settings in place while keeping the summary visible.
 */
@Composable
private fun ExpandableSetting(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    onClickLabel = if (expanded) "Thu gọn" else "Mở rộng",
                    role = Role.Button,
                    onClick = { onExpandChange(!expanded) },
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun MaskingPreview(level: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MaskingSampleRow("Ngân hàng", "MB Bank", QrFieldSensitivity.None, level)
            MaskingSampleRow("Tài khoản", "0123456789", QrFieldSensitivity.Direct, level)
            MaskingSampleRow("Số tiền", "150.000 ₫", QrFieldSensitivity.Direct, level)
            MaskingSampleRow(
                "Tên đơn vị",
                "NGUYEN VAN A",
                QrFieldSensitivity.Personal,
                level,
            )
        }
    }
}

@Composable
private fun MaskingSampleRow(
    label: String,
    value: String,
    sensitivity: QrFieldSensitivity,
    level: Int,
) {
    val hidden = isMaskingActive(sensitivity, level)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (hidden) MASKED_SENSITIVE_VALUE else value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

internal fun QrDecodePreset.label(): String = when (this) {
    QrDecodePreset.FAST -> "Nhanh"
    QrDecodePreset.MAXIMUM -> "Tối đa"
}

internal fun ScanFrameStyle.label(): String = when (this) {
    ScanFrameStyle.ROUNDED -> "Bo góc"
    ScanFrameStyle.CORNERS -> "Góc"
    ScanFrameStyle.SQUARE -> "Vuông"
}

internal fun ScanBoundaryStyle.label(): String = when (this) {
    ScanBoundaryStyle.ROUNDED -> "Bo góc"
    ScanBoundaryStyle.SQUARE -> "Vuông"
}

internal fun AnalysisSize.shortLabel(): String = "${height}p"

internal fun resolutionLabel(
    preset: AnalysisResolutionPreset,
    custom: AnalysisSize,
): String {
    if (preset == AnalysisResolutionPreset.CUSTOM) return custom.toString()
    return CameraCapabilities.fixedPresets
        .firstOrNull { (candidate, _) -> candidate == preset }
        ?.second
        ?.shortLabel()
        ?: preset.name
}

internal fun liveSummary(
    preset: AnalysisResolutionPreset,
    custom: AnalysisSize,
    fps: Int,
): String = "${resolutionLabel(preset, custom)} · $fps FPS"

internal fun clipboardExpiryLabel(seconds: Int): String {
    if (seconds <= 0) return "Không tự xoá"

    val minutes = seconds / 60
    val remainder = seconds % 60
    return when {
        minutes == 0 -> "$remainder giây"
        remainder == 0 -> "$minutes phút"
        else -> "$minutes phút $remainder giây"
    }
}
