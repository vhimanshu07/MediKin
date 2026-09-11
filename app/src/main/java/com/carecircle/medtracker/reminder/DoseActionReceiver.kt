package com.carecircle.medtracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.carecircle.medtracker.MedicineTrackerApp
import com.carecircle.medtracker.domain.DoseStatus
import com.carecircle.medtracker.domain.DoseTime
import java.time.LocalDateTime

class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_ID) ?: return
        val doseTime = intent.getStringExtra(ReminderScheduler.EXTRA_DOSE_TIME)
            ?.let(DoseTime::parse) ?: return
        val app = context.applicationContext as MedicineTrackerApp
        app.repository.recordDose(medicineId, doseTime, DoseStatus.TAKEN, LocalDateTime.now())
        NotificationManagerCompatHelper.cancel(context, "${context.packageName}.REMINDER.DOSE.$medicineId.${doseTime.key}".hashCode())
    }
}

private object NotificationManagerCompatHelper {
    fun cancel(context: Context, id: Int) = androidx.core.app.NotificationManagerCompat.from(context).cancel(id)
}
