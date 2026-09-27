package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_credentials")
data class UserCredential(
    @PrimaryKey
    val username: String, // email or user identifier
    val passwordHash: String, // SHA-256 salted hash
    val salt: String,
    val fullName: String,
    val role: String, // "Master Administrator", "Exam Controller", "Transit Logistics Officer"
    val allowedByCreator: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
