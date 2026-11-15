package vn.io.elsutm.mbqr.handoff

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import vn.io.elsutm.mbqr.settings.LinkBrowserMode
import java.net.URI
import java.net.URISyntaxException

/**
 * Opens an external link through the browser target selected in settings.
 *
 * Custom Tab mode never falls back to a plain browser view: when the default
 * browser does not support Custom Tabs the link is reported as not opened.
 */
object BrowserLauncher {

    private const val SCHEME = "https"
    private const val HOST = "dl.vietqr.io"
    private const val PATH = "/pay"
    private const val PROBE_URL = "https://example.com"

    fun open(
        context: Context,
        url: String,
        mode: LinkBrowserMode,
        browserPackage: String?,
    ): Boolean {
        if (!isPayLink(url)) return false

        return when (mode) {
            LinkBrowserMode.CUSTOM_TAB -> openCustomTab(context, url)
            LinkBrowserMode.BROWSER_APP -> openBrowserApp(context, url, browserPackage)
        }
    }

    internal fun isPayLink(url: String): Boolean = try {
        val uri = URI(url)
        uri.scheme == SCHEME && uri.host == HOST && uri.path == PATH
    } catch (_: URISyntaxException) {
        false
    }

    private fun openCustomTab(context: Context, url: String): Boolean {
        val uri = url.toUri()
        val probe = Intent(Intent.ACTION_VIEW, PROBE_URL.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val defaultPackage = context.packageManager
            .resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
            ?: return false

        if (CustomTabsClient.getPackageName(context, listOf(defaultPackage)) == null) {
            return false
        }

        val customTab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTab.intent.setPackage(defaultPackage)
        customTab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            customTab.launchUrl(context, uri)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun openBrowserApp(context: Context, url: String, browserPackage: String?): Boolean {
        val viewIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val target = if (browserPackage != null) {
            viewIntent.setPackage(browserPackage)
        } else {
            Intent.createChooser(viewIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(target)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
