package com.jake.popupschool.ui.timetable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
    val scope = rememberCoroutineScope()

    var source by remember { mutableStateOf(TimetableSource.NEIS) }
    val entries by timetableRepository.entriesFlow.collectAsState(initial = emptyList())
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }
    var showAddDialog by remember { mutableStateOf(false) }

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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
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
                                Text(entry.subject, modifier = Modifier.weight(1f))
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
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "교시 추가")
                }
            }
        }
    }

    if (showAddDialog) {
        AddPeriodDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { period, subject ->
                scope.launch {
                    timetableRepository.add(
                        ManualTimetableEntry(dayOfWeek = selectedDay, period = period, subject = subject)
                    )
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddPeriodDialog(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var period by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("교시 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it.filter { c -> c.isDigit() } },
                    label = { Text("교시") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("과목") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val periodValue = period.toIntOrNull()
                if (periodValue != null && subject.isNotBlank()) {
                    onConfirm(periodValue, subject)
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
