package com.example.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.pref.AppPreferences
import com.example.data.repository.ChatRepository
import com.example.ui.floating.FloatingBubbleContent
import com.example.ui.floating.FloatingChatContent
import com.example.ui.theme.PandaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingWindowManager(
    private val context: Context,
    private val onCloseRequested: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val preferences = AppPreferences(context)
    private val repository = ChatRepository.getInstance(context)
    private val serviceLifecycleOwner = ServiceLifecycleOwner()

    private var bubbleView: ComposeView? = null
    private var chatWindowView: ComposeView? = null

    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var chatLayoutParams: WindowManager.LayoutParams? = null

    private var isChatOpen = false
    private var isExpanded = false

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun initialize() {
        serviceLifecycleOwner.onCreate()
        showBubble()
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    fun showBubble() {
        if (bubbleView != null) return

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val initialX = preferences.lastBubbleX.coerceIn(0, screenWidth - 160)
        val initialY = preferences.lastBubbleY.coerceIn(50, screenHeight - 200)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        bubbleLayoutParams = params

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(serviceLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(serviceLifecycleOwner)
            setViewTreeViewModelStoreOwner(serviceLifecycleOwner)
            setContent {
                PandaTheme {
                    FloatingBubbleContent()
                }
            }
        }

        setupBubbleTouchListener(composeView, params, screenWidth)

        try {
            windowManager.addView(composeView, params)
            bubbleView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBubbleTouchListener(
        view: View,
        params: WindowManager.LayoutParams,
        screenWidth: Int
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchStartTime = 0L

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)
                    val duration = System.currentTimeMillis() - touchStartTime

                    if (deltaX < 15 && deltaY < 15 && duration < 300) {
                        // Click detected -> Open Chat Window
                        openChatWindow()
                    } else {
                        // Snap to nearest edge
                        val bubbleWidth = view.width.coerceAtLeast(160)
                        val targetX = if (params.x + bubbleWidth / 2 < screenWidth / 2) {
                            16 // Snap to left
                        } else {
                            screenWidth - bubbleWidth - 16 // Snap to right
                        }

                        animateBubbleSnap(view, params, targetX)
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun animateBubbleSnap(
        view: View,
        params: WindowManager.LayoutParams,
        targetX: Int
    ) {
        val startX = params.x
        val animator = ValueAnimator.ofInt(startX, targetX)
        animator.duration = 200
        animator.addUpdateListener { anim ->
            params.x = anim.animatedValue as Int
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                // Ignore if view was removed
            }
        }
        animator.start()

        preferences.lastBubbleX = targetX
        preferences.lastBubbleY = params.y
    }

    fun openChatWindow() {
        if (isChatOpen) return
        removeBubble()

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val windowWidth = (screenWidth * 0.92f).toInt().coerceAtMost(dpToPx(520, displayMetrics))
        val targetHeight = if (isExpanded) (screenHeight * 0.85f).toInt() else (screenHeight * 0.65f).toInt()

        val params = WindowManager.LayoutParams(
            windowWidth,
            targetHeight,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        chatLayoutParams = params

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(serviceLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(serviceLifecycleOwner)
            setViewTreeViewModelStoreOwner(serviceLifecycleOwner)
            setContent {
                PandaTheme {
                    FloatingChatContent(
                        repository = repository,
                        onMinimize = { minimizeChatWindow() },
                        onClose = {
                            destroy()
                            onCloseRequested()
                        },
                        onToggleExpand = { toggleExpandChat() },
                        isExpanded = isExpanded
                    )
                }
            }
        }

        composeView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                if (preferences.autoMinimizeOnOutsideTap) {
                    minimizeChatWindow()
                    return@setOnTouchListener true
                }
            }
            false
        }

        try {
            windowManager.addView(composeView, params)
            chatWindowView = composeView
            isChatOpen = true
        } catch (e: Exception) {
            e.printStackTrace()
            showBubble()
        }
    }

    fun minimizeChatWindow() {
        if (!isChatOpen) return
        removeChatWindow()
        showBubble()
    }

    fun toggleExpandChat() {
        if (!isChatOpen || chatWindowView == null || chatLayoutParams == null) return
        isExpanded = !isExpanded

        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val targetHeight = if (isExpanded) (screenHeight * 0.85f).toInt() else (screenHeight * 0.65f).toInt()

        chatLayoutParams?.height = targetHeight
        try {
            windowManager.updateViewLayout(chatWindowView, chatLayoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeBubble() {
        bubbleView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bubbleView = null
        }
    }

    private fun removeChatWindow() {
        chatWindowView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            chatWindowView = null
            isChatOpen = false
        }
    }

    fun destroy() {
        removeBubble()
        removeChatWindow()
        serviceLifecycleOwner.onDestroy()
    }

    private fun dpToPx(dp: Int, metrics: DisplayMetrics): Int {
        return (dp * metrics.density).toInt()
    }
}
