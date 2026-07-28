package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceDomain: String,
    val accountLabel: String,
    val username: String,
    val encryptedPassword: String,
    val authToken: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
