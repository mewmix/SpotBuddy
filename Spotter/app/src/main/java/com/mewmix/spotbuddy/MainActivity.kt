package com.mewmix.spotbuddy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mewmix.spotbuddy.ui.theme.SpotBuddyTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SpotBuddyTheme {
                SpotBuddyApp()
            }
        }
    }
}

private enum class ExerciseMode { Reps, Timer }

private data class ExerciseTemplate(
    val name: String,
    val shortName: String,
    val mode: ExerciseMode,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultHoldSeconds: Int,
    val accent: Color
)

private data class WorkoutItem(
    val template: ExerciseTemplate,
    val selected: Boolean = true,
    val sets: Int = template.defaultSets,
    val reps: Int = template.defaultReps,
    val holdSeconds: Int = template.defaultHoldSeconds,
    val completed: Int = 0
)

private enum class SessionPhase { Setup, Active, Rest, History, ManualEntry, Complete }

private val WorkoutCatalog = listOf(
    ExerciseTemplate("Pushups", "PUSH", ExerciseMode.Reps, 4, 15, 0, Color(0xFFE86F51)),
    ExerciseTemplate("Crunches", "CORE", ExerciseMode.Reps, 4, 20, 0, Color(0xFF4E6FAE)),
    ExerciseTemplate("Situps", "SIT", ExerciseMode.Reps, 3, 15, 0, Color(0xFF8257A6)),
    ExerciseTemplate("Squats", "LEGS", ExerciseMode.Reps, 4, 20, 0, Color(0xFF3E8D72)),
    ExerciseTemplate("Planks", "HOLD", ExerciseMode.Timer, 3, 0, 45, Color(0xFFD59A21)),
    ExerciseTemplate("Lunges", "MOVE", ExerciseMode.Reps, 3, 12, 0, Color(0xFF2E9DA7))
)

private val CustomAccents = listOf(
    Color(0xFFE86F51),
    Color(0xFF4E6FAE),
    Color(0xFF8257A6),
    Color(0xFF3E8D72),
    Color(0xFFD59A21),
    Color(0xFF2E9DA7)
)

private val MotivationMessages = listOf(
    "DONT GIVE UP",
    "YOU CAN DO IT",
    "HOLD STRONG",
    "BREATHE AND LOCK IN",
    "STAY WITH IT"
)

private data class AnalyticsSummary(
    val totalSessions: Int,
    val totalSets: Int,
    val totalDurationSeconds: Int,
    val totalCooldownSeconds: Int,
    val totalSkippedCooldownSeconds: Int,
    val endedEarlyCount: Int,
    val manualSessionCount: Int,
    val todaySets: Int,
    val weekSets: Int,
    val monthSets: Int,
    val currentStreakDays: Int,
    val lastWorkoutDate: String,
    val lifetime: PeriodAnalytics,
    val weekly: PeriodAnalytics,
    val monthly: PeriodAnalytics,
    val yearly: PeriodAnalytics,
    val exerciseTotals: List<ExerciseTotal>,
    val dayTotals: List<DayTotal>
)

private data class ExerciseTotal(
    val name: String,
    val sets: Int,
    val plannedSets: Int
)

private data class DayTotal(
    val dayStart: Long,
    val label: String,
    val sets: Int,
    val sessions: Int
)

private data class ManualExerciseCount(
    val template: ExerciseTemplate,
    val sets: Int = 0
)

private data class PeriodAnalytics(
    val label: String,
    val sessions: Int,
    val sets: Int,
    val durationSeconds: Int,
    val cooldownSeconds: Int,
    val skippedCooldownSeconds: Int
)

