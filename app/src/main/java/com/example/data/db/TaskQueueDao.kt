package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskQueueDao {

    @Query("SELECT * FROM task_queue ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskQueueEntity>>

    @Query("SELECT * FROM task_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingTasks(): Flow<List<TaskQueueEntity>>

    @Query("SELECT * FROM task_queue WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextPendingTask(): TaskQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskQueueEntity): Long

    @Update
    suspend fun updateTask(task: TaskQueueEntity)

    @Query("DELETE FROM task_queue WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM task_queue")
    suspend fun clearAllTasks()

    // Processed Pages tracking
    @Query("SELECT * FROM processed_pages WHERE url = :url LIMIT 1")
    suspend fun getProcessedPage(url: String): ProcessedPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordProcessedPage(page: ProcessedPageEntity)

    @Query("DELETE FROM processed_pages WHERE url = :url")
    suspend fun removeProcessedPage(url: String)
}
