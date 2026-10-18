package com.dante.macros

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Kind { APP, ACTIVITY, URL, URI, ACTION }

data class Shortcut(
    val id: String,
    val emoji: String,
    val label: String,
    val kind: Kind,
    val pkg: String = "",
    val cls: String = "",
    val data: String = ""
)

object Store {
    private fun p(c: Context) = c.getSharedPreferences("macros", Context.MODE_PRIVATE)

    fun newId(): String = java.lang.Long.toString(System.nanoTime(), 36)

    fun mode(c: Context): String = p(c).getString("mode", "picker") ?: "picker"
    fun setMode(c: Context, m: String) { p(c).edit().putString("mode", m).apply() }

    fun primaryId(c: Context): String? {
        val id = p(c).getString("primary", null)
        val list = shortcuts(c)
        return if (list.any { it.id == id }) id else list.firstOrNull()?.id
    }
    fun setPrimary(c: Context, id: String) { p(c).edit().putString("primary", id).apply() }

    fun shortcuts(c: Context): List<Shortcut> {
        val raw = p(c).getString("list", null)
        if (raw == null) {
            val d = defaults()
            save(c, d)
            return d
        }
        val a = JSONArray(raw)
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Shortcut(
                o.getString("id"), o.getString("emoji"), o.getString("label"),
                Kind.valueOf(o.getString("kind")),
                o.optString("pkg"), o.optString("cls"), o.optString("data")
            )
        }
    }

    fun save(c: Context, l: List<Shortcut>) {
        val a = JSONArray()
        l.forEach {
            a.put(
                JSONObject().put("id", it.id).put("emoji", it.emoji).put("label", it.label)
                    .put("kind", it.kind.name).put("pkg", it.pkg).put("cls", it.cls).put("data", it.data)
            )
        }
        p(c).edit().putString("list", a.toString()).apply()
    }

    private fun defaults() = listOf(
        Shortcut("claude", "💬", "Claude", Kind.APP, "com.anthropic.claude"),
        Shortcut(
            "bvr", "🎥", "BVR Record", Kind.ACTIVITY,
            "com.arbelsolutions.BVRUltimate", "com.arbelsolutions.BVRUltimate.RecordNowActivity"
        ),
        Shortcut(
            "black", "🌑", "Black Screen", Kind.ACTIVITY,
            "io.japp.blackscreen", "io.japp.blackscreen.ui.ToggleBlackScreenActivity"
        ),
        Shortcut("touch", "🖐", "Touch block", Kind.ACTION, data = "BLOCK_TOUCHES"),
        Shortcut("lock", "🔒", "Lock screen", Kind.ACTION, data = "LOCK"),
        Shortcut("shot", "📸", "Screenshot", Kind.ACTION, data = "SCREENSHOT")
    )
}
