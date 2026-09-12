package com.medikin.tracker.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class ScheduleType { DAILY, INTERVAL, AS_NEEDED }

data class ScheduledDose(
    val time: DoseTime,
    val dosage: String? = null,
    val stockUse: Int = 1,
)

data class PausePeriod(
    val startDate: String,
    val endDate: String,
)

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
    // Nullable additions keep snapshots written by older app versions readable by Gson.
    val scheduleType: ScheduleType? = null,
    val weekdays: Set<Int>? = null,
    val intervalHours: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val pausedUntil: String? = null,
    val scheduledDoses: List<ScheduledDose>? = null,
    val stockUnit: String? = null,
    val createdDate: String? = null,
    val pausePeriods: List<PausePeriod>? = null,
) {
    val effectiveScheduleType: ScheduleType
        get() = scheduleType ?: ScheduleType.DAILY

    val effectiveWeekdays: Set<Int>
        get() = weekdays?.takeIf(Set<Int>::isNotEmpty) ?: ALL_WEEKDAYS

    val effectiveStockUnit: String
        get() = stockUnit?.trim()?.takeIf(String::isNotEmpty) ?: "doses"

    val effectiveScheduledDoses: List<ScheduledDose>
        get() = scheduledDoses?.takeIf(List<ScheduledDose>::isNotEmpty)
            ?: times.map { ScheduledDose(it, dosage, 1) }

    fun doseAt(time: DoseTime): ScheduledDose = effectiveScheduledDoses
        .firstOrNull { it.time == time }
        ?: effectiveScheduledDoses.firstOrNull()
            ?.takeIf { effectiveScheduleType == ScheduleType.INTERVAL }
            ?.copy(time = time)
        ?: ScheduledDose(time, dosage, 1)

    fun isPausedOn(date: LocalDate): Boolean {
        val periods = pausePeriods
        if (periods != null) {
            return periods.any { period ->
                val start = parseDate(period.startDate) ?: return@any false
                val end = parseDate(period.endDate) ?: return@any false
                date >= start && date <= end
            }
        }
        return pausedUntil?.let(::parseDate)?.let { !date.isAfter(it) } ?: false
    }

    fun isInDateRange(date: LocalDate): Boolean {
        val start = startDate?.let(::parseDate) ?: createdDate?.let(::parseDate)
        val end = endDate?.let(::parseDate)
        return (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
    }

    fun isScheduledOn(date: LocalDate): Boolean = isActive &&
        !isPausedOn(date) &&
        isInDateRange(date) &&
        (effectiveScheduleType != ScheduleType.DAILY || date.dayOfWeek.value in effectiveWeekdays)

    fun scheduledDosesOn(date: LocalDate): List<ScheduledDose> {
        if (!isScheduledOn(date) || effectiveScheduleType == ScheduleType.AS_NEEDED) return emptyList()
        if (effectiveScheduleType == ScheduleType.DAILY) return effectiveScheduledDoses

        val hours = intervalHours?.takeIf { it in 1..168 } ?: return emptyList()
        val anchorDate = startDate?.let(::parseDate) ?: createdDate?.let(::parseDate) ?: date
        val anchorTime = effectiveScheduledDoses.firstOrNull()?.time ?: DoseTime.MORNING
        val anchor = LocalDateTime.of(anchorDate, anchorTime.asLocalTime())
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        if (dayEnd <= anchor) return emptyList()

        val elapsedHours = ChronoUnit.HOURS.between(anchor, dayStart).coerceAtLeast(0)
        var occurrence = anchor.plusHours((elapsedHours / hours) * hours.toLong())
        while (occurrence < dayStart) occurrence = occurrence.plusHours(hours.toLong())
        val template = effectiveScheduledDoses.firstOrNull() ?: ScheduledDose(anchorTime, dosage, 1)
        return buildList {
            while (occurrence < dayEnd) {
                add(template.copy(time = DoseTime(occurrence.hour, occurrence.minute)))
                occurrence = occurrence.plusHours(hours.toLong())
            }
        }
    }

    companion object {
        val ALL_WEEKDAYS: Set<Int> = (1..7).toSet()

        private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
    }
}

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
    val medicineName: String? = null,
    val dosage: String? = null,
    val stockUse: Int? = null,
)

