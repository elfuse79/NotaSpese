package com.notaspese.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.notaspese.data.model.CategoriaSpesa
import com.notaspese.data.model.NotaSpeseConSpese
import com.notaspese.data.model.PagatoDa
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    
    private const val PAGE_WIDTH = 595  // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 18f
    
    fun generatePdf(context: Context, notaSpeseConSpese: NotaSpeseConSpese): File? {
        return try {
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
            val fileNameDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
            val nota = notaSpeseConSpese.notaSpese
            
            // Crea la struttura cartelle
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val innovolDir = File(downloadsDir, "Innoval Nota Spese")
            if (!innovolDir.exists()) innovolDir.mkdirs()
            
            val folderName = "${nota.nomeCognome.replace(" ", "_")}_${fileNameDateFormatter.format(Date(nota.dataInizioTrasferta))}"
            val notaDir = File(innovolDir, folderName)
            if (!notaDir.exists()) notaDir.mkdirs()
            
            val document = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = MARGIN
            
            // Paint styles
            val titlePaint = Paint().apply {
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#1565C0")
            }
            
            val headerPaint = Paint().apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }
            
            val normalPaint = Paint().apply {
                textSize = 11f
                typeface = Typeface.DEFAULT
                color = android.graphics.Color.BLACK
            }
            
            val smallPaint = Paint().apply {
                textSize = 9f
                typeface = Typeface.DEFAULT
                color = android.graphics.Color.GRAY
            }
            
            val linePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#E0E0E0")
                strokeWidth = 1f
            }
            
            val accentPaint = Paint().apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#1565C0")
            }
            
            // Helper function to check and create new page if needed
            fun checkNewPage(requiredSpace: Float): Boolean {
                if (yPosition + requiredSpace > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = MARGIN
                    return true
                }
                return false
            }
            
            // ===== INTESTAZIONE =====
            canvas.drawText("NOTA SPESE", MARGIN, yPosition + 20f, titlePaint)
            
            // Numero nota in alto a destra (se presente)
            if (nota.numeroNota.isNotBlank()) {
                val numeroNotaPaint = Paint().apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#1565C0")
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("N° ${nota.numeroNota}", PAGE_WIDTH - MARGIN, yPosition + 20f, numeroNotaPaint)
            } else {
                // Spazio vuoto per compilazione manuale
                val numeroNotaLabelPaint = Paint().apply {
                    textSize = 10f
                    typeface = Typeface.DEFAULT
                    color = android.graphics.Color.GRAY
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("N° _______________", PAGE_WIDTH - MARGIN, yPosition + 20f, numeroNotaLabelPaint)
            }
            yPosition += 35f
            
            // Data compilazione
            canvas.drawText("Data compilazione: ${dateFormatter.format(Date(nota.dataCompilazione))}", MARGIN, yPosition, smallPaint)
            yPosition += 25f
            
            // Linea separatore
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 20f
            
            // ===== DATI TRASFERTA =====
            canvas.drawText("DATI TRASFERTA", MARGIN, yPosition, headerPaint)
            yPosition += 25f
            
            // Nome e Cognome
            canvas.drawText("Nome e Cognome:", MARGIN, yPosition, normalPaint)
            canvas.drawText(nota.nomeCognome, MARGIN + 150f, yPosition, accentPaint)
            yPosition += LINE_HEIGHT
            
            // Cliente
            canvas.drawText("Cliente:", MARGIN, yPosition, normalPaint)
            canvas.drawText(nota.cliente, MARGIN + 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT
            
            // Luogo
            canvas.drawText("Luogo Trasferta:", MARGIN, yPosition, normalPaint)
            canvas.drawText(nota.luogoTrasferta, MARGIN + 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT
            
            // Date
            val dataFineStr = if (nota.dataFineTrasferta != nota.dataInizioTrasferta) {
                " - ${dateFormatter.format(Date(nota.dataFineTrasferta))}"
            } else ""
            canvas.drawText("Periodo:", MARGIN, yPosition, normalPaint)
            canvas.drawText("${dateFormatter.format(Date(nota.dataInizioTrasferta))}$dataFineStr", MARGIN + 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT
            
            // Orari (se presenti)
            if (nota.oraInizioTrasferta.isNotBlank() || nota.oraFineTrasferta.isNotBlank()) {
                val oraInizio = nota.oraInizioTrasferta.ifBlank { "--:--" }
                val oraFine = nota.oraFineTrasferta.ifBlank { "--:--" }
                canvas.drawText("Orario:", MARGIN, yPosition, normalPaint)
                canvas.drawText("$oraInizio - $oraFine", MARGIN + 150f, yPosition, normalPaint)
                yPosition += LINE_HEIGHT
            }
            
            // Auto
            if (nota.auto.isNotBlank()) {
                canvas.drawText("Auto:", MARGIN, yPosition, normalPaint)
                canvas.drawText(nota.auto, MARGIN + 150f, yPosition, normalPaint)
                yPosition += LINE_HEIGHT
            }
            
            // Causale
            if (nota.causale.isNotBlank()) {
                canvas.drawText("Causale:", MARGIN, yPosition, normalPaint)
                canvas.drawText(nota.causale, MARGIN + 150f, yPosition, normalPaint)
                yPosition += LINE_HEIGHT
            }
            
            // Altri trasfertisti
            if (nota.altriTrasfertisti.isNotBlank()) {
                canvas.drawText("Altri trasfertisti:", MARGIN, yPosition, normalPaint)
                canvas.drawText(nota.altriTrasfertisti, MARGIN + 150f, yPosition, normalPaint)
                yPosition += LINE_HEIGHT
            }
            
            yPosition += 15f
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 25f
            
            // ===== DETTAGLIO SPESE - DUE COLONNE =====
            val speseAzienda = notaSpeseConSpese.speseAzienda
            val speseDipendente = notaSpeseConSpese.speseDipendente
            
            val tableRowPaint = Paint().apply {
                textSize = 9f
                typeface = Typeface.DEFAULT
                color = android.graphics.Color.BLACK
            }
            
            val tablePaint = Paint().apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.WHITE
            }
            
            val altRowBg = Paint().apply {
                color = android.graphics.Color.parseColor("#F5F5F5")
            }
            
            val colWidth = (PAGE_WIDTH - MARGIN * 3) / 2  // Larghezza colonna
            val leftColX = MARGIN
            val rightColX = MARGIN + colWidth + MARGIN
            
            // ===== COLONNA SINISTRA: SPESE AZIENDA =====
            checkNewPage(100f)
            
            val aziendaHeaderBg = Paint().apply {
                color = android.graphics.Color.parseColor("#1565C0")  // Blu per azienda
            }
            
            canvas.drawText("SPESE SOSTENUTE DALL'AZIENDA", leftColX, yPosition, headerPaint)
            canvas.drawText("SPESE SOSTENUTE DAL DIPENDENTE", rightColX, yPosition, headerPaint)
            yPosition += 20f
            
            // Headers tabelle
            val headerHeight = 18f
            canvas.drawRect(leftColX, yPosition - 12f, leftColX + colWidth, yPosition + 6f, aziendaHeaderBg)
            canvas.drawText("Data", leftColX + 5f, yPosition, tablePaint)
            canvas.drawText("Descrizione", leftColX + 55f, yPosition, tablePaint)
            canvas.drawText("Categoria", leftColX + 140f, yPosition, tablePaint)
            canvas.drawText("Importo", leftColX + colWidth - 55f, yPosition, tablePaint)
            
            val dipendenteHeaderBg = Paint().apply {
                color = android.graphics.Color.parseColor("#2E7D32")  // Verde per dipendente
            }
            
            canvas.drawRect(rightColX, yPosition - 12f, rightColX + colWidth, yPosition + 6f, dipendenteHeaderBg)
            canvas.drawText("Data", rightColX + 5f, yPosition, tablePaint)
            canvas.drawText("Descrizione", rightColX + 55f, yPosition, tablePaint)
            canvas.drawText("Categoria", rightColX + 140f, yPosition, tablePaint)
            canvas.drawText("Importo", rightColX + colWidth - 55f, yPosition, tablePaint)
            yPosition += 18f
            
            // Determina il numero massimo di righe tra le due colonne
            val maxRows = maxOf(speseAzienda.size, speseDipendente.size)
            
            for (rowIdx in 0 until maxRows) {
                checkNewPage(16f)
                
                // Sfondo alternato per entrambe le colonne
                if (rowIdx % 2 == 1) {
                    canvas.drawRect(leftColX, yPosition - 10f, leftColX + colWidth, yPosition + 6f, altRowBg)
                    canvas.drawRect(rightColX, yPosition - 10f, rightColX + colWidth, yPosition + 6f, altRowBg)
                }
                
                // Colonna sinistra (Azienda)
                if (rowIdx < speseAzienda.size) {
                    val spesa = speseAzienda[rowIdx]
                    val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                    val truncatedDesc = if (desc.length > 15) desc.take(12) + "..." else desc
                    
                    canvas.drawText(dateFormatter.format(Date(spesa.data)), leftColX + 5f, yPosition, tableRowPaint)
                    canvas.drawText(truncatedDesc, leftColX + 55f, yPosition, tableRowPaint)
                    canvas.drawText(spesa.categoria.displayName, leftColX + 140f, yPosition, tableRowPaint)
                    canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", spesa.importo)}", leftColX + colWidth - 55f, yPosition, tableRowPaint)
                }
                
                // Colonna destra (Dipendente)
                if (rowIdx < speseDipendente.size) {
                    val spesa = speseDipendente[rowIdx]
                    val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                    val truncatedDesc = if (desc.length > 15) desc.take(12) + "..." else desc
                    
                    canvas.drawText(dateFormatter.format(Date(spesa.data)), rightColX + 5f, yPosition, tableRowPaint)
                    canvas.drawText(truncatedDesc, rightColX + 55f, yPosition, tableRowPaint)
                    canvas.drawText(spesa.categoria.displayName, rightColX + 140f, yPosition, tableRowPaint)
                    canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", spesa.importo)}", rightColX + colWidth - 55f, yPosition, tableRowPaint)
                }
                
                yPosition += 16f
            }
            
            // Totali per colonna
            yPosition += 5f
            val totalRowPaint = Paint().apply {
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }
            
            val aziendaTotalBg = Paint().apply {
                color = android.graphics.Color.parseColor("#E3F2FD")
            }
            val dipendenteTotalBg = Paint().apply {
                color = android.graphics.Color.parseColor("#E8F5E9")
            }
            
            canvas.drawRect(leftColX, yPosition - 5f, leftColX + colWidth, yPosition + 15f, aziendaTotalBg)
            canvas.drawText("TOTALE AZIENDA:", leftColX + 5f, yPosition + 8f, totalRowPaint)
            val aziendaTotalPaint = Paint().apply {
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#1565C0")
            }
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}", leftColX + colWidth - 70f, yPosition + 8f, aziendaTotalPaint)
            
            canvas.drawRect(rightColX, yPosition - 5f, rightColX + colWidth, yPosition + 15f, dipendenteTotalBg)
            canvas.drawText("TOTALE DIPENDENTE:", rightColX + 5f, yPosition + 8f, totalRowPaint)
            val dipendenteTotalPaint = Paint().apply {
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#2E7D32")
            }
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", rightColX + colWidth - 70f, yPosition + 8f, dipendenteTotalPaint)
            
            yPosition += 30f
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 25f
            
            // ===== RIEPILOGO PER CATEGORIA =====
            checkNewPage(150f)
            canvas.drawText("RIEPILOGO PER CATEGORIA", MARGIN, yPosition, headerPaint)
            yPosition += 25f
            
            for (categoria in CategoriaSpesa.entries) {
                val totale = notaSpeseConSpese.totaleByCategoria(categoria)
                if (totale > 0) {
                    canvas.drawText(categoria.displayName, MARGIN + 20f, yPosition, normalPaint)
                    canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", totale)}", MARGIN + 200f, yPosition, normalPaint)
                    yPosition += LINE_HEIGHT
                }
            }
            
            yPosition += 15f
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 25f
            
            // ===== RIEPILOGO PER METODO PAGAMENTO =====
            checkNewPage(100f)
            canvas.drawText("RIEPILOGO PER METODO PAGAMENTO", MARGIN, yPosition, headerPaint)
            yPosition += 25f
            
            canvas.drawText("Carta di Credito", MARGIN + 20f, yPosition, normalPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleByCarta)}", MARGIN + 200f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT
            
            canvas.drawText("Contanti", MARGIN + 20f, yPosition, normalPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleContanti)}", MARGIN + 200f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT
            
            canvas.drawText("Altro", MARGIN + 20f, yPosition, normalPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleAltro)}", MARGIN + 200f, yPosition, normalPaint)
            yPosition += 25f
            
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 30f
            
            // ===== SPESE SOSTENUTE DA =====
            checkNewPage(80f)
            canvas.drawText("SPESE SOSTENUTE DA", MARGIN, yPosition, headerPaint)
            yPosition += 25f
            
            canvas.drawText("Sostenute dall'Azienda", MARGIN + 20f, yPosition, normalPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}", MARGIN + 200f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT
            
            canvas.drawText("Sostenute dal Dipendente", MARGIN + 20f, yPosition, normalPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", MARGIN + 200f, yPosition, normalPaint)
            yPosition += 25f
            
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 30f
            
            // ===== SEZIONE CHILOMETRI (se presenti) =====
            if (nota.kmPercorsi > 0) {
                checkNewPage(120f)
                
                val kmBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FFF3E0")
                }
                
                val kmLabelPaint = Paint().apply {
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.BLACK
                }
                
                canvas.drawText("CHILOMETRI", MARGIN, yPosition, headerPaint)
                yPosition += 20f
                
                canvas.drawRect(MARGIN, yPosition - 5f, PAGE_WIDTH - MARGIN, yPosition + 75f, kmBgPaint)
                
                canvas.drawText("Km Percorsi:", MARGIN + 20f, yPosition + 10f, normalPaint)
                canvas.drawText("${String.format(Locale.ITALY, "%.0f", nota.kmPercorsi)} km", MARGIN + 200f, yPosition + 10f, accentPaint)
                
                if (nota.costoKmRimborso > 0) {
                    canvas.drawText("Costo/km rimborso:", MARGIN + 20f, yPosition + 28f, normalPaint)
                    canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", nota.costoKmRimborso)}", MARGIN + 200f, yPosition + 28f, normalPaint)
                    
                    canvas.drawText("RIMBORSO TRASFERTISTA:", MARGIN + 20f, yPosition + 46f, kmLabelPaint)
                    val rimborsoKmPaint = Paint().apply {
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = android.graphics.Color.parseColor("#2E7D32")
                    }
                    canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}", MARGIN + 200f, yPosition + 46f, rimborsoKmPaint)
                }
                
                if (nota.costoKmCliente > 0) {
                    canvas.drawText("Costo/km cliente:", MARGIN + 280f, yPosition + 10f, normalPaint)
                    canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", nota.costoKmCliente)}", MARGIN + 420f, yPosition + 10f, normalPaint)
                    
                    canvas.drawText("ADDEBITO CLIENTE:", MARGIN + 280f, yPosition + 28f, kmLabelPaint)
                    val addebitoClientePaint = Paint().apply {
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = android.graphics.Color.parseColor("#1565C0")
                    }
                    canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", nota.totaleCostoKmCliente)}", MARGIN + 420f, yPosition + 28f, addebitoClientePaint)
                }
                
                yPosition += 90f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 25f
            }
            
            // ===== RIMBORSO DIPENDENTE (se ci sono spese pagate dal dipendente, rimborso km o anticipo) =====
            if (notaSpeseConSpese.totaleRimborsoDipendenteLordo > 0 || nota.anticipo > 0) {
                checkNewPage(120f)
                
                val rimborsoBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#E8F5E9")
                }
                
                canvas.drawText("DA RIMBORSARE AL DIPENDENTE", MARGIN, yPosition, headerPaint)
                yPosition += 20f
                
                var rimborsoBoxHeight = 50f
                if (notaSpeseConSpese.totalePagatoDipendente > 0) rimborsoBoxHeight += 18f
                if (nota.totaleRimborsoKm > 0) rimborsoBoxHeight += 18f
                if (nota.anticipo > 0) rimborsoBoxHeight += 18f
                
                canvas.drawRect(MARGIN, yPosition - 5f, PAGE_WIDTH - MARGIN, yPosition + rimborsoBoxHeight, rimborsoBgPaint)
                
                var rimborsoY = yPosition + 10f
                
                if (notaSpeseConSpese.totalePagatoDipendente > 0) {
                    canvas.drawText("Spese pagate dal dipendente:", MARGIN + 20f, rimborsoY, normalPaint)
                    canvas.drawText("+ € ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", MARGIN + 250f, rimborsoY, normalPaint)
                    rimborsoY += 18f
                }
                
                if (nota.totaleRimborsoKm > 0) {
                    canvas.drawText("Rimborso chilometri:", MARGIN + 20f, rimborsoY, normalPaint)
                    canvas.drawText("+ € ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}", MARGIN + 250f, rimborsoY, normalPaint)
                    rimborsoY += 18f
                }
                
                if (nota.anticipo > 0) {
                    val anticipoPaint = Paint().apply {
                        textSize = 11f
                        typeface = Typeface.DEFAULT
                        color = android.graphics.Color.parseColor("#C62828")  // Rosso
                    }
                    canvas.drawText("Anticipo ricevuto:", MARGIN + 20f, rimborsoY, anticipoPaint)
                    canvas.drawText("- € ${String.format(Locale.ITALY, "%.2f", nota.anticipo)}", MARGIN + 250f, rimborsoY, anticipoPaint)
                    rimborsoY += 18f
                }
                
                val totaleRimborsoColore = if (notaSpeseConSpese.totaleRimborsoDipendente >= 0) 
                    android.graphics.Color.parseColor("#2E7D32")  // Verde
                else 
                    android.graphics.Color.parseColor("#C62828")  // Rosso
                
                val rimborsoTotalPaint = Paint().apply {
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = totaleRimborsoColore
                }
                
                val rimborsoLabelPaint = Paint().apply {
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = totaleRimborsoColore
                }
                
                canvas.drawText("TOTALE RIMBORSO DIPENDENTE:", MARGIN + 20f, rimborsoY + 10f, rimborsoLabelPaint)
                canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleRimborsoDipendente)}", MARGIN + 250f, rimborsoY + 10f, rimborsoTotalPaint)
                
                yPosition += rimborsoBoxHeight + 25f
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 30f
            }
            
            // ===== TOTALI FINALI =====
            checkNewPage(150f)
            
            val totalBgPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#E3F2FD")
            }
            
            val totalLabelPaint = Paint().apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }
            
            val totalValuePaint = Paint().apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#1565C0")
            }
            
            canvas.drawText("COSTO COMPLESSIVO NOTA SPESE", MARGIN, yPosition, headerPaint)
            yPosition += 20f
            
            // Calcola altezza box in base ai contenuti
            var boxHeight = 90f
            if (nota.totaleRimborsoKm > 0) boxHeight += 20f
            
            // Box totali
            canvas.drawRect(MARGIN, yPosition - 10f, PAGE_WIDTH - MARGIN, yPosition + boxHeight, totalBgPaint)
            
            var currentY = yPosition + 10f
            
            // Spese pagate dall'azienda
            canvas.drawText("Spese pagate dall'Azienda:", MARGIN + 20f, currentY, normalPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}", MARGIN + 250f, currentY, normalPaint)
            currentY += 18f
            
            // Spese pagate dal dipendente
            canvas.drawText("Spese pagate dal Dipendente:", MARGIN + 20f, currentY, normalPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", MARGIN + 250f, currentY, normalPaint)
            currentY += 18f
            
            if (nota.totaleRimborsoKm > 0) {
                canvas.drawText("Rimborso Km:", MARGIN + 20f, currentY, normalPaint)
                canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}", MARGIN + 250f, currentY, normalPaint)
                currentY += 18f
            }
            
            currentY += 10f
            
            // Costo complessivo
            val finalTotalPaint = Paint().apply {
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#1565C0")
            }
            
            val finalLabelPaint = Paint().apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }
            
            canvas.drawText("COSTO TOTALE:", MARGIN + 20f, currentY, finalLabelPaint)
            canvas.drawText("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.costoComplessivoNotaSpese)}", MARGIN + 250f, currentY, finalTotalPaint)
            
            // Finish current page
            document.finishPage(page)
            
            // ===== PAGINE ALLEGATI (FOTO E PDF) =====
            val speseConAllegati = notaSpeseConSpese.spese.filter { it.fotoScontrinoPath != null }
            
            if (speseConAllegati.isNotEmpty()) {
                // Configurazione collage: 2 colonne x 3 righe per pagina
                val IMAGES_PER_ROW = 2
                val ROWS_PER_PAGE = 3
                val IMAGES_PER_PAGE = IMAGES_PER_ROW * ROWS_PER_PAGE
                
                val imageWidth = (PAGE_WIDTH - MARGIN * 3) / IMAGES_PER_ROW
                val imageHeight = (PAGE_HEIGHT - MARGIN * 2 - 60f) / ROWS_PER_PAGE - 30f // Spazio per didascalia
                
                val attachmentTitlePaint = Paint().apply {
                    textSize = 16f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#1565C0")
                }
                
                val captionPaint = Paint().apply {
                    textSize = 9f
                    typeface = Typeface.DEFAULT
                    color = android.graphics.Color.BLACK
                }
                
                val pdfIconPaint = Paint().apply {
                    textSize = 48f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#D32F2F")
                    textAlign = Paint.Align.CENTER
                }
                
                val pdfLabelPaint = Paint().apply {
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#D32F2F")
                    textAlign = Paint.Align.CENTER
                }
                
                val borderPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    color = android.graphics.Color.parseColor("#BDBDBD")
                }
                
                val pdfBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FFEBEE")
                }
                
                var attachmentIndex = 0
                val totalPages = (speseConAllegati.size + IMAGES_PER_PAGE - 1) / IMAGES_PER_PAGE
                
                for (pageIdx in 0 until totalPages) {
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    
                    // Titolo pagina allegati
                    val pageTitle = if (totalPages > 1) {
                        "ALLEGATI SCONTRINI (${pageIdx + 1}/$totalPages)"
                    } else {
                        "ALLEGATI SCONTRINI"
                    }
                    canvas.drawText(pageTitle, MARGIN, MARGIN + 20f, attachmentTitlePaint)
                    
                    val startY = MARGIN + 50f
                    
                    for (row in 0 until ROWS_PER_PAGE) {
                        for (col in 0 until IMAGES_PER_ROW) {
                            if (attachmentIndex >= speseConAllegati.size) break
                            
                            val spesa = speseConAllegati[attachmentIndex]
                            val x = MARGIN + col * (imageWidth + MARGIN)
                            val y = startY + row * (imageHeight + 40f)
                            
                            val isPdfFile = spesa.fotoScontrinoPath?.endsWith(".pdf", ignoreCase = true) == true
                            
                            if (isPdfFile) {
                                // Disegna placeholder per PDF
                                canvas.drawRect(x, y, x + imageWidth, y + imageHeight, pdfBgPaint)
                                canvas.drawRect(x, y, x + imageWidth, y + imageHeight, borderPaint)
                                
                                // Icona PDF
                                canvas.drawText("PDF", x + imageWidth / 2, y + imageHeight / 2 - 10f, pdfIconPaint)
                                canvas.drawText("Documento", x + imageWidth / 2, y + imageHeight / 2 + 20f, pdfLabelPaint)
                            } else {
                                // Carica e disegna immagine
                                val bitmap = loadBitmapFromPath(context, spesa.fotoScontrinoPath!!, imageWidth.toInt(), imageHeight.toInt())
                                if (bitmap != null) {
                                    // Calcola posizione per centrare l'immagine mantenendo aspect ratio
                                    val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                    val dstRatio = imageWidth / imageHeight
                                    
                                    val drawWidth: Float
                                    val drawHeight: Float
                                    if (srcRatio > dstRatio) {
                                        drawWidth = imageWidth
                                        drawHeight = imageWidth / srcRatio
                                    } else {
                                        drawHeight = imageHeight
                                        drawWidth = imageHeight * srcRatio
                                    }
                                    
                                    val drawX = x + (imageWidth - drawWidth) / 2
                                    val drawY = y + (imageHeight - drawHeight) / 2
                                    
                                    // Sfondo grigio chiaro
                                    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F5F5F5") }
                                    canvas.drawRect(x, y, x + imageWidth, y + imageHeight, bgPaint)
                                    
                                    // Disegna immagine
                                    val destRect = RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight)
                                    canvas.drawBitmap(bitmap, null, destRect, null)
                                    
                                    // Bordo
                                    canvas.drawRect(x, y, x + imageWidth, y + imageHeight, borderPaint)
                                    
                                    bitmap.recycle()
                                } else {
                                    // Fallback se immagine non caricata
                                    val errorBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#EEEEEE") }
                                    canvas.drawRect(x, y, x + imageWidth, y + imageHeight, errorBgPaint)
                                    canvas.drawRect(x, y, x + imageWidth, y + imageHeight, borderPaint)
                                    
                                    val errorPaint = Paint().apply {
                                        textSize = 10f
                                        color = android.graphics.Color.GRAY
                                        textAlign = Paint.Align.CENTER
                                    }
                                    canvas.drawText("Immagine non disponibile", x + imageWidth / 2, y + imageHeight / 2, errorPaint)
                                }
                            }
                            
                            // Didascalia sotto l'immagine
                            val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                            val truncDesc = if (desc.length > 30) desc.take(27) + "..." else desc
                            val captionText = "${attachmentIndex + 1}. $truncDesc - €${String.format(Locale.ITALY, "%.2f", spesa.importo)}"
                            canvas.drawText(captionText, x, y + imageHeight + 15f, captionPaint)
                            canvas.drawText(dateFormatter.format(Date(spesa.data)), x, y + imageHeight + 27f, captionPaint)
                            
                            attachmentIndex++
                        }
                        if (attachmentIndex >= speseConAllegati.size) break
                    }
                    
                    document.finishPage(page)
                }
            }
            
            // Save PDF
            val pdfFileName = "NotaSpese_${nota.nomeCognome.replace(" ", "_")}.pdf"
            val pdfFile = File(notaDir, pdfFileName)
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Condividi PDF Nota Spese"))
    }
    
    fun getNotaFolder(notaSpeseConSpese: NotaSpeseConSpese): File {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)
        val nota = notaSpeseConSpese.notaSpese
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val innovolDir = File(downloadsDir, "Innoval Nota Spese")
        val folderName = "${nota.nomeCognome.replace(" ", "_")}_${dateFormatter.format(Date(nota.dataInizioTrasferta))}"
        return File(innovolDir, folderName)
    }
    
    /**
     * Carica un bitmap da un path (file:// o content://) con ridimensionamento e correzione rotazione
     */
    private fun loadBitmapFromPath(context: Context, path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            when {
                path.startsWith("content://") -> {
                    val uri = Uri.parse(path)
                    loadBitmapFromUri(context, uri, targetWidth, targetHeight)
                }
                path.startsWith("file://") -> {
                    val filePath = Uri.parse(path).path ?: return null
                    loadBitmapFromFile(context, File(filePath), targetWidth, targetHeight)
                }
                else -> {
                    loadBitmapFromFile(context, File(path), targetWidth, targetHeight)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun loadBitmapFromUri(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            // Prima ottieni le dimensioni originali
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            
            // Calcola il sample size per ridimensionare
            options.inSampleSize = calculateInSampleSize(options, targetWidth * 2, targetHeight * 2)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            
            // Carica il bitmap ridimensionato
            var bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return null
            
            // Correggi la rotazione EXIF
            bitmap = correctBitmapRotation(context, uri, bitmap)
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun loadBitmapFromFile(context: Context, file: File, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (!file.exists()) return null
        
        return try {
            // Prima ottieni le dimensioni originali
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            // Calcola il sample size per ridimensionare
            options.inSampleSize = calculateInSampleSize(options, targetWidth * 2, targetHeight * 2)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            
            // Carica il bitmap ridimensionato
            var bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            
            // Correggi la rotazione EXIF
            bitmap = correctBitmapRotationFromFile(file, bitmap)
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun correctBitmapRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            
            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            bitmap
        }
    }
    
    private fun correctBitmapRotationFromFile(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            bitmap
        }
    }
    
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }
}
