package com.shanji.minflash.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shanji.minflash.MinFlashApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as MinFlashApp
        CoroutineScope(Dispatchers.IO).launch {
            // 重启所有未触发的闹钟
            val tasks = app.database.taskDao().getAllTasks().first()
            tasks.filter { !it.isNotified && it.remindTime > System.currentTimeMillis() }
                .forEach { task ->
                    AlarmScheduler.schedule(context, task)
                }
        }
    }
}
