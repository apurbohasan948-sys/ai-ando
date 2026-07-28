package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryRuleDao {
    @Query("SELECT * FROM memory_rules ORDER BY lastTriggered DESC")
    fun getAllRules(): Flow<List<MemoryRuleEntity>>

    @Query("SELECT * FROM memory_rules WHERE domainFilter = '*' OR domainFilter LIKE '%' || :domain || '%' ORDER BY successCount DESC")
    suspend fun getRulesForDomain(domain: String): List<MemoryRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MemoryRuleEntity): Long

    @Update
    suspend fun updateRule(rule: MemoryRuleEntity)

    @Delete
    suspend fun deleteRule(rule: MemoryRuleEntity)

    @Query("DELETE FROM memory_rules WHERE id = :id")
    suspend fun deleteById(id: Int)
}
