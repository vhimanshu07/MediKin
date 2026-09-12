package com.medikin.tracker.domain

import java.time.LocalDate
import java.time.LocalDateTime

object MedicationReducer {
    fun addMedicine(snapshot: TrackerSnapshot, medicine: Medicine): TrackerSnapshot {
        require(medicine.name.isNotBlank()) { "Medicine name is required" }
        require(medicine.dosage.isNotBlank()) { "Dosage is required" }
        require(
            medicine.effectiveScheduleType == ScheduleType.AS_NEEDED ||
                medicine.effectiveScheduledDoses.isNotEmpty(),
        ) { "Select at least one reminder" }
        if (medicine.effectiveScheduleType == ScheduleType.INTERVAL) {
            require(medicine.intervalHours in 1..168) { "Interval must be between 1 and 168 hours" }
        }
        medicine.effectiveScheduledDoses.forEach {
            require(it.stockUse > 0) { "Dose stock use must be positive" }
        }
        require(medicine.stock >= 0) { "Stock cannot be negative" }
        require(medicine.refillAt >= 0) { "Refill level cannot be negative" }
        val start = medicine.startDate?.let {
            runCatching { LocalDate.parse(it) }.getOrElse { throw IllegalArgumentException("Invalid start date") }
        }
        val end = medicine.endDate?.let {
            runCatching { LocalDate.parse(it) }.getOrElse { throw IllegalArgumentException("Invalid end date") }
        }
        require(start == null || end == null || !end.isBefore(start)) { "End date cannot be before start date" }
        return snapshot.copy(medicines = snapshot.medicines + medicine)
    }

    fun validateSnapshot(snapshot: TrackerSnapshot): TrackerSnapshot {
        require(snapshot.medicines.map(Medicine::id).distinct().size == snapshot.medicines.size) {
            "Medicine IDs must be unique"
        }
        snapshot.medicines.forEach { addMedicine(TrackerSnapshot(), it) }
        require(snapshot.logs.size <= 250_000) { "Backup contains too many dose records" }
        return snapshot
    }

    fun updateMedicine(snapshot: TrackerSnapshot, medicine: Medicine): TrackerSnapshot {
        val existing = snapshot.medicines.firstOrNull { it.id == medicine.id } ?: return snapshot
        val validated = addMedicine(TrackerSnapshot(), medicine.copy(createdDate = medicine.createdDate ?: existing.createdDate))
            .medicines.single()
        return snapshot.copy(
            medicines = snapshot.medicines.map { if (it.id == medicine.id) validated else it },
        )
    }

    fun recordDose(
        snapshot: TrackerSnapshot,
        medicineId: String,
        time: DoseTime,
        status: DoseStatus,
        at: LocalDateTime,
    ): TrackerSnapshot {
        require(status == DoseStatus.TAKEN || status == DoseStatus.SKIPPED)
        return changeDoseStatus(snapshot, medicineId, time, status, at)
    }

    fun changeDoseStatus(
        snapshot: TrackerSnapshot,
        medicineId: String,
        time: DoseTime,
        status: DoseStatus?,
        at: LocalDateTime,
    ): TrackerSnapshot {
        require(status == null || status == DoseStatus.TAKEN || status == DoseStatus.SKIPPED)
        val medicine = snapshot.medicines.firstOrNull { it.id == medicineId } ?: return snapshot
        val date = at.toLocalDate().toString()
        val existing = snapshot.logs.lastOrNull {
            it.medicineId == medicineId && it.time == time && it.date == date
        }
        if (existing?.status == status) return snapshot

        var stock = medicine.stock
        if (existing?.status == DoseStatus.TAKEN) stock += existing.stockUse ?: medicine.doseAt(time).stockUse
        val requestedStockUse = medicine.doseAt(time).stockUse
        val consumed = if (status == DoseStatus.TAKEN) requestedStockUse.coerceAtMost(stock) else 0
        stock -= consumed
        val updatedMedicine = medicine.copy(stock = stock)
        val retainedLogs = snapshot.logs.filterNot {
            it.medicineId == medicineId && it.time == time && it.date == date
        }
        val replacement = status?.let {
            DoseLog(
                medicineId = medicineId,
                date = date,
                time = time,
                status = it,
                recordedAt = at.toString(),
                medicineName = medicine.name,
                dosage = medicine.doseAt(time).dosage ?: medicine.dosage,
                stockUse = consumed.takeIf { status == DoseStatus.TAKEN },
            )
        }
        return snapshot.copy(
            medicines = snapshot.medicines.map { if (it.id == medicineId) updatedMedicine else it },
            logs = retainedLogs + listOfNotNull(replacement),
        )
    }

