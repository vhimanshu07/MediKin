package com.carecircle.medtracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.carecircle.medtracker.ui.MedicineTrackerRoot
import com.carecircle.medtracker.ui.TrackerViewModel
import com.carecircle.medtracker.ui.theme.FamilyMedicineTheme

class MainActivity : ComponentActivity() {
    private val app: MedicineTrackerApp get() = application as MedicineTrackerApp
    private val viewModel: TrackerViewModel by viewModels {
        TrackerViewModel.Factory(app.repository, app.reminderScheduler)
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FamilyMedicineTheme {
                MedicineTrackerRoot(viewModel)
            }
        }
        requestNotificationsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
