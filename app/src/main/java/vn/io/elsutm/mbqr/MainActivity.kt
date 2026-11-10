package vn.io.elsutm.mbqr

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.io.elsutm.mbqr.handoff.SharedImageIntent
import vn.io.elsutm.mbqr.ui.ScannerScreen
import vn.io.elsutm.mbqr.ui.theme.MbqrTheme

class MainActivity : ComponentActivity() {

    private var sharedImageUri by mutableStateOf<Uri?>(null)
    private var sharedImageNonce by mutableIntStateOf(0)
    private var lastHandledShareUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lastHandledShareUri = savedInstanceState?.getString(KEY_LAST_SHARE_URI)
        val initialUri = SharedImageIntent.extract(intent)
        if (initialUri?.toString() != lastHandledShareUri) {
            handleSharedImage(initialUri)
        }

        setContent {
            MbqrTheme {
                ScannerScreen(
                    sharedImageUri = sharedImageUri,
                    sharedImageNonce = sharedImageNonce,
                    onSharedImageConsumed = { sharedImageUri = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedImage(SharedImageIntent.extract(intent))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LAST_SHARE_URI, lastHandledShareUri)
    }

    private fun handleSharedImage(uri: Uri?) {
        if (uri == null) return

        lastHandledShareUri = uri.toString()
        sharedImageUri = uri
        sharedImageNonce += 1
    }

    private companion object {
        const val KEY_LAST_SHARE_URI = "last_share_uri"
    }
}
