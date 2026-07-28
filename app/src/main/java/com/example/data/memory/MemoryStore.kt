package com.example.data.memory

import com.example.data.db.MemoryRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the persistence contract for AI task patterns, user preferences,
 * and learned DOM automation rules for long-term agent adaptation.
 */
interface MemoryStore {
    fun getAllRules(): Flow<List<MemoryRuleEntity>>
    suspend fun getRulesForDomain(domain: String): List<MemoryRuleEntity>
    suspend fun saveRule(rule: MemoryRuleEntity): Long
    suspend fun saveTaskPattern(
        domainFilter: String,
        title: String,
        description: String,
        jsSnippet: String? = null
    ): Long
    suspend fun saveUserPreference(
        domainFilter: String = "*",
        title: String,
        description: String
    ): Long
    suspend fun updateRule(rule: MemoryRuleEntity)
    suspend fun deleteRule(rule: MemoryRuleEntity)
    suspend fun deleteRuleById(id: Int)
}
