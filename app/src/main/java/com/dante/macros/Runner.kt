package com.dante.macros

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Runner {
    private fun toast(c: Context, m: String) = Toast.makeText(c, m, Toast.LENGTH_LONG).show()

    fun run(ctx: Context, s: Shortcut) {
        try {
            when (s.kind) {
                Kind.APP -> {
                    val i = ctx.packageManager.getLaunchIntentForPackage(s.pkg)
                    if (i == null) toast(ctx, "App not installed: ${s.pkg}")
                    else ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                Kind.ACTIVITY -> ctx.startActivity(
                    Intent().setClassName(s.pkg, s.cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Kind.URL -> ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(s.data)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Kind.URI -> ctx.startActivity(
                    Intent.parseUri(s.data, Intent.URI_INTENT_SCHEME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Kind.ACTION -> {
                    val svc = MacrosAccessibility.instance
                    if (svc == null) toast(ctx, "Turn on the Macros accessibility service first")
                    else svc.perform(s.data)
                }
            }
        } catch (e: Exception) {
            toast(ctx, "Failed: ${e.message}")
        }
    }
}
