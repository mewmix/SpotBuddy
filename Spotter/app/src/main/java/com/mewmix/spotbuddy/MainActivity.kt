package com.mewmix.spotbuddy

import android.os.Bundle
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mewmix.spotbuddy.ui.theme.SpotBuddyTheme
import kotlinx.coroutines.delay
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

private enum class SessionPhase { Setup, Active, Rest, Complete }

private val WorkoutCatalog = listOf(
    ExerciseTemplate("Pushups", "PUSH", ExerciseMode.Reps, 4, 15, 0, Color(0xFFE86F51)),
    ExerciseTemplate("Crunches", "CORE", ExerciseMode.Reps, 4, 20, 0, Color(0xFF4E6FAE)),
    ExerciseTemplate("Situps", "SIT", ExerciseMode.Reps, 3, 15, 0, Color(0xFF8257A6)),
    ExerciseTemplate("Squats", "LEGS", ExerciseMode.Reps, 4, 20, 0, Color(0xFF3E8D72)),
    ExerciseTemplate("Planks", "HOLD", ExerciseMode.Timer, 3, 0, 45, Color(0xFFD59A21)),
    ExerciseTemplate("Lunges", "MOVE", ExerciseMode.Reps, 3, 12, 0, Color(0xFF2E9DA7))
)

@Composable
private fun SpotBuddyApp() {
    val items = remember {
        mutableStateListOf<WorkoutItem>().apply {
            addAll(WorkoutCatalog.map { WorkoutItem(it) })
        }
    }
    var phase by remember { mutableStateOf(SessionPhase.Setup) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var restSeconds by remember { mutableIntStateOf(45) }
    var remainingSeconds by remember { mutableIntStateOf(45) }

    val activeItems = items.filter { it.selected && it.sets > 0 }
    val totalSets = activeItems.sumOf { it.sets }
    val completedSets = activeItems.sumOf { it.completed }
    val currentItem = activeItems.getOrNull(currentIndex.coerceIn(0, max(activeItems.lastIndex, 0)))

    fun updateItem(template: ExerciseTemplate, transform: (WorkoutItem) -> WorkoutItem) {
        val index = items.indexOfFirst { it.template == template }
        if (index >= 0) items[index] = transform(items[index])
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
            phase = SessionPhase.Rest
        } else {
            phase = SessionPhase.Active
        }
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
                    restSeconds = restSeconds,
                    onRestChanged = { restSeconds = it.coerceIn(0, 180) },
                    onItemChanged = ::updateItem,
                    onStart = {
                        items.indices.forEach { index -> items[index] = items[index].copy(completed = 0) }
                        currentIndex = 0
                        phase = SessionPhase.Active
                    }
                )

                SessionPhase.Active -> ActiveScreen(
                    item = currentItem,
                    completedSets = completedSets,
                    totalSets = totalSets,
                    onCompleteSet = { completeCurrentSet(startRest = true) },
                    onSkipRest = { completeCurrentSet(startRest = false) },
                    onEnd = { phase = SessionPhase.Setup }
                )

                SessionPhase.Rest -> RestScreen(
                    nextItem = currentItem,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = restSeconds,
                    completedSets = completedSets,
                    totalSets = totalSets,
                    onTick = { remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0) },
                    onDone = { phase = SessionPhase.Active },
                    onSkip = { phase = SessionPhase.Active }
                )

                SessionPhase.Complete -> CompleteScreen(
                    completedSets = completedSets,
                    totalSets = totalSets,
                    onAgain = {
                        items.indices.forEach { index -> items[index] = items[index].copy(completed = 0) }
                        currentIndex = 0
                        phase = SessionPhase.Active
                    },
                    onEdit = { phase = SessionPhase.Setup }
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    items: List<WorkoutItem>,
    restSeconds: Int,
    onRestChanged: (Int) -> Unit,
    onItemChanged: (ExerciseTemplate, (WorkoutItem) -> WorkoutItem) -> Unit,
    onStart: () -> Unit
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
            rightValue = "${restSeconds}s",
            rightLabel = "cooldown"
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Build your session",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                WorkoutSetupCard(
                    item = item,
                    onToggle = {
                        onItemChanged(item.template) { it.copy(selected = !it.selected) }
                    },
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
    onToggle: () -> Unit,
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
                        if (item.template.mode == ExerciseMode.Timer) "Hold timer" else "${item.reps} reps per set",
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
                    ControlStrip(
                        label = "Sets",
                        value = item.sets.toString(),
                        onMinus = { onSets(-1) },
                        onPlus = { onSets(1) }
                    )
                    Spacer(Modifier.height(10.dp))
                    ControlStrip(
                        label = if (item.template.mode == ExerciseMode.Timer) "Hold" else "Reps",
                        value = if (item.template.mode == ExerciseMode.Timer) "${item.holdSeconds}s" else item.reps.toString(),
                        onMinus = { onAmount(-1) },
                        onPlus = { onAmount(1) }
                    )
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
    onCompleteSet: () -> Unit,
    onSkipRest: () -> Unit,
    onEnd: () -> Unit
) {
    if (item == null) {
        EmptySession(onEnd)
        return
    }

    var timerSeconds by remember(item.template.name, item.completed) {
        mutableIntStateOf(item.holdSeconds)
    }

    if (item.template.mode == ExerciseMode.Timer && !LocalInspectionMode.current) {
        LaunchedEffect(item.template.name, item.completed, timerSeconds) {
            if (timerSeconds > 0) {
                delay(1000)
                timerSeconds -= 1
            } else {
                onCompleteSet()
            }
        }
    }

    val progress = if (item.template.mode == ExerciseMode.Timer) {
        1f - (timerSeconds.toFloat() / item.holdSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        completedSets.toFloat() / totalSets.toFloat().coerceAtLeast(1f)
    }

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
                    item.reps.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = item.template.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "reps",
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
    onSkip: () -> Unit
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
        SessionTopBar(completedSets, totalSets, onSkip)
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
                Text("Start Next Set", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CompleteScreen(
    completedSets: Int,
    totalSets: Int,
    onAgain: () -> Unit,
    onEdit: () -> Unit
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
                "Session Complete",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "$completedSets of $totalSets sets finished",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(30.dp))
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
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
