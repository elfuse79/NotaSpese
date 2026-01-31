package com.notaspese.data.database

import androidx.room.*
import com.notaspese.data.model.NotaSpese
import com.notaspese.data.model.NotaSpeseConSpese
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaSpeseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNotaSpese(notaSpese: NotaSpese): Long
    @Update suspend fun updateNotaSpese(notaSpese: NotaSpese)
    @Delete suspend fun deleteNotaSpese(notaSpese: NotaSpese)
    @Query("SELECT * FROM nota_spese ORDER BY dataCompilazione DESC") fun getAllNoteSpese(): Flow<List<NotaSpese>>
    @Query("SELECT * FROM nota_spese WHERE id = :id") suspend fun getNotaSpeseById(id: Long): NotaSpese?
    @Transaction @Query("SELECT * FROM nota_spese WHERE id = :id") fun getNotaSpeseConSpese(id: Long): Flow<NotaSpeseConSpese?>
    @Transaction @Query("SELECT * FROM nota_spese ORDER BY dataCompilazione DESC") fun getAllNoteSpeseConSpese(): Flow<List<NotaSpeseConSpese>>
}
