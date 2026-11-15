package vn.io.elsutm.mbqr.handoff

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.core.net.toUri

data class BrowserOption(val packageName: String?, val label: String)

object BrowserCatalog {

    private const val PROBE_URL = "https://example.com"
    private const val SELECTED_LABEL = "Trình duyệt đã chọn"

    val chooser = BrowserOption(null, "Hỏi mỗi lần")

    fun options(context: Context, selectedPackage: String?): List<BrowserOption> {
        val probe = Intent(Intent.ACTION_VIEW, PROBE_URL.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)

        val packageManager = context.packageManager
        val candidates = packageManager.queryIntentActivities(probe, 0)
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName
                    ?: return@mapNotNull null
                Candidate(packageName, labelOf(packageManager, resolveInfo))
            }

        return merge(candidates, selectedPackage, context.packageName)
    }

    internal data class Candidate(val packageName: String, val label: String)

    internal fun merge(
        candidates: List<Candidate>,
        selectedPackage: String?,
        selfPackage: String,
    ): List<BrowserOption> {
        val installed = candidates
            .filter { it.packageName != selfPackage }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { candidate: Candidate -> candidate.label },
            )
            .map { BrowserOption(it.packageName, it.label) }

        return buildList {
            add(chooser)
            addAll(installed)
            // Keep the saved choice visible when that browser was uninstalled.
            if (selectedPackage != null &&
                selectedPackage != selfPackage &&
                installed.none { it.packageName == selectedPackage }
            ) {
                add(BrowserOption(selectedPackage, SELECTED_LABEL))
            }
        }
    }

    private fun labelOf(packageManager: PackageManager, resolveInfo: ResolveInfo): String {
        val applicationInfo = resolveInfo.activityInfo?.applicationInfo
        val label = applicationInfo?.loadLabel(packageManager)?.toString()?.trim()
        return label?.takeIf { it.isNotEmpty() } ?: resolveInfo.activityInfo?.packageName.orEmpty()
    }
}
