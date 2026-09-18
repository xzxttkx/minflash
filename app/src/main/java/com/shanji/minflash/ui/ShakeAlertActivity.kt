package com.shanji.minflash.ui

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import com.shanji.minflash.R
import kotlin.math.sqrt

class ShakeAlertActivity : Activity(), SensorEventListener {
    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_CONTENT = "content"
        private const val SHAKE_THRESHOLD = 12f // 摇一摇灵敏度
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var vibrator: Vibrator
    private var ringtone: android.media.Ringtone? = null
    private var lastShakeTime = 0L
    private var taskId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 锁屏上显示 + 点亮屏幕 + 保持屏幕常亮（兼容各 API 级别）
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContentView(R.layout.activity_shake_alert)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: "任务提醒"
        findViewById<TextView>(R.id.tv_task_content).text = content

        // 初始化传感器
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 初始化震动和铃声
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        startAlertSoundAndVibrate()
    }

    private fun startAlertSoundAndVibrate() {
        // 播放默认闹铃声
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri).apply {
            play()
        }

        // 循环震动
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500), 0)
        }
    }

    private fun stopAlert() {
        ringtone?.stop()
        vibrator.cancel()
        // 摇一摇后同时取消通知栏和锁屏上的通知
        if (taskId >= 0) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(taskId.toInt())
        }
        finish()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 计算加速度矢量和
        val gForce = sqrt((x*x + y*y + z*z).toDouble()) - SensorManager.GRAVITY_EARTH

        val currentTime = System.currentTimeMillis()
        // 防止连续触发，间隔1秒
        if (gForce > SHAKE_THRESHOLD && currentTime - lastShakeTime > 1000) {
            lastShakeTime = currentTime
            stopAlert()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
        vibrator.cancel()
    }
}
