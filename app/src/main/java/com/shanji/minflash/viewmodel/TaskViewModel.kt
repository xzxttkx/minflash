package com.shanji.minflash.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shanji.minflash.MinFlashApp
import com.shanji.minflash.data.Task
import com.shanji.minflash.reminder.AlarmScheduler
import com.shanji.minflash.util.TimeParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = (application as MinFlashApp).database.taskDao()
    val taskList: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTask(input: String) {
        viewModelScope.launch {
            val result = TimeParser.parse(input)
            val remindTime = result.remindTime ?: return@launch
            val task = Task(
                content = result.content,
                remindTime = remindTime
            )
            val taskId = taskDao.insertTask(task)
            val savedTask = task.copy(id = taskId)
            AlarmScheduler.schedule(getApplication(), savedTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            AlarmScheduler.cancel(getApplication(), task)
        }
    }
}

class TaskViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
