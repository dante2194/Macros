package com.dante.macros

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val cBg = 0xFF0F172A.toInt()
    private val cCard = 0xFF1E293B.toInt()
    private val cTxt = 0xFFE2E8F0.toInt()
    private val cSub = 0xFF94A3B8.toInt()
    private val cAmb = 0xFFFBBF24.toInt()

    private lateinit var body: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(40))
        }
        val sv = ScrollView(this).apply {
            setBackgroundColor(cBg)
            fitsSystemWindows = true
            addView(body)
        }
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ---------- UI helpers ----------
    private fun tv(t: String, size: Float = 15f, color: Int = cTxt, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(cCard)
            cornerRadius = dp(16).toFloat()
        }
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }

    private fun chip(t: String, on: Boolean = false, click: () -> Unit) = TextView(this).apply {
        text = t
        textSize = 14f
        setTextColor(if (on) cBg else cAmb)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(if (on) cAmb else 0x00000000)
            setStroke(dp(1), cAmb)
            cornerRadius = dp(20).toFloat()
        }
        setPadding(dp(14), dp(8), dp(14), dp(8))
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply {
            rightMargin = dp(8)
            topMargin = dp(8)
        }
    }

    private fun icon(t: String, click: () -> Unit) = TextView(this).apply {
        text = t
        textSize = 20f
        setTextColor(cAmb)
        setPadding(dp(12), dp(6), dp(12), dp(6))
        setOnClickListener { click() }
    }

    private fun status(ok: Boolean, name: String, hint: String, click: () -> Unit): View {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            setOnClickListener { click() }
        }
        r.addView(tv(if (ok) "✅" else "⚠️", 18f))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        col.addView(tv(name, 15f, cTxt, true))
        col.addView(tv(hint, 12f, cSub))
        r.addView(col)
        return r
    }

    // ---------- Screen ----------
    private fun render() {
        body.removeAllViews()
        body.addView(tv("🔧 Macros", 28f, cAmb, true))
        body.addView(tv("Power-button shortcuts", 14f, cSub))

        val setup = card()
        setup.addView(tv("Setup (tap a row)", 16f, cTxt, true))
        setup.addView(status(isAssistant(), "Default assistant", "Choose Macros as your digital assistant") {
            openAssistantSettings()
        })
        setup.addView(status(MacrosAccessibility.instance != null, "Accessibility service", "For lock, screenshot, touch block") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        setup.addView(status(Settings.canDrawOverlays(this), "Display over other apps", "Helps launch apps from the power button") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        })
        setup.addView(
            tv(
                "Greyed-out switch? Settings → Apps → Macros → ⋮ → Allow restricted settings. " +
                    "Also make sure your phone's power-button hold is set to the assistant.",
                12f, cSub
            )
        )
        body.addView(setup)

        val list = Store.shortcuts(this)
        val pid = Store.primaryId(this)
        val mode = Store.mode(this)

        val hold = card()
        hold.addView(tv("When I hold power", 16f, cTxt, true))
        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        modeRow.addView(chip("Show picker", mode == "picker") {
            Store.setMode(this, "picker"); render()
        })
        modeRow.addView(chip("Run primary ★", mode == "primary") {
            Store.setMode(this, "primary"); render()
        })
        hold.addView(modeRow)
        val pName = list.firstOrNull { it.id == pid }?.let { "${it.emoji} ${it.label}" } ?: "none"
        hold.addView(tv("Primary: $pName", 12f, cSub))
        body.addView(hold)

        val lc = card()
        lc.addView(tv("Shortcuts", 16f, cTxt, true))
        list.forEach { s ->
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            col.addView(tv("${s.emoji}  ${s.label}${if (s.id == pid) "  ★" else ""}", 16f, cTxt, true))
            col.addView(tv(describe(s), 11f, cSub))
            r.addView(col)
            r.addView(icon("▶") { Runner.run(this, s) })
            r.addView(icon("★") { Store.setPrimary(this, s.id); render() })
            r.addView(icon("🗑") {
                AlertDialog.Builder(this).setTitle("Delete ${s.label}?")
                    .setPositiveButton("Delete") { _, _ ->
                        Store.save(this, Store.shortcuts(this).filter { it.id != s.id })
                        render()
                    }
                    .setNegativeButton("Cancel", null).show()
            })
            lc.addView(r)
        }
        lc.addView(chip("＋ Add shortcut", true) { addFlow() })
        body.addView(lc)
    }

    private fun describe(s: Shortcut): String = when (s.kind) {
        Kind.APP -> s.pkg
        Kind.ACTIVITY -> "${s.pkg} / ${s.cls.substringAfterLast('.')}"
        Kind.URL, Kind.URI -> s.data
        Kind.ACTION -> "Action · ${s.data}"
    }

    // ---------- Role / settings ----------
    private fun isAssistant(): Boolean = try {
        (getSystemService(Context.ROLE_SERVICE) as RoleManager).isRoleHeld("android.app.role.ASSISTANT")
    } catch (e: Exception) {
        false
    }

    private fun openAssistantSettings() {
        try {
            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, "Open Settings → Apps → Default apps → Digital assistant", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------- Add shortcut ----------
    private fun addFlow() {
        val opts = arrayOf("📱 App", "🧩 App activity", "🌐 URL", "🧷 Intent URI", "⚙️ Action")
        AlertDialog.Builder(this).setTitle("Add shortcut").setItems(opts) { _, i ->
            when (i) {
                0 -> pickApp { n, p -> commit("📱", n, Kind.APP, p) }
                1 -> pickApp { n, p -> pickActivity(n, p) }
                2 -> askText("URL", "https://") { u -> commit("🌐", u.removePrefix("https://"), Kind.URL, data = u) }
                3 -> askText("Intent URI (intent:#Intent;…;end)", "") { u -> commit("🧷", "Intent", Kind.URI, data = u) }
                4 -> pickAction()
            }
        }.show()
    }

    private fun commit(emoji: String, label: String, kind: Kind, pkg: String = "", cls: String = "", data: String = "") {
        askText("Name", label) { name ->
            val l = Store.shortcuts(this).toMutableList()
            l.add(Shortcut(Store.newId(), emoji, name.ifBlank { label }, kind, pkg, cls, data))
            Store.save(this, l)
            render()
        }
    }

    @Suppress("DEPRECATION")
    private fun pickApp(cb: (String, String) -> Unit) {
        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
        AlertDialog.Builder(this).setTitle("Pick app")
            .setItems(apps.map { it.first }.toTypedArray()) { _, i -> cb(apps[i].first, apps[i].second) }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun pickActivity(appName: String, pkg: String) {
        val acts = try {
            packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES).activities
                ?.filter { it.exported && it.enabled }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        if (acts.isEmpty()) {
            Toast.makeText(this, "No launchable activities found", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this).setTitle("$appName activities")
            .setItems(acts.map { it.removePrefix("$pkg.") }.toTypedArray()) { _, i ->
                commit("🧩", "$appName · ${acts[i].substringAfterLast('.')}", Kind.ACTIVITY, pkg, acts[i])
            }.show()
    }

    private fun pickAction() {
        val a = listOf(
            Triple("🖐", "Touch block", "BLOCK_TOUCHES"),
            Triple("🔒", "Lock screen", "LOCK"),
            Triple("📸", "Screenshot", "SCREENSHOT"),
            Triple("⏻", "Power menu", "POWER_MENU"),
            Triple("🔔", "Notifications", "NOTIFICATIONS"),
            Triple("⚡", "Quick settings", "QUICK_SETTINGS"),
            Triple("🏠", "Home", "HOME"),
            Triple("◀", "Back", "BACK"),
            Triple("🗂", "Recents", "RECENTS")
        )
        AlertDialog.Builder(this).setTitle("Action")
            .setItems(a.map { "${it.first}  ${it.second}" }.toTypedArray()) { _, i ->
                commit(a[i].first, a[i].second, Kind.ACTION, data = a[i].third)
            }.show()
    }

    private fun askText(title: String, def: String, ok: (String) -> Unit) {
        val e = EditText(this).apply { setText(def) }
        AlertDialog.Builder(this).setTitle(title).setView(e)
            .setPositiveButton("OK") { _, _ -> ok(e.text.toString().trim()) }
            .setNegativeButton("Cancel", null).show()
    }
}
