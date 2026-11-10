package vn.io.elsutm.mbqr.handoff

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Opens the MB transfer hub. Falls back to the MB launcher activity when the
 * deeplink cannot be resolved.
 */
object MbLauncher {

    private const val PACKAGE = "com.mbmobile"
    private const val TRANSFER_DEEPLINK = "mbbank://?ad_dp=transfer"

    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    fun open(context: Context): Boolean {
        val deeplink = Intent(Intent.ACTION_VIEW, TRANSFER_DEEPLINK.toUri()).apply {
            setPackage(PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(deeplink)
            return true
        } catch (_: ActivityNotFoundException) {
            // Fall through to the launcher activity.
        }

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(PACKAGE)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?: return false

        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
