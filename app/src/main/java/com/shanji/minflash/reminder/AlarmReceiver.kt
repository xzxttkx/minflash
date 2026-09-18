package com.shanji.minflash.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shanji.minflash.MinFlashApp
import com.shanji.minflash.R
import com.shanji.minflash.ui.MainActivity
import com.shanji.minflash.ui.ShakeAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_CONTENT = "task_content"
        const val ACTION_MARK_DONE = "com.shanji.minflash.ACTION_MARK_DONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 处理通知栏"完成"按钮点击
        if (intent.action == ACTION_MARK_DONE) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
            if (taskId >= 0) {
                markTaskDone(context, taskId)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(taskId.toInt())
            }
            return
        }

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val content = intent.getStringExtra(EXTRA_TASK_CONTENT) ?: return

        Log.d("AlarmReceiver", "闹钟触发: taskId=$taskId, content=$content")

        // 标记任务已通知
        val app = context.applicationContext as MinFlashApp
        CoroutineScope(Dispatchers.IO).launch {
            app.database.taskDao().markAsNotified(taskId)
        }

        // 全屏提醒意图（熄屏/锁屏时唤起摇一摇界面）
        val fullScreenIntent = Intent(context, ShakeAlertActivity::class.java).apply {
            putExtra(ShakeAlertActivity.EXTRA_TASK_ID, taskId)
            putExtra(ShakeAlertActivity.EXTRA_CONTENT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, taskId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 点击通知打开主界面
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, taskId.toInt() + 20000, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "完成"按钮
        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, taskId.toInt() + 10000, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建高优先级通知（会在通知栏显示 + 横幅弹窗 + 熄屏时全屏提醒）
        val notification = NotificationCompat.Builder(context, MinFlashApp.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("任务提醒")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_notification, "完成", donePendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(taskId.toInt(), notification)
    }

    private fun markTaskDone(context: Context, taskId: Long) {
        val app = context.applicationContext as MinFlashApp
        CoroutineScope(Dispatchers.IO).launch {
            app.database.taskDao().getTaskById(taskId)?.let { task ->
                app.database.taskDao().deleteTask(task)
                AlarmScheduler.cancel(context, task)
            }
        }
    }
}
