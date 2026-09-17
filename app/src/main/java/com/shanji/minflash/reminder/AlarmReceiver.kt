package com.shanji.minflash.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.shanji.minflash.MinFlashApp
import com.shanji.minflash.ui.ShakeAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_CONTENT = "task_content"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val content = intent.getStringExtra(EXTRA_TASK_CONTENT) ?: return

        Log.d("AlarmReceiver", "闹钟触发: taskId=$taskId, content=$content")

        // 标记任务已通知
        val app = context.applicationContext as MinFlashApp
        CoroutineScope(Dispatchers.IO).launch {
            app.database.taskDao().markAsNotified(taskId)
        }

        // 启动全屏提醒界面（带摇一摇关提醒）
        val alertIntent = Intent(context, ShakeAlertActivity::class.java).apply {
            putExtra(ShakeAlertActivity.EXTRA_TASK_ID, taskId)
            putExtra(ShakeAlertActivity.EXTRA_CONTENT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(alertIntent)
    }
}
