package com.medikin.tracker

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class MedicineTrackerUiTest {
    private val permissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(permissionRule).around(composeRule)

    @Test
    fun dashboardAndAllPrimaryDestinationsAreReachable() {
        composeRule.onNodeWithText("MediKin").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_parent_name").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_medicines").performClick()
        composeRule.onNodeWithText("Medicine cabinet").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.onNodeWithText("History & insights").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_family").performClick()
        composeRule.onNodeWithText("Family circle").assertIsDisplayed()
    }

    @Test
    fun addMedicineFormValidatesAndSaves() {
        composeRule.onNodeWithTag("add_medicine").performClick()
        composeRule.onNodeWithTag("medicine_name").performTextInput("Calcium")
        composeRule.onNodeWithTag("medicine_dosage").performTextInput("1 tablet")
        composeRule.onNodeWithTag("add_medicine_form").performScrollToIndex(12)
        composeRule.onNodeWithTag("save_medicine").performClick()

        composeRule.onNodeWithText("Calcium").assertIsDisplayed()
    }

    @Test
    fun backFromParentSetupReturnsToTodayInsteadOfClosingApp() {
        composeRule.onNodeWithTag("edit_parent_name").performClick()
        composeRule.onNodeWithText("Family circle").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("MediKin").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_today").assertIsDisplayed()
    }

    @Test
    fun flexibleScheduleModesAreAvailable() {
        composeRule.onNodeWithTag("add_medicine").performClick()
        composeRule.onNodeWithText("Every X hours").performClick()
        composeRule.onNodeWithText("Repeat every (hours)").assertIsDisplayed()
        composeRule.onNodeWithText("As needed").performClick()
        composeRule.onNodeWithText("Set the plan exactly as prescribed.").assertIsDisplayed()
    }

    @Test
    fun medicineCanBeEditedAndTakenDoseCanBeCorrected() {
        val name = "Emulator flow medicine"
        composeRule.onNodeWithTag("add_medicine").performClick()
        composeRule.onNodeWithTag("medicine_name").performTextInput(name)
        composeRule.onNodeWithTag("medicine_dosage").performTextInput("5 mg")
        composeRule.onNodeWithTag("add_medicine_form").performScrollToIndex(12)
        composeRule.onNodeWithTag("save_medicine").performClick()

        composeRule.onNodeWithTag("nav_medicines").performClick()
        composeRule.onNodeWithContentDescription("Edit $name").performScrollTo().performClick()
        composeRule.onNodeWithTag("medicine_dosage").performTextClearance()
        composeRule.onNodeWithTag("medicine_dosage").performTextInput("10 mg")
        composeRule.onNodeWithTag("add_medicine_form").performScrollToIndex(12)
        composeRule.onNodeWithTag("save_medicine").performClick()
        composeRule.onNodeWithText("10 mg", substring = true).assertIsDisplayed()

        composeRule.onNodeWithTag("nav_today").performClick()
        composeRule.onNodeWithContentDescription("Dose for $name").performScrollTo()
        composeRule.onNode(
            (hasText("Mark taken") or hasText("Take now")) and
                hasAnyAncestor(hasContentDescription("Dose for $name")),
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNode(
            hasText("Change record") and hasAnyAncestor(hasContentDescription("Dose for $name")),
            useUnmergedTree = true,
        ).performClick()
        composeRule.onNodeWithText("Mark skipped").performClick()
        composeRule.onNode(
            hasText("Skipped") and hasAnyAncestor(hasContentDescription("Dose for $name")),
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }
}
