package com.medikin.tracker.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.medikin.tracker.domain.DosePlanner
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.Medicine
import java.time.LocalDateTime

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun createNotificationChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    DOSE_CHANNEL,
                    "Medicine reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts when a medicine is due"
                    setSound(alarmSound, alarmAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 350, 180, 350)
                },
                NotificationChannel(
                    FAMILY_CHANNEL,
                    "Family check-ins",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts five minutes after a dose is not recorded"
                    setSound(alarmSound, alarmAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 450, 180, 450, 180, 650)
                },
            ),
        )
    }

    fun scheduleAll(medicines: List<Medicine>, now: LocalDateTime = LocalDateTime.now()) {
        medicines.filter(Medicine::isActive).forEach { scheduleMedicine(it, now) }
    }

    fun scheduleMedicine(medicine: Medicine, now: LocalDateTime = LocalDateTime.now()) {
        medicine.times.forEach { time ->
            schedule(medicine, time, ReminderKind.DOSE, DosePlanner.nextOccurrenceMillis(time, now))
            schedule(
                medicine,
                time,
                ReminderKind.MISSED,
                DosePlanner.nextOccurrenceMillis(time, now, DosePlanner.MISSED_AFTER_MINUTES),
            )
        }
    }

    fun cancelMedicine(medicine: Medicine) {
        medicine.times.forEach { time ->
            ReminderKind.entries.forEach { kind ->
                alarmManager.cancel(pendingIntent(medicine, time, kind))
            }
        }
    }

    private fun schedule(
        medicine: Medicine,
        time: DoseTime,
        kind: ReminderKind,
        triggerAtMillis: Long,
    ) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(medicine, time, kind),
        )
    }

    private fun pendingIntent(
        medicine: Medicine,
        time: DoseTime,
        kind: ReminderKind,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "${context.packageName}.REMINDER.${kind.name}.${medicine.id}.${time.key}"
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_DOSE_TIME, time.key)
            putExtra(EXTRA_REMINDER_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            context,
            intent.action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    enum class ReminderKind { DOSE, MISSED }

    companion object {
        const val DOSE_CHANNEL = "medicine_doses_alarm_v2"
        const val FAMILY_CHANNEL = "family_checkins_alarm_v2"
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_DOSE_TIME = "dose_time"
        const val EXTRA_REMINDER_KIND = "reminder_kind"
    }
}
