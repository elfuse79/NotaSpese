package com.notaspese.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notaspese.data.database.AppDatabase
import com.notaspese.data.model.NotaSpese
import com.notaspese.data.model.NotaSpeseConSpese
import com.notaspese.data.model.Spesa
import com.notaspese.data.repository.NotaSpeseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NotaSpeseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NotaSpeseRepository
    val allNoteSpeseConSpese: StateFlow<List<NotaSpeseConSpese>>
    private val _currentNotaSpese = MutableStateFlow<NotaSpeseConSpese?>(null)
    val currentNotaSpese: StateFlow<NotaSpeseConSpese?> = _currentNotaSpese.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NotaSpeseRepository(database.notaSpeseDao(), database.spesaDao())
        allNoteSpeseConSpese = repository.allNoteSpeseConSpese.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun loadNotaSpese(id: Long) {
        viewModelScope.launch { repository.getNotaSpeseConSpese(id).collect { _currentNotaSpese.value = it } }
    }

    fun createNotaSpese(notaSpese: NotaSpese, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val id = repository.insertNotaSpese(notaSpese)
            _isLoading.value = false
            onSuccess(id)
        }
    }

    fun updateNotaSpese(notaSpese: NotaSpese) { viewModelScope.launch { repository.updateNotaSpese(notaSpese) } }
    fun deleteNotaSpese(notaSpese: NotaSpese) { viewModelScope.launch { repository.deleteNotaSpese(notaSpese) } }
    
    // Elimina la nota spese e la relativa cartella
    fun deleteNotaSpeseWithFolder(notaSpeseConSpese: NotaSpeseConSpese) {
        viewModelScope.launch {
            // Prima elimina la cartella
            withContext(Dispatchers.IO) {
                try {
                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
                    val nota = notaSpeseConSpese.notaSpese
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val innovolDir = File(downloadsDir, "Innoval Nota Spese")
                    val folderName = "${nota.nomeCognome.replace(" ", "_")}_${dateFormatter.format(Date(nota.dataInizioTrasferta))}"
                    val notaDir = File(innovolDir, folderName)
                    if (notaDir.exists()) {
                        notaDir.deleteRecursively()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Poi elimina dal database
            repository.deleteNotaSpese(notaSpeseConSpese.notaSpese)
        }
    }
    
    fun addSpesa(spesa: Spesa, onSuccess: () -> Unit = {}) { viewModelScope.launch { repository.insertSpesa(spesa); onSuccess() } }
    fun updateSpesa(spesa: Spesa) { viewModelScope.launch { repository.updateSpesa(spesa) } }
    fun deleteSpesa(spesa: Spesa) { viewModelScope.launch { repository.deleteSpesa(spesa) } }
    fun updateAnticipo(notaSpeseId: Long, anticipo: Double) {
        viewModelScope.launch { repository.getNotaSpeseById(notaSpeseId)?.let { repository.updateNotaSpese(it.copy(anticipo = anticipo)) } }
    }
}
