package com.carecircle.medtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.carecircle.medtracker.data.MedicationRepository
import com.carecircle.medtracker.domain.Adherence
import com.carecircle.medtracker.domain.Caregiver
import com.carecircle.medtracker.domain.DoseOccurrence
import com.carecircle.medtracker.domain.DosePlanner
import com.carecircle.medtracker.domain.DoseStatus
import com.carecircle.medtracker.domain.DoseTime
import com.carecircle.medtracker.domain.Medicine
import com.carecircle.medtracker.reminder.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

data class TrackerUiState(
    val parentName: String = "",
    val doses: List<DoseOccurrence> = emptyList(),
    val medicines: List<Medicine> = emptyList(),
    val refillMedicines: List<Medicine> = emptyList(),
    val caregiver: Caregiver = Caregiver(),
    val adherence: Adherence = Adherence(0, 0),
) {
    val missedDoses: List<DoseOccurrence> get() = doses.filter { it.status == DoseStatus.MISSED }
    val displayParentName: String get() = parentName.ifBlank { "your parent" }
}

class TrackerViewModel(
    private val repository: MedicationRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val clock = MutableStateFlow(LocalDateTime.now())

    val uiState = combine(repository.snapshot, clock) { snapshot, now ->
        val doses = DosePlanner.dosesFor(now.toLocalDate(), now, snapshot.medicines, snapshot.logs)
        TrackerUiState(
            parentName = snapshot.caregiver.parentName,
            doses = doses,
            medicines = snapshot.medicines,
            refillMedicines = DosePlanner.refillMedicines(snapshot.medicines),
            caregiver = snapshot.caregiver,
            adherence = DosePlanner.adherence(doses),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerUiState())

    init {
        viewModelScope.launch {
            while (isActive) {
                clock.value = LocalDateTime.now()
                delay(30_000)
            }
        }
    }

    fun refresh() { clock.value = LocalDateTime.now() }

    fun markTaken(dose: DoseOccurrence) {
        repository.recordDose(dose.medicine.id, dose.time, DoseStatus.TAKEN, LocalDateTime.now())
    }

    fun skipDose(dose: DoseOccurrence) {
        repository.recordDose(dose.medicine.id, dose.time, DoseStatus.SKIPPED, LocalDateTime.now())
    }

    fun addMedicine(
        name: String,
        dosage: String,
        instructions: String,
        times: Set<DoseTime>,
        stock: Int,
        refillAt: Int,
    ) {
        val medicine = Medicine(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            dosage = dosage.trim(),
            instructions = instructions.trim().ifBlank { "Follow doctor's instructions" },
            times = times.sortedBy(DoseTime::hour),
            stock = stock,
            refillAt = refillAt,
            colorIndex = repository.snapshot.value.medicines.size % 4,
        )
        repository.addMedicine(medicine)
        reminderScheduler.scheduleMedicine(medicine)
    }

    fun restock(medicineId: String) = repository.restock(medicineId)

    fun deleteMedicine(medicine: Medicine) {
        reminderScheduler.cancelMedicine(medicine)
        repository.deleteMedicine(medicine.id)
    }

    fun updateCaregiver(parentName: String, caregiverName: String, phone: String) {
        repository.updateCaregiver(
            Caregiver(parentName.trim(), caregiverName.trim(), phone.filter { it.isDigit() || it == '+' }),
        )
    }

    class Factory(
        private val repository: MedicationRepository,
        private val scheduler: ReminderScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrackerViewModel(repository, scheduler) as T
    }
}
