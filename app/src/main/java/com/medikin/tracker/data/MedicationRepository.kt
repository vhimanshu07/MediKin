package com.medikin.tracker.data

import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.TrackerSnapshot
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
