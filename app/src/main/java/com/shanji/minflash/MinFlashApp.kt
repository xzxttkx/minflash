package com.shanji.minflash

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.shanji.minflash.data.TaskDatabase
import com.shanji.minflash.reminder.MidnightCleanupReceiver
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MinFlashApp : Application() {
    val database: TaskDatabase by lazy { TaskDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        createNotificationChannel()
        // 启动午夜自动清理任务
        MidnightCleanupReceiver.scheduleNextCleanup(this)
    }

    /**
     * 全局未捕获异常处理器：将崩溃堆栈写入外部文件，方便无 adb 环境下排查。
     * 文件路径：Android/data/com.shanji.minflash/files/crash_log.txt
     */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val log = "===== Crash at $timestamp =====\nThread: ${thread.name}\n$sw\n\n"
                val dir = getExternalFilesDir(null)
                if (dir != null) {
                    val file = File(dir, "crash_log.txt")
                    file.appendText(log)
                }
            } catch (_: Exception) {
                // 日志写入失败也不能阻止默认处理
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDER,
                getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.reminder_channel_desc)
                enableVibration(true)
                setSound(null, null) // 我们自己控制铃声，这里静音
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_REMINDER = "reminder_channel"
        const val NOTIFICATION_ID_TASK = 1001
    }
}
