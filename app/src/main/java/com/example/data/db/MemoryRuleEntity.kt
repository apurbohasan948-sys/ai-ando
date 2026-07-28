package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_rules")
data class MemoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "TASK_PATTERN", "USER_PREFERENCE", "DOM_SELECTOR", "CUSTOM_RULE"
    val domainFilter: String, // e.g., "wikipedia.org" or "*"
    val ruleTitle: String,
    val ruleDescription: String,
    val jsSnippet: String? = null,
    val successCount: Int = 1,
    val lastTriggered: Long = System.currentTimeMillis()
)
