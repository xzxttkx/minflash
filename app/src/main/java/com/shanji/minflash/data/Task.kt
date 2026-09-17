package com.shanji.minflash.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val remindTime: Long, // 提醒时间戳，毫秒
    val createdAt: Long = System.currentTimeMillis(),
    val isNotified: Boolean = false // 是否已触发提醒
)
