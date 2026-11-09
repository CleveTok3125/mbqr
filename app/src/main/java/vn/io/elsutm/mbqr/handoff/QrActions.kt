package vn.io.elsutm.mbqr.handoff

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import vn.io.elsutm.mbqr.R

/**
 * Shares raw QR content or opens it as an HTTP(S) link.
 *
 * Returns false when no activity can handle the intent so the caller can show a
 * message without crashing.
 */
object QrActions {

    fun shareText(context: Context, text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.share_qr_title)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, chooser)
    }

    fun openHttpUrl(context: Context, url: String): Boolean {
        if (!UrlDetector.isHttpUrl(url)) return false

        val intent = Intent(Intent.ACTION_VIEW, url.trim().toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
