package com.jake.popupschool.overlay

import android.app.DatePickerDialog
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
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
import com.jake.popupschool.data.study.StudyTimeRepository
import com.jake.popupschool.data.timetable.TimetableRepository
import com.jake.popupschool.data.timetable.TimetableSubjectRepository
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
    private lateinit var timetableSubjectRepository: TimetableSubjectRepository
    private lateinit var studyTimeRepository: StudyTimeRepository
    private var currentStyle: PopupStyle = PopupStyle.DEFAULT

    private var viewingDate: LocalDate = LocalDate.now()
    private var countDownTimer: CountDownTimer? = null
    private var timerRemainingSeconds: Long = 0L
    private var timerRunning: Boolean = false

    private val stopwatchHandler = Handler(Looper.getMainLooper())
    private var stopwatchRunnable: Runnable? = null
    private var stopwatchSeconds: Long = 0L
    private var stopwatchRunning: Boolean = false

    // Persistent daily study-time counter, shown under the date row. Unlike the
    // toolbar timer/stopwatch, this survives popup collapse and service restarts.
    private val studyHandler = Handler(Looper.getMainLooper())
    private var studyRunnable: Runnable? = null
    private var studyDate: LocalDate = LocalDate.now()
    private var studySeconds: Long = 0L
    private var studyRunning: Boolean = false
    private var studyLastCheckpointKey: String = ""

    private val movingClassHighlight = Color.parseColor("#40FFC107")

    private val dayGestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) > abs(dy) && abs(dx) > 80 && abs(velocityX) > 200) {
                    if (dx < 0) changeDay(1) else changeDay(-1)
                    return true
                }
                return false
            }
        })
    }

    private enum class ContentMode { NORMAL, TIMER, STOPWATCH, MINIGAME }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(applicationContext)
        ddayRepository = DdayRepository(applicationContext)
        schoolDataRepository = SchoolDataRepository(NeisClient.api)
        timetableRepository = TimetableRepository(applicationContext)
        timetableSubjectRepository = TimetableSubjectRepository(applicationContext)
        studyTimeRepository = StudyTimeRepository(applicationContext)

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
        view.findViewById<TextView>(R.id.dateLabelText).setTextColor(currentStyle.headerTextColor.toInt())
        view.findViewById<TextView>(R.id.timerDisplayText).setTextColor(currentStyle.headerTextColor.toInt())
        view.findViewById<TextView>(R.id.stopwatchDisplayText).setTextColor(currentStyle.headerTextColor.toInt())
        view.findViewById<TextView>(R.id.studyTimeText).setTextColor(currentStyle.headerTextColor.toInt())
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
        view.findViewById<View>(R.id.refreshButton).setOnClickListener {
            loadDday(view)
            loadTimetableAndMeal(view)
        }
        view.findViewById<View>(R.id.prevDayButton).setOnClickListener { changeDay(-1) }
        view.findViewById<View>(R.id.nextDayButton).setOnClickListener { changeDay(1) }
        view.findViewById<View>(R.id.contentScrollView).setOnTouchListener { _, event ->
            dayGestureDetector.onTouchEvent(event)
            false
        }
        setupToolbar(view)
        view.findViewById<View>(R.id.studyTimeToggleButton).setOnClickListener { toggleStudyTimer(view) }

        windowManager.addView(view, params)
        popupView = view
        expanded = true
        viewingDate = LocalDate.now()
        loadDday(view)
        loadTimetableAndMeal(view)
        initStudyTimer(view)
    }

    private fun collapse() {
        cancelTimer()
        cancelStopwatch()
        pauseStudyTimer()
        popupView?.findViewById<FrameLayout>(R.id.minigameContainer)?.removeAllViews()
        popupView?.let { runCatching { windowManager.removeView(it) } }
        popupView = null
        expanded = false
        addBubble()
    }

    private fun changeDay(deltaDays: Int) {
        viewingDate = viewingDate.plusDays(deltaDays.toLong())
        popupView?.let { loadTimetableAndMeal(it) }
    }

    private fun formatDateLabel(date: LocalDate): String {
        val dayNames = arrayOf("월", "화", "수", "목", "금", "토", "일")
        val dayName = dayNames[date.dayOfWeek.value - 1]
        val base = "${date.monthValue}월 ${date.dayOfMonth}일 (${dayName})"
        return if (date == LocalDate.now()) "$base · 오늘" else base
    }

    // --- Toolbar: calendar / timer / stopwatch / minigame ---

    private fun setupToolbar(view: View) {
        view.findViewById<View>(R.id.toolCalendarButton).setOnClickListener { showDatePicker(view) }
        view.findViewById<View>(R.id.toolTimerButton).setOnClickListener { toggleMode(view, ContentMode.TIMER) }
        view.findViewById<View>(R.id.toolStopwatchButton).setOnClickListener { toggleMode(view, ContentMode.STOPWATCH) }
        view.findViewById<View>(R.id.toolMinigameButton).setOnClickListener { toggleMode(view, ContentMode.MINIGAME) }

        view.findViewById<View>(R.id.timerAdd30SecButton).setOnClickListener { addTimerTime(view, 30L) }
        view.findViewById<View>(R.id.timerAdd1MinButton).setOnClickListener { addTimerTime(view, 60L) }
        view.findViewById<View>(R.id.timerStartButton).setOnClickListener { startTimerCountdown(view) }
        view.findViewById<View>(R.id.timerPauseButton).setOnClickListener { pauseTimer(view) }
        view.findViewById<View>(R.id.timerResetButton).setOnClickListener { resetTimer(view) }

        view.findViewById<View>(R.id.stopwatchStartButton).setOnClickListener { startStopwatch(view) }
        view.findViewById<View>(R.id.stopwatchPauseButton).setOnClickListener { pauseStopwatch(view) }
        view.findViewById<View>(R.id.stopwatchResetButton).setOnClickListener { resetStopwatch(view) }
    }

    private fun showContentMode(view: View, mode: ContentMode) {
        view.findViewById<View>(R.id.contentScrollView).visibility =
            if (mode == ContentMode.NORMAL) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.timerPanel).visibility =
            if (mode == ContentMode.TIMER) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.stopwatchPanel).visibility =
            if (mode == ContentMode.STOPWATCH) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.minigameContainer).visibility =
            if (mode == ContentMode.MINIGAME) View.VISIBLE else View.GONE
    }

    /** Tears down whichever tool panel isn't [mode] and shows [mode]. Toggling the active mode's own tool returns to NORMAL. */
    private fun toggleMode(view: View, mode: ContentMode) {
        val currentlyActive = view.findViewById<View>(
            when (mode) {
                ContentMode.TIMER -> R.id.timerPanel
                ContentMode.STOPWATCH -> R.id.stopwatchPanel
                ContentMode.MINIGAME -> R.id.minigameContainer
                ContentMode.NORMAL -> R.id.contentScrollView
            }
        ).visibility == View.VISIBLE
        enterMode(view, if (currentlyActive) ContentMode.NORMAL else mode)
    }

    private fun enterMode(view: View, mode: ContentMode) {
        if (mode != ContentMode.TIMER) resetTimer(view)
        if (mode != ContentMode.STOPWATCH) resetStopwatch(view)
        val minigameContainer = view.findViewById<FrameLayout>(R.id.minigameContainer)
        if (mode != ContentMode.MINIGAME) {
            minigameContainer.removeAllViews()
        } else {
            minigameContainer.removeAllViews()
            minigameContainer.addView(
                SwingHeroView(this),
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )
        }
        showContentMode(view, mode)
    }

    private fun showDatePicker(view: View) {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                viewingDate = LocalDate.of(year, month + 1, dayOfMonth)
                loadTimetableAndMeal(view)
            },
            viewingDate.year,
            viewingDate.monthValue - 1,
            viewingDate.dayOfMonth
        )
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    // --- Timer (counts down; +30초/+1분 accumulate before or during a run) ---

    private fun updateTimerDisplay(view: View) {
        val seconds = timerRemainingSeconds.coerceAtLeast(0)
        view.findViewById<TextView>(R.id.timerDisplayText).text =
            "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun updateTimerButtons(view: View) {
        view.findViewById<View>(R.id.timerStartButton).visibility = if (timerRunning) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.timerResetButton).visibility = if (timerRunning) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.timerPauseButton).visibility = if (timerRunning) View.VISIBLE else View.GONE
    }

    private fun addTimerTime(view: View, deltaSeconds: Long) {
        timerRemainingSeconds = (timerRemainingSeconds + deltaSeconds).coerceAtLeast(0)
        updateTimerDisplay(view)
        if (timerRunning) {
            runCountdown(view)
        }
    }

    private fun startTimerCountdown(view: View) {
        if (timerRemainingSeconds <= 0) return
        timerRunning = true
        runCountdown(view)
        updateTimerButtons(view)
    }

    private fun pauseTimer(view: View) {
        countDownTimer?.cancel()
        countDownTimer = null
        timerRunning = false
        updateTimerButtons(view)
    }

    private fun runCountdown(view: View) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timerRemainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                timerRemainingSeconds = millisUntilFinished / 1000
                updateTimerDisplay(view)
            }

            override fun onFinish() {
                timerRemainingSeconds = 0
                timerRunning = false
                updateTimerDisplay(view)
                updateTimerButtons(view)
            }
        }.start()
    }

    private fun resetTimer(view: View) {
        cancelTimer()
        updateTimerDisplay(view)
        updateTimerButtons(view)
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        timerRunning = false
        timerRemainingSeconds = 0
    }

    // --- Stopwatch (counts up) ---

    private fun updateStopwatchDisplay(view: View) {
        val seconds = stopwatchSeconds
        view.findViewById<TextView>(R.id.stopwatchDisplayText).text =
            "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun updateStopwatchButtons(view: View) {
        view.findViewById<View>(R.id.stopwatchStartButton).visibility = if (stopwatchRunning) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.stopwatchResetButton).visibility = if (stopwatchRunning) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.stopwatchPauseButton).visibility = if (stopwatchRunning) View.VISIBLE else View.GONE
    }

    private fun startStopwatch(view: View) {
        if (stopwatchRunning) return
        stopwatchRunning = true
        val runnable = object : Runnable {
            override fun run() {
                stopwatchSeconds++
                updateStopwatchDisplay(view)
                stopwatchHandler.postDelayed(this, 1000)
            }
        }
        stopwatchRunnable = runnable
        stopwatchHandler.postDelayed(runnable, 1000)
        updateStopwatchButtons(view)
    }

    private fun pauseStopwatch(view: View) {
        stopwatchRunnable?.let { stopwatchHandler.removeCallbacks(it) }
        stopwatchRunnable = null
        stopwatchRunning = false
        updateStopwatchButtons(view)
    }

    private fun resetStopwatch(view: View) {
        cancelStopwatch()
        updateStopwatchDisplay(view)
        updateStopwatchButtons(view)
    }

    private fun cancelStopwatch() {
        stopwatchRunnable?.let { stopwatchHandler.removeCallbacks(it) }
        stopwatchRunnable = null
        stopwatchRunning = false
        stopwatchSeconds = 0
    }

    // --- Study time: persistent daily counter shown under the date row ---

    private fun initStudyTimer(view: View) {
        serviceScope.launch {
            finalizeStudyDayIfRolledOver()
            if (!studyRunning) {
                studySeconds = studyTimeRepository.getSeconds(studyDate)
            }
            updateStudyTimeDisplay(view)
            updateStudyTimeButton(view)
        }
    }

    private suspend fun finalizeStudyDayIfRolledOver() {
        val today = LocalDate.now()
        if (studyDate != today) {
            studyTimeRepository.setSeconds(studyDate, studySeconds)
            studyDate = today
            studySeconds = studyTimeRepository.getSeconds(today)
            studyLastCheckpointKey = ""
        }
    }

    private fun updateStudyTimeDisplay(view: View) {
        val h = studySeconds / 3600
        val m = (studySeconds % 3600) / 60
        val s = studySeconds % 60
        view.findViewById<TextView>(R.id.studyTimeText).text =
            "오늘 공부시간 %02d:%02d:%02d".format(h, m, s)
    }

    private fun updateStudyTimeButton(view: View) {
        view.findViewById<TextView>(R.id.studyTimeToggleButton).text = if (studyRunning) "일시정지" else "시작"
    }

    private fun toggleStudyTimer(view: View) {
        if (studyRunning) pauseStudyTimer(view) else startStudyTimer(view)
    }

    private fun startStudyTimer(view: View) {
        if (studyRunning) return
        studyRunning = true
        val runnable = object : Runnable {
            override fun run() {
                tickStudyTimer(view)
                studyHandler.postDelayed(this, 1000)
            }
        }
        studyRunnable = runnable
        studyHandler.postDelayed(runnable, 1000)
        updateStudyTimeButton(view)
    }

    private fun tickStudyTimer(view: View) {
        val today = LocalDate.now()
        if (today != studyDate) {
            val finishedDate = studyDate
            val finishedSeconds = studySeconds
            serviceScope.launch { studyTimeRepository.setSeconds(finishedDate, finishedSeconds) }
            studyDate = today
            studySeconds = 0
            studyLastCheckpointKey = ""
        }

        studySeconds++
        updateStudyTimeDisplay(view)

        val now = LocalTime.now()
        if (now.hour == 23 && now.minute == 59) {
            val checkpointKey = "$studyDate-2359"
            if (studyLastCheckpointKey != checkpointKey) {
                studyLastCheckpointKey = checkpointKey
                persistStudyTime()
            }
        } else if (studySeconds % 10L == 0L) {
            persistStudyTime()
        }
    }

    private fun persistStudyTime() {
        val date = studyDate
        val seconds = studySeconds
        serviceScope.launch { studyTimeRepository.setSeconds(date, seconds) }
    }

    /** Stops ticking without resetting the accumulated seconds. Safe to call with no view (e.g. on collapse). */
    private fun pauseStudyTimer(view: View? = null) {
        studyRunnable?.let { studyHandler.removeCallbacks(it) }
        studyRunnable = null
        val wasRunning = studyRunning
        studyRunning = false
        if (wasRunning) persistStudyTime()
        view?.let { updateStudyTimeButton(it) }
    }

    // --- Data loading ---

    private fun loadDday(view: View) {
        val ddayContainer = view.findViewById<LinearLayout>(R.id.ddayContainer)
        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            currentStyle = settings.popupStyle
            applyPopupStyle(view)
            val ddayItems = ddayRepository.itemsFlow.first()
            renderDday(ddayContainer, ddayItems)
        }
    }

    private fun loadTimetableAndMeal(view: View) {
        val timetableContainer = view.findViewById<LinearLayout>(R.id.timetableContainer)
        val mealContainer = view.findViewById<LinearLayout>(R.id.mealContainer)
        val dateLabelText = view.findViewById<TextView>(R.id.dateLabelText)
        val lastUpdatedText = view.findViewById<TextView>(R.id.lastUpdatedText)

        dateLabelText.text = formatDateLabel(viewingDate)
        dateLabelText.setTextColor(currentStyle.headerTextColor.toInt())

        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val date = viewingDate

            if (settings.timetableSource == TimetableSource.MANUAL) {
                val subjects = timetableSubjectRepository.subjectsFlow.first()
                val slots = timetableRepository.entriesFlow.first()
                    .filter { it.dayOfWeek == date.dayOfWeek.value }
                    .sortedBy { it.period }
                    .mapNotNull { entry ->
                        val subject = subjects.find { it.id == entry.subjectId } ?: return@mapNotNull null
                        TimetableSlot(period = entry.period, subject = subject.name, isMovingClass = subject.isMovingClass)
                    }
                if (slots.isEmpty()) renderMessage(timetableContainer, "등록된 시간표가 없습니다.")
                else renderTimetable(timetableContainer, slots)
            } else if (settings.isConfigured) {
                schoolDataRepository.getTimetableForDate(settings, date)
                    .onSuccess { slots ->
                        if (slots.isEmpty()) renderMessage(timetableContainer, "시간표가 없습니다.")
                        else renderTimetable(timetableContainer, slots)
                    }
                    .onFailure { renderMessage(timetableContainer, "시간표를 불러오지 못했습니다.") }
            } else {
                renderMessage(timetableContainer, "설정에서 학교 정보를 입력해주세요.")
            }

            if (settings.isConfigured) {
                schoolDataRepository.getMealsForDate(settings, date)
                    .onSuccess { meals ->
                        if (meals.isEmpty()) renderMessage(mealContainer, "급식 정보가 없습니다.")
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
        val density = resources.displayMetrics.density
        slots.forEach { slot ->
            val tv = TextView(this)
            tv.text = if (slot.isMovingClass) {
                "${slot.period}교시  ${slot.subject} (이동수업)"
            } else {
                "${slot.period}교시  ${slot.subject}"
            }
            tv.textSize = 14f
            tv.setTextColor(currentStyle.bodyTextColor.toInt())
            if (slot.isMovingClass) {
                tv.setTypeface(null, Typeface.BOLD)
                tv.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8f * density
                    setColor(movingClassHighlight)
                }
                val hPad = (8 * density).toInt()
                val vPad = (4 * density).toInt()
                tv.setPadding(hPad, vPad, hPad, vPad)
            } else {
                tv.background = null
                tv.setPadding(0, 4, 0, 4)
            }
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
        countDownTimer?.cancel()
        stopwatchHandler.removeCallbacksAndMessages(null)
        studyHandler.removeCallbacksAndMessages(null)
        removeBubble()
        popupView?.let { runCatching { windowManager.removeView(it) } }
        serviceJob.cancel()
    }

    companion object {
        const val ACTION_STOP = "com.jake.popupschool.action.STOP"
        private const val NOTIFICATION_ID = 42
    }
}
