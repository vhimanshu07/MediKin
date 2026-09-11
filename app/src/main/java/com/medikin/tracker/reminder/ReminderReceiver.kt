package com.medikin.tracker.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.medikin.tracker.MainActivity
import com.medikin.tracker.MedicineTrackerApp
import com.medikin.tracker.R
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as MedicineTrackerApp
        val medicineId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_ID) ?: return
        val doseTime = intent.getStringExtra(ReminderScheduler.EXTRA_DOSE_TIME)
            ?.let(DoseTime::parse) ?: return
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_KIND)
            ?.let { runCatching { ReminderScheduler.ReminderKind.valueOf(it) }.getOrNull() } ?: return
        val medicine = app.repository.snapshot.value.medicines.firstOrNull { it.id == medicineId } ?: return
        val alreadyRecorded = app.repository.snapshot.value.logs.any {
            it.medicineId == medicineId && it.time == doseTime &&
                it.date == LocalDate.now().toString() &&
                (it.status == DoseStatus.TAKEN || it.status == DoseStatus.SKIPPED)
        }

        if (!alreadyRecorded && canNotify(context)) {
            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            if (kind == ReminderScheduler.ReminderKind.DOSE) {
                val takenIntent = Intent(context, DoseActionReceiver::class.java).apply {
                    action = "${context.packageName}.TAKEN.$medicineId.${doseTime.key}"
                    putExtra(ReminderScheduler.EXTRA_MEDICINE_ID, medicineId)
                    putExtra(ReminderScheduler.EXTRA_DOSE_TIME, doseTime.key)
                }
                val takenPendingIntent = PendingIntent.getBroadcast(
                    context,
                    takenIntent.action.hashCode(),
                    takenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val notification = NotificationCompat.Builder(context, ReminderScheduler.DOSE_CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("${doseTime.label} medicine is due")
                    .setContentText("${medicine.name} · ${medicine.dosage} · ${medicine.instructions}")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(openIntent)
                    .addAction(0, "Mark taken", takenPendingIntent)
                    .build()
                NotificationManagerCompat.from(context).notify(intent.action.hashCode(), notification)
            } else {
                MissedDoseNotifier.show(
                    context = context,
                    medicineName = medicine.name,
                    doseLabel = "${medicine.dosage} · ${doseTime.label}",
                    caregiver = app.repository.snapshot.value.caregiver,
                    parentName = app.repository.snapshot.value.caregiver.parentName,
                    notificationId = intent.action.hashCode(),
                )
            }
        }

        app.reminderScheduler.scheduleMedicine(medicine)
    }

    private fun canNotify(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
