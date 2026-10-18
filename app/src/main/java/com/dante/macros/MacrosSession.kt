package com.dante.macros

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MacrosSession(ctx: Context) : VoiceInteractionSession(ctx) {

    private val ui = Handler(Looper.getMainLooper())

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

        // Touch block active? Power-hold removes it instead of opening the picker.
        val svc = MacrosAccessibility.instance
        if (svc != null && svc.isBlocking) {
            svc.perform("BLOCK_TOUCHES")
            ui.post { hide() }
            return
        }

        val list = Store.shortcuts(context)
        val primary = list.firstOrNull { it.id == Store.primaryId(context) }
        if (Store.mode(context) == "primary" && primary != null) {
            setContentView(FrameLayout(context))
            pick(primary)
            return
        }
        showPicker(list)
    }

    private fun pick(s: Shortcut) {
        if (s.kind == Kind.ACTION) {
            hide()
            ui.postDelayed({ Runner.run(context, s) }, 350)
        } else {
            Runner.run(context, s)
            ui.post { hide() }
        }
    }

    private fun showPicker(list: List<Shortcut>) {
        val d = context.resources.displayMetrics
        fun dp(x: Int) = (x * d.density).toInt()

        fun row(t: String, click: () -> Unit) = TextView(context).apply {
            text = t
            textSize = 17f
            setTextColor(0xFFE2E8F0.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            background = GradientDrawable().apply {
                setColor(0xFF0F172A.toInt())
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(8) }
            setOnClickListener { click() }
        }

        val dim = FrameLayout(context).apply {
            setBackgroundColor(0x99000000.toInt())
            setOnClickListener { hide() }
        }
        val r = dp(24).toFloat()
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF1E293B.toInt())
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding(dp(16), dp(16), dp(16), dp(36))
            isClickable = true
        }
        card.addView(TextView(context).apply {
            text = "🔧 Macros"
            textSize = 18f
            setTextColor(0xFFFBBF24.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(4), 0, 0, dp(8))
        })

        val maxH = (d.heightPixels * 0.6f).toInt()
        val sv = object : ScrollView(context) {
            override fun onMeasure(w: Int, h: Int) {
                super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST))
            }
        }
        val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        list.forEach { s -> rows.addView(row("${s.emoji}  ${s.label}") { pick(s) }) }
        rows.addView(row("⚙️  Macros settings") {
            hide()
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        })
        sv.addView(rows)
        card.addView(sv)
        dim.addView(card, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(dim)
    }
}
