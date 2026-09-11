package com.carecircle.medtracker.data

import com.carecircle.medtracker.domain.Caregiver
import com.carecircle.medtracker.domain.DoseStatus
import com.carecircle.medtracker.domain.DoseTime
import com.carecircle.medtracker.domain.Medicine
import com.carecircle.medtracker.domain.TrackerSnapshot
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

interface MedicationRepository {
    val snapshot: StateFlow<TrackerSnapshot>

    fun addMedicine(medicine: Medicine)
    fun recordDose(medicineId: String, time: DoseTime, status: DoseStatus, at: LocalDateTime)
    fun restock(medicineId: String, amount: Int = 30)
    fun deleteMedicine(medicineId: String)
    fun updateCaregiver(caregiver: Caregiver)
}
