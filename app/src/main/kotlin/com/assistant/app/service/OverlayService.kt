package com.assistant.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.assistant.app.R
import com.assistant.app.ai.ChatMessage
import com.assistant.app.ai.ConversationManager
import com.assistant.app.ai.MessageRole
import com.assistant.app.data.AppPreferences
import com.assistant.app.utils.Constants
import com.assistant.app.utils.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var prefs: AppPreferences
    private lateinit var convManager: ConversationManager
    private lateinit var speechSvc: SpeechService
    private lateinit var notifHelper: NotificationHelper
    private val handler = Handler(Looper.getMainLooper())

    private var bubbleView: View? = null
    private var chatView: View? = null
    private var chatParams: WindowManager.LayoutParams? = null

    private var isChatOpen = false
    private var isCalibrating = false
    private var calibrationStartTime = 0L
    private val calibrationDurationMs = 5000L

    // Drag state
    private var isWindowDragEnabled = false
    private var longPressRunnable: Runnable? = null
    private val longPressDurationMs = 3000L
    private var longPressDownX = 0f
    private var longPressDownY = 0f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var msgContainer: LinearLayout? = null
    private var scrollView: ScrollView? = null
    private var statusTv: TextView? = null
    private var progressBar: ProgressBar? = null
    private var partialTv: TextView? = null
    private var speakerTv: TextView? = null
    private var dragHintTv: TextView? = null

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Constants.ACTION_STOP_OVERLAY) stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        val hasAudio = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudio) { stopSelf(); return }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = AppPreferences(this)
        convManager = ConversationManager(prefs)
        speechSvc = SpeechService(this)
        notifHelper = NotificationHelper(this)

        val filter = IntentFilter(Constants.ACTION_STOP_OVERLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopReceiver, filter)
        }

        try {
            startForeground(
                Constants.OVERLAY_NOTIFICATION_ID,
                notifHelper.buildNotification()
            )
        } catch (e: SecurityException) { stopSelf(); return }

        createBubble()
        observeConversation()
    }

    // ── Screen measurement ──

    private fun getScreenMetrics(): ScreenMetrics {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(dm)

        val statusBarH = getBarHeight("status_bar_height")
        val navBarH = getBarHeight("navigation_bar_height")
        val usableH = dm.heightPixels - statusBarH - navBarH

        return ScreenMetrics(
            fullWidth = dm.widthPixels,
            fullHeight = dm.heightPixels,
            usableHeight = usableH,
            statusBarHeight = statusBarH,
            navBarHeight = navBarH
        )
    }

    private fun getBarHeight(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    data class ScreenMetrics(
        val fullWidth: Int,
        val fullHeight: Int,
        val usableHeight: Int,
        val statusBarHeight: Int,
        val navBarHeight: Int
    )

    // ── Observe ──

    private fun observeConversation() {
        scope.launch {
            convManager.messages.collect { msgs ->
                if (isChatOpen) renderMessages(msgs)
            }
        }
        scope.launch {
            convManager.isProcessing.collect { busy ->
                progressBar?.visibility = if (busy) View.VISIBLE else View.GONE
                if (busy) statusTv?.text = "🧠 Thinking..."
                else if (isChatOpen) statusTv?.text = "🔴 Listening..."
            }
        }
        scope.launch {
            convManager.speakerStatus.collect { s ->
                if (s.isNotBlank()) {
                    speakerTv?.text = s
                    speakerTv?.visibility = View.VISIBLE
                }
            }
        }
    }

    // ── Bubble ──

    @SuppressLint("ClickableViewAccessibility")
    private fun createBubble() {
        val size = Constants.BUBBLE_SIZE_DP.dpToPx(this)
        val screen = getScreenMetrics()

        val iv = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble)
            setBackgroundResource(R.drawable.bg_bubble)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val p = 14.dpToPx(this@OverlayService)
            setPadding(p, p, p, p)
        }

        val params = WindowManager.LayoutParams(
            size, size, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = screen.statusBarHeight + 10.dpToPx(this@OverlayService)
        }

        var startX = 0; var startY = 0
        var touchX = 0f; var touchY = 0f
        var isClick = true
        var lastClickTime = 0L

        iv.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = ev.rawX; touchY = ev.rawY
                    isClick = true; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - touchX
                    val dy = ev.rawY - touchY
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) isClick = false
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    try { wm.updateViewLayout(iv, params) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < 400) stopSelf()
                        else { lastClickTime = now; toggleChat() }
                    }; true
                }
                else -> false
            }
        }

        bubbleView = iv
        try { wm.addView(iv, params) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun toggleChat() { if (isChatOpen) closeChat() else openChat() }

    // ── Open Chat Window ──

    @SuppressLint("InflateParams")
    private fun openChat() {
        if (isChatOpen) return

        val screen = getScreenMetrics()
        val windowW = (screen.fullWidth * Constants.OVERLAY_WIDTH_RATIO).toInt()
        val windowH = (screen.usableHeight * Constants.OVERLAY_HEIGHT_RATIO).toInt()

        // Initial X: center horizontally
        val initX = (screen.fullWidth - windowW) / 2
        // Initial Y: center in safe area
        val initY = screen.statusBarHeight + (screen.usableHeight - windowH) / 2

        chatParams = WindowManager.LayoutParams(
            windowW, windowH, overlayType,
            // Use NOT_FOCUSABLE so touches pass through to bubble/other apps
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            // IMPORTANT: Use TOP|START so x/y coordinates work correctly for dragging
            gravity = Gravity.TOP or Gravity.START
            x = initX
            y = initY
            alpha = prefs.overlayTransparency
        }

        chatView = LayoutInflater.from(this)
            .inflate(R.layout.overlay_window, null, false)

        bindViews()
        setupWindowDrag()

        try {
            wm.addView(chatView, chatParams)
            isChatOpen = true
            renderMessages(convManager.messages.value)
            if (!convManager.isCalibrationDone()) startCalibration()
            else startListening()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── Window Drag Logic ──

    @SuppressLint("ClickableViewAccessibility")
    private fun setupWindowDrag() {
        val view = chatView ?: return

        var dragStartRawX = 0f
        var dragStartRawY = 0f
        var dragStartParamX = 0
        var dragStartParamY = 0
        var didMove = false

        view.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartRawX = ev.rawX
                    dragStartRawY = ev.rawY
                    dragStartParamX = chatParams?.x ?: 0
                    dragStartParamY = chatParams?.y ?: 0
                    didMove = false
                    longPressDownX = ev.rawX
                    longPressDownY = ev.rawY

                    if (!isWindowDragEnabled) {
                        // Start long press countdown
                        startLongPress()
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - dragStartRawX
                    val dy = ev.rawY - dragStartRawY
                    val moved = kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10

                    if (moved && !isWindowDragEnabled) {
                        // Finger moved before long press fired — cancel it
                        cancelLongPress()
                    }

                    if (isWindowDragEnabled && moved) {
                        didMove = true
                        moveWindow(dragStartParamX + dx.toInt(), dragStartParamY + dy.toInt())
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (!isWindowDragEnabled) {
                        cancelLongPress()
                    } else if (!didMove) {
                        // Tap while in drag mode → exit drag mode
                        disableDragMode()
                    }
                    didMove = false
                    true
                }

                else -> false
            }
        }
    }

    private fun moveWindow(newX: Int, newY: Int) {
        val screen = getScreenMetrics()
        val p = chatParams ?: return
        val windowW = p.width
        val windowH = p.height

        // Clamp X: keep window fully on screen horizontally
        val clampedX = newX.coerceIn(0, screen.fullWidth - windowW)

        // Clamp Y: keep window within safe area (below status bar, above nav bar)
        val minY = screen.statusBarHeight
        val maxY = screen.statusBarHeight + screen.usableHeight - windowH
        val clampedY = newY.coerceIn(minY, maxY)

        p.x = clampedX
        p.y = clampedY

        try { wm.updateViewLayout(chatView, p) } catch (_: Exception) {}
    }

    private fun startLongPress() {
        cancelLongPress()
        longPressRunnable = Runnable {
            enableDragMode()
        }
        handler.postDelayed(longPressRunnable!!, longPressDurationMs)
    }

    private fun cancelLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun enableDragMode() {
        isWindowDragEnabled = true
        showDragHint()
        chatView?.setBackgroundResource(R.drawable.bg_chat_window_drag)

        // Switch to focusable so drag touches register properly
        chatParams?.let { p ->
            p.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            try { wm.updateViewLayout(chatView, p) } catch (_: Exception) {}
        }
    }

    private fun disableDragMode() {
        isWindowDragEnabled = false
        hideDragHint()
        chatView?.setBackgroundResource(R.drawable.bg_chat_window)

        // Restore flags
        chatParams?.let { p ->
            p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            try { wm.updateViewLayout(chatView, p) } catch (_: Exception) {}
        }
    }

    private fun showDragHint() {
        if (dragHintTv != null) return
        dragHintTv = TextView(this).apply {
            text = "✋ Drag to move  •  Tap to lock"
            textSize = 10f
            setTextColor(android.graphics.Color.parseColor("#FFD166"))
            gravity = Gravity.CENTER
            val pd = 6.dpToPx(this@OverlayService)
            setPadding(pd, pd / 2, pd, pd / 2)
            setBackgroundColor(android.graphics.Color.parseColor("#DD1A1A2C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        msgContainer?.addView(dragHintTv, 0)
    }

    private fun hideDragHint() {
        dragHintTv?.let {
            try { msgContainer?.removeView(it) } catch (_: Exception) {}
        }
        dragHintTv = null
        scrollView?.post { scrollView?.fullScroll(View.FOCUS_DOWN) }
    }

    // ── Views ──

    private fun bindViews() {
        chatView?.let { v ->
            msgContainer = v.findViewById(R.id.msgContainer)
            scrollView = v.findViewById(R.id.scrollView)
            statusTv = v.findViewById(R.id.statusText)
            progressBar = v.findViewById(R.id.progressBar)
            v.findViewById<View>(R.id.btnClose)?.setOnClickListener { closeChat() }

            val header = statusTv?.parent as? LinearLayout
            if (header != null) {
                speakerTv = TextView(this).apply {
                    textSize = 9f
                    setTextColor(android.graphics.Color.parseColor("#06D6A0"))
                    visibility = View.GONE
                    val pd = 4.dpToPx(this@OverlayService)
                    setPadding(pd, 0, pd, 0)
                }
                header.addView(speakerTv, header.indexOfChild(statusTv) + 1)
            }
        }
    }

    private fun closeChat() {
        cancelLongPress()
        isWindowDragEnabled = false
        isCalibrating = false
        handler.removeCallbacksAndMessages(null)
        speechSvc.stopContinuous()
        chatView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        chatView = null; chatParams = null
        msgContainer = null; scrollView = null
        statusTv = null; progressBar = null
        partialTv = null; speakerTv = null; dragHintTv = null
        isChatOpen = false
    }

    // ── Calibration ──

    private fun startCalibration() {
        isCalibrating = true
        calibrationStartTime = System.currentTimeMillis()
        statusTv?.text = "🎙️ Calibrating..."
        showCalibrationMsg()

        speechSvc.startContinuous(
            onResult = { _, vol -> if (isCalibrating) convManager.addCalibrationReading(vol) },
            onPartialResult = {},
            onErr = {},
            onStarted = {},
            onSpeechBegin = {},
            onSpeechEnd = {},
            onVolume = { rms ->
                if (isCalibrating) {
                    convManager.addCalibrationReading(rms)
                    val remaining = ((calibrationDurationMs -
                            (System.currentTimeMillis() - calibrationStartTime)) / 1000).toInt()
                    if (remaining > 0) statusTv?.text = "🎙️ Speak... ${remaining}s"
                }
            }
        )
        handler.postDelayed({ finishCalibration() }, calibrationDurationMs)
    }

    private fun showCalibrationMsg() {
        val tv = TextView(this).apply {
            text = "🎙️ CALIBRATION\nSay a few words at your normal volume."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#FFD166"))
            val pd = 10.dpToPx(this@OverlayService)
            setPadding(pd, pd, pd, pd)
            setBackgroundColor(android.graphics.Color.parseColor("#33FFD166"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { val m = 4.dpToPx(this@OverlayService); setMargins(m, m, m, m) }
            tag = "cal_msg"
        }
        msgContainer?.addView(tv)
    }

    private fun finishCalibration() {
        isCalibrating = false
        speechSvc.stopContinuous()
        msgContainer?.let { c ->
            for (i in c.childCount - 1 downTo 0) {
                if (c.getChildAt(i).tag == "cal_msg") c.removeViewAt(i)
            }
        }
        convManager.finishCalibration()

        val tv = TextView(this).apply {
            text = "✅ Calibrated!\n💡 Hold window 3s to move it"
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#06D6A0"))
            val pd = 8.dpToPx(this@OverlayService)
            setPadding(pd, pd, pd, pd)
            setBackgroundColor(android.graphics.Color.parseColor("#2206D6A0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { val m = 4.dpToPx(this@OverlayService); setMargins(m, m, m, m) }
        }
        msgContainer?.addView(tv)
        handler.postDelayed({
            try { msgContainer?.removeView(tv) } catch (_: Exception) {}
            startListening()
        }, 3000)
    }

    // ── Listening ──

    private fun startListening() {
        statusTv?.text = "🔴 Listening..."

        speechSvc.startContinuous(
            onResult = { text, vol -> removePartial(); convManager.onSpeechDetected(text, vol) },
            onPartialResult = { showPartial(it) },
            onErr = { err ->
                if (err.isNotBlank()) {
                    statusTv?.text = "⚠️ $err"
                    statusTv?.postDelayed({
                        if (isChatOpen) statusTv?.text = "🔴 Listening..."
                    }, 2000)
                }
            },
            onStarted = { if (!isCalibrating) statusTv?.text = "🔴 Listening..." },
            onSpeechBegin = { if (!isCalibrating) statusTv?.text = "🟡 Hearing..." },
            onSpeechEnd = { if (!isCalibrating) statusTv?.text = "⏳ Analyzing..." },
            onVolume = {}
        )
    }

    private fun showPartial(text: String) {
        if (!isChatOpen || msgContainer == null) return
        if (partialTv == null) {
            partialTv = TextView(this).apply {
                textSize = prefs.textSize - 1f
                setTextColor(android.graphics.Color.parseColor("#99AAAAAA"))
                val pd = 8.dpToPx(this@OverlayService)
                setPadding(pd, pd / 2, pd, pd / 2)
                setBackgroundResource(R.drawable.bg_msg_ai)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.START
                    val m = 3.dpToPx(this@OverlayService); setMargins(m, m, m, m)
                }
                maxWidth = (resources.displayMetrics.widthPixels * 0.65).toInt()
            }
            msgContainer?.addView(partialTv)
        }
        partialTv?.text = "🎤 $text..."
        scrollView?.post { scrollView?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun removePartial() {
        partialTv?.let { try { msgContainer?.removeView(it) } catch (_: Exception) {} }
        partialTv = null
    }

    private fun renderMessages(messages: List<ChatMessage>) {
        if (!isChatOpen || msgContainer == null) return
        msgContainer?.removeAllViews()
        partialTv = null
        messages.forEach { msgContainer?.addView(createMsgView(it)) }
        scrollView?.post { scrollView?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun createMsgView(msg: ChatMessage): TextView {
        return TextView(this).apply {
            val isUser = msg.role == MessageRole.USER
            val content = msg.content
            val isSkipped = content.startsWith("🔵")

            text = if (msg.isStreaming) "${content}▌" else content
            textSize = if (isSkipped) prefs.textSize - 2f else prefs.textSize

            setTextColor(when {
                msg.isError -> android.graphics.Color.parseColor("#FF5C7A")
                isSkipped -> android.graphics.Color.parseColor("#666666")
                isUser -> android.graphics.Color.parseColor("#BBBBDD")
                else -> if (prefs.darkText) android.graphics.Color.parseColor("#1A1A1A")
                else android.graphics.Color.WHITE
            })

            val pd = 8.dpToPx(this@OverlayService)
            setPadding(pd, pd / 2, pd, pd / 2)

            if (isSkipped) setBackgroundColor(android.graphics.Color.parseColor("#11FFFFFF"))
            else setBackgroundResource(if (isUser) R.drawable.bg_msg_user else R.drawable.bg_msg_ai)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                val m = 3.dpToPx(this@OverlayService)
                setMargins(m, if (isSkipped) 1 else m, m, if (isSkipped) 1 else m)
            }
            maxWidth = (resources.displayMetrics.widthPixels * 0.70).toInt()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Constants.ACTION_STOP_OVERLAY) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        speechSvc.destroy()
        bubbleView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        chatView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        scope.cancel()
    }
}