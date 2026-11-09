package vn.io.elsutm.mbqr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.io.elsutm.mbqr.R
import vn.io.elsutm.mbqr.handoff.ClipboardHandoff

/**
 * Keeps the transfer text on the clipboard for at most one minute, then clears
 * it. The service is also stopped immediately when the task is removed from
 * recents so the app never keeps running in the background.
 */
class ClipboardExpiryService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var expectedText: String? = null
    private var expiryJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(EXTRA_TEXT)
        if (text.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // A new handoff restarts the window instead of letting an older timer
        // clear the clipboard early.
        expiryJob?.cancel()
        expectedText = text
        startInForeground()

        expiryJob = scope.launch {
            delay(EXPIRY_MILLIS)
            clearAndStop()
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        expiryJob?.cancel()
        clearAndStop()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        clearClipboard()
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clipboard)
            .setContentTitle(getString(R.string.clipboard_notification_title))
            .setContentText(getString(R.string.clipboard_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun clearAndStop() {
        clearClipboard()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun clearClipboard() {
        val text = expectedText ?: return
        ClipboardHandoff.clearIfMatches(this, text)
        expectedText = null
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.clipboard_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val EXPIRY_MILLIS = 60_000L

        private const val CHANNEL_ID = "clipboard_expiry"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_TEXT = "extra_text"

        fun start(context: Context, text: String) {
            val intent = Intent(context, ClipboardExpiryService::class.java)
                .putExtra(EXTRA_TEXT, text)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
