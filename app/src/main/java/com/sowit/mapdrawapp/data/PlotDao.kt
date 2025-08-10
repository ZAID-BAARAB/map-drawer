package com.sowit.mapdrawapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlotDao {
    @Insert
    suspend fun insert(area: PlotArea): Long

    @Query("SELECT * FROM plots ORDER BY id DESC")
    fun getAll(): Flow<List<PlotArea>>
}