package com.carecircle.medtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationReducerTest {
    private val medicine = Medicine(
        id = "med-1",
        name = "Metformin",
        dosage = "500 mg",
        instructions = "After dinner",
        times = listOf(DoseTime.EVENING),
        stock = 2,
        refillAt = 1,
    )
    private val snapshot = TrackerSnapshot(medicines = listOf(medicine))
    private val now = LocalDateTime.of(2026, 9, 11, 20, 5)

    @Test
    fun `adding valid medicine appends it`() {
        val added = medicine.copy(id = "med-2", name = "Vitamin D")

        val result = MedicationReducer.addMedicine(snapshot, added)

        assertEquals(listOf("med-1", "med-2"), result.medicines.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank medicine name is rejected`() {
        MedicationReducer.addMedicine(snapshot, medicine.copy(id = "bad", name = " "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `medicine without reminder time is rejected`() {
        MedicationReducer.addMedicine(snapshot, medicine.copy(id = "bad", times = emptyList()))
    }

    @Test
    fun `taking a dose creates log and removes one item from stock`() {
        val result = MedicationReducer.recordDose(snapshot, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now)

        assertEquals(1, result.medicines.single().stock)
        assertEquals(DoseStatus.TAKEN, result.logs.single().status)
        assertEquals("2026-09-11", result.logs.single().date)
    }

    @Test
    fun `recording same dose twice is idempotent`() {
        val once = MedicationReducer.recordDose(snapshot, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now)
        val twice = MedicationReducer.recordDose(once, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now.plusMinutes(4))

        assertEquals(1, twice.medicines.single().stock)
        assertEquals(1, twice.logs.size)
    }

    @Test
    fun `taking dose with empty stock never creates negative stock`() {
        val empty = snapshot.copy(medicines = listOf(medicine.copy(stock = 0)))

        val result = MedicationReducer.recordDose(empty, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now)

        assertEquals(0, result.medicines.single().stock)
    }

    @Test
    fun `skipping dose does not change stock`() {
        val result = MedicationReducer.recordDose(snapshot, "med-1", DoseTime.EVENING, DoseStatus.SKIPPED, now)

        assertEquals(2, result.medicines.single().stock)
        assertEquals(DoseStatus.SKIPPED, result.logs.single().status)
    }

    @Test
    fun `unknown medicine recording is ignored`() {
        assertEquals(snapshot, MedicationReducer.recordDose(snapshot, "unknown", DoseTime.MORNING, DoseStatus.TAKEN, now))
    }

    @Test
    fun `restocking adds requested quantity`() {
        val result = MedicationReducer.restock(snapshot, "med-1", 30)

        assertEquals(32, result.medicines.single().stock)
    }

    @Test
    fun `deleting medicine also removes its logs`() {
        val withLog = MedicationReducer.recordDose(snapshot, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now)

        val result = MedicationReducer.delete(withLog, "med-1")

        assertTrue(result.medicines.isEmpty())
        assertTrue(result.logs.isEmpty())
    }

    @Test
    fun `log pruning keeps only recent history`() {
        val old = DoseLog("med-1", "2026-07-01", DoseTime.EVENING, DoseStatus.TAKEN, "2026-07-01T20:00")
        val recent = old.copy(date = "2026-09-10", recordedAt = "2026-09-10T20:00")

        val result = MedicationReducer.pruneLogs(snapshot.copy(logs = listOf(old, recent)), LocalDate.of(2026, 9, 11))

        assertEquals(listOf(recent), result.logs)
    }
}
