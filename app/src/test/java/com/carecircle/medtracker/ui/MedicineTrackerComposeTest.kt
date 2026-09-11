package com.carecircle.medtracker.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.carecircle.medtracker.data.MedicationRepository
import com.carecircle.medtracker.domain.Caregiver
import com.carecircle.medtracker.domain.DoseStatus
import com.carecircle.medtracker.domain.DoseTime
import com.carecircle.medtracker.domain.MedicationReducer
import com.carecircle.medtracker.domain.Medicine
import com.carecircle.medtracker.domain.TrackerSnapshot
import com.carecircle.medtracker.reminder.ReminderScheduler
import com.carecircle.medtracker.ui.theme.FamilyMedicineTheme
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

        composeRule.onNodeWithTag("nav_family").performClick()
        composeRule.onNodeWithText("Family circle").assertIsDisplayed()
    }

    @Test
    fun `adding medicine from sheet updates repository and dashboard`() {
        composeRule.onNodeWithTag("add_medicine").performClick()
        composeRule.onNodeWithTag("medicine_name").performTextInput("Calcium")
        composeRule.onNodeWithTag("medicine_dosage").performTextInput("1 tablet")
        composeRule.onNodeWithTag("add_medicine_form").performScrollToIndex(6)
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
}

private class FakeMedicationRepository(initial: TrackerSnapshot) : MedicationRepository {
    private val mutableSnapshot = MutableStateFlow(initial)
    override val snapshot: StateFlow<TrackerSnapshot> = mutableSnapshot

    override fun addMedicine(medicine: Medicine) {
        mutableSnapshot.value = MedicationReducer.addMedicine(mutableSnapshot.value, medicine)
    }

    override fun recordDose(medicineId: String, time: DoseTime, status: DoseStatus, at: LocalDateTime) {
        mutableSnapshot.value = MedicationReducer.recordDose(mutableSnapshot.value, medicineId, time, status, at)
    }

    override fun restock(medicineId: String, amount: Int) {
        mutableSnapshot.value = MedicationReducer.restock(mutableSnapshot.value, medicineId, amount)
    }

    override fun deleteMedicine(medicineId: String) {
        mutableSnapshot.value = MedicationReducer.delete(mutableSnapshot.value, medicineId)
    }

    override fun updateCaregiver(caregiver: Caregiver) {
        mutableSnapshot.value = mutableSnapshot.value.copy(caregiver = caregiver)
    }
}
