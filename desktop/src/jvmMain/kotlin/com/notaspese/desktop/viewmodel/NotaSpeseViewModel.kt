package com.notaspese.desktop.viewmodel

import com.notaspese.desktop.data.database.NotaSpeseDatabase
import com.notaspese.desktop.data.model.NotaSpese
import com.notaspese.desktop.data.model.NotaSpeseConSpese
import com.notaspese.desktop.data.model.Spesa
import com.notaspese.desktop.util.CsvExporter
import com.notaspese.desktop.util.CsvImporter
import com.notaspese.desktop.util.NotaSpeseExporter
import com.notaspese.desktop.util.NotaSpeseImporter
import com.notaspese.desktop.util.PdfGenerator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NotaSpeseViewModel {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val database = NotaSpeseDatabase()

    private val _allNoteSpese = MutableStateFlow<List<NotaSpeseConSpese>>(emptyList())
    val allNoteSpeseConSpese: StateFlow<List<NotaSpeseConSpese>> = _allNoteSpese.asStateFlow()

    private val _currentNotaSpese = MutableStateFlow<NotaSpeseConSpese?>(null)
    val currentNotaSpese: StateFlow<NotaSpeseConSpese?> = _currentNotaSpese.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        scope.launch { refreshAll() }
    }

    private suspend fun refreshAll() {
        database.getAllNoteSpeseConSpese().collect { _allNoteSpese.value = it }
    }

    fun loadNotaSpese(id: Long) {
        scope.launch {
            _currentNotaSpese.value = database.getNotaSpeseConSpese(id)
        }
    }

    fun createNotaSpese(nota: NotaSpese, onSuccess: (Long) -> Unit) {
        scope.launch {
            _isLoading.value = true
            val id = database.insertNotaSpese(nota)
            _isLoading.value = false
            refreshAll()
            onSuccess(id)
        }
    }

    fun updateNotaSpese(nota: NotaSpese) {
        scope.launch {
            database.updateNotaSpese(nota)
            refreshAll()
            _currentNotaSpese.value?.let { if (it.notaSpese.id == nota.id) loadNotaSpese(nota.id) }
        }
    }

    fun deleteNotaSpese(nota: NotaSpese) {
        scope.launch {
            database.deleteNotaSpese(nota)
            val folder = database.getNotaFolder(nota.id)
            if (folder.exists()) folder.deleteRecursively()
            refreshAll()
            _currentNotaSpese.value = null
        }
    }

    fun deleteNotaSpeseWithFolder(notaConSpese: NotaSpeseConSpese) {
        scope.launch {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
            val nota = notaConSpese.notaSpese
            val downloadsDir = File(System.getProperty("user.home"), "Downloads")
            val innovolDir = File(downloadsDir, "Innoval Nota Spese")
            val folderName = "${nota.nomeCognome.replace(" ", "_")}_${dateFormatter.format(Date(nota.dataInizioTrasferta))}"
            val notaDir = File(innovolDir, folderName)
            if (notaDir.exists()) notaDir.deleteRecursively()
            database.deleteNotaSpese(nota)
            refreshAll()
            _currentNotaSpese.value = null
        }
    }

    fun addSpesa(spesa: Spesa, onSuccess: () -> Unit = {}) {
        scope.launch {
            database.insertSpesa(spesa)
            refreshAll()
            _currentNotaSpese.value?.let { if (it.notaSpese.id == spesa.notaSpeseId) loadNotaSpese(spesa.notaSpeseId) }
            onSuccess()
        }
    }

    fun updateSpesa(spesa: Spesa) {
        scope.launch {
            database.updateSpesa(spesa)
            refreshAll()
            _currentNotaSpese.value?.let { if (it.notaSpese.id == spesa.notaSpeseId) loadNotaSpese(spesa.notaSpeseId) }
        }
    }

    fun deleteSpesa(spesa: Spesa) {
        scope.launch {
            database.deleteSpesa(spesa)
            refreshAll()
            _currentNotaSpese.value?.let { if (it.notaSpese.id == spesa.notaSpeseId) loadNotaSpese(spesa.notaSpeseId) }
        }
    }

    fun updateAnticipo(notaSpeseId: Long, anticipo: Double) {
        scope.launch {
            database.getNotaSpeseById(notaSpeseId)?.let { 
                database.updateNotaSpese(it.copy(anticipo = anticipo))
                refreshAll()
                loadNotaSpese(notaSpeseId)
            }
        }
    }

    fun exportPdfAndCsv(notaConSpese: NotaSpeseConSpese, targetParentDir: File? = null, baseName: String? = null): File? {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.ITALY)
        val nota = notaConSpese.notaSpese
        val defaultFolderName = "${nota.nomeCognome.replace(" ", "_")}_${dateFormatter.format(Date(nota.dataInizioTrasferta))}"
        val folderName = baseName?.trim()?.replace(Regex("[\\\\/:*?\"<>|]"), "_")?.ifBlank { null } ?: defaultFolderName
        val outputDir = if (targetParentDir != null) {
            File(targetParentDir, folderName).also { it.mkdirs() }
        } else {
            val downloadsDir = File(System.getProperty("user.home"), "Downloads")
            val innovolDir = File(downloadsDir, "Innoval Nota Spese")
            File(innovolDir, folderName).also { it.mkdirs() }
        }
        PdfGenerator.generatePdf(outputDir, notaConSpese, baseName)
        CsvExporter.exportToCsv(notaConSpese, outputDir, baseName)
        NotaSpeseExporter.exportToNotaSpeseFile(notaConSpese, outputDir, baseName)
        return outputDir
    }

    fun getDefaultExportBaseName(nota: com.notaspese.desktop.data.model.NotaSpese): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.ITALY)
        return "${nota.nomeCognome.replace(" ", "_")}_${dateFormatter.format(Date(nota.dataInizioTrasferta))}"
    }

    fun chooseExportDirectory(): File? {
        fun showChooser(): File? {
            return javax.swing.JFileChooser().apply {
                fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Scegli dove salvare la nota spese"
                currentDirectory = File(System.getProperty("user.home"), "Desktop")
            }.let {
                if (it.showSaveDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) it.selectedFile else null
            }
        }
        return if (java.awt.EventQueue.isDispatchThread()) {
            showChooser()
        } else {
            var result: File? = null
            java.awt.EventQueue.invokeAndWait { result = showChooser() }
            result
        }
    }

    fun importFromFolder(folder: File): NotaSpeseConSpese? = CsvImporter.importFromFolder(folder)

    fun importFromNotaSpeseFile(file: File): NotaSpeseConSpese? = NotaSpeseImporter.importFromFile(file)

    fun importFromPath(path: String): NotaSpeseConSpese? {
        val trimmed = path.trim().trim('"', '\'')
        val f = File(trimmed)
        if (!f.exists()) return null
        return when {
            f.isDirectory -> importFromFolder(f)
            f.name.endsWith(".notaspese", ignoreCase = true) -> importFromNotaSpeseFile(f)
            f.extension.equals("csv", ignoreCase = true) -> importFromFolder(f.parentFile ?: f)
            else -> importFromFolder(f.parentFile ?: f)
        }
    }

    fun importAndSave(notaConSpese: NotaSpeseConSpese) {
        scope.launch {
            val nota = notaConSpese.notaSpese
            val id = database.insertNotaSpese(nota)
            val notaFolder = database.getNotaFolder(id)
            notaFolder.mkdirs()
            for (spesa in notaConSpese.spese) {
                var photoPath: String? = spesa.fotoScontrinoPath
                if (spesa.fotoScontrinoPath != null) {
                    val src = File(spesa.fotoScontrinoPath)
                    if (src.exists()) {
                        val dest = File(notaFolder, src.name)
                        src.copyTo(dest, overwrite = true)
                        photoPath = dest.absolutePath
                    }
                }
                database.insertSpesa(spesa.copy(notaSpeseId = id, fotoScontrinoPath = photoPath))
            }
            refreshAll()
        }
    }

    fun getNotaFolder(notaSpeseId: Long): File = database.getNotaFolder(notaSpeseId)
}
