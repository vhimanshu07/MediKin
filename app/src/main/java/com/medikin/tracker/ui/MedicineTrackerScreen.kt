package com.medikin.tracker.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.medikin.tracker.domain.Caregiver
import com.medikin.tracker.domain.DoseOccurrence
import com.medikin.tracker.domain.DoseStatus
import com.medikin.tracker.domain.DoseTime
import com.medikin.tracker.domain.Medicine
import com.medikin.tracker.domain.ScheduleType
import com.medikin.tracker.domain.ScheduledDose
import com.medikin.tracker.ui.theme.Coral
import com.medikin.tracker.ui.theme.Evergreen
import com.medikin.tracker.ui.theme.Marigold
import com.medikin.tracker.ui.theme.Mint
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class AppSection(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.Home),
    MEDICINES("Medicines", Icons.Default.Medication),
    HISTORY("History", Icons.Default.CalendarMonth),
    FAMILY("Family", Icons.Default.FamilyRestroom),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineTrackerRoot(viewModel: TrackerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSection by rememberSaveable { mutableStateOf(AppSection.TODAY.name) }
    var showAddMedicine by rememberSaveable { mutableStateOf(false) }
    var editingMedicineId by rememberSaveable { mutableStateOf<String?>(null) }
    val section = AppSection.valueOf(selectedSection)

    BackHandler(enabled = section != AppSection.TODAY) {
        selectedSection = AppSection.TODAY.name
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppSection.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item,
                        onClick = { selectedSection = item.name },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        modifier = Modifier.testTag("nav_${item.name.lowercase()}"),
                    )
                }
            }
        },
        floatingActionButton = {
            if (section == AppSection.TODAY || section == AppSection.MEDICINES) {
                ExtendedFloatingActionButton(
                    onClick = { showAddMedicine = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add medicine") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_medicine"),
                )
            }
        },
    ) { padding ->
        when (section) {
            AppSection.TODAY -> TodayScreen(
                state = state,
                padding = padding,
                onTaken = viewModel::markTaken,
                onSkipped = viewModel::skipDose,
                onCorrected = viewModel::correctDose,
                onAsNeededTaken = viewModel::recordAsNeeded,
                onEditParent = { selectedSection = AppSection.FAMILY.name },
            )
            AppSection.MEDICINES -> MedicinesScreen(
                state = state,
                padding = padding,
                onRestock = viewModel::restock,
                onDelete = viewModel::deleteMedicine,
                onEdit = { editingMedicineId = it.id },
                onPause = viewModel::pauseMedicine,
            )
            AppSection.HISTORY -> HistoryScreen(state, padding)
            AppSection.FAMILY -> FamilyScreen(
                state = state,
                padding = padding,
                onSave = viewModel::updateCaregiver,
                onCreateBackup = viewModel::createEncryptedBackup,
                onRestoreBackup = viewModel::restoreEncryptedBackup,
            )
        }
    }

    if (showAddMedicine) {
        MedicineEditorSheet(
            medicine = null,
            onDismiss = { showAddMedicine = false },
            onSave = { values ->
                viewModel.addMedicine(
                    name = values.name,
                    dosage = values.dosage,
                    instructions = values.instructions,
                    times = values.scheduledDoses.mapTo(mutableSetOf()) { it.time },
                    stock = values.stock,
                    refillAt = values.refillAt,
                    scheduleType = values.scheduleType,
                    weekdays = values.weekdays,
                    intervalHours = values.intervalHours,
                    startDate = values.startDate,
                    endDate = values.endDate,
                    scheduledDoses = values.scheduledDoses,
                    stockUnit = values.stockUnit,
                )
                showAddMedicine = false
            },
        )
    }

    state.medicines.firstOrNull { it.id == editingMedicineId }?.let { medicine ->
        MedicineEditorSheet(
            medicine = medicine,
            onDismiss = { editingMedicineId = null },
            onSave = { values ->
                viewModel.updateMedicine(
                    medicine.copy(
                        name = values.name.trim(),
                        dosage = values.dosage.trim(),
                        instructions = values.instructions.trim().ifBlank { "Follow doctor's instructions" },
                        times = values.scheduledDoses.map { it.time },
                        stock = values.stock,
                        refillAt = values.refillAt,
                        scheduleType = values.scheduleType,
                        weekdays = values.weekdays,
                        intervalHours = values.intervalHours,
                        startDate = values.startDate.toString(),
                        endDate = values.endDate?.toString(),
                        scheduledDoses = values.scheduledDoses,
                        stockUnit = values.stockUnit.trim().ifBlank { "doses" },
                    ),
                )
                editingMedicineId = null
            },
        )
    }
}

