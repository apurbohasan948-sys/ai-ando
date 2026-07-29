package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_queue")
data class TaskQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetUrl: String,
    val taskType: String, // e.g., "MULTI_STEP_SEQUENCE", "EXTRACT_AND_POST", "SEARCH_AND_EXTRACT"
    val stepsJson: String, // e.g. ["NAVIGATE", "EXTRACT_MEDIA", "SUBMIT_ADMIN"]
    val currentStepIndex: Int = 0,
    val status: String = "PENDING", // PENDING, EXECUTING, COMPLETED, FAILED, SKIPPED
    val resultSummary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "processed_pages")
data class ProcessedPageEntity(
    @PrimaryKey val url: String,
    val pageTitle: String,
    val extractedData: String,
    val processedAt: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED"
)
