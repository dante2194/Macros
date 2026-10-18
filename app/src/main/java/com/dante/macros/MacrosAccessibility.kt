package com.dante.macros

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout

class MacrosAccessibility : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MacrosAccessibility? = null
    }

    private var overlay: View? = null
    val isBlocking: Boolean get() = overlay != null

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        removeOverlay()
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun perform(action: String) {
        when (action) {
            "BLOCK_TOUCHES" -> toggleBlock()
            "LOCK" -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            "SCREENSHOT" -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            "POWER_MENU" -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            "NOTIFICATIONS" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "QUICK_SETTINGS" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    private fun toggleBlock() {
        if (overlay != null) {
            removeOverlay()
            return
        }
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        // Real physical screen size: includes status bar, nav bar and cutout area
        val dm = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        val sw = dm.widthPixels
        val sh = dm.heightPixels

        // Fully invisible: no background, no text, no dim. Just eats every touch.
        val v = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = false
            setOnTouchListener { _, _ -> true }
            setOnGenericMotionListener { _, _ -> true }
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                post { systemGestureExclusionRects = listOf(android.graphics.Rect(0, 0, sw, sh)) }
            }
        }

        val lp = WindowManager.LayoutParams(
            sw,
            sh,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        wm.addView(v, lp)
        overlay = v
    }

    private fun removeOverlay() {
        overlay?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (_: Exception) {
            }
        }
        overlay = null
    }
}