@Composable
private fun TodayScreen(
    state: TrackerUiState,
    padding: PaddingValues,
    onTaken: (DoseOccurrence) -> Unit,
    onSkipped: (DoseOccurrence) -> Unit,
    onCorrected: (DoseOccurrence, DoseStatus?) -> Unit,
    onAsNeededTaken: (String) -> Unit,
    onEditParent: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())
            .testTag("today_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { AppHeader(state.parentName, onEditParent, Modifier.statusBarsPadding()) }
        item { AdherenceHero(state) }
        if (state.refillMedicines.isNotEmpty()) {
            item { RefillBanner(state.refillMedicines.size) }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Today's medicines", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    "${state.adherence.taken} of ${state.adherence.total} taken",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.doses.isEmpty()) {
            item { EmptyState("No medicines yet", "Tap Add medicine to create the first reminder.") }
        } else {
            items(state.doses, key = { "${it.medicine.id}_${it.time.key}" }) { dose ->
                DoseCard(dose, onTaken, onSkipped, onCorrected)
            }
        }
        if (state.asNeededMedicines.isNotEmpty()) {
            item { Text("As needed", style = MaterialTheme.typography.titleLarge) }
            items(state.asNeededMedicines, key = { "prn_${it.id}" }) { medicine ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(medicine.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${medicine.dosage} · ${medicine.instructions}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = { onAsNeededTaken(medicine.id) },
                            modifier = Modifier.testTag("take_prn_${medicine.id}"),
                        ) { Text("Record taken") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun AppHeader(
    parentName: String,
    onEditParent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "MediKin",
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (parentName.isBlank()) "Set up who you're caring for" else "Caring for $parentName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onEditParent,
                    modifier = Modifier.testTag("edit_parent_name"),
                ) {
                    Icon(Icons.Outlined.Edit, "Change parent's name", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(Mint),
            contentAlignment = Alignment.Center,
        ) {
            if (parentName.isBlank()) {
                Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Evergreen)
            } else {
                Text(parentName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Evergreen)
            }
        }
    }
}

@Composable
private fun AdherenceHero(state: TrackerUiState) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Evergreen),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "${state.adherence.percent} percent of today's medicines taken"
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Today's care", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(5.dp))
                Text(
                    when {
                        state.adherence.total == 0 -> "Ready when you are"
                        state.adherence.taken == state.adherence.total -> "All done. Lovely!"
                        state.missedDoses.isNotEmpty() -> "A check-in may help"
                        else -> "You're on track"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${state.adherence.taken} completed · ${state.adherence.total - state.adherence.taken} remaining",
                    color = Color.White.copy(alpha = .85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { state.adherence.percent / 100f },
                    modifier = Modifier.size(76.dp),
                    color = Marigold,
                    trackColor = Color.White.copy(alpha = .18f),
                    strokeWidth = 8.dp,
                )
                Text("${state.adherence.percent}%", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RefillBanner(count: Int) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Marigold.copy(alpha = .16f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF875500))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Refill reminder", fontWeight = FontWeight.Bold, color = Color(0xFF704700))
                Text(
                    "$count ${if (count == 1) "medicine is" else "medicines are"} running low",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF704700),
                )
            }
        }
    }
}

