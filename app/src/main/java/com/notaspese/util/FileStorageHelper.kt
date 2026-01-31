package com.notaspese.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileStorageHelper {
    
    /**
     * Salva un file (immagine o PDF) nella cartella della nota spese
     * @param context Context dell'applicazione
     * @param sourceUri URI del file da salvare
     * @param notaSpeseId ID della nota spese (usato per organizzare i file)
     * @param fileType Tipo di file ("image" o "pdf")
     * @return Path del file salvato, o null in caso di errore
     */
    fun saveFileToNotaFolder(context: Context, sourceUri: Uri, notaSpeseId: Long, fileType: String): String? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = when (fileType) {
                "pdf" -> "pdf"
                else -> "jpg"
            }
            val prefix = when (fileType) {
                "pdf" -> "documento"
                else -> "scontrino"
            }
            
            // Crea la cartella per la nota spese
            val notaFolder = getNotaFolder(context, notaSpeseId)
            if (!notaFolder.exists()) {
                notaFolder.mkdirs()
            }
            
            val fileName = "${prefix}_${timeStamp}_${notaSpeseId}.$extension"
            val destFile = File(notaFolder, fileName)
            
            // Copia il file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Salva un'immagine ritagliata nella cartella della nota spese
     */
    fun saveCroppedImageToNotaFolder(context: Context, sourceUri: Uri, notaSpeseId: Long): String? {
        return saveFileToNotaFolder(context, sourceUri, notaSpeseId, "image")
    }
    
    /**
     * Ottiene la cartella per una specifica nota spese
     */
    fun getNotaFolder(context: Context, notaSpeseId: Long): File {
        val baseFolder = context.getExternalFilesDir("note_spese")
        return File(baseFolder, "nota_$notaSpeseId")
    }
    
    /**
     * Copia tutti i file allegati di una nota spese nella cartella di esportazione
     */
    fun copyAttachmentsToExportFolder(context: Context, attachmentPaths: List<String>, exportFolder: File): List<Pair<String, String>> {
        val copiedFiles = mutableListOf<Pair<String, String>>()
        var imageCounter = 1
        var pdfCounter = 1
        
        for (path in attachmentPaths) {
            try {
                val sourceFile = when {
                    path.startsWith("content://") -> {
                        // È un URI content, dobbiamo copiarlo prima
                        val uri = Uri.parse(path)
                        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }
                    path.startsWith("file://") -> {
                        File(Uri.parse(path).path ?: continue)
                    }
                    else -> {
                        File(path)
                    }
                }
                
                if (sourceFile.exists()) {
                    val isPdf = path.endsWith(".pdf", ignoreCase = true) || 
                               sourceFile.extension.equals("pdf", ignoreCase = true)
                    
                    val (prefix, counter) = if (isPdf) {
                        "documento" to pdfCounter++
                    } else {
                        "scontrino" to imageCounter++
                    }
                    
                    val extension = if (isPdf) "pdf" else sourceFile.extension.ifBlank { "jpg" }
                    val destFileName = "${prefix}_${String.format("%03d", counter)}.$extension"
                    val destFile = File(exportFolder, destFileName)
                    
                    sourceFile.copyTo(destFile, overwrite = true)
                    copiedFiles.add(path to destFileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return copiedFiles
    }
    
    /**
     * Elimina la cartella di una nota spese e tutti i suoi contenuti
     */
    fun deleteNotaFolder(context: Context, notaSpeseId: Long): Boolean {
        return try {
            val folder = getNotaFolder(context, notaSpeseId)
            if (folder.exists()) {
                folder.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
