package com.medikin.tracker

import android.app.Application
import com.medikin.tracker.data.MedicationRepository
import com.medikin.tracker.data.PreferencesMedicationRepository
import com.medikin.tracker.reminder.ReminderScheduler

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
