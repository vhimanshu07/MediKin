package com.medikin.tracker.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.test.core.app.ApplicationProvider
import com.medikin.tracker.data.MedicationRepository
import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.MedicationReducer
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.TrackerSnapshot
import com.medikin.tracker.reminder.ReminderScheduler
import com.medikin.tracker.ui.theme.FamilyMedicineTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MedicineTrackerComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeMedicationRepository
    private lateinit var viewModel: TrackerViewModel

    @Before
    fun setUp() {
        repository = FakeMedicationRepository(
            TrackerSnapshot(
                medicines = listOf(
                    Medicine(
                        id = "test-med",
                        name = "Test tablet",
                        dosage = "5 mg",
                        instructions = "After food",
                        times = listOf(DoseTime.MORNING),
                        stock = 2,
                        refillAt = 3,
                    ),
                ),
            ),
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = TrackerViewModel(repository, ReminderScheduler(context))
        composeRule.setContent { FamilyMedicineTheme { MedicineTrackerRoot(viewModel) } }
    }

    @Test
    fun `primary destinations render their content`() {
        composeRule.onNodeWithText("MediKin").assertIsDisplayed()
        composeRule.onNodeWithText("Set up who you're caring for").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_medicines").performClick()
        composeRule.onNodeWithText("Medicine cabinet").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.onNodeWithText("History & insights").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_family").performClick()
        composeRule.onNodeWithText("Family circle").assertIsDisplayed()
    }

    @Test
    fun `adding medicine from sheet updates repository and dashboard`() {
        composeRule.onNodeWithTag("add_medicine").performClick()
        composeRule.onNodeWithTag("medicine_name").performTextInput("Calcium")
        composeRule.onNodeWithTag("medicine_dosage").performTextInput("1 tablet")
        composeRule.onNodeWithTag("add_medicine_form").performScrollToIndex(12)
        composeRule.onNodeWithTag("save_medicine").performClick()
        composeRule.waitForIdle()

        assertTrue(repository.snapshot.value.medicines.any { it.name == "Calcium" })
        composeRule.onNodeWithTag("nav_medicines").performClick()
        composeRule.onNodeWithTag("medicine_list").performScrollToIndex(2)
        composeRule.onNodeWithText("Calcium").assertIsDisplayed()
    }

    @Test
    fun `parent name can be changed from dashboard title`() {
        composeRule.onNodeWithTag("edit_parent_name").performClick()
        composeRule.onNodeWithTag("parent_name").performTextInput("Dad")
        composeRule.onNodeWithTag("caregiver_name").performTextInput("Neha")
        composeRule.onNodeWithTag("save_caregiver").performScrollTo().performClick()

        composeRule.onNodeWithTag("nav_today").performClick()
        composeRule.onNodeWithText("Caring for Dad").assertIsDisplayed()
    }

    @Test
    fun `medicine can be edited from cabinet`() {
        composeRule.onNodeWithTag("nav_medicines").performClick()
        composeRule.onNodeWithTag("edit_test-med").performClick()
        composeRule.onNodeWithTag("medicine_dosage").performTextClearance()
        composeRule.onNodeWithTag("medicine_dosage").performTextInput("10 mg")
        composeRule.onNodeWithTag("add_medicine_form").performScrollToIndex(12)
        composeRule.onNodeWithTag("save_medicine").performClick()

        assertTrue(repository.snapshot.value.medicines.single().dosage == "10 mg")
        composeRule.onNodeWithText("10 mg", substring = true).assertIsDisplayed()
    }

    @Test
    fun `taken dose can be corrected to skipped with stock restored`() {
        composeRule.onNodeWithTag("today_list").performScrollToIndex(4)
        composeRule.onNodeWithTag("take_test-med_08-00").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("change_test-med_08-00").performClick()
        composeRule.onNodeWithText("Mark skipped").performClick()
        composeRule.waitForIdle()

        assertTrue(repository.snapshot.value.logs.single().status == DoseStatus.SKIPPED)
        assertTrue(repository.snapshot.value.medicines.single().stock == 2)
    }
}

private class FakeMedicationRepository(initial: TrackerSnapshot) : MedicationRepository {
    private val mutableSnapshot = MutableStateFlow(initial)
    override val snapshot: StateFlow<TrackerSnapshot> = mutableSnapshot

    override fun addMedicine(medicine: Medicine) {
        mutableSnapshot.value = MedicationReducer.addMedicine(mutableSnapshot.value, medicine)
    }

    override fun updateMedicine(medicine: Medicine) {
        mutableSnapshot.value = MedicationReducer.updateMedicine(mutableSnapshot.value, medicine)
    }

    override fun recordDose(medicineId: String, time: DoseTime, status: DoseStatus, at: LocalDateTime) {
        mutableSnapshot.value = MedicationReducer.recordDose(mutableSnapshot.value, medicineId, time, status, at)
    }

    override fun changeDoseStatus(medicineId: String, time: DoseTime, status: DoseStatus?, at: LocalDateTime) {
        mutableSnapshot.value = MedicationReducer.changeDoseStatus(mutableSnapshot.value, medicineId, time, status, at)
    }

    override fun recordAsNeeded(medicineId: String, at: LocalDateTime) {
        mutableSnapshot.value = MedicationReducer.recordAsNeeded(mutableSnapshot.value, medicineId, at)
    }

    override fun restock(medicineId: String, amount: Int) {
        mutableSnapshot.value = MedicationReducer.restock(mutableSnapshot.value, medicineId, amount)
    }

    override fun pauseMedicine(medicineId: String, pausedUntil: LocalDate?) {
        mutableSnapshot.value = MedicationReducer.pauseMedicine(mutableSnapshot.value, medicineId, pausedUntil)
    }

    override fun deleteMedicine(medicineId: String) {
        mutableSnapshot.value = MedicationReducer.delete(mutableSnapshot.value, medicineId)
    }

    override fun updateCaregiver(caregiver: Caregiver) {
        mutableSnapshot.value = mutableSnapshot.value.copy(caregiver = caregiver)
    }

    override fun synchronizeHistory(today: LocalDate) {
        mutableSnapshot.value = MedicationReducer.materializeMissedHistory(mutableSnapshot.value, today)
    }

    override fun replaceSnapshot(snapshot: TrackerSnapshot) {
        mutableSnapshot.value = snapshot
    }
}
