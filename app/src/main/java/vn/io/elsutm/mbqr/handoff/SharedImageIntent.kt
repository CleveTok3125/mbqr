package vn.io.elsutm.mbqr.handoff

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/** Extracts a single image URI from an incoming share intent. */
object SharedImageIntent {

    fun extract(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_SEND) return null

        val mimeType = intent.type ?: intent.clipData?.description?.getMimeType(0)
        if (mimeType?.startsWith("image/") != true) return null

        return IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.uri
    }
}
