package com.jake.popupschool.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.jake.popupschool.data.remote.NeisClient
import com.jake.popupschool.data.repository.SchoolDataRepository
import com.jake.popupschool.data.settings.AppSettings
import com.jake.popupschool.data.settings.SettingsRepository
import com.jake.popupschool.domain.model.SchoolSearchResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val schoolDataRepository = remember { SchoolDataRepository(NeisClient.api) }
    val scope = rememberCoroutineScope()

    val savedSettings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

    var apiKey by remember { mutableStateOf("") }
    var schoolQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SchoolSearchResult>>(emptyList()) }
    var selectedSchool by remember { mutableStateOf<SchoolSearchResult?>(null) }
    var grade by remember { mutableStateOf("") }
    var classNum by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(savedSettings) {
        if (initialized) return@LaunchedEffect
        initialized = true
        apiKey = savedSettings.apiKey
        grade = savedSettings.grade
        classNum = savedSettings.classNum
        if (savedSettings.schoolCode.isNotBlank()) {
            selectedSchool = SchoolSearchResult(
                officeCode = savedSettings.officeCode,
                officeName = "",
                schoolCode = savedSettings.schoolCode,
                schoolName = savedSettings.schoolName,
                schoolLevel = savedSettings.schoolLevel
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("학교 정보 설정", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("NEIS Open API 키") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = schoolQuery,
                onValueChange = { schoolQuery = it },
                label = { Text("학교 이름 검색") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (apiKey.isBlank() || schoolQuery.isBlank()) {
                        statusMessage = "API 키와 학교 이름을 입력해주세요."
                        return@Button
                    }
                    searching = true
                    scope.launch {
                        val result = schoolDataRepository.searchSchools(apiKey, schoolQuery)
                        searching = false
                        result.onSuccess {
                            searchResults = it
                            statusMessage = if (it.isEmpty()) "검색 결과가 없습니다." else null
                        }.onFailure {
                            statusMessage = "검색 중 오류가 발생했습니다."
                        }
                    }
                },
                enabled = !searching
            ) {
                Text(if (searching) "검색 중" else "검색")
            }
        }

        statusMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (searchResults.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(searchResults) { school ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            selectedSchool = school
                            searchResults = emptyList()
                            schoolQuery = school.schoolName
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(school.schoolName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${school.officeName} · ${school.schoolLevel.displayName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        selectedSchool?.let {
            Spacer(Modifier.height(8.dp))
            Text("선택된 학교: ${it.schoolName} (${it.schoolLevel.displayName})")
        }

        Spacer(Modifier.height(16.dp))

        Row {
            OutlinedTextField(
                value = grade,
                onValueChange = { grade = it.filter { c -> c.isDigit() } },
                label = { Text("학년") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = classNum,
                onValueChange = { classNum = it.filter { c -> c.isDigit() } },
                label = { Text("반") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Row {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val school = selectedSchool
                if (apiKey.isBlank() || school == null || grade.isBlank() || classNum.isBlank()) {
                    statusMessage = "모든 항목을 입력해주세요."
                    return@Button
                }
                scope.launch {
                    settingsRepository.save(
                        AppSettings(
                            apiKey = apiKey,
                            officeCode = school.officeCode,
                            schoolCode = school.schoolCode,
                            schoolName = school.schoolName,
                            schoolLevel = school.schoolLevel,
                            grade = grade,
                            classNum = classNum
                        )
                    )
                    statusMessage = "저장되었습니다."
                }
            }) {
                Text("저장")
            }
        }
    }
}
