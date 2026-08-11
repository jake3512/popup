package com.jake.popupschool.ui.timetable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jake.popupschool.data.settings.SettingsRepository
import com.jake.popupschool.data.timetable.ManualTimetableEntry
import com.jake.popupschool.data.timetable.TimetableRepository
import com.jake.popupschool.data.timetable.TimetableSubject
import com.jake.popupschool.data.timetable.TimetableSubjectRepository
import com.jake.popupschool.domain.model.TimetableSource
import kotlinx.coroutines.launch
import java.time.LocalDate

private val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val timetableRepository = remember { TimetableRepository(context) }
    val subjectRepository = remember { TimetableSubjectRepository(context) }
    val scope = rememberCoroutineScope()

    var source by remember { mutableStateOf(TimetableSource.NEIS) }
    val entries by timetableRepository.entriesFlow.collectAsState(initial = emptyList())
    val subjects by subjectRepository.subjectsFlow.collectAsState(initial = emptyList())
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        source = settingsRepository.current().timetableSource
    }

    Scaffold(topBar = { TopAppBar(title = { Text("시간표 설정") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("뒤로") }

            Text("시간표 가져오는 방식", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            TimetableSource.entries.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            source = option
                            scope.launch { settingsRepository.setTimetableSource(option) }
                        }
                ) {
                    RadioButton(selected = source == option, onClick = null)
                    Text(option.displayName)
                }
            }

            if (source == TimetableSource.MANUAL) {
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("과목 목록", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAddSubjectDialog = true }) { Text("+ 과목 추가") }
                }
                Text(
                    "이동수업(음악실·과학실 등 특별실 이동)인 과목은 체크해두면 시간표에 강조 표시됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                if (subjects.isEmpty()) {
                    Text(
                        "등록된 과목이 없습니다. 먼저 과목을 추가해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjects.forEach { subject ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (subject.isMovingClass) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(subject.name, style = MaterialTheme.typography.labelLarge)
                                    IconButton(
                                        onClick = { scope.launch { subjectRepository.remove(subject.id) } },
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "과목 삭제", modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("요일 선택", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        val dayValue = index + 1
                        FilterChip(
                            selected = selectedDay == dayValue,
                            onClick = { selectedDay = dayValue },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                val dayEntries = entries.filter { it.dayOfWeek == selectedDay }.sortedBy { it.period }
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(dayEntries, key = { it.id }) { entry ->
                        val subject = subjects.find { it.id == entry.subjectId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (subject?.isMovingClass == true) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${entry.period}교시",
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    subject?.name ?: "(삭제된 과목)",
                                    modifier = Modifier.weight(1f)
                                )
                                if (subject?.isMovingClass == true) {
                                    Text(
                                        "이동수업",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch { timetableRepository.remove(entry.id) }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "삭제")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                FloatingActionButton(onClick = { showAddPeriodDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "교시 추가")
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, isMovingClass ->
                scope.launch {
                    subjectRepository.add(TimetableSubject(name = name, isMovingClass = isMovingClass))
                }
                showAddSubjectDialog = false
            }
        )
    }

    if (showAddPeriodDialog) {
        val nextPeriod = entries.count { it.dayOfWeek == selectedDay } + 1
        AddPeriodDialog(
            subjects = subjects,
            nextPeriod = nextPeriod,
            onDismiss = { showAddPeriodDialog = false },
            onConfirm = { subjectId ->
                scope.launch {
                    timetableRepository.add(
                        ManualTimetableEntry(dayOfWeek = selectedDay, period = nextPeriod, subjectId = subjectId)
                    )
                }
                showAddPeriodDialog = false
            }
        )
    }
}

@Composable
private fun AddSubjectDialog(onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isMovingClass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("과목 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("과목명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isMovingClass = !isMovingClass }
                ) {
                    Checkbox(checked = isMovingClass, onCheckedChange = { isMovingClass = it })
                    Text("이동수업 (특별실 이동)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name, isMovingClass)
            }) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun AddPeriodDialog(
    subjects: List<TimetableSubject>,
    nextPeriod: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${nextPeriod}교시 추가") },
        text = {
            Column {
                Text(
                    "입력한 순서대로 교시가 매겨집니다. 이번에는 ${nextPeriod}교시로 등록됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                if (subjects.isEmpty()) {
                    Text(
                        "등록된 과목이 없습니다. 먼저 과목을 추가해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("과목 선택", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(subjects, key = { it.id }) { subject ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSubjectId = subject.id }
                            ) {
                                RadioButton(
                                    selected = selectedSubjectId == subject.id,
                                    onClick = { selectedSubjectId = subject.id }
                                )
                                Text(subject.name)
                                if (subject.isMovingClass) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "(이동수업)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val subjectId = selectedSubjectId
                if (subjectId != null) {
                    onConfirm(subjectId)
                }
            }) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
