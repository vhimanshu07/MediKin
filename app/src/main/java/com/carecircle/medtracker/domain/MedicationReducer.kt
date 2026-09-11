package com.carecircle.medtracker.domain

import java.time.LocalDate
import java.time.LocalDateTime

object MedicationReducer {
    fun addMedicine(snapshot: TrackerSnapshot, medicine: Medicine): TrackerSnapshot {
        require(medicine.name.isNotBlank()) { "Medicine name is required" }
        require(medicine.dosage.isNotBlank()) { "Dosage is required" }
        require(medicine.times.isNotEmpty()) { "Select at least one reminder" }
        require(medicine.stock >= 0) { "Stock cannot be negative" }
        require(medicine.refillAt >= 0) { "Refill level cannot be negative" }
        return snapshot.copy(medicines = snapshot.medicines + medicine)
    }

    fun recordDose(
        snapshot: TrackerSnapshot,
        medicineId: String,
        time: DoseTime,
        status: DoseStatus,
        at: LocalDateTime,
    ): TrackerSnapshot {
        require(status == DoseStatus.TAKEN || status == DoseStatus.SKIPPED)
        val medicine = snapshot.medicines.firstOrNull { it.id == medicineId } ?: return snapshot
        val date = at.toLocalDate().toString()
        if (snapshot.logs.any { it.medicineId == medicineId && it.time == time && it.date == date }) {
            return snapshot
        }
        val updatedMedicine = if (status == DoseStatus.TAKEN) {
            medicine.copy(stock = (medicine.stock - 1).coerceAtLeast(0))
        } else medicine
        return snapshot.copy(
            medicines = snapshot.medicines.map { if (it.id == medicineId) updatedMedicine else it },
            logs = snapshot.logs + DoseLog(medicineId, date, time, status, at.toString()),
        )
    }

    fun restock(snapshot: TrackerSnapshot, medicineId: String, amount: Int): TrackerSnapshot {
        require(amount > 0)
        return snapshot.copy(medicines = snapshot.medicines.map {
            if (it.id == medicineId) it.copy(stock = it.stock + amount) else it
        })
    }

    fun delete(snapshot: TrackerSnapshot, medicineId: String): TrackerSnapshot = snapshot.copy(
        medicines = snapshot.medicines.filterNot { it.id == medicineId },
        logs = snapshot.logs.filterNot { it.medicineId == medicineId },
    )

    fun pruneLogs(snapshot: TrackerSnapshot, today: LocalDate): TrackerSnapshot {
        val cutoff = today.minusDays(45)
        return snapshot.copy(logs = snapshot.logs.filter { LocalDate.parse(it.date) >= cutoff })
    }
}
