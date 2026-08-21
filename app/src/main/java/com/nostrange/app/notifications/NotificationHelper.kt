package com.nostrange.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nostrange.app.MainActivity
import com.nostrange.app.R

object NotificationHelper {

    const val CHANNEL_ID = "nostrange_messages_channel"
    private const val CHANNEL_NAME = "پیام‌های دریافتی"
    private const val CHANNEL_DESC = "اعلان پیام‌های رمزنگاری‌شده و درخواست‌های آشنایی Nostrange"

    const val EXTRA_OPEN_CHAT_PUBKEY = "extra_open_chat_pubkey"

    /**
     * Creates NotificationChannel for Android 8.0+ (API 26+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Builds and displays a notification when a new encrypted message is received.
     * Tapping on the notification opens MainActivity and navigates directly to the specific chat.
     */
    fun showMessageNotification(
        context: Context,
        senderPubkey: String,
        messageText: String,
        senderTitle: String? = null
    ) {
        try {
            val title = senderTitle ?: "پیام جدید در Nostrange"

            // Intent to open chat directly with sender
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_CHAT_PUBKEY, senderPubkey)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                senderPubkey.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = senderPubkey.hashCode()

            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission on Android 13+
        } catch (e: Exception) {
            // Silently handle any notification display exception
        }
    }
}
