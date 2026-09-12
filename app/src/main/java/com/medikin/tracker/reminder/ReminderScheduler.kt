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
import java.time.Instant
import java.time.ZoneId

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
        cancelCurrentPendingIntents(medicine)
        DosePlanner.nextOccurrenceMillis(medicine, now)?.let { (dose, triggerAt) ->
            schedule(medicine, dose.time, ReminderKind.DOSE, triggerAt, scheduledDate(triggerAt, 0))
        }
        DosePlanner.nextOccurrenceMillis(medicine, now, DosePlanner.MISSED_AFTER_MINUTES)
            ?.let { (dose, triggerAt) ->
                schedule(
                    medicine,
                    dose.time,
                    ReminderKind.MISSED,
                    triggerAt,
                    scheduledDate(triggerAt, DosePlanner.MISSED_AFTER_MINUTES),
                )
            }
    }

    fun cancelMedicine(medicine: Medicine) {
        cancelCurrentPendingIntents(medicine)
        // Cancel request identities used by versions before flexible schedules.
        (medicine.times + medicine.effectiveScheduledDoses.map { it.time }).distinct().forEach { time ->
            ReminderKind.entries.forEach { kind -> alarmManager.cancel(legacyPendingIntent(medicine, time, kind)) }
        }
    }

    fun cancelAll(medicines: List<Medicine>) = medicines.forEach(::cancelMedicine)

    private fun cancelCurrentPendingIntents(medicine: Medicine) {
        ReminderKind.entries.forEach { kind ->
            alarmManager.cancel(pendingIntent(medicine, DoseTime.MORNING, kind))
        }
    }

    private fun schedule(
        medicine: Medicine,
        time: DoseTime,
        kind: ReminderKind,
        triggerAtMillis: Long,
        scheduledDate: String,
    ) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(medicine, time, kind, scheduledDate),
        )
    }

    private fun pendingIntent(
        medicine: Medicine,
        time: DoseTime,
        kind: ReminderKind,
        scheduledDate: String = java.time.LocalDate.now().toString(),
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "${context.packageName}.REMINDER.${kind.name}.${medicine.id}"
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_DOSE_TIME, time.key)
            putExtra(EXTRA_REMINDER_KIND, kind.name)
            putExtra(EXTRA_SCHEDULED_DATE, scheduledDate)
        }
        return PendingIntent.getBroadcast(
            context,
            intent.action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun scheduledDate(triggerAtMillis: Long, delayMinutes: Long): String = Instant
        .ofEpochMilli(triggerAtMillis)
        .minusSeconds(delayMinutes * 60)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

    private fun legacyPendingIntent(
        medicine: Medicine,
        time: DoseTime,
        kind: ReminderKind,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "${context.packageName}.REMINDER.${kind.name}.${medicine.id}.${time.key}"
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
        const val EXTRA_SCHEDULED_DATE = "scheduled_date"
    }
}
