package com.carecircle.medtracker.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Medicine(
    val id: String,
    val name: String,
    val dosage: String,
    val instructions: String,
    val times: List<DoseTime>,
    val stock: Int,
    val refillAt: Int,
    val colorIndex: Int = 0,
    val isActive: Boolean = true,
)

data class DoseTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
    }

    val key: String get() = "%02d-%02d".format(java.util.Locale.ROOT, hour, minute)
    val label: String get() = when (hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        else -> "Evening"
    }
    fun asLocalTime(): LocalTime = LocalTime.of(hour, minute)

    companion object {
        val MORNING = DoseTime(8, 0)
        val AFTERNOON = DoseTime(14, 0)
        val EVENING = DoseTime(20, 0)
        val presets = listOf(MORNING, AFTERNOON, EVENING)

        fun parse(value: String): DoseTime? = when (value) {
            "MORNING" -> MORNING
            "AFTERNOON" -> AFTERNOON
            "EVENING" -> EVENING
            else -> value.split('-', ':').takeIf { it.size == 2 }?.let { parts ->
                val hour = parts[0].toIntOrNull() ?: return null
                val minute = parts[1].toIntOrNull() ?: return null
                runCatching { DoseTime(hour, minute) }.getOrNull()
            }
        }
    }
}

enum class DoseStatus { UPCOMING, DUE, TAKEN, SKIPPED, MISSED }

data class DoseLog(
    val medicineId: String,
    val date: String,
    val time: DoseTime,
    val status: DoseStatus,
    val recordedAt: String,
)

data class DoseOccurrence(
    val medicine: Medicine,
    val time: DoseTime,
    val scheduledAt: LocalDateTime,
    val status: DoseStatus,
)

data class Caregiver(
    val parentName: String = "",
    val name: String = "",
    val phone: String = "",
)

data class TrackerSnapshot(
    val medicines: List<Medicine> = emptyList(),
    val logs: List<DoseLog> = emptyList(),
    val caregiver: Caregiver = Caregiver(),
)

data class Adherence(val taken: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else (taken * 100) / total
}

object DosePlanner {
    const val MISSED_AFTER_MINUTES = 5L

    fun dosesFor(
        date: LocalDate,
        now: LocalDateTime,
        medicines: List<Medicine>,
        logs: List<DoseLog>,
    ): List<DoseOccurrence> = medicines
        .asSequence()
        .filter(Medicine::isActive)
        .flatMap { medicine ->
            medicine.times.asSequence().map { time ->
                val scheduledAt = LocalDateTime.of(date, time.asLocalTime())
                val log = logs.lastOrNull {
                    it.medicineId == medicine.id && it.date == date.toString() && it.time == time
                }
                val status = log?.status ?: when {
                    now.toLocalDate().isBefore(date) || now.isBefore(scheduledAt) -> DoseStatus.UPCOMING
                    now.isAfter(scheduledAt.plusMinutes(MISSED_AFTER_MINUTES)) -> DoseStatus.MISSED
                    else -> DoseStatus.DUE
                }
                DoseOccurrence(medicine, time, scheduledAt, status)
            }
        }
        .sortedBy(DoseOccurrence::scheduledAt)
        .toList()

    fun adherence(doses: List<DoseOccurrence>): Adherence = Adherence(
        taken = doses.count { it.status == DoseStatus.TAKEN },
        total = doses.size,
    )

    fun refillMedicines(medicines: List<Medicine>): List<Medicine> =
        medicines.filter { it.isActive && it.stock <= it.refillAt }.sortedBy(Medicine::stock)

    fun nextOccurrenceMillis(
        time: DoseTime,
        now: LocalDateTime,
        delayMinutes: Long = 0,
    ): Long {
        var candidate = LocalDateTime.of(now.toLocalDate(), time.asLocalTime()).plusMinutes(delayMinutes)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