data class DoseOccurrence(
    val medicine: Medicine,
    val time: DoseTime,
    val scheduledAt: LocalDateTime,
    val status: DoseStatus,
) {
    val scheduledDose: ScheduledDose get() = medicine.doseAt(time)
    val displayDosage: String get() = scheduledDose.dosage?.takeIf(String::isNotBlank) ?: medicine.dosage
}

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

data class HistoryEntry(
    val medicineId: String,
    val medicineName: String,
    val dosage: String,
    val date: LocalDate,
    val time: DoseTime,
    val status: DoseStatus,
    val recordedAt: String,
)

data class MedicineHistory(
    val medicineId: String,
    val medicineName: String,
    val taken: Int,
    val skipped: Int,
    val missed: Int,
) {
    val total: Int get() = taken + skipped + missed
    val adherencePercent: Int get() = if (total == 0) 0 else taken * 100 / total
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
        .filter { it.isScheduledOn(date) }
        .flatMap { medicine ->
            medicine.scheduledDosesOn(date).asSequence().map { scheduledDose ->
                val time = scheduledDose.time
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

    fun historyEntries(
        snapshot: TrackerSnapshot,
        from: LocalDate,
        through: LocalDate,
    ): List<HistoryEntry> = snapshot.logs.asSequence()
        .filter { it.status in setOf(DoseStatus.TAKEN, DoseStatus.SKIPPED, DoseStatus.MISSED) }
        .mapNotNull { log ->
            val date = runCatching { LocalDate.parse(log.date) }.getOrNull() ?: return@mapNotNull null
            if (date < from || date > through) return@mapNotNull null
            val medicine = snapshot.medicines.firstOrNull { it.id == log.medicineId }
            HistoryEntry(
                medicineId = log.medicineId,
                medicineName = log.medicineName ?: medicine?.name ?: "Removed medicine",
                dosage = log.dosage ?: medicine?.doseAt(log.time)?.dosage ?: medicine?.dosage.orEmpty(),
                date = date,
                time = log.time,
                status = log.status,
                recordedAt = log.recordedAt,
            )
        }
        .sortedWith(compareByDescending<HistoryEntry> { it.date }.thenByDescending { it.time.asLocalTime() })
        .toList()

    fun medicineHistory(entries: List<HistoryEntry>): List<MedicineHistory> = entries
        .groupBy { it.medicineId to it.medicineName }
        .map { (identity, medicineEntries) ->
            MedicineHistory(
                medicineId = identity.first,
                medicineName = identity.second,
                taken = medicineEntries.count { it.status == DoseStatus.TAKEN },
                skipped = medicineEntries.count { it.status == DoseStatus.SKIPPED },
                missed = medicineEntries.count { it.status == DoseStatus.MISSED },
            )
        }
        .sortedByDescending(MedicineHistory::total)

    fun nextOccurrenceMillis(
        time: DoseTime,
        now: LocalDateTime,
        delayMinutes: Long = 0,
    ): Long {
        var candidate = LocalDateTime.of(now.toLocalDate(), time.asLocalTime()).plusMinutes(delayMinutes)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }


    fun nextOccurrenceMillis(
        medicine: Medicine,
        now: LocalDateTime,
        delayMinutes: Long = 0,
    ): Pair<ScheduledDose, Long>? {
        for (offset in 0..370) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            medicine.scheduledDosesOn(date).sortedBy { it.time.asLocalTime() }.forEach { dose ->
                val candidate = LocalDateTime.of(date, dose.time.asLocalTime()).plusMinutes(delayMinutes)
                if (candidate.isAfter(now)) {
                    return dose to candidate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
        }
        return null
    }
}
