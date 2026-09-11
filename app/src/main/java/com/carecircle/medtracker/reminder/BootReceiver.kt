package com.carecircle.medtracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.carecircle.medtracker.MedicineTrackerApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val app = context.applicationContext as MedicineTrackerApp
        app.reminderScheduler.scheduleAll(app.repository.snapshot.value.medicines)
    }
}
