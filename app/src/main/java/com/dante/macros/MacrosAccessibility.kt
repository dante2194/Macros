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
import android.widget.TextView

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
        val v = FrameLayout(this).apply {
            setBackgroundColor(0x66000000)
            isClickable = true
            setOnTouchListener { _, _ -> true }
            addView(
                TextView(context).apply {
                    text = "🔧 Touch blocked · hold power to unblock"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ).apply { bottomMargin = 160 }
            )
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
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
