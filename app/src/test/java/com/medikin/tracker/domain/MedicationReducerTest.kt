package com.medikin.tracker.domain

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
    fun `changing taken dose to skipped restores consumed stock`() {
        val taken = MedicationReducer.recordDose(snapshot, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now)

        val corrected = MedicationReducer.changeDoseStatus(
            taken,
            "med-1",
            DoseTime.EVENING,
            DoseStatus.SKIPPED,
            now.plusMinutes(2),
        )

        assertEquals(2, corrected.medicines.single().stock)
        assertEquals(DoseStatus.SKIPPED, corrected.logs.single().status)
    }

    @Test
    fun `undoing taken dose removes log and restores stock`() {
        val taken = MedicationReducer.recordDose(snapshot, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now)

        val undone = MedicationReducer.changeDoseStatus(taken, "med-1", DoseTime.EVENING, null, now.plusMinutes(2))

        assertEquals(2, undone.medicines.single().stock)
        assertTrue(undone.logs.isEmpty())
    }

    @Test
    fun `editing medicine replaces details without changing identity`() {
        val result = MedicationReducer.updateMedicine(
            snapshot,
            medicine.copy(name = "Metformin XR", dosage = "750 mg", stock = 20),
        )

        assertEquals("med-1", result.medicines.single().id)
        assertEquals("Metformin XR", result.medicines.single().name)
        assertEquals("750 mg", result.medicines.single().dosage)
        assertEquals(20, result.medicines.single().stock)
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
    fun `deleting medicine keeps a self describing history log`() {
        val withLog = MedicationReducer.recordDose(snapshot, "med-1", DoseTime.EVENING, DoseStatus.TAKEN, now)

        val result = MedicationReducer.delete(withLog, "med-1")

        assertTrue(result.medicines.isEmpty())
        assertEquals(1, result.logs.size)
        assertEquals("Metformin", result.logs.single().medicineName)
    }

    @Test
    fun `elapsed unrecorded doses are materialized as missed history`() {
        val tracked = medicine.copy(
            createdDate = "2026-09-10",
            times = listOf(DoseTime.MORNING),
        )

        val result = MedicationReducer.materializeMissedHistory(
            TrackerSnapshot(medicines = listOf(tracked)),
            LocalDate.of(2026, 9, 11),
            LocalDateTime.of(2026, 9, 11, 7, 0),
        )

        assertEquals(1, result.logs.size)
        assertEquals("2026-09-10", result.logs.single().date)
        assertEquals(DoseStatus.MISSED, result.logs.single().status)
        assertEquals("Metformin", result.logs.single().medicineName)
    }

    @Test
    fun `temporary pause can be resumed without marking paused dates missed`() {
        val tracked = medicine.copy(createdDate = "2026-09-11")
        val paused = MedicationReducer.pauseMedicine(
            TrackerSnapshot(medicines = listOf(tracked)),
            tracked.id,
            LocalDate.of(2026, 9, 17),
            LocalDate.of(2026, 9, 11),
        )

        assertTrue(paused.medicines.single().isPausedOn(LocalDate.of(2026, 9, 12)))
        val resumed = MedicationReducer.pauseMedicine(
            paused,
            tracked.id,
            null,
            LocalDate.of(2026, 9, 13),
        )
        assertTrue(resumed.medicines.single().isPausedOn(LocalDate.of(2026, 9, 12)))
        assertTrue(!resumed.medicines.single().isPausedOn(LocalDate.of(2026, 9, 13)))
    }
}
