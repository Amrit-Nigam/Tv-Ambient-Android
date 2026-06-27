package com.tvport.dashboard.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tvport.dashboard.MainActivity

/**
 * Relaunches the dashboard after the TV reboots (BUILD SPEC §11). Registered for
 * BOOT_COMPLETED in the manifest with RECEIVE_BOOT_COMPLETED.
 *
 * Manufacturer caveat: some Android TV / Google TV builds (and Android 10+ background
 * activity-start limits) restrict launching an Activity directly from a boot receiver. On a
 * dedicated wall display this generally works; if a given TV blocks it, set the app as the
 * device's default "start on boot" app in its settings, or use a leanback launcher shortcut.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i("BootReceiver", "BOOT_COMPLETED received — relaunching dashboard")
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(launch) }
            .onFailure { Log.w("BootReceiver", "Could not auto-launch on boot: ${it.message}") }
    }
}
