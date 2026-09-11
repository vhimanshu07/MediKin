package com.carecircle.medtracker.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.carecircle.medtracker.MainActivity
import com.carecircle.medtracker.R
import com.carecircle.medtracker.domain.Caregiver

object MissedDoseNotifier {
    fun show(
        context: Context,
        medicineName: String,
        doseLabel: String,
        caregiver: Caregiver,
        parentName: String,
        notificationId: Int,
    ): Boolean {
        if (!canNotify(context)) return false
        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val detail = "$medicineName is 5 minutes late. A family check-in may help."
        val builder = NotificationCompat.Builder(context, ReminderScheduler.FAMILY_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Medicine still not taken")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openApp)

        if (caregiver.phone.isNotBlank()) {
            val message = "$medicineName ($doseLabel) is still not recorded for $parentName. Could you please check in?"
            val messageIntent = Intent(
                Intent.ACTION_SENDTO,
                "smsto:${android.net.Uri.encode(caregiver.phone)}".toUri(),
            ).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val messageAction = PendingIntent.getActivity(
                context,
                ("message_${caregiver.phone}_$notificationId").hashCode(),
                messageIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "Message ${caregiver.name.ifBlank { "family" }}", messageAction)
        }

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
