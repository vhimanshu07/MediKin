package com.medikin.tracker.data

import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.TrackerSnapshot
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime
import java.time.LocalDate

interface MedicationRepository {
    val snapshot: StateFlow<TrackerSnapshot>

    fun addMedicine(medicine: Medicine)
    fun updateMedicine(medicine: Medicine)
    fun recordDose(medicineId: String, time: DoseTime, status: DoseStatus, at: LocalDateTime)
    fun changeDoseStatus(medicineId: String, time: DoseTime, status: DoseStatus?, at: LocalDateTime)
    fun recordAsNeeded(medicineId: String, at: LocalDateTime)
    fun restock(medicineId: String, amount: Int = 30)
    fun pauseMedicine(medicineId: String, pausedUntil: LocalDate?)
    fun deleteMedicine(medicineId: String)
    fun updateCaregiver(caregiver: Caregiver)
    fun synchronizeHistory(today: LocalDate = LocalDate.now())
    fun replaceSnapshot(snapshot: TrackerSnapshot)
}
