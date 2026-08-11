package com.jake.popupschool.ui.home

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.jake.popupschool.data.dday.DdayRepository
import com.jake.popupschool.data.settings.SettingsRepository
import com.jake.popupschool.overlay.BubbleService
import com.jake.popupschool.overlay.OverlayPermissionHelper
import com.jake.popupschool.util.ddayLabel

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val ddayRepository = remember { DdayRepository(context) }

    val settings by settingsRepository.settingsFlow.collectAsState(initial = null)
    val ddayItems by ddayRepository.itemsFlow.collectAsState(initial = emptyList())

    var overlayGranted by remember { mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context)) }
    var bubbleRunning by remember { mutableStateOf(false) }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        overlayGranted = OverlayPermissionHelper.canDrawOverlays(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("팝업스쿨", style = MaterialTheme.typography.headlineMedium)

        val nearest = ddayItems.minByOrNull { it.targetDate() }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("가까운 디데이", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                if (nearest == null) {
                    Text("등록된 디데이가 없습니다.")
                } else {
                    Text("${nearest.title}  ${ddayLabel(nearest.targetDate())}")
                }
            }
        }

        if (settings?.isConfigured == false) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("학교 정보가 설정되지 않았습니다. 아래에서 먼저 설정해주세요.")
                }
            }
        }

        if (!overlayGranted) {
            Button(onClick = {
                overlayLauncher.launch(OverlayPermissionHelper.buildPermissionIntent(context))
            }) {
                Text("다른 앱 위에 표시 권한 허용하기")
            }
        }

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                val intent = Intent(context, BubbleService::class.java)
                if (bubbleRunning) {
                    intent.action = BubbleService.ACTION_STOP
                }
                ContextCompat.startForegroundService(context, intent)
                bubbleRunning = !bubbleRunning
            },
            enabled = overlayGranted && settings?.isConfigured == true
        ) {
            Text(if (bubbleRunning) "버블 중지" else "버블 시작")
        }

        Button(onClick = { navController.navigate("settings") }) {
            Text("학교 정보 설정")
        }

        Button(onClick = { navController.navigate("dday") }) {
            Text("디데이 관리")
        }

        Button(onClick = { navController.navigate("timetable") }) {
            Text("시간표 설정")
        }
    }
}
