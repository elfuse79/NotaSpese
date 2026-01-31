package com.notaspese.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.notaspese.data.model.CategoriaSpesa
import com.notaspese.data.model.NotaSpeseConSpese
import com.notaspese.data.model.PagatoDa
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    
    fun exportToCsv(context: Context, notaSpeseConSpese: NotaSpeseConSpese): File? {
        return try {
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
            val fileNameDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
            val nota = notaSpeseConSpese.notaSpese
            
            // Crea la struttura cartelle: Download/Innoval Nota Spese/[Nome Nota Spese]
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val innovolDir = File(downloadsDir, "Innoval Nota Spese")
            if (!innovolDir.exists()) innovolDir.mkdirs()
            
            // Nome cartella: NomeCognome_DataInizio
            val folderName = "${nota.nomeCognome.replace(" ", "_")}_${fileNameDateFormatter.format(Date(nota.dataInizioTrasferta))}"
            val notaDir = File(innovolDir, folderName)
            if (!notaDir.exists()) notaDir.mkdirs()
            
            // Genera contenuto CSV
            val sb = StringBuilder()
            
            // INTESTAZIONE NOTA SPESE
            sb.appendLine("NOTA SPESE")
            if (nota.numeroNota.isNotBlank()) {
                sb.appendLine("Numero Nota;${nota.numeroNota}")
            }
            sb.appendLine("Nome e Cognome;${nota.nomeCognome}")
            sb.appendLine("Cliente;${nota.cliente}")
            sb.appendLine("Luogo Trasferta;${nota.luogoTrasferta}")
            sb.appendLine("Data Inizio;${dateFormatter.format(Date(nota.dataInizioTrasferta))}")
            if (nota.oraInizioTrasferta.isNotBlank()) {
                sb.appendLine("Ora Inizio;${nota.oraInizioTrasferta}")
            }
            if (nota.dataFineTrasferta != nota.dataInizioTrasferta) {
                sb.appendLine("Data Fine;${dateFormatter.format(Date(nota.dataFineTrasferta))}")
            }
            if (nota.oraFineTrasferta.isNotBlank()) {
                sb.appendLine("Ora Fine;${nota.oraFineTrasferta}")
            }
            sb.appendLine("Causale;${nota.causale}")
            sb.appendLine("Auto;${nota.auto}")
            if (nota.altriTrasfertisti.isNotBlank()) {
                sb.appendLine("Altri Trasfertisti;${nota.altriTrasfertisti}")
            }
            sb.appendLine("Data Compilazione;${dateFormatter.format(Date(nota.dataCompilazione))}")
            sb.appendLine()
            
            // DETTAGLIO SPESE
            sb.appendLine("DETTAGLIO SPESE")
            sb.appendLine("Data;Descrizione;Categoria;Metodo Pagamento;Sostenuto Da;Importo;Foto Scontrino")
            
            var photoCounter = 1
            for (spesa in notaSpeseConSpese.spese) {
                val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                var photoFileName = ""
                val pagatoDaStr = if (spesa.pagatoDa == PagatoDa.AZIENDA) "Azienda" else "Dipendente"
                
                // Copia la foto dello scontrino se esiste
                if (spesa.fotoScontrinoPath != null) {
                    try {
                        val sourceFile = getFileFromPath(context, spesa.fotoScontrinoPath)
                        if (sourceFile != null && sourceFile.exists()) {
                            val extension = sourceFile.extension.ifBlank { "jpg" }
                            photoFileName = "scontrino_${String.format("%03d", photoCounter)}_${spesa.categoria.name.lowercase()}.$extension"
                            val destFile = File(notaDir, photoFileName)
                            copyFile(sourceFile, destFile)
                            photoCounter++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                sb.appendLine("${dateFormatter.format(Date(spesa.data))};$desc;${spesa.categoria.displayName};${spesa.metodoPagamento.displayName};$pagatoDaStr;${String.format(Locale.ITALY, "%.2f", spesa.importo)};$photoFileName")
            }
            sb.appendLine()
            
            // RIEPILOGO PER CATEGORIA
            sb.appendLine("RIEPILOGO PER CATEGORIA")
            sb.appendLine("Categoria;Importo")
            for (categoria in CategoriaSpesa.entries) {
                val totale = notaSpeseConSpese.totaleByCategoria(categoria)
                if (totale > 0) {
                    sb.appendLine("${categoria.displayName};${String.format(Locale.ITALY, "%.2f", totale)}")
                }
            }
            sb.appendLine()
            
            // RIEPILOGO PER METODO PAGAMENTO
            sb.appendLine("RIEPILOGO PER METODO PAGAMENTO")
            sb.appendLine("Metodo;Importo")
            sb.appendLine("Carta di Credito;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleByCarta)}")
            sb.appendLine("Contanti;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleContanti)}")
            sb.appendLine("Altro;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleAltro)}")
            sb.appendLine()
            
            // SPESE SOSTENUTE DA
            sb.appendLine("SPESE SOSTENUTE DA")
            sb.appendLine("Sostenute da;Importo")
            sb.appendLine("Azienda;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}")
            sb.appendLine("Dipendente;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}")
            sb.appendLine()
            
            // CHILOMETRI (se presenti)
            if (nota.kmPercorsi > 0) {
                sb.appendLine("CHILOMETRI")
                sb.appendLine("Km Percorsi;${String.format(Locale.ITALY, "%.0f", nota.kmPercorsi)}")
                if (nota.costoKmRimborso > 0) {
                    sb.appendLine("Costo/km Rimborso;${String.format(Locale.ITALY, "%.2f", nota.costoKmRimborso)}")
                    sb.appendLine("RIMBORSO TRASFERTISTA;${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}")
                }
                if (nota.costoKmCliente > 0) {
                    sb.appendLine("Costo/km Cliente;${String.format(Locale.ITALY, "%.2f", nota.costoKmCliente)}")
                    sb.appendLine("ADDEBITO CLIENTE;${String.format(Locale.ITALY, "%.2f", nota.totaleCostoKmCliente)}")
                }
                sb.appendLine()
            }
            
            // RIMBORSO DIPENDENTE (se presente)
            if (notaSpeseConSpese.totaleRimborsoDipendenteLordo > 0 || nota.anticipo > 0) {
                sb.appendLine("DA RIMBORSARE AL DIPENDENTE")
                if (notaSpeseConSpese.totalePagatoDipendente > 0) {
                    sb.appendLine("Spese pagate dal dipendente;+ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}")
                }
                if (nota.totaleRimborsoKm > 0) {
                    sb.appendLine("Rimborso chilometri;+ ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}")
                }
                if (nota.anticipo > 0) {
                    sb.appendLine("Anticipo ricevuto;- ${String.format(Locale.ITALY, "%.2f", nota.anticipo)}")
                }
                sb.appendLine("TOTALE RIMBORSO DIPENDENTE;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleRimborsoDipendente)}")
                sb.appendLine()
            }
            
            // TOTALI
            sb.appendLine("RIEPILOGO FINALE")
            sb.appendLine("Spese pagate dall'Azienda;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}")
            sb.appendLine("Spese pagate dal Dipendente;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}")
            if (nota.totaleRimborsoKm > 0) {
                sb.appendLine("Rimborso Km;${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}")
            }
            sb.appendLine("COSTO COMPLESSIVO NOTA SPESE;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.costoComplessivoNotaSpese)}")
            
            // Salva file CSV
            val csvFileName = "NotaSpese_${nota.nomeCognome.replace(" ", "_")}.csv"
            val csvFile = File(notaDir, csvFileName)
            FileWriter(csvFile).use { it.write(sb.toString()) }
            
            // Ritorna la cartella (non il file) per la condivisione
            notaDir
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getFileFromPath(context: Context, path: String): File? {
        return try {
            when {
                path.startsWith("content://") -> {
                    // E' un URI content, dobbiamo copiarlo
                    val uri = Uri.parse(path)
                    val tempFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
                path.startsWith("file://") -> {
                    File(Uri.parse(path).path ?: return null)
                }
                else -> {
                    File(path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }
    
    fun shareFile(context: Context, folder: File) {
        // Trova tutti i file nella cartella
        val files = folder.listFiles() ?: return
        
        if (files.size == 1) {
            // Se c'e' solo un file (il CSV), condividi quello
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", files[0])
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Condividi Nota Spese"))
        } else {
            // Se ci sono piu' file, condividi tutti
            val uris = ArrayList<Uri>()
            for (file in files) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Condividi Nota Spese con Scontrini"))
        }
    }
    
    fun getExportFolderPath(notaSpeseConSpese: NotaSpeseConSpese): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
        val nota = notaSpeseConSpese.notaSpese
        val folderName = "${nota.nomeCognome.replace(" ", "_")}_${dateFormatter.format(Date(nota.dataInizioTrasferta))}"
        return "Download/Innoval Nota Spese/$folderName"
    }
}
