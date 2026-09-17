package com.shanji.minflash

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.shanji.minflash.data.TaskDatabase

class MinFlashApp : Application() {
    val database: TaskDatabase by lazy { TaskDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // 启动午夜自动清理任务
        MidnightCleanupReceiver.scheduleNextCleanup(this)
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
