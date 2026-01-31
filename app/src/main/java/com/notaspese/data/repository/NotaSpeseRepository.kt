package com.notaspese.data.repository

import com.notaspese.data.database.NotaSpeseDao
import com.notaspese.data.database.SpesaDao
import com.notaspese.data.model.NotaSpese
import com.notaspese.data.model.NotaSpeseConSpese
import com.notaspese.data.model.Spesa
import kotlinx.coroutines.flow.Flow

class NotaSpeseRepository(private val notaSpeseDao: NotaSpeseDao, private val spesaDao: SpesaDao) {
    val allNoteSpese: Flow<List<NotaSpese>> = notaSpeseDao.getAllNoteSpese()
    val allNoteSpeseConSpese: Flow<List<NotaSpeseConSpese>> = notaSpeseDao.getAllNoteSpeseConSpese()
    suspend fun insertNotaSpese(notaSpese: NotaSpese): Long = notaSpeseDao.insertNotaSpese(notaSpese)
    suspend fun updateNotaSpese(notaSpese: NotaSpese) = notaSpeseDao.updateNotaSpese(notaSpese)
    suspend fun deleteNotaSpese(notaSpese: NotaSpese) = notaSpeseDao.deleteNotaSpese(notaSpese)
    suspend fun getNotaSpeseById(id: Long): NotaSpese? = notaSpeseDao.getNotaSpeseById(id)
    fun getNotaSpeseConSpese(id: Long): Flow<NotaSpeseConSpese?> = notaSpeseDao.getNotaSpeseConSpese(id)
    fun getSpeseByNotaSpese(notaSpeseId: Long): Flow<List<Spesa>> = spesaDao.getSpeseByNotaSpese(notaSpeseId)
    suspend fun insertSpesa(spesa: Spesa): Long = spesaDao.insertSpesa(spesa)
    suspend fun updateSpesa(spesa: Spesa) = spesaDao.updateSpesa(spesa)
    suspend fun deleteSpesa(spesa: Spesa) = spesaDao.deleteSpesa(spesa)
    suspend fun getSpesaById(id: Long): Spesa? = spesaDao.getSpesaById(id)
    suspend fun getTotaleSpese(notaSpeseId: Long): Double = spesaDao.getTotaleSpese(notaSpeseId) ?: 0.0
}