    fun recordAsNeeded(
        snapshot: TrackerSnapshot,
        medicineId: String,
        at: LocalDateTime,
    ): TrackerSnapshot {
        val medicine = snapshot.medicines.firstOrNull { it.id == medicineId } ?: return snapshot
        if (medicine.effectiveScheduleType != ScheduleType.AS_NEEDED) return snapshot
        val time = DoseTime(at.hour, at.minute)
        val requested = medicine.effectiveScheduledDoses.firstOrNull()?.stockUse ?: 1
        val consumed = requested.coerceAtMost(medicine.stock)
        return snapshot.copy(
            medicines = snapshot.medicines.map {
                if (it.id == medicineId) it.copy(stock = it.stock - consumed) else it
            },
            logs = snapshot.logs + DoseLog(
                medicineId = medicineId,
                date = at.toLocalDate().toString(),
                time = time,
                status = DoseStatus.TAKEN,
                recordedAt = at.toString(),
                medicineName = medicine.name,
                dosage = medicine.effectiveScheduledDoses.firstOrNull()?.dosage ?: medicine.dosage,
                stockUse = consumed,
            ),
        )
    }

    fun restock(snapshot: TrackerSnapshot, medicineId: String, amount: Int): TrackerSnapshot {
        require(amount > 0)
        return snapshot.copy(medicines = snapshot.medicines.map {
            if (it.id == medicineId) it.copy(stock = it.stock + amount) else it
        })
    }

    fun delete(snapshot: TrackerSnapshot, medicineId: String): TrackerSnapshot {
        val medicine = snapshot.medicines.firstOrNull { it.id == medicineId }
        return snapshot.copy(
            medicines = snapshot.medicines.filterNot { it.id == medicineId },
            logs = snapshot.logs.map { log ->
                if (log.medicineId == medicineId && medicine != null) {
                    log.copy(
                        medicineName = log.medicineName ?: medicine.name,
                        dosage = log.dosage ?: medicine.doseAt(log.time).dosage ?: medicine.dosage,
                    )
                } else log
            },
        )
    }

    fun pauseMedicine(
        snapshot: TrackerSnapshot,
        medicineId: String,
        pausedUntil: LocalDate?,
        today: LocalDate = LocalDate.now(),
    ): TrackerSnapshot = snapshot.copy(medicines = snapshot.medicines.map { medicine ->
        if (medicine.id != medicineId) return@map medicine
        val periods = medicine.pausePeriods.orEmpty().toMutableList()
        if (pausedUntil == null) {
            val activeIndex = periods.indexOfLast { period ->
                val start = runCatching { LocalDate.parse(period.startDate) }.getOrNull()
                val end = runCatching { LocalDate.parse(period.endDate) }.getOrNull()
                start != null && end != null && today in start..end
            }
            if (activeIndex >= 0) {
                val active = periods[activeIndex]
                if (active.startDate == today.toString()) periods.removeAt(activeIndex)
                else periods[activeIndex] = active.copy(endDate = today.minusDays(1).toString())
            }
        } else {
            require(!pausedUntil.isBefore(today)) { "Pause end cannot be before today" }
            periods += PausePeriod(today.toString(), pausedUntil.toString())
        }
        medicine.copy(
            pausedUntil = pausedUntil?.toString(),
            pausePeriods = periods,
        )
    })

    fun materializeMissedHistory(
        snapshot: TrackerSnapshot,
        today: LocalDate,
        now: LocalDateTime = today.atStartOfDay(),
    ): TrackerSnapshot {
        val newLogs = buildList {
            snapshot.medicines.forEach { medicine ->
                val firstDate = medicine.createdDate
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return@forEach
                var date = firstDate
                while (date <= today) {
                    medicine.scheduledDosesOn(date).forEach { dose ->
                        val scheduledAt = LocalDateTime.of(date, dose.time.asLocalTime())
                        if (!scheduledAt.plusMinutes(DosePlanner.MISSED_AFTER_MINUTES).isBefore(now)) {
                            return@forEach
                        }
                        val exists = snapshot.logs.any {
                            it.medicineId == medicine.id && it.date == date.toString() && it.time == dose.time
                        }
                        if (!exists) {
                            add(
                                DoseLog(
                                    medicineId = medicine.id,
                                    date = date.toString(),
                                    time = dose.time,
                                    status = DoseStatus.MISSED,
                                    recordedAt = scheduledAt
                                        .plusMinutes(DosePlanner.MISSED_AFTER_MINUTES)
                                        .toString(),
                                    medicineName = medicine.name,
                                    dosage = dose.dosage ?: medicine.dosage,
                                    stockUse = 0,
                                ),
                            )
                        }
                    }
                    date = date.plusDays(1)
                }
            }
        }
        return if (newLogs.isEmpty()) snapshot else snapshot.copy(logs = snapshot.logs + newLogs)
    }

}
