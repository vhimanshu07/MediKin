package com.carecircle.medtracker

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

        composeRule.onNodeWithTag("nav_family").performClick()
        composeRule.onNodeWithText("Family circle").assertIsDisplayed()
    }

    @Test
    fun addMedicineFormValidatesAndSaves() {
        composeRule.onNodeWithTag("add_medicine").performClick()
        composeRule.onNodeWithTag("medicine_name").performTextInput("Calcium")
        composeRule.onNodeWithTag("medicine_dosage").performTextInput("1 tablet")
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
}
