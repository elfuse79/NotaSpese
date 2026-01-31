package com.notaspese.data.database

import androidx.room.*
import com.notaspese.data.model.Spesa
import kotlinx.coroutines.flow.Flow

@Dao
interface SpesaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSpesa(spesa: Spesa): Long
    @Update suspend fun updateSpesa(spesa: Spesa)
    @Delete suspend fun deleteSpesa(spesa: Spesa)
    @Query("SELECT * FROM spesa WHERE notaSpeseId = :notaSpeseId ORDER BY data DESC") fun getSpeseByNotaSpese(notaSpeseId: Long): Flow<List<Spesa>>
    @Query("SELECT * FROM spesa WHERE id = :id") suspend fun getSpesaById(id: Long): Spesa?
    @Query("SELECT SUM(importo) FROM spesa WHERE notaSpeseId = :notaSpeseId") suspend fun getTotaleSpese(notaSpeseId: Long): Double?
}
