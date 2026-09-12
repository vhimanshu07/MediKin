package com.medikin.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TimeZone
import org.junit.After

class DosePlannerTest {
    private val originalTimeZone = TimeZone.getDefault()
    private val date = LocalDate.of(2026, 9, 11)
    private val medicine = Medicine(
        id = "med-1",
        name = "Amlodipine",
        dosage = "5 mg",
        instructions = "After food",
        times = listOf(DoseTime.MORNING, DoseTime.EVENING),
        stock = 10,
        refillAt = 3,
    )

    @Test
    fun `dose before scheduled time is upcoming`() {
        val doses = DosePlanner.dosesFor(date, at(7, 30), listOf(medicine), emptyList())

        assertEquals(DoseStatus.UPCOMING, doses.first().status)
    }

    @Test
    fun `dose is due during five minute grace period`() {
        val doses = DosePlanner.dosesFor(date, at(8, 4), listOf(medicine), emptyList())

        assertEquals(DoseStatus.DUE, doses.first().status)
    }

    @Test
    fun `dose after grace period is missed`() {
        val doses = DosePlanner.dosesFor(date, at(8, 6), listOf(medicine), emptyList())

        assertEquals(DoseStatus.MISSED, doses.first().status)
    }

    @Test
    fun `taken log overrides calculated missed state`() {
        val log = DoseLog("med-1", date.toString(), DoseTime.MORNING, DoseStatus.TAKEN, at(8, 2).toString())

        val doses = DosePlanner.dosesFor(date, at(12, 0), listOf(medicine), listOf(log))

        assertEquals(DoseStatus.TAKEN, doses.first().status)
    }

    @Test
    fun `doses are sorted by time across medicines`() {
        val afternoon = medicine.copy(id = "med-2", times = listOf(DoseTime.AFTERNOON))

        val doses = DosePlanner.dosesFor(date, at(6, 0), listOf(afternoon, medicine), emptyList())

        assertEquals(listOf(DoseTime.MORNING, DoseTime.AFTERNOON, DoseTime.EVENING), doses.map { it.time })
    }

    @Test
    fun `inactive medicines do not appear`() {
        val doses = DosePlanner.dosesFor(date, at(8, 0), listOf(medicine.copy(isActive = false)), emptyList())

        assertTrue(doses.isEmpty())
    }

    @Test
    fun `adherence counts only taken doses`() {
        val doses = listOf(
            occurrence(DoseStatus.TAKEN),
            occurrence(DoseStatus.SKIPPED),
            occurrence(DoseStatus.MISSED),
            occurrence(DoseStatus.UPCOMING),
        )

        val adherence = DosePlanner.adherence(doses)

        assertEquals(1, adherence.taken)
        assertEquals(4, adherence.total)
        assertEquals(25, adherence.percent)
    }

    @Test
    fun `refill list includes stock at or below threshold`() {
        val safe = medicine.copy(id = "safe", stock = 4, refillAt = 3)
        val exact = medicine.copy(id = "exact", stock = 3, refillAt = 3)
        val low = medicine.copy(id = "low", stock = 1, refillAt = 3)

        val result = DosePlanner.refillMedicines(listOf(safe, exact, low))

        assertEquals(listOf("low", "exact"), result.map { it.id })
    }

    @Test
    fun `next occurrence uses today when still ahead`() {
        val result = DosePlanner.nextOccurrenceMillis(DoseTime.MORNING, at(7, 0))
        val expected = at(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expected, result)
    }

    @Test
    fun `next occurrence rolls to tomorrow after time passes`() {
        val result = DosePlanner.nextOccurrenceMillis(DoseTime.MORNING, at(10, 0))
        val expected = at(8, 0).plusDays(1).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expected, result)
    }

    @Test
    fun `missed reminder delay is included in next occurrence`() {
        val result = DosePlanner.nextOccurrenceMillis(DoseTime.MORNING, at(8, 3), 5)
        val expected = at(8, 5).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expected, result)
    }

    @Test
    fun `custom reminder time is scheduled and classified`() {
        val custom = DoseTime(10, 37)
        val customMedicine = medicine.copy(times = listOf(custom))

        val doses = DosePlanner.dosesFor(date, at(10, 38), listOf(customMedicine), emptyList())

        assertEquals(custom, doses.single().time)
        assertEquals(DoseStatus.DUE, doses.single().status)
    }

    @Test
    fun `custom reminder key round trips through alarm extras`() {
        assertEquals(DoseTime(22, 17), DoseTime.parse(DoseTime(22, 17).key))
    }

    @Test
    fun `weekday and date range control daily occurrences`() {
        val mondayOnly = medicine.copy(
            weekdays = setOf(1),
            startDate = "2026-09-14",
            endDate = "2026-09-21",
        )

        assertTrue(DosePlanner.dosesFor(LocalDate.of(2026, 9, 13), at(7, 0), listOf(mondayOnly), emptyList()).isEmpty())
        assertEquals(2, DosePlanner.dosesFor(LocalDate.of(2026, 9, 14), at(7, 0), listOf(mondayOnly), emptyList()).size)
        assertTrue(DosePlanner.dosesFor(LocalDate.of(2026, 9, 22), at(7, 0), listOf(mondayOnly), emptyList()).isEmpty())
    }

    @Test
    fun `interval schedule produces occurrences from its anchor`() {
        val interval = medicine.copy(
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 8,
            startDate = "2026-09-11",
            scheduledDoses = listOf(ScheduledDose(DoseTime.MORNING, "500 mg", 2)),
        )

        assertEquals(
            listOf(DoseTime(0, 0), DoseTime(8, 0), DoseTime(16, 0)),
            interval.scheduledDosesOn(LocalDate.of(2026, 9, 12)).map { it.time },
        )
        assertEquals("500 mg", interval.doseAt(DoseTime(16, 0)).dosage)
        assertEquals(2, interval.doseAt(DoseTime(16, 0)).stockUse)
    }

    @Test
    fun `different scheduled times can carry different doses`() {
        val varied = medicine.copy(
            scheduledDoses = listOf(
                ScheduledDose(DoseTime.MORNING, "5 mg", 1),
                ScheduledDose(DoseTime.EVENING, "10 mg", 2),
            ),
        )

        val evening = DosePlanner.dosesFor(date, at(7, 0), listOf(varied), emptyList()).last()

        assertEquals("10 mg", evening.displayDosage)
        assertEquals(2, evening.scheduledDose.stockUse)
    }

    @Test
    fun `next wall clock occurrence spans daylight saving transition`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val daily = medicine.copy(times = listOf(DoseTime.MORNING), startDate = "2026-03-01")
        val before = LocalDateTime.of(2026, 3, 7, 9, 0)

        val result = checkNotNull(DosePlanner.nextOccurrenceMillis(daily, before)).second
        val expected = LocalDateTime.of(2026, 3, 8, 8, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(expected, result)
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 9, 11, hour, minute)

    private fun occurrence(status: DoseStatus) = DoseOccurrence(
        medicine,
        DoseTime.MORNING,
        at(8, 0),
        status,
    )
}
