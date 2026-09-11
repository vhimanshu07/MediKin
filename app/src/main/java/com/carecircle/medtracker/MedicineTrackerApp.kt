package com.carecircle.medtracker

import android.app.Application
import com.carecircle.medtracker.data.MedicationRepository
import com.carecircle.medtracker.data.PreferencesMedicationRepository
import com.carecircle.medtracker.reminder.ReminderScheduler

class MedicineTrackerApp : Application() {
    lateinit var repository: MedicationRepository
        private set
    lateinit var reminderScheduler: ReminderScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PreferencesMedicationRepository(
            getSharedPreferences("medicine_tracker", MODE_PRIVATE),
        )
        reminderScheduler = ReminderScheduler(this)
        reminderScheduler.createNotificationChannels()
        reminderScheduler.scheduleAll(repository.snapshot.value.medicines)
    }
}
