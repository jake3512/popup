package com.jake.popupschool.ui.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jake.popupschool.data.study.StudyTimeRepository
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StudyCalendarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { StudyTimeRepository(context) }
    val records by repository.recordsFlow.collectAsState(initial = emptyList())

    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    val secondsByDate = remember(records) {
        records.associate { LocalDate.ofEpochDay(it.dateEpochDay) to it.seconds }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("공부시간 달력") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            TextButton(onClick = onBack) { Text("뒤로") }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 달")
                }
                Text(
                    "${displayedMonth.year}년 ${displayedMonth.monthValue}월",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "다음 달")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("월", "화", "수", "목", "금", "토", "일").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val firstOfMonth = displayedMonth.atDay(1)
            val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
            val daysInMonth = displayedMonth.lengthOfMonth()
            val totalCells = leadingBlanks + daysInMonth
            val rowCount = (totalCells + 6) / 7
            val today = LocalDate.now()

            Column(modifier = Modifier.padding(top = 4.dp)) {
                for (row in 0 until rowCount) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val dayNum = row * 7 + col - leadingBlanks + 1
                            Box(modifier = Modifier.weight(1f).aspectRatio(0.85f).padding(2.dp)) {
                                if (dayNum in 1..daysInMonth) {
                                    val date = displayedMonth.atDay(dayNum)
                                    StudyDayCell(
                                        day = dayNum,
                                        seconds = secondsByDate[date] ?: 0L,
                                        isToday = date == today
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyDayCell(day: Int, seconds: Long, isToday: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (seconds > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isToday) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (seconds > 0) {
                Text(
                    formatStudyDuration(seconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatStudyDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}시간${m}분" else "${m}분"
}
