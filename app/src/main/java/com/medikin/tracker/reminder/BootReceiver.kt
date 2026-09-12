package com.medikin.tracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medikin.tracker.MedicineTrackerApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != Intent.ACTION_TIMEZONE_CHANGED &&
            intent.action != Intent.ACTION_TIME_CHANGED
        ) return
        val app = context.applicationContext as MedicineTrackerApp
        app.reminderScheduler.scheduleAll(app.repository.snapshot.value.medicines)
    }
}
