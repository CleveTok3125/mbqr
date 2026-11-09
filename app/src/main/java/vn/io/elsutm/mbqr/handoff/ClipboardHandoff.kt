package vn.io.elsutm.mbqr.handoff

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

/**
 * Writes and clears the transfer text on the system clipboard.
 *
 * The clipboard entry is marked sensitive so Android 13+ hides its preview.
 * Clearing only happens when the current clipboard still holds our text, so a
 * newer copy made by the user is never discarded.
 */
object ClipboardHandoff {

    private const val LABEL = "MBQR transfer"

    fun copy(context: Context, text: String) {
        val clip = ClipData.newPlainText(LABEL, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard(context).setPrimaryClip(clip)
    }

    fun clearIfMatches(context: Context, expected: String) {
        val manager = clipboard(context)
        val current = manager.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?: return
        if (current != expected) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.clearPrimaryClip()
        } else {
            manager.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun clipboard(context: Context): ClipboardManager =
        context.getSystemService(ClipboardManager::class.java)
}
