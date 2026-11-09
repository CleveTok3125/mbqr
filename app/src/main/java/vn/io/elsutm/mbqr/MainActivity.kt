package vn.io.elsutm.mbqr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.io.elsutm.mbqr.ui.ScannerScreen
import vn.io.elsutm.mbqr.ui.theme.MbqrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MbqrTheme {
                ScannerScreen()
            }
        }
    }
}
