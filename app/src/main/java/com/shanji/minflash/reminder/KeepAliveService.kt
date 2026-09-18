package com.shanji.minflash.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shanji.minflash.R

/**
 * 前台保活服务：以低优先级通知保持 app 进程存活，
 * 防止国产ROM在后台杀掉进程导致闹钟不触发。
 */
class KeepAliveService : Service() {
    companion object {
        private const val CHANNEL_KEEP_ALIVE = "keep_alive_channel"
        private const val NOTIFICATION_ID = 9999

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_KEEP_ALIVE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("极简闪记")
            .setContentText("提醒服务运行中")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        // START_STICKY：被系统杀掉后自动重启
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_KEEP_ALIVE,
                "保活服务",
                NotificationManager.IMPORTANCE_MIN // 最低重要性，不弹窗不发声
            ).apply {
                description = "保持提醒服务在后台运行"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