@Composable
private fun DoseCard(
    dose: DoseOccurrence,
    onTaken: (DoseOccurrence) -> Unit,
    onSkipped: (DoseOccurrence) -> Unit,
    onCorrected: (DoseOccurrence, DoseStatus?) -> Unit,
) {
    val accent = medicineColor(dose.medicine.colorIndex)
    var showCorrection by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("dose_${dose.medicine.id}_${dose.time.key}")
            .semantics { contentDescription = "Dose for ${dose.medicine.name}" },
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = accent)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${dose.scheduledAt.format(DateTimeFormatter.ofPattern("h:mm a"))} · ${dose.time.label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor(dose.status),
                    )
                    Text(dose.medicine.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${dose.displayDosage} · ${dose.medicine.instructions}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusBadge(dose.status)
            }
            AnimatedVisibility(dose.status !in listOf(DoseStatus.TAKEN, DoseStatus.SKIPPED)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onSkipped(dose) }) { Text("Skip") }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = { onTaken(dose) },
                        modifier = Modifier.testTag("take_${dose.medicine.id}_${dose.time.key}"),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (dose.status == DoseStatus.MISSED) "Take now" else "Mark taken")
                    }
                }
            }
            if (dose.status in listOf(DoseStatus.TAKEN, DoseStatus.SKIPPED)) {
                TextButton(
                    onClick = { showCorrection = true },
                    modifier = Modifier.align(Alignment.End).testTag("change_${dose.medicine.id}_${dose.time.key}"),
                ) { Text("Change record") }
            }
        }
    }
    if (showCorrection) {
        AlertDialog(
            onDismissRequest = { showCorrection = false },
            title = { Text("Correct this dose") },
            text = { Text("Change the status or undo the recorded action. Stock will be corrected automatically.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { onCorrected(dose, DoseStatus.TAKEN); showCorrection = false }) {
                        Text("Mark taken")
                    }
                    TextButton(onClick = { onCorrected(dose, DoseStatus.SKIPPED); showCorrection = false }) {
                        Text("Mark skipped")
                    }
                    TextButton(onClick = { onCorrected(dose, null); showCorrection = false }) {
                        Text("Undo record")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showCorrection = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatusBadge(status: DoseStatus) {
    val (label, color) = when (status) {
        DoseStatus.UPCOMING -> "Later" to MaterialTheme.colorScheme.onSurfaceVariant
        DoseStatus.DUE -> "Due" to Marigold
        DoseStatus.TAKEN -> "Taken" to Evergreen
        DoseStatus.SKIPPED -> "Skipped" to MaterialTheme.colorScheme.onSurfaceVariant
        DoseStatus.MISSED -> "Missed" to Coral
    }
    Surface(shape = CircleShape, color = color.copy(alpha = .13f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

@Composable
private fun MedicinesScreen(
    state: TrackerUiState,
    padding: PaddingValues,
    onRestock: (String, Int) -> Unit,
    onDelete: (Medicine) -> Unit,
    onEdit: (Medicine) -> Unit,
    onPause: (Medicine, Long?) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).testTag("medicine_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 8.dp)) {
                Text("Medicine cabinet", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Stock, schedules and refill levels in one place",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.medicines.isEmpty()) {
            item { EmptyState("The cabinet is empty", "Add a medicine to start the daily plan.") }
        } else {
            items(state.medicines, key = Medicine::id) { medicine ->
                MedicineInventoryCard(medicine, onRestock, onDelete, onEdit, onPause)
            }
        }
    }
}

@Composable
private fun MedicineInventoryCard(
    medicine: Medicine,
    onRestock: (String, Int) -> Unit,
    onDelete: (Medicine) -> Unit,
    onEdit: (Medicine) -> Unit,
    onPause: (Medicine, Long?) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showRestock by remember { mutableStateOf(false) }
    var showPause by remember { mutableStateOf(false) }
    var restockAmount by rememberSaveable { mutableStateOf("30") }
    val isLow = medicine.stock <= medicine.refillAt
    val isPaused = medicine.isPausedOn(LocalDate.now())
    val progress = (medicine.stock / (medicine.refillAt.coerceAtLeast(1) * 3f)).coerceIn(0f, 1f)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("medicine_${medicine.id}"),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(medicineColor(medicine.colorIndex).copy(alpha = .15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Medication, null, tint = medicineColor(medicine.colorIndex))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(medicine.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${medicine.dosage} · ${scheduleSummary(medicine)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = { onEdit(medicine) }, modifier = Modifier.testTag("edit_${medicine.id}")) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit ${medicine.name}")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete ${medicine.name}")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${medicine.stock} ${medicine.effectiveStockUnit} left",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when {
                        isPaused -> "Paused"
                        isLow -> "Refill now"
                        else -> estimatedRunOutDays(medicine)?.let { "About $it ${if (it == 1) "day" else "days"}" }.orEmpty()
                    },
                    color = if (isLow) Coral else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (isLow) Coral else Evergreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                "Alert at ${medicine.refillAt} ${medicine.effectiveStockUnit}",
                modifier = Modifier.padding(top = 7.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { showRestock = true },
                    modifier = Modifier.weight(1f).testTag("restock_${medicine.id}"),
                ) {
                    Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add stock")
                }
                OutlinedButton(onClick = { showPause = true }, modifier = Modifier.weight(1f)) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPaused) "Resume" else "Pause")
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${medicine.name}?") },
            text = { Text("Its reminders will be removed. Existing dose history will be kept.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(medicine) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
    if (showRestock) {
        AlertDialog(
            onDismissRequest = { showRestock = false },
            title = { Text("Add ${medicine.effectiveStockUnit}") },
            text = {
                OutlinedTextField(
                    value = restockAmount,
                    onValueChange = { restockAmount = it.filter(Char::isDigit).take(4) },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = (restockAmount.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        onRestock(medicine.id, restockAmount.toInt())
                        showRestock = false
                    },
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showRestock = false }) { Text("Cancel") } },
        )
    }
    if (showPause) {
        AlertDialog(
            onDismissRequest = { showPause = false },
            title = { Text(if (isPaused) "Resume ${medicine.name}?" else "Pause ${medicine.name}") },
            text = { Text(if (isPaused) "Scheduled reminders will start again." else "Choose how long to pause scheduled reminders.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (isPaused) {
                        TextButton(onClick = { onPause(medicine, null); showPause = false }) { Text("Resume now") }
                    } else {
                        TextButton(onClick = { onPause(medicine, 0); showPause = false }) { Text("Pause today") }
                        TextButton(onClick = { onPause(medicine, 6); showPause = false }) { Text("Pause 7 days") }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showPause = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HistoryScreen(
    state: TrackerUiState,
    padding: PaddingValues,
) {
    var rangeDays by rememberSaveable { mutableStateOf(7) }
    val today = LocalDate.now()
    val from = today.minusDays((rangeDays - 1).toLong())
    val entries = state.historyEntries.filter { it.date >= from }
    val adherence = if (rangeDays == 7) state.weeklyAdherence else state.monthlyAdherence
    val medicineStats = entries.groupBy { it.medicineId to it.medicineName }.map { (identity, values) ->
        Triple(
            identity.second,
            values.count { it.status == DoseStatus.TAKEN },
            values.count { it.status != DoseStatus.TAKEN },
        )
    }.sortedByDescending { it.second + it.third }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).testTag("history_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 4.dp)) {
                Text("History & insights", style = MaterialTheme.typography.headlineMedium)
                Text("See adherence and spot missed-dose patterns", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = rangeDays == 7,
                    onClick = { rangeDays = 7 },
                    label = { Text("7 days") },
                )
                FilterChip(
                    selected = rangeDays == 30,
                    onClick = { rangeDays = 30 },
                    label = { Text("30 days") },
                )
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Evergreen),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "${adherence.percent} percent adherence over $rangeDays days"
                },
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$rangeDays-day adherence", color = Color.White.copy(alpha = .78f))
                        Text("${adherence.percent}%", color = Color.White, style = MaterialTheme.typography.displaySmall)
                        Text(
                            "${adherence.taken} taken · ${entries.count { it.status == DoseStatus.MISSED }} missed · ${entries.count { it.status == DoseStatus.SKIPPED }} skipped",
                            color = Color.White.copy(alpha = .86f),
                        )
                    }
                    CircularProgressIndicator(
                        progress = { adherence.percent / 100f },
                        modifier = Modifier.size(72.dp),
                        color = Marigold,
                        trackColor = Color.White.copy(alpha = .18f),
                        strokeWidth = 8.dp,
                    )
                }
            }
        }
        item { Text("Missed-dose trend", style = MaterialTheme.typography.titleLarge) }
        items((0L..6L).map { today.minusDays(it) }, key = { "trend_$it" }) { date ->
            val dayEntries = entries.filter { it.date == date }
            val missed = dayEntries.count { it.status == DoseStatus.MISSED }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEE d")),
                    modifier = Modifier.width(72.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { (missed / dayEntries.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = Coral,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text("$missed missed", modifier = Modifier.width(82.dp), textAlign = TextAlign.End)
            }
        }
        item { Text("By medicine", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
        if (medicineStats.isEmpty()) {
            item { EmptyState("No history yet", "Taken, skipped, and missed doses will appear here.") }
        } else {
            items(medicineStats, key = { it.first }) { (name, taken, notTaken) ->
                val total = taken + notTaken
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text("$taken taken · $notTaken not taken", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${if (total == 0) 0 else taken * 100 / total}%", fontWeight = FontWeight.Bold, color = Evergreen)
                    }
                }
            }
        }
        item { Text("Recent records", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
        items(entries.take(20), key = { "history_${it.medicineId}_${it.recordedAt}" }) { entry ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.medicineName, fontWeight = FontWeight.Bold)
                        Text(
                            "${entry.date.format(DateTimeFormatter.ofPattern("d MMM"))} · ${formatTime(entry.time)} · ${entry.dosage}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusBadge(entry.status)
                }
            }
        }
    }
}

@Composable
private fun FamilyScreen(
    state: TrackerUiState,
    padding: PaddingValues,
    onSave: (String, String, String) -> Unit,
    onCreateBackup: (String) -> Result<ByteArray>,
    onRestoreBackup: (ByteArray, String) -> Result<Unit>,
) {
    val context = LocalContext.current
    var editing by rememberSaveable(state.caregiver) { mutableStateOf(state.caregiver.parentName.isBlank()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 8.dp)) {
                Text("Family circle", style = MaterialTheme.typography.headlineMedium)
                Text("Know when a gentle check-in could help", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            BackupRestoreCard(
                onCreateBackup = onCreateBackup,
                onRestoreBackup = onRestoreBackup,
            )
        }
        item {
            if (editing) {
                CaregiverForm(
                    caregiver = state.caregiver,
                    onCancel = { editing = false },
                    onSave = { parent, name, phone -> onSave(parent, name, phone); editing = false },
                )
            } else {
                CaregiverCard(
                    caregiver = state.caregiver,
                    onEdit = { editing = true },
                    onCall = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${state.caregiver.phone}".toUri()))
                    },
                    onMessage = {
                        messageCaregiver(
                            context = context,
                            phone = state.caregiver.phone,
                            message = "Hi ${state.caregiver.name}, here's a medicine update for ${state.displayParentName} from MediKin.",
                        )
                    },
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("Needs attention", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                val count = state.missedDoses.size + state.refillMedicines.size
                Surface(shape = CircleShape, color = if (count > 0) Coral.copy(alpha = .14f) else Mint) {
                    Text(
                        count.toString(),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                        color = if (count > 0) Coral else Evergreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (state.missedDoses.isEmpty() && state.refillMedicines.isEmpty()) {
            item { EmptyState("All calm", "There are no missed doses or refill alerts right now.") }
        }
        items(state.missedDoses, key = { "missed_${it.medicine.id}_${it.time.key}" }) { dose ->
            AttentionCard(
                icon = Icons.Default.WarningAmber,
                title = "${dose.medicine.name} may be missed",
                detail = "Due at ${dose.scheduledAt.format(DateTimeFormatter.ofPattern("h:mm a"))} · Check in with ${state.displayParentName}",
                color = Coral,
                action = if (state.caregiver.phone.isNotBlank()) "Message ${state.caregiver.name}" else null,
                onAction = {
                    messageCaregiver(
                        context,
                        state.caregiver.phone,
                        "${dose.medicine.name} (${dose.medicine.dosage}) may have been missed at ${dose.scheduledAt.format(DateTimeFormatter.ofPattern("h:mm a"))}. Could you check in with ${state.displayParentName}?",
                    )
                },
            )
        }
        items(state.refillMedicines, key = { "refill_${it.id}" }) { medicine ->
            AttentionCard(
                icon = Icons.Default.Inventory2,
                title = "Refill ${medicine.name}",
                detail = "Only ${medicine.stock} doses remain · alert level ${medicine.refillAt}",
                color = Marigold,
            )
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "MediKin sends a dose reminder at the scheduled time and raises a family check-in after 5 minutes if it is not recorded.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaregiverCard(
    caregiver: Caregiver,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Evergreen)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FamilyRestroom, null, tint = Color.White)
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        caregiver.name.ifBlank { "No family contact yet" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        caregiver.phone.ifBlank { "No phone added" },
                        color = Color.White.copy(alpha = .78f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit family contact", tint = Color.White) }
            }
            if (caregiver.phone.isNotBlank()) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp)); Text("Call", color = Color.White)
                    }
                    OutlinedButton(onClick = onMessage, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.Message, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp)); Text("Message", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaregiverForm(
    caregiver: Caregiver,
    onCancel: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var parentName by rememberSaveable { mutableStateOf(caregiver.parentName) }
    var name by rememberSaveable { mutableStateOf(caregiver.name) }
    var phone by rememberSaveable { mutableStateOf(caregiver.phone) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Who is in your circle?", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = parentName,
                onValueChange = { parentName = it },
                label = { Text("Parent's name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("parent_name"),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Family contact (optional)") },
                placeholder = { Text("e.g. Rahul") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("caregiver_name"),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (caregiver.name.isNotBlank()) TextButton(onClick = onCancel) { Text("Cancel") }
                Button(
                    onClick = { onSave(parentName, name, phone) },
                    enabled = parentName.isNotBlank(),
                    modifier = Modifier.testTag("save_caregiver"),
                ) { Text("Save profile") }
            }
        }
    }
}

private enum class BackupAction { EXPORT, IMPORT }

@Composable
private fun BackupRestoreCard(
    onCreateBackup: (String) -> Result<ByteArray>,
    onRestoreBackup: (ByteArray, String) -> Result<Unit>,
) {
    val context = LocalContext.current
    var requestedAction by remember { mutableStateOf<BackupAction?>(null) }
    var password by rememberSaveable { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingImportPassword by remember { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = checkNotNull(pendingExport)
                checkNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(bytes) }
            }.onSuccess {
                message = "Encrypted backup saved"
            }.onFailure {
                message = it.message ?: "Could not save backup"
            }
        }
        pendingExport = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                    val bytes = input.readBytes()
                    require(bytes.size <= 10 * 1024 * 1024) { "Backup file is too large" }
                    bytes
                }
            }.fold(
                onSuccess = { bytes ->
                    onRestoreBackup(bytes, pendingImportPassword)
                        .onSuccess { message = "Backup restored" }
                        .onFailure { message = "Restore failed: check the file and password" }
                },
                onFailure = { message = it.message ?: "Could not read backup" },
            )
        }
        pendingImportPassword = ""
    }

    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Encrypted backup", style = MaterialTheme.typography.titleMedium)
                    Text("Move your local data without creating an account", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { password = ""; requestedAction = BackupAction.EXPORT },
                    modifier = Modifier.weight(1f).testTag("export_backup"),
                ) { Text("Export") }
                OutlinedButton(
                    onClick = { password = ""; requestedAction = BackupAction.IMPORT },
                    modifier = Modifier.weight(1f).testTag("import_backup"),
                ) { Text("Restore") }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
        }
    }

    requestedAction?.let { action ->
        AlertDialog(
            onDismissRequest = { requestedAction = null; password = "" },
            title = { Text(if (action == BackupAction.EXPORT) "Protect your backup" else "Restore backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (action == BackupAction.EXPORT) {
                            "Choose a password with at least 8 characters. It cannot be recovered if forgotten."
                        } else {
                            "Enter the password used when this MediKin backup was created. Restoring replaces current data."
                        },
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Backup password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("backup_password"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = password.length >= 8,
                    onClick = {
                        if (action == BackupAction.EXPORT) {
                            onCreateBackup(password)
                                .onSuccess {
                                    pendingExport = it
                                    exportLauncher.launch("medikin-${LocalDate.now()}.mkin")
                                }
                                .onFailure { message = it.message ?: "Could not create backup" }
                        } else {
                            pendingImportPassword = password
                            importLauncher.launch(arrayOf("application/octet-stream", "application/*"))
                        }
                        password = ""
                        requestedAction = null
                    },
                ) { Text(if (action == BackupAction.EXPORT) "Choose location" else "Choose backup") }
            },
            dismissButton = { TextButton(onClick = { requestedAction = null; password = "" }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AttentionCard(
    icon: ImageVector,
    title: String,
    detail: String,
    color: Color,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(icon, null, tint = color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (action != null) {
                TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.AutoMirrored.Filled.Message, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp)); Text(action)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicineEditorSheet(
    medicine: Medicine?,
    onDismiss: () -> Unit,
    onSave: (MedicineEditValues) -> Unit,
) {
    val initialDoses = medicine?.effectiveScheduledDoses.orEmpty().ifEmpty {
        listOf(ScheduledDose(DoseTime.MORNING, medicine?.dosage, 1))
    }
    var name by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.name.orEmpty()) }
    var dosage by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.dosage.orEmpty()) }
    var instructions by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.instructions.orEmpty()) }
    var stock by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.stock?.toString() ?: "30") }
    var refillAt by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.refillAt?.toString() ?: "5") }
    var stockUnit by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.effectiveStockUnit ?: "tablets") }
    var scheduleType by remember(medicine?.id) { mutableStateOf(medicine?.effectiveScheduleType ?: ScheduleType.DAILY) }
    var weekdays by remember(medicine?.id) { mutableStateOf(medicine?.effectiveWeekdays ?: Medicine.ALL_WEEKDAYS) }
    var intervalHours by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.intervalHours?.toString() ?: "8") }
    var startDate by rememberSaveable(medicine?.id) {
        mutableStateOf(medicine?.startDate ?: medicine?.createdDate ?: LocalDate.now().toString())
    }
    var endDate by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.endDate.orEmpty()) }
    var selectedTimes by remember(medicine?.id) { mutableStateOf<Set<DoseTime>>(initialDoses.mapTo(mutableSetOf()) { it.time }) }
    var doseByTime by remember(medicine?.id) {
        mutableStateOf(initialDoses.associate { it.time.key to it.dosage.orEmpty() })
    }
    var stockUseByTime by remember(medicine?.id) {
        mutableStateOf(initialDoses.associate { it.time.key to it.stockUse.toString() })
    }
    val context = LocalContext.current
    val parsedStart = runCatching { LocalDate.parse(startDate) }.getOrNull()
    val parsedEnd = endDate.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val scheduleIsValid = when (scheduleType) {
        ScheduleType.DAILY -> selectedTimes.isNotEmpty() && weekdays.isNotEmpty()
        ScheduleType.INTERVAL -> selectedTimes.isNotEmpty() && intervalHours.toIntOrNull() in 1..168
        ScheduleType.AS_NEEDED -> true
    }
    val valid = name.isNotBlank() && dosage.isNotBlank() && scheduleIsValid &&
        stock.toIntOrNull() != null && refillAt.toIntOrNull() != null && stockUnit.isNotBlank() &&
        parsedStart != null && (endDate.isBlank() || parsedEnd != null) &&
        (parsedEnd == null || !parsedEnd.isBefore(parsedStart))

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().imePadding().testTag("add_medicine_form"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(if (medicine == null) "Add a medicine" else "Edit ${medicine.name}", style = MaterialTheme.typography.headlineMedium)
                Text("Set the plan exactly as prescribed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine name") },
                    placeholder = { Text("e.g. Amlodipine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("medicine_name"),
                )
            }
            item {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") },
                    placeholder = { Text("e.g. 5 mg or 1 tablet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("medicine_dosage"),
                )
            }
            item {
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions (optional)") },
                    placeholder = { Text("e.g. After breakfast") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text("Schedule", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ScheduleType.entries.forEach { type ->
                        FilterChip(
                            selected = scheduleType == type,
                            onClick = { scheduleType = type },
                            label = {
                                Text(
                                    when (type) {
                                        ScheduleType.DAILY -> "Times & weekdays"
                                        ScheduleType.INTERVAL -> "Every X hours"
                                        ScheduleType.AS_NEEDED -> "As needed"
                                    },
                                )
                            },
                        )
                    }
                }
            }
            if (scheduleType == ScheduleType.DAILY) {
                item {
                    Text("Specific weekdays", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day.value in weekdays,
                                onClick = {
                                    weekdays = if (day.value in weekdays) weekdays - day.value else weekdays + day.value
                                },
                                label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                            )
                        }
                    }
                }
            }
            if (scheduleType == ScheduleType.INTERVAL) {
                item {
                    OutlinedTextField(
                        value = intervalHours,
                        onValueChange = { intervalHours = it.filter(Char::isDigit).take(3) },
                        label = { Text("Repeat every (hours)") },
                        supportingText = { Text("Between 1 and 168 hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (scheduleType != ScheduleType.AS_NEEDED) item {
                Text("Reminder times", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DoseTime.presets.forEach { time ->
                        AssistChip(
                            onClick = {
                                selectedTimes = if (scheduleType == ScheduleType.INTERVAL) setOf(time) else selectedTimes + time
                            },
                            label = { Text("${time.label} · ${formatTime(time)}") },
                            leadingIcon = {
                                if (time in selectedTimes) Icon(Icons.Default.Check, null, Modifier.size(17.dp))
                                else Icon(Icons.Default.Schedule, null, Modifier.size(17.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (time in selectedTimes) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            ),
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        val now = LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val selected = DoseTime(hour, minute)
                                selectedTimes = if (scheduleType == ScheduleType.INTERVAL) setOf(selected) else selectedTimes + selected
                            },
                            now.hour,
                            now.minute,
                            android.text.format.DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    modifier = Modifier.padding(top = 8.dp).testTag("choose_custom_time"),
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Choose custom time")
                }
                Text(
                    "Selected times",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedTimes.sortedWith(compareBy(DoseTime::hour, DoseTime::minute)).forEach { time ->
                        InputChip(
                            selected = true,
                            onClick = { selectedTimes = selectedTimes - time },
                            label = { Text(formatTime(time)) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, "Remove ${formatTime(time)}", Modifier.size(17.dp))
                            },
                        )
                    }
                }
            }
            if (scheduleType != ScheduleType.AS_NEEDED) {
                items(
                    selectedTimes.sortedWith(compareBy(DoseTime::hour, DoseTime::minute)),
                    key = { "dose_detail_${it.key}" },
                ) { time ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = doseByTime[time.key].orEmpty(),
                            onValueChange = { doseByTime = doseByTime + (time.key to it) },
                            label = { Text("Dose at ${formatTime(time)}") },
                            placeholder = { Text(dosage.ifBlank { "Same as above" }) },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                        )
                        OutlinedTextField(
                            value = stockUseByTime[time.key] ?: "1",
                            onValueChange = { stockUseByTime = stockUseByTime + (time.key to it.filter(Char::isDigit).take(2)) },
                            label = { Text("Uses") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it.take(10) },
                        label = { Text("Start date") },
                        supportingText = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it.take(10) },
                        label = { Text("End date (optional)") },
                        supportingText = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter(Char::isDigit).take(3) },
                        label = { Text("Stock in hand") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = refillAt,
                        onValueChange = { refillAt = it.filter(Char::isDigit).take(3) },
                        label = { Text("Refill at") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = stockUnit,
                    onValueChange = { stockUnit = it.take(24) },
                    label = { Text("Stock unit") },
                    placeholder = { Text("tablets, capsules, mL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)) {
                    Text(
                        "Follow your clinician's instructions. MediKin helps organize medicines but does not provide medical advice.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        val timesForSave = when (scheduleType) {
                            ScheduleType.DAILY -> selectedTimes
                            ScheduleType.INTERVAL -> setOf(selectedTimes.firstOrNull() ?: DoseTime.MORNING)
                            ScheduleType.AS_NEEDED -> setOf(selectedTimes.firstOrNull() ?: DoseTime.MORNING)
                        }
                        onSave(
                            MedicineEditValues(
                                name = name,
                                dosage = dosage,
                                instructions = instructions,
                                scheduleType = scheduleType,
                                weekdays = weekdays,
                                intervalHours = intervalHours.toIntOrNull(),
                                startDate = checkNotNull(parsedStart),
                                endDate = parsedEnd,
                                scheduledDoses = timesForSave.map { time ->
                                    ScheduledDose(
                                        time = time,
                                        dosage = doseByTime[time.key]?.trim()?.takeIf(String::isNotEmpty) ?: dosage.trim(),
                                        stockUse = (stockUseByTime[time.key]?.toIntOrNull() ?: 1).coerceAtLeast(1),
                                    )
                                },
                                stock = stock.toInt(),
                                refillAt = refillAt.toInt(),
                                stockUnit = stockUnit,
                            ),
                        )
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("save_medicine"),
                ) {
                    Icon(if (medicine == null) Icons.Default.Add else Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (medicine == null) "Save medicine" else "Save changes")
                }
            }
        }
    }
}

private data class MedicineEditValues(
    val name: String,
    val dosage: String,
    val instructions: String,
    val scheduleType: ScheduleType,
    val weekdays: Set<Int>,
    val intervalHours: Int?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val scheduledDoses: List<ScheduledDose>,
    val stock: Int,
    val refillAt: Int,
    val stockUnit: String,
)

@Composable
private fun EmptyState(title: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = Evergreen)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun statusColor(status: DoseStatus): Color = when (status) {
    DoseStatus.TAKEN -> Evergreen
    DoseStatus.DUE -> Marigold
    DoseStatus.MISSED -> Coral
    else -> Color(0xFF64716C)
}

private fun medicineColor(index: Int): Color = listOf(
    Evergreen,
    Color(0xFF4B6FB5),
    Color(0xFF8C5BA7),
    Color(0xFFC56D35),
)[index.mod(4)]

private fun scheduleSummary(medicine: Medicine): String = when (medicine.effectiveScheduleType) {
    ScheduleType.AS_NEEDED -> "As needed"
    ScheduleType.INTERVAL -> "Every ${medicine.intervalHours ?: "?"} hours"
    ScheduleType.DAILY -> {
        val days = medicine.effectiveWeekdays
        val dayLabel = if (days == Medicine.ALL_WEEKDAYS) {
            "Daily"
        } else {
            DayOfWeek.entries.filter { it.value in days }.joinToString("/") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
        }
        "$dayLabel · ${medicine.effectiveScheduledDoses.joinToString { formatTime(it.time) }}"
    }
}

private fun estimatedRunOutDays(medicine: Medicine): Int? {
    if (medicine.effectiveScheduleType == ScheduleType.AS_NEEDED) return null
    var remaining = medicine.stock
    if (remaining <= 0) return 0
    val today = LocalDate.now()
    for (offset in 0..365) {
        remaining -= medicine.scheduledDosesOn(today.plusDays(offset.toLong())).sumOf(ScheduledDose::stockUse)
        if (remaining <= 0) return offset + 1
    }
    return null
}

private fun formatTime(time: DoseTime): String =
    time.asLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))

private fun messageCaregiver(context: android.content.Context, phone: String, message: String) {
    if (phone.isBlank()) return
    val intent = Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(phone)}".toUri()).apply {
        putExtra("sms_body", message)
    }
    runCatching { context.startActivity(intent) }
}
