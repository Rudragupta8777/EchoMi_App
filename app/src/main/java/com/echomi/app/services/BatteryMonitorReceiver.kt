package com.echomi.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.echomi.app.data.BatteryStatusRequest
import com.echomi.app.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()

            // Send an update if battery is low
            if (batteryPct < 20) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // This requires the AuthInterceptor to be set up
                        RetrofitInstance.api.updateBatteryStatus(BatteryStatusRequest(batteryPct))
                    } catch (e: Exception) {
                        // Log error, but don't crash the app
                    }
                }
            }
        }
    }
}
