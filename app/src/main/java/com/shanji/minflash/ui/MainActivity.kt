package com.shanji.minflash.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 读取上次崩溃日志（如果有）
        val crashLog = readCrashLog()
        setContent {
            MinFlashTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TaskListScreen(viewModel, crashLog) { clearCrashLog() }
                }
            }
        }
    }

    private fun readCrashLog(): String? {
        return try {
            val dir = getExternalFilesDir(null) ?: return null
            val file = File(dir, "crash_log.txt")
            if (file.exists() && file.length() > 0) file.readText() else null
        } catch (_: Exception) { null }
    }

    private fun clearCrashLog() {
        try {
            val dir = getExternalFilesDir(null) ?: return
            File(dir, "crash_log.txt").delete()
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel, crashLog: String?, onCrashDismissed: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val tasks by viewModel.taskList.collectAsState(initial = emptyList())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 上次崩溃日志弹窗
    if (crashLog != null) {
        AlertDialog(
            onDismissRequest = onCrashDismissed,
            title = { Text("上次崩溃日志", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text(crashLog, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = onCrashDismissed) { Text("知道了") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "今日闪记",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom=16.dp)
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
                modifier = Modifier.padding(start=8.dp)
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
