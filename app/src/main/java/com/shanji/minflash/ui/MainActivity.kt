package com.shanji.minflash.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shanji.minflash.ui.theme.MinFlashTheme
import com.shanji.minflash.viewmodel.TaskViewModel
import com.shanji.minflash.viewmodel.TaskViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(application)
    }

    // Android 13+ 通知权限请求
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 授权后通知自然能显示，无需额外处理 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 运行时请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val crashLog = readCrashLog()
        if (crashLog != null) {
            showNativeCrashDialog(crashLog)
        } else {
            setupComposeContent()
        }
    }

    private fun showNativeCrashDialog(crashLog: String) {
        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = crashLog
            setPadding(48, 48, 48, 48)
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        scrollView.addView(textView)

        android.app.AlertDialog.Builder(this)
            .setTitle("上次崩溃日志（截图发给开发者）")
            .setView(scrollView)
            .setPositiveButton("清除并继续") { _, _ ->
                clearCrashLog()
                setupComposeContent()
            }
            .setNegativeButton("保留日志") { _, _ ->
                setupComposeContent()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupComposeContent() {
        try {
            setContent {
                MinFlashTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        TaskListScreen(viewModel)
                    }
                }
            }
        } catch (e: Throwable) {
            // Compose 初始化失败，用纯 TextView 显示错误
            writeCrashLog("Compose setContent failed:\n${stackTraceToString(e)}")
            val tv = TextView(this).apply {
                text = "Compose 初始化失败，请截图发给开发者：\n\n${stackTraceToString(e)}"
                setPadding(48, 48, 48, 48)
                textSize = 11f
                movementMethod = android.text.method.ScrollingMovementMethod()
            }
            setContentView(tv)
        }
    }

    private fun stackTraceToString(e: Throwable): String {
        val sw = java.io.StringWriter()
        e.printStackTrace(java.io.PrintWriter(sw))
        return sw.toString()
    }

    private fun readCrashLog(): String? {
        return try {
            val file = File(filesDir, "crash_log.txt")
            if (file.exists() && file.length() > 0) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCrashLog(log: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            File(filesDir, "crash_log.txt").appendText("===== Crash at $timestamp =====\n$log\n\n")
        } catch (_: Exception) {
        }
    }

    private fun clearCrashLog() {
        try {
            File(filesDir, "crash_log.txt").delete()
        } catch (_: Exception) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    var inputText by remember { mutableStateOf("") }
    val tasks by viewModel.taskList.collectAsState(initial = emptyList())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "今日闪记",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 输入栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入任务，或点语音按钮说时间+内容") },
                maxLines = 1
            )
            IconButton(onClick = { /* TODO: 语音输入 */ }) {
                Icon(Icons.Default.Mic, contentDescription = "语音输入")
            }
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.addTask(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("添加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 任务列表
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = task.content, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "提醒时间: ${timeFormat.format(Date(task.remindTime))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { viewModel.deleteTask(task) }) {
                            Text("完成")
                        }
                    }
                }
            }
        }
    }
}
