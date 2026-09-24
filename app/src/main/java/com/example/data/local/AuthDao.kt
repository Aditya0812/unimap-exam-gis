package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserCredential
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {

    @Query("SELECT * FROM user_credentials WHERE username = :username LIMIT 1")
    suspend fun getCredential(username: String): UserCredential?

    @Query("SELECT * FROM user_credentials ORDER BY createdAt DESC")
    fun getAllCredentials(): Flow<List<UserCredential>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: UserCredential)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credentials: List<UserCredential>)

    @Update
    suspend fun updateCredential(credential: UserCredential)

    @Query("UPDATE user_credentials SET allowedByCreator = :allowed WHERE username = :username")
    suspend fun setAllowedStatus(username: String, allowed: Boolean)

    @Query("SELECT COUNT(*) FROM user_credentials")
    suspend fun getCount(): Int
}
