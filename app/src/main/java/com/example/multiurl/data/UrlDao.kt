package com.example.multiurl.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UrlDao {
    @Query("SELECT * FROM urls ORDER BY id DESC")
    fun observeAll(): Flow<List<UrlEntity>>

    @Query("SELECT * FROM urls ORDER BY id DESC")
    suspend fun getAll(): List<UrlEntity>

    @Insert
    suspend fun insert(entity: UrlEntity): Long

    @Update
    suspend fun update(entity: UrlEntity)

    @Delete
    suspend fun delete(entity: UrlEntity)

    @Query("UPDATE urls SET isDefault = (id = :id)")
    suspend fun setDefault(id: Long)

    @Query("SELECT * FROM urls WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): UrlEntity?
}
