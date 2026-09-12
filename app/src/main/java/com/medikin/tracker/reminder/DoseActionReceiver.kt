package com.medikin.tracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medikin.tracker.MedicineTrackerApp
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import java.time.LocalDateTime
import java.time.LocalDate

class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_ID) ?: return
        val doseTime = intent.getStringExtra(ReminderScheduler.EXTRA_DOSE_TIME)
            ?.let(DoseTime::parse) ?: return
        val scheduledDate = intent.getStringExtra(ReminderScheduler.EXTRA_SCHEDULED_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
        val app = context.applicationContext as MedicineTrackerApp
        app.repository.recordDose(
            medicineId,
            doseTime,
            DoseStatus.TAKEN,
            LocalDateTime.of(scheduledDate, LocalDateTime.now().toLocalTime()),
        )
        NotificationManagerCompatHelper.cancel(context, "${context.packageName}.REMINDER.DOSE.$medicineId".hashCode())
    }
}

private object NotificationManagerCompatHelper {
    fun cancel(context: Context, id: Int) = androidx.core.app.NotificationManagerCompat.from(context).cancel(id)
}
