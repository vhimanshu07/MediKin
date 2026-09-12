package com.medikin.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medikin.tracker.data.MedicationRepository
import com.medikin.tracker.data.EncryptedBackupCodec
import com.medikin.tracker.domain.Adherence
import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseOccurrence
import com.medikin.tracker.domain.DosePlanner
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.HistoryEntry
import com.medikin.tracker.domain.MedicineHistory
import com.medikin.tracker.domain.ScheduleType
import com.medikin.tracker.reminder.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.UUID

data class TrackerUiState(
    val parentName: String = "",
    val doses: List<DoseOccurrence> = emptyList(),
    val medicines: List<Medicine> = emptyList(),
    val refillMedicines: List<Medicine> = emptyList(),
    val caregiver: Caregiver = Caregiver(),
    val adherence: Adherence = Adherence(0, 0),
    val historyEntries: List<HistoryEntry> = emptyList(),
    val weeklyAdherence: Adherence = Adherence(0, 0),
    val monthlyAdherence: Adherence = Adherence(0, 0),
    val medicineHistory: List<MedicineHistory> = emptyList(),
) {
    val missedDoses: List<DoseOccurrence> get() = doses.filter { it.status == DoseStatus.MISSED }
    val asNeededMedicines: List<Medicine>
        get() = medicines.filter { it.isActive && it.effectiveScheduleType == ScheduleType.AS_NEEDED }
    val displayParentName: String get() = parentName.ifBlank { "your parent" }
}

class TrackerViewModel(
    private val repository: MedicationRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupCodec: EncryptedBackupCodec = EncryptedBackupCodec(),
) : ViewModel() {
    private val clock = MutableStateFlow(LocalDateTime.now())

    val uiState = combine(repository.snapshot, clock) { snapshot, now ->
        val doses = DosePlanner.dosesFor(now.toLocalDate(), now, snapshot.medicines, snapshot.logs)
        val monthStart = now.toLocalDate().minusDays(29)
        val history = DosePlanner.historyEntries(snapshot, monthStart, now.toLocalDate()).toMutableList()
        doses.filter { it.status == DoseStatus.MISSED }.forEach { dose ->
            if (history.none {
                    it.medicineId == dose.medicine.id &&
                        it.date == dose.scheduledAt.toLocalDate() &&
                        it.time == dose.time
                }
            ) {
                history += HistoryEntry(
                    medicineId = dose.medicine.id,
                    medicineName = dose.medicine.name,
                    dosage = dose.displayDosage,
                    date = dose.scheduledAt.toLocalDate(),
                    time = dose.time,
                    status = DoseStatus.MISSED,
                    recordedAt = dose.scheduledAt.plusMinutes(DosePlanner.MISSED_AFTER_MINUTES).toString(),
                )
            }
        }
        val sortedHistory = history.sortedWith(
            compareByDescending<HistoryEntry> { it.date }.thenByDescending { it.time.asLocalTime() },
        )
        TrackerUiState(
            parentName = snapshot.caregiver.parentName,
            doses = doses,
            medicines = snapshot.medicines,
            refillMedicines = DosePlanner.refillMedicines(snapshot.medicines),
            caregiver = snapshot.caregiver,
            adherence = DosePlanner.adherence(doses),
            historyEntries = sortedHistory,
            weeklyAdherence = historyAdherence(sortedHistory.filter { it.date >= now.toLocalDate().minusDays(6) }),
            monthlyAdherence = historyAdherence(sortedHistory),
            medicineHistory = DosePlanner.medicineHistory(sortedHistory),
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

    fun refresh() {
        repository.synchronizeHistory(LocalDate.now())
        clock.value = LocalDateTime.now()
    }

    fun markTaken(dose: DoseOccurrence) {
        correctDose(dose, DoseStatus.TAKEN)
    }

    fun skipDose(dose: DoseOccurrence) {
        correctDose(dose, DoseStatus.SKIPPED)
    }

    fun addMedicine(
        name: String,
        dosage: String,
        instructions: String,
        times: Set<DoseTime>,
        stock: Int,
        refillAt: Int,
        scheduleType: ScheduleType = ScheduleType.DAILY,
        weekdays: Set<Int> = Medicine.ALL_WEEKDAYS,
        intervalHours: Int? = null,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate? = null,
        scheduledDoses: List<com.medikin.tracker.domain.ScheduledDose>? = null,
        stockUnit: String = "doses",
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
            scheduleType = scheduleType,
            weekdays = weekdays,
            intervalHours = intervalHours,
            startDate = startDate.toString(),
            endDate = endDate?.toString(),
            scheduledDoses = scheduledDoses,
            stockUnit = stockUnit.trim().ifBlank { "doses" },
            createdDate = LocalDate.now().toString(),
        )
        repository.addMedicine(medicine)
        reminderScheduler.scheduleMedicine(medicine)
    }

    fun updateMedicine(medicine: Medicine) {
        val existing = repository.snapshot.value.medicines.firstOrNull { it.id == medicine.id } ?: return
        reminderScheduler.cancelMedicine(existing)
        repository.updateMedicine(medicine)
        repository.snapshot.value.medicines.firstOrNull { it.id == medicine.id }
            ?.let(reminderScheduler::scheduleMedicine)
    }

    fun correctDose(dose: DoseOccurrence, status: DoseStatus?) {
        val recordedAt = LocalDateTime.of(dose.scheduledAt.toLocalDate(), LocalDateTime.now().toLocalTime())
        repository.changeDoseStatus(dose.medicine.id, dose.time, status, recordedAt)
    }

    fun recordAsNeeded(medicineId: String) = repository.recordAsNeeded(medicineId, LocalDateTime.now())

    fun restock(medicineId: String, amount: Int = 30) = repository.restock(medicineId, amount)

    fun pauseMedicine(medicine: Medicine, days: Long?) {
        reminderScheduler.cancelMedicine(medicine)
        val until = days?.let { LocalDate.now().plusDays(it) }
        repository.pauseMedicine(medicine.id, until)
        repository.snapshot.value.medicines.firstOrNull { it.id == medicine.id }
            ?.let(reminderScheduler::scheduleMedicine)
    }

    fun deleteMedicine(medicine: Medicine) {
        reminderScheduler.cancelMedicine(medicine)
        repository.deleteMedicine(medicine.id)
    }

    fun updateCaregiver(parentName: String, caregiverName: String, phone: String) {
        repository.updateCaregiver(
            Caregiver(parentName.trim(), caregiverName.trim(), phone.filter { it.isDigit() || it == '+' }),
        )
    }

    fun createEncryptedBackup(password: String): Result<ByteArray> = runCatching {
        val chars = password.toCharArray()
        try {
            backupCodec.encode(repository.snapshot.value, chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    fun restoreEncryptedBackup(bytes: ByteArray, password: String): Result<Unit> = runCatching {
        val chars = password.toCharArray()
        val restored = try {
            backupCodec.decode(bytes, chars)
        } finally {
            chars.fill('\u0000')
        }
        reminderScheduler.cancelAll(repository.snapshot.value.medicines)
        repository.replaceSnapshot(restored)
        reminderScheduler.scheduleAll(repository.snapshot.value.medicines)
    }

    class Factory(
        private val repository: MedicationRepository,
        private val scheduler: ReminderScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrackerViewModel(repository, scheduler) as T
    }

    private companion object {
        fun historyAdherence(entries: List<HistoryEntry>) = Adherence(
            taken = entries.count { it.status == DoseStatus.TAKEN },
            total = entries.size,
        )
    }
}
