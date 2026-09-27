package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CollegeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollegeDao {

    @Query("SELECT * FROM colleges ORDER BY name ASC")
    fun getAllColleges(): Flow<List<CollegeEntity>>

    @Query("SELECT * FROM colleges WHERE isExamCentre = 1 ORDER BY seatingCapacity DESC")
    fun getExamCentres(): Flow<List<CollegeEntity>>

    @Query("SELECT * FROM colleges WHERE id = :id LIMIT 1")
    suspend fun getCollegeById(id: Long): CollegeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollege(college: CollegeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colleges: List<CollegeEntity>)

    @Update
    suspend fun updateCollege(college: CollegeEntity)

    @Delete
    suspend fun deleteCollege(college: CollegeEntity)

    @Query("DELETE FROM colleges WHERE id = :id")
    suspend fun deleteCollegeById(id: Long)

    @Query("DELETE FROM colleges")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM colleges")
    suspend fun getCount(): Int
}
