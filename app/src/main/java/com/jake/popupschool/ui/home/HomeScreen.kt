package com.jake.popupschool.ui.home

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val nearest = ddayItems.minByOrNull { it.targetDate() }
    val isConfigured = settings?.isConfigured == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                "팝업스쿨",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "디데이 · 시간표 · 급식을 한 화면에",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "가까운 디데이",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (nearest == null) {
                    Text(
                        "등록된 디데이가 없습니다.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        nearest.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        ddayLabel(nearest.targetDate()),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "상태",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusRow(icon = Icons.Filled.Shield, label = "오버레이 권한", granted = overlayGranted)
                StatusRow(icon = Icons.Filled.Settings, label = "학교 정보", granted = isConfigured)
            }
        }

        if (!overlayGranted) {
            FilledTonalButton(
                onClick = { overlayLauncher.launch(OverlayPermissionHelper.buildPermissionIntent(context)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
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
            enabled = overlayGranted && isConfigured,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                if (bubbleRunning) Icons.Filled.PauseCircle else Icons.Filled.Bolt,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (bubbleRunning) "버블 중지" else "버블 시작",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Text(
            "빠른 메뉴",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessItem(
                icon = Icons.Filled.Settings,
                title = "학교 정보 설정",
                subtitle = "NEIS API 키 · 학교 검색",
                onClick = { navController.navigate("settings") }
            )
            QuickAccessItem(
                icon = Icons.Filled.CalendarMonth,
                title = "디데이 관리",
                subtitle = "중요한 날짜 등록",
                onClick = { navController.navigate("dday") }
            )
            QuickAccessItem(
                icon = Icons.Filled.Schedule,
                title = "시간표 설정",
                subtitle = "자동 조회 또는 직접 입력",
                onClick = { navController.navigate("timetable") }
            )
        }
    }
}

@Composable
private fun StatusRow(icon: ImageVector, label: String, granted: Boolean) {
    val tint = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (granted) "완료" else "필요",
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

@Composable
private fun QuickAccessItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
