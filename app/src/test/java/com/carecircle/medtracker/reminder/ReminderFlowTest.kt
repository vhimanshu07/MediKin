package com.carecircle.medtracker.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.test.core.app.ApplicationProvider
import com.carecircle.medtracker.domain.Caregiver
import com.carecircle.medtracker.domain.DoseTime
import com.carecircle.medtracker.domain.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReminderFlowTest {
    private lateinit var context: Context
    private lateinit var scheduler: ReminderScheduler
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        scheduler = ReminderScheduler(context)
        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
        scheduler.createNotificationChannels()
    }

    @Test
    fun `family channel uses alarm audio and vibration`() {
        val channel = notificationManager.getNotificationChannel(ReminderScheduler.FAMILY_CHANNEL)

        assertNotNull(channel.sound)
        assertEquals(AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
        assertTrue(channel.shouldVibrate())
    }

    @Test
    fun `missed dose alert contains message family action`() {
        val sent = MissedDoseNotifier.show(
            context = context,
            medicineName = "Metformin",
            doseLabel = "500 mg · Morning",
            caregiver = Caregiver("Mom", "Rahul", "+919999999999"),
            parentName = "Mom",
            notificationId = 5050,
        )

        val notification = shadowOf(notificationManager).allNotifications.single()
        assertTrue(sent)
        assertEquals("Medicine still not taken", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Message Rahul", notification.actions.single().title)
    }

    @Test
    fun `medicine schedules missed alert five minutes after dose alert`() {
        val medicine = Medicine(
            id = "medicine-1",
            name = "Metformin",
            dosage = "500 mg",
            instructions = "After food",
            times = listOf(DoseTime(9, 0)),
            stock = 30,
            refillAt = 5,
        )
        scheduler.scheduleMedicine(medicine, LocalDateTime.of(2026, 9, 11, 8, 0))

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val alarms = shadowOf(alarmManager).scheduledAlarms.sortedBy { it.triggerAtMs }
        assertEquals(2, alarms.size)
        assertEquals(5 * 60_000L, alarms[1].triggerAtMs - alarms[0].triggerAtMs)
    }
}
