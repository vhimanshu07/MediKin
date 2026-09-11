package com.medikin.tracker.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.MedicationReducer
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.TrackerSnapshot
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

class PreferencesMedicationRepository(
    private val preferences: SharedPreferences,
    private val gson: Gson = trackerGson(),
) : MedicationRepository {
    private val mutableSnapshot = MutableStateFlow(load().withoutLegacyStarterData())
    override val snapshot: StateFlow<TrackerSnapshot> = mutableSnapshot.asStateFlow()

    init {
        mutate { MedicationReducer.pruneLogs(it, LocalDate.now()) }
    }

    @Synchronized
    override fun addMedicine(medicine: Medicine) = mutate {
        MedicationReducer.addMedicine(it, medicine)
    }

    @Synchronized
    override fun recordDose(
        medicineId: String,
        time: DoseTime,
        status: DoseStatus,
        at: LocalDateTime,
    ) = mutate { MedicationReducer.recordDose(it, medicineId, time, status, at) }

    @Synchronized
    override fun restock(medicineId: String, amount: Int) = mutate {
        MedicationReducer.restock(it, medicineId, amount)
    }

    @Synchronized
    override fun deleteMedicine(medicineId: String) = mutate {
        MedicationReducer.delete(it, medicineId)
    }

    @Synchronized
    override fun updateCaregiver(caregiver: Caregiver) = mutate {
        it.copy(caregiver = caregiver)
    }

    private fun mutate(transform: (TrackerSnapshot) -> TrackerSnapshot) {
        val updated = transform(mutableSnapshot.value)
        mutableSnapshot.value = updated
        preferences.edit { putString(KEY_SNAPSHOT, gson.toJson(updated)) }
    }

    private fun load(): TrackerSnapshot = runCatching {
        preferences.getString(KEY_SNAPSHOT, null)
            ?.let { gson.fromJson(it, TrackerSnapshot::class.java) }
            ?: TrackerSnapshot()
    }.getOrDefault(TrackerSnapshot())

    private fun TrackerSnapshot.withoutLegacyStarterData(): TrackerSnapshot {
        val starterIds = setOf(
            "starter-blood-pressure",
            "starter-vitamin",
            "starter-diabetes",
        )
        val cleanedCaregiver = if (
            caregiver.parentName == "Mom" && caregiver.name.isBlank() && caregiver.phone.isBlank()
        ) Caregiver() else caregiver
        return copy(
            medicines = medicines.filterNot { it.id in starterIds },
            logs = logs.filterNot { it.medicineId in starterIds },
            caregiver = cleanedCaregiver,
        )
    }

    private companion object {
        const val KEY_SNAPSHOT = "tracker_snapshot"
    }
}
