package com.jake.popupschool.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationHelper {
    const val CHANNEL_ID = "bubble_service_channel"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "팝업스쿨 실행 알림",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "떠있는 버블이 실행 중임을 알려줍니다."
            }
            manager.createNotificationChannel(channel)
        }
    }
}
