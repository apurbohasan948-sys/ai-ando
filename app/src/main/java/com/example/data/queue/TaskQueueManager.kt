package com.example.data.queue

import com.example.data.db.ProcessedPageEntity
import com.example.data.db.TaskQueueDao
import com.example.data.db.TaskQueueEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

class TaskQueueManager(private val taskQueueDao: TaskQueueDao) {

    val allTasks: Flow<List<TaskQueueEntity>> = taskQueueDao.getAllTasks()
    val pendingTasks: Flow<List<TaskQueueEntity>> = taskQueueDao.getPendingTasks()

    suspend fun isPageProcessed(url: String): Boolean {
        val cleanUrl = cleanUrlForMatching(url)
        val existing = taskQueueDao.getProcessedPage(cleanUrl)
        return existing != null
    }

    suspend fun getProcessedPage(url: String): ProcessedPageEntity? {
        val cleanUrl = cleanUrlForMatching(url)
        return taskQueueDao.getProcessedPage(cleanUrl)
    }

    suspend fun enqueueSequence(
        targetUrl: String,
        taskType: String = "MULTI_STEP_SEQUENCE",
        steps: List<String> = listOf("NAVIGATE", "EXTRACT_MEDIA", "SUBMIT_ADMIN")
    ): Long {
        val stepsJson = JSONArray(steps).toString()
        val task = TaskQueueEntity(
            targetUrl = targetUrl,
            taskType = taskType,
            stepsJson = stepsJson,
            currentStepIndex = 0,
            status = "PENDING",
            resultSummary = "Sequence enqueued"
        )
        return taskQueueDao.insertTask(task)
    }

    suspend fun recordPageProcessed(url: String, title: String, extractedData: String) {
        val cleanUrl = cleanUrlForMatching(url)
        val record = ProcessedPageEntity(
            url = cleanUrl,
            pageTitle = title,
            extractedData = extractedData,
            processedAt = System.currentTimeMillis(),
            status = "COMPLETED"
        )
        taskQueueDao.recordProcessedPage(record)
    }

    suspend fun markTaskExecuting(taskId: Long) {
        val pending = taskQueueDao.getNextPendingTask()
        if (pending != null && pending.id == taskId) {
            val updated = pending.copy(
                status = "EXECUTING",
                updatedAt = System.currentTimeMillis()
            )
            taskQueueDao.updateTask(updated)
        }
    }

    suspend fun advanceStep(taskId: Long, nextStepIndex: Int, summary: String) {
        val pending = taskQueueDao.getNextPendingTask() ?: return
        val updated = pending.copy(
            currentStepIndex = nextStepIndex,
            resultSummary = summary,
            updatedAt = System.currentTimeMillis()
        )
        taskQueueDao.updateTask(updated)
    }

    suspend fun completeTask(taskId: Long, summary: String) {
        val pending = taskQueueDao.getNextPendingTask() ?: return
        val updated = pending.copy(
            status = "COMPLETED",
            resultSummary = summary,
            updatedAt = System.currentTimeMillis()
        )
        taskQueueDao.updateTask(updated)
    }

    suspend fun clearQueue() {
        taskQueueDao.clearAllTasks()
    }

    private fun cleanUrlForMatching(url: String): String {
        return url.trim()
            .replace(Regex("^https?://"), "")
            .replace(Regex("/$"), "")
            .lowercase()
    }
}
