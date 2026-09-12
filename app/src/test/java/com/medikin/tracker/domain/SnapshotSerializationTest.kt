package com.medikin.tracker.domain

import com.medikin.tracker.data.trackerGson
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotSerializationTest {
    @Test
    fun `snapshot round trips through persistent json`() {
        val snapshot = TrackerSnapshot(
            medicines = listOf(
                Medicine("one", "Aspirin", "75 mg", "After food", listOf(DoseTime.MORNING), 9, 3),
            ),
            logs = listOf(
                DoseLog("one", "2026-09-11", DoseTime.MORNING, DoseStatus.TAKEN, "2026-09-11T08:02"),
            ),
            caregiver = Caregiver("Dad", "Neha", "+919999999999"),
        )

        val gson = trackerGson()
        val restored = gson.fromJson(gson.toJson(snapshot), TrackerSnapshot::class.java)

        assertEquals(snapshot, restored)
    }

    @Test
    fun `legacy preset names migrate to flexible reminder times`() {
        val json = """{"medicines":[{"id":"one","name":"Aspirin","dosage":"75 mg","instructions":"After food","times":["MORNING","22-17"],"stock":9,"refillAt":3,"colorIndex":0,"isActive":true}],"logs":[],"caregiver":{"parentName":"Dad","name":"","phone":""}}"""

        val restored = trackerGson().fromJson(json, TrackerSnapshot::class.java)

        assertEquals(listOf(DoseTime.MORNING, DoseTime(22, 17)), restored.medicines.single().times)
        assertEquals(ScheduleType.DAILY, restored.medicines.single().effectiveScheduleType)
        assertEquals(Medicine.ALL_WEEKDAYS, restored.medicines.single().effectiveWeekdays)
        assertEquals("doses", restored.medicines.single().effectiveStockUnit)
    }
}
