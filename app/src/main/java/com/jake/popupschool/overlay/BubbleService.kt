package com.jake.popupschool.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.jake.popupschool.MainActivity
import com.jake.popupschool.R
import com.jake.popupschool.data.dday.DdayItem
import com.jake.popupschool.data.dday.DdayRepository
import com.jake.popupschool.data.remote.NeisClient
import com.jake.popupschool.data.repository.SchoolDataRepository
import com.jake.popupschool.data.settings.SettingsRepository
import com.jake.popupschool.data.timetable.TimetableRepository
import com.jake.popupschool.domain.model.MealInfo
import com.jake.popupschool.domain.model.PopupStyle
import com.jake.popupschool.domain.model.TimetableSlot
import com.jake.popupschool.domain.model.TimetableSource
import com.jake.popupschool.util.ddayLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var popupView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var expanded = false

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var ddayRepository: DdayRepository
    private lateinit var schoolDataRepository: SchoolDataRepository
    private lateinit var timetableRepository: TimetableRepository
    private var currentStyle: PopupStyle = PopupStyle.DEFAULT

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(applicationContext)
        ddayRepository = DdayRepository(applicationContext)
        schoolDataRepository = SchoolDataRepository(NeisClient.api)
        timetableRepository = TimetableRepository(applicationContext)

        NotificationHelper.ensureChannel(this)
        startForegroundWithType(buildNotification())
        addBubble()

        serviceScope.launch {
            currentStyle = settingsRepository.settingsFlow.first().popupStyle
            applyBubbleStyle()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startForegroundWithType(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0, Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentTitle("팝업스쿨 실행 중")
            .setContentText("탭하여 열기")
            .setContentIntent(openIntent)
            .addAction(0, "중지", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun addBubble() {
        if (bubbleView != null) return
        val view = LayoutInflater.from(this).inflate(R.layout.view_bubble, null)
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var downTime = 0L
        var moved = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    downTime = System.currentTimeMillis()
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    bubbleParams.x = initialX + dx
                    bubbleParams.y = initialY + dy
                    windowManager.updateViewLayout(v, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved && System.currentTimeMillis() - downTime < 250) {
                        toggleExpanded()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, bubbleParams)
        bubbleView = view
        applyBubbleStyle()
    }

    private fun applyBubbleStyle() {
        val icon = bubbleView?.findViewById<View>(R.id.bubbleIcon) ?: return
        val density = resources.displayMetrics.density
        icon.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(currentStyle.bubbleColor.toInt())
            setStroke((2 * density).toInt(), currentStyle.borderColor.toInt())
        }
    }

    private fun applyPopupStyle(view: View) {
        val density = resources.displayMetrics.density
        view.findViewById<View>(R.id.popupRoot).background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = currentStyle.cornerRadiusDp * density
            setColor(currentStyle.backgroundColor.toInt())
            setStroke((1 * density).toInt(), currentStyle.borderColor.toInt())
        }
        view.findViewById<TextView>(R.id.popupTitle).setTextColor(currentStyle.headerTextColor.toInt())
        view.findViewById<TextView>(R.id.sectionDdayHeader).setTextColor(currentStyle.headerTextColor.toInt())
        view.findViewById<TextView>(R.id.sectionTimetableHeader).setTextColor(currentStyle.headerTextColor.toInt())
        view.findViewById<TextView>(R.id.sectionMealHeader).setTextColor(currentStyle.headerTextColor.toInt())
        view.findViewById<TextView>(R.id.lastUpdatedText).setTextColor(currentStyle.secondaryTextColor.toInt())
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
    }

    private fun toggleExpanded() {
        if (expanded) collapse() else expand()
    }

    private fun expand() {
        if (popupView != null) return
        val bubbleX = bubbleParams.x
        val bubbleY = bubbleParams.y
        removeBubble()

        val view = LayoutInflater.from(this).inflate(R.layout.view_popup, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
        }

        view.findViewById<View>(R.id.closeButton).setOnClickListener { collapse() }
        view.findViewById<View>(R.id.refreshButton).setOnClickListener { loadData(view) }

        windowManager.addView(view, params)
        popupView = view
        expanded = true
        loadData(view)
    }

    private fun collapse() {
        popupView?.let { runCatching { windowManager.removeView(it) } }
        popupView = null
        expanded = false
        addBubble()
    }

    private fun loadData(view: View) {
        val ddayContainer = view.findViewById<LinearLayout>(R.id.ddayContainer)
        val timetableContainer = view.findViewById<LinearLayout>(R.id.timetableContainer)
        val mealContainer = view.findViewById<LinearLayout>(R.id.mealContainer)
        val lastUpdatedText = view.findViewById<TextView>(R.id.lastUpdatedText)

        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            currentStyle = settings.popupStyle
            applyPopupStyle(view)

            val ddayItems = ddayRepository.itemsFlow.first()

            renderDday(ddayContainer, ddayItems)

            val today = LocalDate.now()

            if (settings.timetableSource == TimetableSource.MANUAL) {
                val slots = timetableRepository.entriesFlow.first()
                    .filter { it.dayOfWeek == today.dayOfWeek.value }
                    .sortedBy { it.period }
                    .map { TimetableSlot(period = it.period, subject = it.subject) }
                if (slots.isEmpty()) renderMessage(timetableContainer, "등록된 시간표가 없습니다.")
                else renderTimetable(timetableContainer, slots)
            } else if (settings.isConfigured) {
                schoolDataRepository.getTimetableForDate(settings, today)
                    .onSuccess { slots ->
                        if (slots.isEmpty()) renderMessage(timetableContainer, "오늘은 시간표가 없습니다.")
                        else renderTimetable(timetableContainer, slots)
                    }
                    .onFailure { renderMessage(timetableContainer, "시간표를 불러오지 못했습니다.") }
            } else {
                renderMessage(timetableContainer, "설정에서 학교 정보를 입력해주세요.")
            }

            if (settings.isConfigured) {
                schoolDataRepository.getMealsForDate(settings, today)
                    .onSuccess { meals ->
                        if (meals.isEmpty()) renderMessage(mealContainer, "오늘은 급식 정보가 없습니다.")
                        else renderMeals(mealContainer, meals)
                    }
                    .onFailure { renderMessage(mealContainer, "급식 정보를 불러오지 못했습니다.") }
            } else {
                renderMessage(mealContainer, "설정에서 학교 정보를 입력해주세요.")
            }

            lastUpdatedText.text = "업데이트: ${LocalTime.now().withNano(0)}"
        }
    }

    private fun renderDday(container: LinearLayout, items: List<DdayItem>) {
        container.removeAllViews()
        if (items.isEmpty()) {
            renderMessage(container, "등록된 디데이가 없습니다.")
            return
        }
        items.sortedBy { it.targetDate() }.take(3).forEach { item ->
            val tv = TextView(this)
            tv.text = "${item.title}  ${ddayLabel(item.targetDate())}"
            tv.textSize = 14f
            tv.setTextColor(currentStyle.bodyTextColor.toInt())
            tv.setPadding(0, 4, 0, 4)
            container.addView(tv)
        }
    }

    private fun renderTimetable(container: LinearLayout, slots: List<TimetableSlot>) {
        container.removeAllViews()
        slots.forEach { slot ->
            val tv = TextView(this)
            tv.text = "${slot.period}교시  ${slot.subject}"
            tv.textSize = 14f
            tv.setTextColor(currentStyle.bodyTextColor.toInt())
            tv.setPadding(0, 4, 0, 4)
            container.addView(tv)
        }
    }

    private fun renderMeals(container: LinearLayout, meals: List<MealInfo>) {
        container.removeAllViews()
        meals.forEach { meal ->
            val header = TextView(this)
            header.text = meal.mealType
            header.textSize = 14f
            header.setTextColor(currentStyle.headerTextColor.toInt())
            header.setPadding(0, 4, 0, 0)
            header.setTypeface(null, Typeface.BOLD)
            container.addView(header)

            val body = TextView(this)
            body.text = meal.menuItems.joinToString("\n")
            body.textSize = 13f
            body.setTextColor(currentStyle.bodyTextColor.toInt())
            body.setPadding(0, 2, 0, 4)
            container.addView(body)
        }
    }

    private fun renderMessage(container: LinearLayout, message: String) {
        container.removeAllViews()
        val tv = TextView(this)
        tv.text = message
        tv.textSize = 13f
        tv.setTextColor(currentStyle.secondaryTextColor.toInt())
        container.addView(tv)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble()
        popupView?.let { runCatching { windowManager.removeView(it) } }
        serviceJob.cancel()
    }

    companion object {
        const val ACTION_STOP = "com.jake.popupschool.action.STOP"
        private const val NOTIFICATION_ID = 42
    }
}
