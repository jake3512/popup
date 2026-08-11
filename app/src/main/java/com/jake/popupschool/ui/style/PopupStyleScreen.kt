package com.jake.popupschool.ui.style

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jake.popupschool.data.settings.SettingsRepository
import com.jake.popupschool.domain.model.BubbleIconType
import com.jake.popupschool.domain.model.PopupStyle
import com.jake.popupschool.util.bubbleIconImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupStyleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf(PopupStyle.DEFAULT) }

    var iconType by remember { mutableStateOf(BubbleIconType.DEFAULT) }
    var iconText by remember { mutableStateOf("") }
    var hasCustomImage by remember { mutableStateOf(false) }
    var iconStatusMessage by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copied = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            bubbleIconImageFile(context).outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }.isSuccess
                }
                if (copied) {
                    iconType = BubbleIconType.IMAGE
                    hasCustomImage = true
                    iconStatusMessage = null
                    settingsRepository.setBubbleIcon(BubbleIconType.IMAGE, iconText)
                } else {
                    iconStatusMessage = "사진을 불러오지 못했습니다."
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val current = settingsRepository.current()
        selected = current.popupStyle
        iconType = current.bubbleIconType
        iconText = current.bubbleIconText
        hasCustomImage = bubbleIconImageFile(context).exists()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("팝업 스타일") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) { Text("뒤로") }

            Text(
                "버블을 탭했을 때 펼쳐지는 팝업 카드의 스타일을 선택하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PopupStyle.entries.forEach { style ->
                StylePreviewCard(
                    style = style,
                    selected = style == selected,
                    onClick = {
                        selected = style
                        scope.launch { settingsRepository.setPopupStyle(style) }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "버블 아이콘",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "화면에 떠 있는 버블에 표시할 아이콘을 선택하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = iconType == BubbleIconType.DEFAULT,
                    onClick = {
                        iconType = BubbleIconType.DEFAULT
                        scope.launch { settingsRepository.setBubbleIcon(BubbleIconType.DEFAULT, iconText) }
                    },
                    label = { Text("기본") }
                )
                FilterChip(
                    selected = iconType == BubbleIconType.TEXT,
                    onClick = {
                        iconType = BubbleIconType.TEXT
                        scope.launch { settingsRepository.setBubbleIcon(BubbleIconType.TEXT, iconText) }
                    },
                    label = { Text("텍스트・이모지") }
                )
                FilterChip(
                    selected = iconType == BubbleIconType.IMAGE,
                    onClick = {
                        iconType = BubbleIconType.IMAGE
                        scope.launch { settingsRepository.setBubbleIcon(BubbleIconType.IMAGE, iconText) }
                    },
                    label = { Text("사진") }
                )
            }

            if (iconType == BubbleIconType.TEXT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = iconText,
                        onValueChange = { if (it.length <= 4) iconText = it },
                        label = { Text("텍스트 또는 이모지 (최대 4자)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch { settingsRepository.setBubbleIcon(BubbleIconType.TEXT, iconText) }
                    }) {
                        Text("적용")
                    }
                }
            }

            if (iconType == BubbleIconType.IMAGE) {
                Button(onClick = { pickImageLauncher.launch("image/*") }) {
                    Text("갤러리에서 사진 선택")
                }
                if (hasCustomImage) {
                    Text(
                        "사진이 설정되었습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            iconStatusMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StylePreviewCard(style: PopupStyle, selected: Boolean, onClick: () -> Unit) {
    val bgColor = Color(style.backgroundColor.toInt())
    val accent = Color(style.headerTextColor.toInt())

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(accent, CircleShape)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(style.displayName, style = MaterialTheme.typography.titleMedium)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "선택됨", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
