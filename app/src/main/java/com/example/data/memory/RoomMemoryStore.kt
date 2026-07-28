package com.example.data.memory

import com.example.data.db.MemoryRuleDao
import com.example.data.db.MemoryRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room database backed implementation of [MemoryStore].
 * Manages persisting and retrieving AI task execution patterns, learned DOM scripts,
 * and user preference rules.
 */
class RoomMemoryStore(
    private val memoryRuleDao: MemoryRuleDao
) : MemoryStore {

    override fun getAllRules(): Flow<List<MemoryRuleEntity>> {
        return memoryRuleDao.getAllRules()
    }

    override suspend fun getRulesForDomain(domain: String): List<MemoryRuleEntity> {
        return memoryRuleDao.getRulesForDomain(domain)
    }

    override suspend fun saveRule(rule: MemoryRuleEntity): Long {
        return memoryRuleDao.insertRule(rule)
    }

    override suspend fun saveTaskPattern(
        domainFilter: String,
        title: String,
        description: String,
        jsSnippet: String?
    ): Long {
        val rule = MemoryRuleEntity(
            category = "TASK_PATTERN",
            domainFilter = domainFilter,
            ruleTitle = title,
            ruleDescription = description,
            jsSnippet = jsSnippet
        )
        return memoryRuleDao.insertRule(rule)
    }

    override suspend fun saveUserPreference(
        domainFilter: String,
        title: String,
        description: String
    ): Long {
        val rule = MemoryRuleEntity(
            category = "USER_PREFERENCE",
            domainFilter = domainFilter,
            ruleTitle = title,
            ruleDescription = description
        )
        return memoryRuleDao.insertRule(rule)
    }

    override suspend fun updateRule(rule: MemoryRuleEntity) {
        memoryRuleDao.updateRule(rule)
    }

    override suspend fun deleteRule(rule: MemoryRuleEntity) {
        memoryRuleDao.deleteRule(rule)
    }

    override suspend fun deleteRuleById(id: Int) {
        memoryRuleDao.deleteById(id)
    }
}