@Composable
private fun SpotBuddyApp() {
    val context = LocalContext.current
    val database = remember { SpotBuddyDatabase(context.applicationContext) }
    val preferences = remember { SpotBuddyPreferences(context.applicationContext) }
    val savedState = remember { preferences.load() }
    var sessions by remember { mutableStateOf(database.getSessions()) }
    val items = remember {
        mutableStateListOf<WorkoutItem>().apply {
            val savedByName = savedState?.items.orEmpty().associateBy { it.name }
            val orderedTemplates = buildList<ExerciseTemplate> {
                savedState?.items.orEmpty().forEachIndexed { index, saved ->
                    val template = WorkoutCatalog.firstOrNull { it.name == saved.name }
                        ?: customTemplate(saved.name, saved.mode, index)
                    if (none { it.name == template.name }) add(template)
                }
                WorkoutCatalog.filterNot { template -> any { it.name == template.name } }.forEach(::add)
            }
            addAll(
                orderedTemplates.map { template ->
                    val saved = savedByName[template.name]
                    WorkoutItem(
                        template = template,
                        selected = saved?.selected ?: true,
                        sets = saved?.sets ?: template.defaultSets,
                        reps = saved?.reps ?: template.defaultReps,
                        holdSeconds = saved?.holdSeconds ?: template.defaultHoldSeconds,
                        completed = saved?.completed ?: 0
                    )
                }
            )
        }
    }

    val loadedPhase = savedState?.phase?.let { phaseName ->
        SessionPhase.entries.firstOrNull { it.name == phaseName }
    }?.takeIf { it != SessionPhase.ManualEntry } ?: SessionPhase.Setup
    var phase by remember { mutableStateOf(loadedPhase) }
    var currentIndex by remember { mutableIntStateOf(savedState?.currentIndex ?: 0) }
    var restSeconds by remember { mutableIntStateOf(savedState?.restSeconds ?: 45) }
    var remainingSeconds by remember { mutableIntStateOf(savedState?.remainingSeconds ?: 45) }
    var activeTimerSeconds by remember { mutableIntStateOf(savedState?.activeTimerSeconds ?: 0) }
    var sessionStartedAt by remember { mutableLongStateOf(savedState?.sessionStartedAt ?: 0L) }
    var actualCooldownSeconds by remember { mutableIntStateOf(savedState?.actualCooldownSeconds ?: 0) }
    var skippedCooldowns by remember { mutableIntStateOf(savedState?.skippedCooldowns ?: 0) }
    var skippedCooldownSeconds by remember { mutableIntStateOf(savedState?.skippedCooldownSeconds ?: 0) }
    var sessionEndedEarly by remember { mutableStateOf(savedState?.sessionEndedEarly ?: false) }
    var savedSessionId by remember { mutableLongStateOf(savedState?.savedSessionId ?: 0L) }
    val manualItems = remember {
        mutableStateListOf<ManualExerciseCount>().apply {
            addAll(WorkoutCatalog.map { ManualExerciseCount(it) })
        }
    }
    var manualDayOffset by remember { mutableIntStateOf(0) }
    var manualSkipCooldown by remember { mutableStateOf(false) }
    var manualDurationMinutes by remember { mutableIntStateOf(0) }
    var manualCalendarMonthOffset by remember { mutableIntStateOf(0) }
    var customWorkoutName by remember { mutableStateOf("") }

    fun currentSavedAppState(targetPhase: SessionPhase = phase): SavedAppState {
        return SavedAppState(
            items = items.map {
                SavedWorkoutItem(
                    name = it.template.name,
                    mode = it.template.mode.name,
                    selected = it.selected,
                    sets = it.sets,
                    reps = it.reps,
                    holdSeconds = it.holdSeconds,
                    completed = it.completed
                )
            },
            phase = targetPhase.name,
            currentIndex = currentIndex,
            restSeconds = restSeconds,
            remainingSeconds = remainingSeconds,
            activeTimerSeconds = activeTimerSeconds,
            sessionStartedAt = sessionStartedAt,
            actualCooldownSeconds = actualCooldownSeconds,
            skippedCooldowns = skippedCooldowns,
            skippedCooldownSeconds = skippedCooldownSeconds,
            sessionEndedEarly = sessionEndedEarly,
            savedSessionId = savedSessionId
        )
    }

    fun sanitizeImportedState(state: SavedAppState): SavedAppState {
        return state.copy(
            items = state.items.map { it.copy(completed = 0) },
            phase = SessionPhase.Setup.name,
            currentIndex = 0,
            remainingSeconds = state.restSeconds,
            activeTimerSeconds = 0,
            sessionStartedAt = 0L,
            actualCooldownSeconds = 0,
            skippedCooldowns = 0,
            skippedCooldownSeconds = 0,
            sessionEndedEarly = false,
            savedSessionId = 0L
        )
    }

    fun applySavedAppState(state: SavedAppState) {
        val savedByName = state.items.associateBy { it.name }
        val orderedTemplates = buildList<ExerciseTemplate> {
            state.items.forEachIndexed { index, saved ->
                val template = WorkoutCatalog.firstOrNull { it.name == saved.name }
                    ?: customTemplate(saved.name, saved.mode, index)
                if (none { it.name == template.name }) add(template)
            }
            WorkoutCatalog.filterNot { template -> any { it.name == template.name } }.forEach(::add)
        }
        items.clear()
        items.addAll(
            orderedTemplates.map { template ->
                val saved = savedByName[template.name]
                WorkoutItem(
                    template = template,
                    selected = saved?.selected ?: true,
                    sets = saved?.sets ?: template.defaultSets,
                    reps = saved?.reps ?: template.defaultReps,
                    holdSeconds = saved?.holdSeconds ?: template.defaultHoldSeconds,
                    completed = saved?.completed ?: 0
                )
            }
        )
        manualItems.clear()
        manualItems.addAll(items.map { ManualExerciseCount(it.template) })
        currentIndex = state.currentIndex
        restSeconds = state.restSeconds
        remainingSeconds = state.remainingSeconds
        activeTimerSeconds = state.activeTimerSeconds
        sessionStartedAt = state.sessionStartedAt
        actualCooldownSeconds = state.actualCooldownSeconds
        skippedCooldowns = state.skippedCooldowns
        skippedCooldownSeconds = state.skippedCooldownSeconds
        sessionEndedEarly = state.sessionEndedEarly
        savedSessionId = state.savedSessionId
        phase = SessionPhase.entries.firstOrNull { it.name == state.phase }
            ?.takeIf { it != SessionPhase.ManualEntry } ?: SessionPhase.Setup
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val backup = SpotBuddyBackup.encode(
            sessions = database.getSessions(),
            savedState = currentSavedAppState(SessionPhase.Setup)
        )
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(backup.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open backup file")
        }.onSuccess {
            Toast.makeText(context, "SpotBuddy backup exported", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Backup export failed", Toast.LENGTH_LONG).show()
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val csv = SpotBuddyCsv.encodeSessions(database.getSessions())
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open CSV file")
        }.onSuccess {
            Toast.makeText(context, "SpotBuddy CSV exported", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "CSV export failed", Toast.LENGTH_LONG).show()
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to open backup file")
            SpotBuddyBackup.decode(raw)
        }.onSuccess { backup ->
            val importedCount = database.importSessions(backup.sessions)
            backup.savedState?.let { importedState ->
                val sanitized = sanitizeImportedState(importedState)
                preferences.save(sanitized)
                applySavedAppState(sanitized)
            }
            sessions = database.getSessions()
            Toast.makeText(context, "Imported $importedCount new sessions", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(context, "Backup import failed", Toast.LENGTH_LONG).show()
        }
    }

    val activeItems = items.filter { it.selected && it.sets > 0 }
    val totalSets = activeItems.sumOf { it.sets }
    val completedSets = activeItems.sumOf { it.completed }
    val currentItem = activeItems.getOrNull(currentIndex.coerceIn(0, max(activeItems.lastIndex, 0)))

    fun persistAppState(targetPhase: SessionPhase = phase) {
        preferences.save(currentSavedAppState(targetPhase))
    }

    LaunchedEffect(
        items.toList(),
        phase,
        currentIndex,
        restSeconds,
        remainingSeconds,
        activeTimerSeconds,
        sessionStartedAt,
        actualCooldownSeconds,
        skippedCooldowns,
        skippedCooldownSeconds,
        sessionEndedEarly,
        savedSessionId
    ) {
        persistAppState()
    }

    fun updateItem(template: ExerciseTemplate, transform: (WorkoutItem) -> WorkoutItem) {
        val index = items.indexOfFirst { it.template == template }
        if (index >= 0) items[index] = transform(items[index])
    }

    fun addCustomWorkout(name: String) {
        val cleanName = normalizeWorkoutName(name)
        if (cleanName.isBlank()) return
        if (items.any { it.template.name.equals(cleanName, ignoreCase = true) }) return
        val template = customTemplate(cleanName, ExerciseMode.Reps.name, items.size)
        items.add(WorkoutItem(template = template, selected = true, sets = 3, reps = 0, holdSeconds = 0))
        manualItems.add(ManualExerciseCount(template))
        customWorkoutName = ""
    }

    fun moveItem(fromIndex: Int, direction: Int) {
        val toIndex = (fromIndex + direction).coerceIn(items.indices)
        if (fromIndex == toIndex) return
        val moving = items.removeAt(fromIndex)
        items.add(toIndex, moving)
    }

    fun nextOpenIndex(from: Int = currentIndex): Int? {
        if (activeItems.isEmpty()) return null
        val start = from.coerceIn(0, activeItems.lastIndex)
        for (offset in activeItems.indices) {
            val index = (start + offset) % activeItems.size
            if (activeItems[index].completed < activeItems[index].sets) return index
        }
        return null
    }

    fun startSession() {
        items.indices.forEach { index -> items[index] = items[index].copy(completed = 0) }
        currentIndex = 0
        actualCooldownSeconds = 0
        skippedCooldowns = 0
        skippedCooldownSeconds = 0
        sessionEndedEarly = false
        savedSessionId = 0L
        sessionStartedAt = System.currentTimeMillis()
        activeTimerSeconds = activeItems.firstOrNull()
            ?.takeIf { it.template.mode == ExerciseMode.Timer }
            ?.holdSeconds ?: 0
        phase = SessionPhase.Active
    }

    fun saveFinishedSession() {
        if (savedSessionId != 0L || sessionStartedAt == 0L || totalSets == 0) return
        val endedAt = System.currentTimeMillis()
        val record = SessionRecord(
            id = 0L,
            startedAt = sessionStartedAt,
            endedAt = endedAt,
            durationSeconds = ((endedAt - sessionStartedAt) / 1000L).toInt().coerceAtLeast(0),
            completedSets = completedSets,
            plannedSets = totalSets,
            actualCooldownSeconds = actualCooldownSeconds,
            skippedCooldowns = skippedCooldowns,
            skippedCooldownSeconds = skippedCooldownSeconds,
            endedEarly = sessionEndedEarly,
            exercises = activeItems.map {
                ExerciseSummary(
                    name = it.template.name,
                    completedSets = it.completed,
                    plannedSets = it.sets,
                    reps = it.reps,
                    holdSeconds = it.holdSeconds
                )
            }
        )
        savedSessionId = database.insertSession(record)
        sessions = database.getSessions()
        sessionStartedAt = 0L
        activeTimerSeconds = 0
        persistAppState(SessionPhase.Complete)
    }

    fun finishEarly(fromCooldown: Boolean) {
        if (fromCooldown) {
            val taken = (restSeconds - remainingSeconds).coerceIn(0, restSeconds)
            actualCooldownSeconds += taken
            skippedCooldowns += 1
            skippedCooldownSeconds += remainingSeconds
        }
        sessionEndedEarly = true
        activeTimerSeconds = 0
        phase = SessionPhase.Complete
    }

    fun completeCurrentSet(startRest: Boolean) {
        val item = currentItem ?: return
        updateItem(item.template) { it.copy(completed = (it.completed + 1).coerceAtMost(it.sets)) }

        val refreshed = activeItems.map {
            if (it.template == item.template) it.copy(completed = (it.completed + 1).coerceAtMost(it.sets)) else it
        }
        val allDone = refreshed.all { it.completed >= it.sets }
        if (allDone) {
            phase = SessionPhase.Complete
            return
        }

        val next = nextOpenIndex(currentIndex + 1)
        currentIndex = next ?: 0
        if (startRest && restSeconds > 0) {
            remainingSeconds = restSeconds
            activeTimerSeconds = 0
            phase = SessionPhase.Rest
        } else {
            skippedCooldowns += 1
            skippedCooldownSeconds += restSeconds
            val nextItem = activeItems.getOrNull(next ?: 0)
            activeTimerSeconds = nextItem
                ?.takeIf { it.template.mode == ExerciseMode.Timer }
                ?.holdSeconds ?: 0
            phase = SessionPhase.Active
        }
    }

    fun finishCooldown() {
        actualCooldownSeconds += restSeconds
        val nextItem = activeItems.getOrNull(currentIndex)
        activeTimerSeconds = nextItem
            ?.takeIf { it.template.mode == ExerciseMode.Timer }
            ?.holdSeconds ?: 0
        phase = SessionPhase.Active
    }

    fun skipCooldown() {
        val taken = (restSeconds - remainingSeconds).coerceIn(0, restSeconds)
        actualCooldownSeconds += taken
        skippedCooldowns += 1
        skippedCooldownSeconds += remainingSeconds
        val nextItem = activeItems.getOrNull(currentIndex)
        activeTimerSeconds = nextItem
            ?.takeIf { it.template.mode == ExerciseMode.Timer }
            ?.holdSeconds ?: 0
        phase = SessionPhase.Active
    }

    fun addManualSession() {
        val selected = manualItems.filter { it.sets > 0 }
        if (selected.isEmpty()) return
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, manualDayOffset)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startedAt = calendar.timeInMillis
        val cooldown = if (manualSkipCooldown) 0 else restSeconds
        val skipped = if (manualSkipCooldown) restSeconds else 0
        database.insertSession(
            SessionRecord(
                id = 0L,
                startedAt = startedAt,
                endedAt = startedAt + manualDurationSeconds(manualDurationMinutes) * 1_000L,
                durationSeconds = manualDurationSeconds(manualDurationMinutes),
                completedSets = selected.sumOf { it.sets },
                plannedSets = selected.sumOf { it.sets },
                actualCooldownSeconds = cooldown,
                skippedCooldowns = if (manualSkipCooldown) 1 else 0,
                skippedCooldownSeconds = skipped,
                endedEarly = false,
                exercises = selected.map {
                    ExerciseSummary(
                        name = it.template.name,
                        completedSets = it.sets,
                        plannedSets = it.sets,
                        reps = it.template.defaultReps,
                        holdSeconds = it.template.defaultHoldSeconds
                    )
                }
            )
        )
        sessions = database.getSessions()
        manualItems.indices.forEach { index -> manualItems[index] = manualItems[index].copy(sets = 0) }
        manualDayOffset = 0
        manualSkipCooldown = false
        manualDurationMinutes = 0
        phase = SessionPhase.History
    }

    LaunchedEffect(phase, completedSets, totalSets) {
        if (phase == SessionPhase.Complete) saveFinishedSession()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "phase"
        ) { target ->
            when (target) {
                SessionPhase.Setup -> SetupScreen(
                    items = items,
                    sessions = sessions,
                    restSeconds = restSeconds,
                    customWorkoutName = customWorkoutName,
                    onCustomWorkoutNameChanged = { customWorkoutName = it },
                    onRestChanged = { restSeconds = it.coerceIn(0, 180) },
                    onItemChanged = ::updateItem,
                    onMoveItem = ::moveItem,
                    onAddCustomWorkout = ::addCustomWorkout,
                    onStart = ::startSession,
                    onHistory = { phase = SessionPhase.History }
                )

                SessionPhase.Active -> ActiveScreen(
                    item = currentItem,
                    completedSets = completedSets,
                    totalSets = totalSets,
                    timerSeconds = activeTimerSeconds,
                    onTimerSecondsChanged = { activeTimerSeconds = it },
                    onCompleteSet = { completeCurrentSet(startRest = true) },
                    onSkipRest = { completeCurrentSet(startRest = false) },
                    onEnd = { finishEarly(fromCooldown = false) }
                )

                SessionPhase.Rest -> RestScreen(
                    nextItem = currentItem,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = restSeconds,
                    completedSets = completedSets,
                    totalSets = totalSets,
                    onTick = { remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0) },
                    onDone = ::finishCooldown,
                    onSkip = ::skipCooldown,
                    onEnd = { finishEarly(fromCooldown = true) }
                )

                SessionPhase.History -> HistoryScreen(
                    sessions = sessions,
                    onBack = { phase = SessionPhase.Setup },
                    onAddHistorical = { phase = SessionPhase.ManualEntry },
                    onExportBackup = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                        exportBackupLauncher.launch("spotbuddy-backup-$stamp.json")
                    },
                    onExportCsv = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                        exportCsvLauncher.launch("spotbuddy-history-$stamp.csv")
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/*"))
                    },
                    onDelete = { id ->
                        database.deleteSession(id)
                        sessions = database.getSessions()
                    }
                )

                SessionPhase.ManualEntry -> ManualEntryScreen(
                    items = manualItems,
                    dayOffset = manualDayOffset,
                    skipCooldown = manualSkipCooldown,
                    durationMinutes = manualDurationMinutes,
                    calendarMonthOffset = manualCalendarMonthOffset,
                    restSeconds = restSeconds,
                    onBack = { phase = SessionPhase.History },
                    onDayOffsetChanged = { manualDayOffset = clampManualDayOffset(it) },
                    onCalendarMonthChanged = { manualCalendarMonthOffset = it.coerceIn(-12, 0) },
                    onSkipCooldownChanged = { manualSkipCooldown = it },
                    onDurationChanged = { manualDurationMinutes = clampManualDurationMinutes(it) },
                    onSetsChanged = { template, delta ->
                        val index = manualItems.indexOfFirst { it.template == template }
                        if (index >= 0) {
                            manualItems[index] = manualItems[index].copy(
                                sets = (manualItems[index].sets + delta).coerceIn(0, 200)
                            )
                        }
                    },
                    onSave = ::addManualSession
                )

                SessionPhase.Complete -> CompleteScreen(
                    completedSets = completedSets,
                    totalSets = totalSets,
                    actualCooldownSeconds = actualCooldownSeconds,
                    skippedCooldowns = skippedCooldowns,
                    skippedCooldownSeconds = skippedCooldownSeconds,
                    endedEarly = sessionEndedEarly,
                    onAgain = ::startSession,
                    onEdit = { phase = SessionPhase.Setup },
                    onHistory = { phase = SessionPhase.History }
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    items: List<WorkoutItem>,
    sessions: List<SessionRecord>,
    restSeconds: Int,
    customWorkoutName: String,
    onCustomWorkoutNameChanged: (String) -> Unit,
    onRestChanged: (Int) -> Unit,
    onItemChanged: (ExerciseTemplate, (WorkoutItem) -> WorkoutItem) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onAddCustomWorkout: (String) -> Unit,
    onStart: () -> Unit,
    onHistory: () -> Unit
) {
    val selectedCount = items.count { it.selected }
    val totalSets = items.filter { it.selected }.sumOf { it.sets }

    AppScaffold {
        Header(
            title = "SpotBuddy",
            subtitle = "$selectedCount moves selected"
        )

        MetricBand(
            leftValue = totalSets.toString(),
            leftLabel = "sets today",
            rightValue = sessions.size.toString(),
            rightLabel = "saved sessions"
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.BarChart, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Review History", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Build your session",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Custom workout", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customWorkoutName,
                        onValueChange = onCustomWorkoutNameChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Name") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { onAddCustomWorkout(customWorkoutName) },
                        enabled = customWorkoutName.isNotBlank(),
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEachIndexed { index, item ->
                WorkoutSetupCard(
                    item = item,
                    canMoveUp = index > 0,
                    canMoveDown = index < items.lastIndex,
                    onToggle = {
                        onItemChanged(item.template) { it.copy(selected = !it.selected) }
                    },
                    onMoveUp = { onMoveItem(index, -1) },
                    onMoveDown = { onMoveItem(index, 1) },
                    onSets = { delta ->
                        onItemChanged(item.template) { it.copy(sets = (it.sets + delta).coerceIn(1, 12)) }
                    },
                    onAmount = { delta ->
                        onItemChanged(item.template) {
                            if (it.template.mode == ExerciseMode.Timer) {
                                it.copy(holdSeconds = (it.holdSeconds + delta * 5).coerceIn(10, 180))
                            } else {
                                it.copy(reps = (it.reps + delta).coerceIn(1, 100))
                            }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        ControlStrip(
            label = "Cooldown",
            value = "${restSeconds}s",
            onMinus = { onRestChanged(restSeconds - 15) },
            onPlus = { onRestChanged(restSeconds + 15) }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStart,
            enabled = selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Session", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun WorkoutSetupCard(
    item: WorkoutItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSets: (Int) -> Unit,
    onAmount: (Int) -> Unit
) {
    val color = item.template.accent
    val bg = if (item.selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val border = if (item.selected) color else MaterialTheme.colorScheme.outline

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.5.dp, border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (item.selected) 1f else 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.selected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    } else {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = color)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.template.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (item.template.mode == ExerciseMode.Timer) "${item.holdSeconds}s hold" else "Set counter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    item.template.shortName,
                    style = MaterialTheme.typography.labelLarge,
                    color = color
                )
            }

            AnimatedVisibility(item.selected) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Earlier", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Later", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    ControlStrip(
                        label = "Sets",
                        value = item.sets.toString(),
                        onMinus = { onSets(-1) },
                        onPlus = { onSets(1) }
                    )
                    if (item.template.mode == ExerciseMode.Timer) {
                        Spacer(Modifier.height(10.dp))
                        ControlStrip(
                            label = "Hold",
                            value = "${item.holdSeconds}s",
                            onMinus = { onAmount(-1) },
                            onPlus = { onAmount(1) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveScreen(
    item: WorkoutItem?,
    completedSets: Int,
    totalSets: Int,
    timerSeconds: Int,
    onTimerSecondsChanged: (Int) -> Unit,
    onCompleteSet: () -> Unit,
    onSkipRest: () -> Unit,
    onEnd: () -> Unit
) {
    if (item == null) {
        EmptySession(onEnd)
        return
    }

    var motivationIndex by remember(item.template.name, item.completed) {
        mutableIntStateOf(0)
    }
    var showMotivation by remember(item.template.name, item.completed) {
        mutableStateOf(false)
    }

    if (item.template.mode == ExerciseMode.Timer && !LocalInspectionMode.current) {
        LaunchedEffect(item.template.name, item.completed) {
            if (timerSeconds <= 0) onTimerSecondsChanged(item.holdSeconds)
        }
        LaunchedEffect(item.template.name, item.completed, timerSeconds) {
            when {
                timerSeconds > 1 -> {
                    delay(1000)
                    onTimerSecondsChanged(timerSeconds - 1)
                }
                timerSeconds == 1 -> {
                    delay(1000)
                    onCompleteSet()
                }
            }
        }
        LaunchedEffect(item.template.name, item.completed, timerSeconds) {
            val elapsed = item.holdSeconds - timerSeconds
            if (elapsed > 0 && elapsed % 10 == 0) {
                motivationIndex = (motivationIndex + 1) % MotivationMessages.size
                showMotivation = true
                delay(1600)
                showMotivation = false
            }
        }
    }

    val progress = if (item.template.mode == ExerciseMode.Timer) {
        1f - (timerSeconds.toFloat() / item.holdSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        completedSets.toFloat() / totalSets.toFloat().coerceAtLeast(1f)
    }

    Box(Modifier.fillMaxSize()) {
        AppScaffold {
            SessionTopBar(completedSets, totalSets, onEnd)
            Spacer(Modifier.height(24.dp))

            FocusPanel(accent = item.template.accent) {
                Text(
                    item.template.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Set ${item.completed + 1} of ${item.sets}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(28.dp))

                if (item.template.mode == ExerciseMode.Timer) {
                    TimerDial(
                        seconds = timerSeconds,
                        totalSeconds = item.holdSeconds,
                        accent = item.template.accent
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onCompleteSet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Finish Hold", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Text(
                        "SET",
                        style = MaterialTheme.typography.displayLarge,
                        color = item.template.accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "count reps your way",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = onCompleteSet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = item.template.accent)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Set Complete", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = item.template.accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(18.dp))
            OutlinedButton(
                onClick = onSkipRest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Complete and Skip Cooldown", style = MaterialTheme.typography.labelLarge)
            }
        }

        AnimatedVisibility(
            visible = showMotivation,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(220))
        ) {
            MotivationOverlay(MotivationMessages[motivationIndex], item.template.accent)
        }
    }
}

@Composable
private fun RestScreen(
    nextItem: WorkoutItem?,
    remainingSeconds: Int,
    totalSeconds: Int,
    completedSets: Int,
    totalSets: Int,
    onTick: () -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onEnd: () -> Unit
) {
    if (!LocalInspectionMode.current) {
        LaunchedEffect(remainingSeconds) {
            if (remainingSeconds > 0) {
                delay(1000)
                onTick()
            } else {
                onDone()
            }
        }
    }

    AppScaffold {
        SessionTopBar(completedSets, totalSets, onEnd)
        Spacer(Modifier.height(26.dp))

        FocusPanel(accent = MaterialTheme.colorScheme.secondary) {
            Text(
                "Cooldown",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Next: ${nextItem?.template?.name ?: "Done"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(30.dp))
            TimerDial(
                seconds = remainingSeconds,
                totalSeconds = totalSeconds.coerceAtLeast(1),
                accent = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Next Set", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ManualEntryScreen(
    items: List<ManualExerciseCount>,
    dayOffset: Int,
    skipCooldown: Boolean,
    durationMinutes: Int,
    calendarMonthOffset: Int,
    restSeconds: Int,
    onBack: () -> Unit,
    onDayOffsetChanged: (Int) -> Unit,
    onCalendarMonthChanged: (Int) -> Unit,
    onSkipCooldownChanged: (Boolean) -> Unit,
    onDurationChanged: (Int) -> Unit,
    onSetsChanged: (ExerciseTemplate, Int) -> Unit,
    onSave: () -> Unit
) {
    val totalSets = items.sumOf { it.sets }
    val dateLabel = manualDateLabel(dayOffset)

    AppScaffold {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Add History", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(dateLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ElevatedButton(onClick = onBack, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Back")
            }
        }

        MetricBand(
            leftValue = totalSets.toString(),
            leftLabel = "sets to add",
            rightValue = if (skipCooldown) "${restSeconds}s" else "0s",
            rightLabel = "cooldown skipped"
        )

        Spacer(Modifier.height(14.dp))

        CalendarDatePicker(
            selectedDayOffset = dayOffset,
            monthOffset = calendarMonthOffset,
            onMonthOffsetChanged = onCalendarMonthChanged,
            onDayOffsetChanged = onDayOffsetChanged
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onDayOffsetChanged(dayOffset - 1) },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Earlier", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = { onDayOffsetChanged(dayOffset + 1) },
                enabled = dayOffset < 0,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Later", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(12.dp))

        ControlStrip(
            label = "Duration",
            value = "${durationMinutes}m",
            onMinus = { onDurationChanged(durationMinutes - 5) },
            onPlus = { onDurationChanged(durationMinutes + 5) }
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSkipCooldownChanged(!skipCooldown) },
            shape = RoundedCornerShape(18.dp),
            color = if (skipCooldown) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (skipCooldown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (skipCooldown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (skipCooldown) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cooldown skipped", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Adds skipped cooldown seconds to analytics", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, item.template.accent.copy(alpha = 0.65f))
                ) {
                    ControlStrip(
                        label = item.template.name,
                        value = item.sets.toString(),
                        onMinus = { onSetsChanged(item.template, -1) },
                        onPlus = { onSetsChanged(item.template, 1) }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSave,
            enabled = totalSets > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Historical Session", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CalendarDatePicker(
    selectedDayOffset: Int,
    monthOffset: Int,
    onMonthOffsetChanged: (Int) -> Unit,
    onDayOffsetChanged: (Int) -> Unit
) {
    val todayStart = startOfDay(System.currentTimeMillis())
    val selectedStart = dayStartForOffset(selectedDayOffset)
    val month = Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.MONTH, monthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = (month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onMonthOffsetChanged(monthOffset - 1) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Previous")
                }
                Text(
                    monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedButton(
                    onClick = { onMonthOffsetChanged(monthOffset + 1) },
                    enabled = monthOffset < 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Next")
                }
            }

            Spacer(Modifier.height(12.dp))

            listOf("S", "M", "T", "W", "T", "F", "S").chunked(7).forEach { labels ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.forEach { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            val cells = buildList<Int?> {
                repeat(firstDayOffset) { add(null) }
                (1..daysInMonth).forEach(::add)
                while (size % 7 != 0) add(null)
            }
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { day ->
                        val dayStart = day?.let {
                            Calendar.getInstance().apply {
                                timeInMillis = month.timeInMillis
                                set(Calendar.DAY_OF_MONTH, it)
                            }.timeInMillis
                        }
                        val isFuture = dayStart != null && dayStart > todayStart
                        val isSelected = dayStart == selectedStart
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .then(
                                    if (dayStart != null && !isFuture) {
                                        Modifier.clickable { onDayOffsetChanged(dayOffsetFor(dayStart)) }
                                    } else {
                                        Modifier
                                    }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                dayStart == null -> Color.Transparent
                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    day?.toString().orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    sessions: List<SessionRecord>,
    onBack: () -> Unit,
    onAddHistorical: () -> Unit,
    onExportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onImportBackup: () -> Unit,
    onDelete: (Long) -> Unit
) {
    AppScaffold {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("History", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("${sessions.size} saved sessions", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ElevatedButton(onClick = onBack, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Back")
            }
        }

        OutlinedButton(
            onClick = onAddHistorical,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Historical Counts", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onExportBackup,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onImportBackup,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onExportCsv,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.BarChart, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Export CSV", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            FocusPanel(accent = MaterialTheme.colorScheme.primary) {
                Text(
                    "No sessions saved yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Finish a workout and it will land here automatically.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@AppScaffold
        }

        val summary = remember(sessions) { buildAnalyticsSummary(sessions) }
        var analyticsExpanded by remember { mutableStateOf(true) }
        var chartsExpanded by remember { mutableStateOf(true) }
        var sessionsExpanded by remember { mutableStateOf(true) }

        CollapsibleHistorySection(
            title = "Analytics",
            expanded = analyticsExpanded,
            onToggle = { analyticsExpanded = !analyticsExpanded }
        ) {
            AnalyticsPanel(summary)
        }

        CollapsibleHistorySection(
            title = "Charts",
            expanded = chartsExpanded,
            onToggle = { chartsExpanded = !chartsExpanded }
        ) {
            ChartsSection(sessions = sessions, summary = summary)
        }

        CollapsibleHistorySection(
            title = "Sessions",
            expanded = sessionsExpanded,
            onToggle = { sessionsExpanded = !sessionsExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sessions.forEach { session ->
                    SessionCard(session = session, onDelete = { onDelete(session.id) })
                }
            }
        }
    }
}

@Composable
private fun CollapsibleHistorySection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Spacer(Modifier.height(12.dp))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    AnimatedVisibility(expanded) {
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun AnalyticsPanel(summary: AnalyticsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricBand(
            leftValue = summary.todaySets.toString(),
            leftLabel = "sets today",
            rightValue = summary.currentStreakDays.toString(),
            rightLabel = "day streak"
        )
        MetricBand(
            leftValue = summary.weekSets.toString(),
            leftLabel = "sets this week",
            rightValue = summary.monthSets.toString(),
            rightLabel = "sets this month"
        )
        MetricBand(
            leftValue = formatDuration(summary.totalDurationSeconds),
            leftLabel = "recorded time",
            rightValue = "${summary.endedEarlyCount}",
            rightLabel = "ended early"
        )

        PeriodAnalyticsCard(summary.lifetime)
        PeriodAnalyticsCard(summary.weekly)
        PeriodAnalyticsCard(summary.monthly)
        PeriodAnalyticsCard(summary.yearly)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Date tracking", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Last workout: ${summary.lastWorkoutDate}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${summary.totalSessions} sessions | ${summary.totalSets} sets total",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (summary.manualSessionCount > 0) {
                    Text(
                        "${summary.manualSessionCount} manual entries may only include entered duration.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                MiniDayGrid(summary.dayTotals)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Cooldown discipline", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                MetricBand(
                    leftValue = "${summary.totalCooldownSeconds}s",
                    leftLabel = "taken",
                    rightValue = "${summary.totalSkippedCooldownSeconds}s",
                    rightLabel = "skipped"
                )
            }
        }

        if (summary.exerciseTotals.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Exercise totals", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    summary.exerciseTotals.forEach { total ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                total.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${total.sets}/${total.plannedSets} sets",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodAnalyticsCard(period: PeriodAnalytics) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(period.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            MetricBand(
                leftValue = period.sessions.toString(),
                leftLabel = "sessions",
                rightValue = period.sets.toString(),
                rightLabel = "sets"
            )
            Spacer(Modifier.height(10.dp))
            MetricBand(
                leftValue = formatDuration(period.durationSeconds),
                leftLabel = "recorded time",
                rightValue = "${period.skippedCooldownSeconds}s",
                rightLabel = "skipped cooldown"
            )
        }
    }
}

@Composable
private fun ChartsSection(sessions: List<SessionRecord>, summary: AnalyticsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DailyVolumeChart(summary.dayTotals)
        HistoryChart(sessions)
        DurationChart(sessions)
        CooldownChart(sessions)
        ExerciseTotalsChart(summary.exerciseTotals)
    }
}

@Composable
private fun DailyVolumeChart(days: List<DayTotal>) {
    ChartCard(title = "7-day volume", caption = "Completed sets by workout date.") {
        val maxSets = days.maxOfOrNull { it.sets }?.coerceAtLeast(1) ?: 1
        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val gap = 8.dp.toPx()
                val barWidth = ((size.width - gap * (days.size - 1)) / days.size).coerceAtLeast(8.dp.toPx())
                days.forEachIndexed { index, day ->
                    val ratio = day.sets.toFloat() / maxSets.toFloat()
                    val barHeight = (size.height * ratio).coerceAtLeast(if (day.sets > 0) 8.dp.toPx() else 0f)
                    val x = index * (barWidth + gap)
                    drawRoundRect(
                        color = Color(0xFF3E8D72),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    Text(
                        day.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationChart(sessions: List<SessionRecord>) {
    val chartSessions = sessions.take(8).asReversed()
    ChartCard(title = "Session duration", caption = "Recent workout length in minutes.") {
        LineChart(
            values = chartSessions.map { it.durationSeconds / 60f },
            color = Color(0xFF4E6FAE),
            emptyLabel = "Finish more sessions to draw a trend."
        )
    }
}

@Composable
private fun CooldownChart(sessions: List<SessionRecord>) {
    val chartSessions = sessions.take(8).asReversed()
    ChartCard(title = "Skipped cooldown", caption = "Skipped cooldown seconds per recent session.") {
        BarChart(
            values = chartSessions.map { it.skippedCooldownSeconds },
            color = Color(0xFFD59A21),
            minVisibleValue = 4f
        )
    }
}

@Composable
private fun ExerciseTotalsChart(exerciseTotals: List<ExerciseTotal>) {
    if (exerciseTotals.isEmpty()) return
    val maxSets = exerciseTotals.maxOf { it.sets }.coerceAtLeast(1)
    ChartCard(title = "Exercise mix", caption = "Total completed sets by movement.") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            exerciseTotals.forEach { total ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            total.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            total.sets.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(total.sets.toFloat() / maxSets.toFloat())
                                .height(12.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    caption: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            content()
            Spacer(Modifier.height(8.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BarChart(values: List<Int>, color: Color, minVisibleValue: Float = 0f) {
    if (values.isEmpty()) {
        Text("No chart data yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val gap = 10.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(8.dp.toPx())
        values.forEachIndexed { index, value ->
            val ratio = value.toFloat() / maxValue.toFloat()
            val barHeight = if (value == 0) 0f else (size.height * ratio).coerceAtLeast(minVisibleValue.dp.toPx())
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

@Composable
private fun LineChart(values: List<Float>, color: Color, emptyLabel: String) {
    if (values.size < 2) {
        Text(emptyLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val step = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            Offset(index * step, size.height - (value / maxValue) * size.height)
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(color = color, start = start, end = end, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        }
        points.forEach { point ->
            drawCircle(color = color, radius = 6.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun MiniDayGrid(days: List<DayTotal>) {
    val maxSets = days.maxOfOrNull { it.sets }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val alpha = if (day.sets == 0) 0.16f else 0.35f + (day.sets.toFloat() / maxSets.toFloat()) * 0.65f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (day.sets > 0) day.sets.toString() else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    day.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HistoryChart(sessions: List<SessionRecord>) {
    val chartSessions = sessions.take(8).asReversed()
    val maxSets = chartSessions.maxOfOrNull { it.completedSets }?.coerceAtLeast(1) ?: 1

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Completed sets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val barGap = 10.dp.toPx()
                val barWidth = ((size.width - barGap * (chartSessions.size - 1)) / chartSessions.size).coerceAtLeast(8.dp.toPx())
                chartSessions.forEachIndexed { index, session ->
                    val ratio = session.completedSets.toFloat() / maxSets.toFloat()
                    val barHeight = size.height * ratio
                    val x = index * (barWidth + barGap)
                    drawRoundRect(
                        color = Color(0xFFE86F51),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    if (session.skippedCooldowns > 0) {
                        drawLine(
                            color = Color(0xFFD59A21),
                            start = Offset(x, size.height - barHeight - 6.dp.toPx()),
                            end = Offset(x + barWidth, size.height - barHeight - 6.dp.toPx()),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Gold marks sessions with skipped cooldown time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionCard(session: SessionRecord, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatDate(session.startedAt), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${session.completedSets}/${session.plannedSets} sets | ${formatDuration(session.durationSeconds)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (session.endedEarly) {
                        Text(
                            "Ended early",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete session", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(10.dp))
            MetricBand(
                leftValue = "${session.actualCooldownSeconds}s",
                leftLabel = "cooldown taken",
                rightValue = "${session.skippedCooldownSeconds}s",
                rightLabel = "cooldown skipped"
            )

            Spacer(Modifier.height(10.dp))
            Text(
                session.exercises.joinToString("  |  ") { "${it.name} ${it.completedSets}/${it.plannedSets}" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (session.skippedCooldowns > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${session.skippedCooldowns} cooldown skips tracked",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFD59A21)
                )
            }
        }
    }
}

@Composable
private fun CompleteScreen(
    completedSets: Int,
    totalSets: Int,
    actualCooldownSeconds: Int,
    skippedCooldowns: Int,
    skippedCooldownSeconds: Int,
    endedEarly: Boolean,
    onAgain: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit
) {
    AppScaffold {
        Spacer(Modifier.height(36.dp))
        FocusPanel(accent = MaterialTheme.colorScheme.secondary) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                if (endedEarly) "Workout Ended" else "Session Complete",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (endedEarly) "$completedSets of $totalSets sets saved" else "$completedSets of $totalSets sets finished",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (endedEarly) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Marked ended early",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
            MetricBand(
                leftValue = "${actualCooldownSeconds}s",
                leftLabel = "cooldown taken",
                rightValue = "${skippedCooldownSeconds}s",
                rightLabel = "skipped"
            )
            if (skippedCooldowns > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "$skippedCooldowns cooldown skips saved",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFD59A21),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Run It Again", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Review History", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Edit Workout", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MotivationOverlay(message: String, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(accent.copy(alpha = 0.96f))
            .padding(26.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimerDial(seconds: Int, totalSeconds: Int, accent: Color) {
    val progress by animateFloatAsState(
        targetValue = 1f - (seconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "timer-progress"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(226.dp),
            color = accent,
            strokeWidth = 14.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                seconds.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = accent
            )
            Text(
                "seconds",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 22.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            .verticalScroll(rememberScrollState()),
        content = content
    )
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 20.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricBand(leftValue: String, leftLabel: String, rightValue: String, rightLabel: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Metric(leftValue, leftLabel, Modifier.weight(1f))
            Metric(rightValue, rightLabel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ControlStrip(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onMinus) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 58.dp)
            )
            IconButton(onClick = onPlus) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label")
            }
        }
    }
}

@Composable
private fun FocusPanel(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, accent.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun SessionTopBar(completedSets: Int, totalSets: Int, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Workout", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("$completedSets / $totalSets sets", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ElevatedButton(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("End")
        }
    }
}

@Composable
private fun EmptySession(onEnd: () -> Unit) {
    AppScaffold {
        Header("No workout", "Pick at least one exercise")
        Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Setup")
        }
    }
}

private fun buildAnalyticsSummary(sessions: List<SessionRecord>): AnalyticsSummary {
    val now = System.currentTimeMillis()
    val todayStart = startOfDay(now)
    val weekStart = startOfWeek(now)
    val monthStart = startOfMonth(now)
    val yearStart = startOfYear(now)
    val byExercise = linkedMapOf<String, Pair<Int, Int>>()
    val sessionsByDay = sessions.groupBy { startOfDay(it.startedAt) }
    val weeklySessions = sessions.filter { it.startedAt >= weekStart }
    val monthlySessions = sessions.filter { it.startedAt >= monthStart }
    val yearlySessions = sessions.filter { it.startedAt >= yearStart }

    sessions.flatMap { it.exercises }.forEach { exercise ->
        val current = byExercise[exercise.name] ?: (0 to 0)
        byExercise[exercise.name] = (current.first + exercise.completedSets) to (current.second + exercise.plannedSets)
    }

    val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    val dayTotals = (6 downTo 0).map { offset ->
        val calendar = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -offset)
        }
        val dayStart = calendar.timeInMillis
        val daySessions = sessionsByDay[dayStart].orEmpty()
        DayTotal(
            dayStart = dayStart,
            label = dayFormatter.format(Date(dayStart)).take(1),
            sets = daySessions.sumOf { it.completedSets },
            sessions = daySessions.size
        )
    }

    return AnalyticsSummary(
        totalSessions = sessions.size,
        totalSets = sessions.sumOf { it.completedSets },
        totalDurationSeconds = sessions.sumOf { it.durationSeconds },
        totalCooldownSeconds = sessions.sumOf { it.actualCooldownSeconds },
        totalSkippedCooldownSeconds = sessions.sumOf { it.skippedCooldownSeconds },
        endedEarlyCount = sessions.count { it.endedEarly },
        manualSessionCount = sessions.count { it.durationSeconds == 0 && it.startedAt == it.endedAt },
        todaySets = sessions.filter { it.startedAt >= todayStart }.sumOf { it.completedSets },
        weekSets = sessions.filter { it.startedAt >= weekStart }.sumOf { it.completedSets },
        monthSets = sessions.filter { it.startedAt >= monthStart }.sumOf { it.completedSets },
        currentStreakDays = currentStreakDays(sessionsByDay.keys, todayStart),
        lastWorkoutDate = sessions.maxByOrNull { it.startedAt }?.let { formatDate(it.startedAt) } ?: "None",
        lifetime = buildPeriodAnalytics("Lifetime", sessions),
        weekly = buildPeriodAnalytics("This week", weeklySessions),
        monthly = buildPeriodAnalytics("This month", monthlySessions),
        yearly = buildPeriodAnalytics("This year", yearlySessions),
        exerciseTotals = byExercise.map { (name, counts) ->
            ExerciseTotal(name = name, sets = counts.first, plannedSets = counts.second)
        }.sortedByDescending { it.sets },
        dayTotals = dayTotals
    )
}

private fun buildPeriodAnalytics(label: String, sessions: List<SessionRecord>): PeriodAnalytics {
    return PeriodAnalytics(
        label = label,
        sessions = sessions.size,
        sets = sessions.sumOf { it.completedSets },
        durationSeconds = sessions.sumOf { it.durationSeconds },
        cooldownSeconds = sessions.sumOf { it.actualCooldownSeconds },
        skippedCooldownSeconds = sessions.sumOf { it.skippedCooldownSeconds }
    )
}

private fun currentStreakDays(workoutDays: Set<Long>, todayStart: Long): Int {
    if (workoutDays.isEmpty()) return 0
    var streak = 0
    val calendar = Calendar.getInstance().apply { timeInMillis = todayStart }
    while (workoutDays.contains(calendar.timeInMillis)) {
        streak += 1
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun startOfDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfWeek(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = startOfDay(timestamp)
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }.timeInMillis
}

private fun startOfMonth(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = startOfDay(timestamp)
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
}

private fun startOfYear(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = startOfDay(timestamp)
        set(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

private fun dayStartForOffset(dayOffset: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = startOfDay(System.currentTimeMillis())
        add(Calendar.DAY_OF_YEAR, dayOffset)
    }.timeInMillis
}

private fun dayOffsetFor(dayStart: Long): Int {
    val today = startOfDay(System.currentTimeMillis())
    return ((dayStart - today) / 86_400_000L).toInt()
}

private fun customTemplate(name: String, modeName: String, index: Int): ExerciseTemplate {
    val mode = runCatching { ExerciseMode.valueOf(modeName) }.getOrDefault(ExerciseMode.Reps)
    return ExerciseTemplate(
        name = name,
        shortName = shortWorkoutName(name),
        mode = mode,
        defaultSets = 3,
        defaultReps = 0,
        defaultHoldSeconds = if (mode == ExerciseMode.Timer) 45 else 0,
        accent = CustomAccents[index % CustomAccents.size]
    )
}

private fun manualDateLabel(dayOffset: Int): String {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
    }
    return when (dayOffset) {
        0 -> "Today"
        -1 -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(calendar.time)
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return if (minutes > 0) "${minutes}m ${remaining}s" else "${remaining}s"
}
