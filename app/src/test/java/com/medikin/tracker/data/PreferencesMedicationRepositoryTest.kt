package com.medikin.tracker.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseLog
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.TrackerSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PreferencesMedicationRepositoryTest {
    private val preferences by lazy {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("repository_test", Context.MODE_PRIVATE)
    }

    @Before
    fun clearStorage() {
        preferences.edit(commit = true) { clear() }
    }

    @Test
    fun `fresh install has no dummy medicines or parent name`() {
        val repository = PreferencesMedicationRepository(preferences)

        assertTrue(repository.snapshot.value.medicines.isEmpty())
        assertTrue(repository.snapshot.value.caregiver.parentName.isEmpty())
    }

    @Test
    fun `legacy starter records are removed while user medicines remain`() {
        val starter = medicine("starter-vitamin", "Vitamin D3")
        val userMedicine = medicine("user-created", "Calcium")
        val snapshot = TrackerSnapshot(
            medicines = listOf(starter, userMedicine),
            logs = listOf(
                DoseLog(starter.id, "2026-09-11", DoseTime.MORNING, DoseStatus.TAKEN, "2026-09-11T08:00"),
            ),
            caregiver = Caregiver(parentName = "Mom"),
        )
        preferences.edit(commit = true) { putString("tracker_snapshot", trackerGson().toJson(snapshot)) }

        val restored = PreferencesMedicationRepository(preferences).snapshot.value

        assertEquals(listOf("Calcium"), restored.medicines.map { it.name })
        assertTrue(restored.logs.isEmpty())
        assertTrue(restored.caregiver.parentName.isEmpty())
    }

    private fun medicine(id: String, name: String) = Medicine(
        id = id,
        name = name,
        dosage = "1 tablet",
        instructions = "After food",
        times = listOf(DoseTime.MORNING),
        stock = 10,
        refillAt = 3,
    )
}
