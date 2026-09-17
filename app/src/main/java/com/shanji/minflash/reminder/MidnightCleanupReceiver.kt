package com.shanji.minflash.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shanji.minflash.MinFlashApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MidnightCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as MinFlashApp
        CoroutineScope(Dispatchers.IO).launch {
            // 清空所有当天之前的任务
            val now = Calendar.getInstance()
            app.database.taskDao().deleteTasksBefore(now.timeInMillis)
            // 调度下一个午夜的清理任务
            scheduleNextCleanup(context)
        }
    }

    companion object {
        fun scheduleNextCleanup(context: Context) {
            val alarmManager = context.getService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, MidnightCleanupReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                9999, // 固定requestCode
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // 计算下一个0点的时间
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }

            alarmManager.setInexactRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                android.app.AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }
}
