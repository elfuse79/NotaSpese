package com.notaspese.desktop.util

import com.notaspese.desktop.data.model.CategoriaSpesa
import com.notaspese.desktop.data.model.NotaSpeseConSpese
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 18f

    private fun truncateText(text: String, maxLength: Int): String =
        if (text.length > maxLength) text.take(maxLength - 3) + "..." else text

    private fun yCoord(y: Float): Float = PAGE_HEIGHT - y  // PDFBox: y=0 at bottom

    fun generatePdf(outputDir: File, notaSpeseConSpese: NotaSpeseConSpese, baseName: String? = null): File? {
        return try {
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
            val fileNameDateFormatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.ITALY)
            val nota = notaSpeseConSpese.notaSpese

            if (!outputDir.exists()) outputDir.mkdirs()

            val document = PDDocument()
            var pageNumber = 1
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)
            var contentStream = PDPageContentStream(document, page)
            var yPosition = MARGIN

            fun checkNewPage(requiredSpace: Float): Boolean {
                if (yPosition + requiredSpace > PAGE_HEIGHT - MARGIN - 30f) {
                    contentStream.setFont(PDType1Font.HELVETICA, 8f)
                    contentStream.setNonStrokingColor(Color.GRAY)
                    contentStream.beginText()
                    contentStream.newLineAtOffset(PAGE_WIDTH - MARGIN - 80, yCoord(yPosition + 20))
                    contentStream.showText("v${com.notaspese.desktop.data.model.APP_VERSION} - Pag. $pageNumber")
                    contentStream.endText()
                    contentStream.close()

                    pageNumber++
                    page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    contentStream = PDPageContentStream(document, page)
                    yPosition = MARGIN
                    return true
                }
                return false
            }

            fun drawText(text: String, x: Float, y: Float, font: PDType1Font, size: Float, color: Color = Color.BLACK) {
                contentStream.setFont(font, size)
                contentStream.setNonStrokingColor(color)
                contentStream.beginText()
                contentStream.newLineAtOffset(x, yCoord(y))
                contentStream.showText(text)
                contentStream.endText()
            }

            fun textWidth(text: String, font: PDType1Font, size: Float): Float =
                font.getStringWidth(text) / 1000f * size

            fun drawTextRight(text: String, xRight: Float, y: Float, font: PDType1Font, size: Float, color: Color = Color.BLACK) {
                val w = textWidth(text, font, size)
                drawText(text, xRight - w, y, font, size, color)
            }

            fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
                contentStream.setStrokingColor(Color(0xE0, 0xE0, 0xE0))
                contentStream.setLineWidth(1f)
                contentStream.moveTo(x1, yCoord(y1))
                contentStream.lineTo(x2, yCoord(y2))
                contentStream.stroke()
            }

            fun drawRect(x: Float, y: Float, w: Float, h: Float, fillColor: Color) {
                contentStream.setNonStrokingColor(fillColor)
                contentStream.addRect(x, yCoord(y + h), w, h)
                contentStream.fill()
            }

            // ===== INTESTAZIONE =====
            drawText("NOTA SPESE", MARGIN, yPosition + 20, PDType1Font.HELVETICA_BOLD, 20f, Color(0x15, 0x65, 0xC0))
            if (nota.numeroNota.isNotBlank()) {
                drawText("N° ${nota.numeroNota}", PAGE_WIDTH - MARGIN - 80, yPosition + 20, PDType1Font.HELVETICA_BOLD, 14f, Color(0x15, 0x65, 0xC0))
            } else {
                drawText("N° _______________", PAGE_WIDTH - MARGIN - 80, yPosition + 20, PDType1Font.HELVETICA, 10f, Color.GRAY)
            }
            yPosition += 35f

            drawText("Data compilazione: ${dateFormatter.format(Date(nota.dataCompilazione))}", MARGIN, yPosition, PDType1Font.HELVETICA, 9f, Color.GRAY)
            yPosition += 25f
            drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition)
            yPosition += 20f

            // ===== DATI TRASFERTA =====
            drawText("DATI TRASFERTA", MARGIN, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
            yPosition += 25f

            drawText("Nome:", MARGIN, yPosition, PDType1Font.HELVETICA, 11f)
            drawText(truncateText(nota.nomeCognome, 35), MARGIN + 100f, yPosition, PDType1Font.HELVETICA_BOLD, 12f, Color(0x15, 0x65, 0xC0))
            yPosition += LINE_HEIGHT
            drawText("Cliente:", MARGIN, yPosition, PDType1Font.HELVETICA, 11f)
            drawText(truncateText(nota.cliente, 35), MARGIN + 100f, yPosition, PDType1Font.HELVETICA, 11f)
            yPosition += LINE_HEIGHT
            drawText("Luogo:", MARGIN, yPosition, PDType1Font.HELVETICA, 11f)
            drawText(truncateText(nota.luogoTrasferta, 35), MARGIN + 100f, yPosition, PDType1Font.HELVETICA, 11f)
            yPosition += LINE_HEIGHT
            val oraInizio = nota.oraInizioTrasferta.ifBlank { "--:--" }
            val oraFine = nota.oraFineTrasferta.ifBlank { "--:--" }
            val inizioStr = "${dateFormatter.format(Date(nota.dataInizioTrasferta))} $oraInizio"
            val fineStr = "${dateFormatter.format(Date(nota.dataFineTrasferta))} $oraFine"
            val periodoStr = "Inizio: $inizioStr Fine: $fineStr"
            drawText("Periodo:", MARGIN, yPosition, PDType1Font.HELVETICA, 11f)
            drawText(truncateText(periodoStr, 55), MARGIN + 100f, yPosition, PDType1Font.HELVETICA, 11f)
            yPosition += LINE_HEIGHT
            if (nota.auto.isNotBlank()) {
                drawText("Auto:", MARGIN, yPosition, PDType1Font.HELVETICA, 11f)
                drawText(truncateText(nota.auto, 35), MARGIN + 100f, yPosition, PDType1Font.HELVETICA, 11f)
                yPosition += LINE_HEIGHT
            }
            if (nota.causale.isNotBlank()) {
                drawText("Causale:", MARGIN, yPosition, PDType1Font.HELVETICA, 11f)
                drawText(truncateText(nota.causale, 35), MARGIN + 100f, yPosition, PDType1Font.HELVETICA, 11f)
                yPosition += LINE_HEIGHT
            }
            if (nota.altriTrasfertisti.isNotBlank()) {
                drawText("Altri:", MARGIN, yPosition, PDType1Font.HELVETICA, 11f)
                drawText(truncateText(nota.altriTrasfertisti, 35), MARGIN + 100f, yPosition, PDType1Font.HELVETICA, 11f)
                yPosition += LINE_HEIGHT
            }
            yPosition += 15f
            drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition)
            yPosition += 25f

            // ===== DETTAGLIO SPESE =====
            val speseAzienda = notaSpeseConSpese.speseAzienda
            val speseDipendente = notaSpeseConSpese.speseDipendente
            val haSpeseDipendente = notaSpeseConSpese.haSpeseDipendente

            if (haSpeseDipendente) {
                checkNewPage(100f)
                val colWidth = (PAGE_WIDTH - MARGIN * 3) / 2
                val leftColX = MARGIN
                val rightColX = MARGIN + colWidth + MARGIN

                drawText("SPESE AZIENDA", leftColX, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
                drawText("SPESE DIPENDENTE", rightColX, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
                yPosition += 20f

                drawRect(leftColX, yPosition - 12, colWidth, 18f, Color(0x15, 0x65, 0xC0))
                drawRect(rightColX, yPosition - 12, colWidth, 18f, Color(0x2E, 0x7D, 0x32))
                drawText("Data", leftColX + 5, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("Cat.", leftColX + 58, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("Desc.", leftColX + 88, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("€", leftColX + colWidth - 5, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("Data", rightColX + 5, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("Cat.", rightColX + 58, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("Desc.", rightColX + 88, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("€", rightColX + colWidth - 5, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                yPosition += 18f

                val maxRows = maxOf(speseAzienda.size, speseDipendente.size)
                for (rowIdx in 0 until maxRows) {
                    checkNewPage(16f)
                    if (rowIdx % 2 == 1) {
                        drawRect(leftColX, yPosition - 10, colWidth, 16f, Color(0xF5, 0xF5, 0xF5))
                        drawRect(rightColX, yPosition - 10, colWidth, 16f, Color(0xF5, 0xF5, 0xF5))
                    }
                    if (rowIdx < speseAzienda.size) {
                        val spesa = speseAzienda[rowIdx]
                        val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                        drawText(dateFormatter.format(Date(spesa.data)), leftColX + 5, yPosition, PDType1Font.HELVETICA, 9f)
                        drawText(truncateText(spesa.categoria.displayName, 8), leftColX + 58, yPosition, PDType1Font.HELVETICA, 9f)
                        drawText(truncateText(desc, 16), leftColX + 88, yPosition, PDType1Font.HELVETICA, 9f)
                        drawTextRight(String.format(Locale.ITALY, "%.2f", spesa.importo), leftColX + colWidth - 5, yPosition, PDType1Font.HELVETICA, 9f)
                    }
                    if (rowIdx < speseDipendente.size) {
                        val spesa = speseDipendente[rowIdx]
                        val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                        drawText(dateFormatter.format(Date(spesa.data)), rightColX + 5, yPosition, PDType1Font.HELVETICA, 9f)
                        drawText(truncateText(spesa.categoria.displayName, 8), rightColX + 58, yPosition, PDType1Font.HELVETICA, 9f)
                        drawText(truncateText(desc, 16), rightColX + 88, yPosition, PDType1Font.HELVETICA, 9f)
                        drawTextRight(String.format(Locale.ITALY, "%.2f", spesa.importo), rightColX + colWidth - 5, yPosition, PDType1Font.HELVETICA, 9f)
                    }
                    yPosition += 16f
                }

                yPosition += 5f
                drawRect(leftColX, yPosition - 5, colWidth, 20f, Color(0xE3, 0xF2, 0xFD))
                drawRect(rightColX, yPosition - 5, colWidth, 20f, Color(0xE8, 0xF5, 0xE9))
                drawText("TOT.AZIENDA:", leftColX + 5, yPosition + 8, PDType1Font.HELVETICA_BOLD, 10f)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}", leftColX + colWidth - 5, yPosition + 8, PDType1Font.HELVETICA_BOLD, 11f, Color(0x15, 0x65, 0xC0))
                drawText("TOT.DIP.:", rightColX + 5, yPosition + 8, PDType1Font.HELVETICA_BOLD, 10f)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", rightColX + colWidth - 5, yPosition + 8, PDType1Font.HELVETICA_BOLD, 11f, Color(0x2E, 0x7D, 0x32))
                yPosition += 30f
            } else {
                checkNewPage(100f)
                drawText("DETTAGLIO SPESE", MARGIN, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
                yPosition += 20f
                drawRect(MARGIN, yPosition - 12, PAGE_WIDTH - MARGIN * 2, 18f, Color(0x15, 0x65, 0xC0))
                drawText("Data", MARGIN + 10, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("Cat.", MARGIN + 58, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("Desc.", MARGIN + 88, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                drawText("€", PAGE_WIDTH - MARGIN - 5, yPosition, PDType1Font.HELVETICA_BOLD, 9f, Color.WHITE)
                yPosition += 18f

                for ((rowIdx, spesa) in speseAzienda.withIndex()) {
                    checkNewPage(16f)
                    if (rowIdx % 2 == 1) drawRect(MARGIN, yPosition - 10, PAGE_WIDTH - MARGIN * 2, 16f, Color(0xF5, 0xF5, 0xF5))
                    val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                    drawText(dateFormatter.format(Date(spesa.data)), MARGIN + 10, yPosition, PDType1Font.HELVETICA, 9f)
                    drawText(truncateText(spesa.categoria.displayName, 8), MARGIN + 58, yPosition, PDType1Font.HELVETICA, 9f)
                    drawText(truncateText(desc, 42), MARGIN + 88, yPosition, PDType1Font.HELVETICA, 9f)
                    drawTextRight(String.format(Locale.ITALY, "%.2f", spesa.importo), PAGE_WIDTH - MARGIN - 5, yPosition, PDType1Font.HELVETICA, 9f)
                    yPosition += 16f
                }
                yPosition += 5f
                drawRect(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN * 2, 20f, Color(0xE3, 0xF2, 0xFD))
                drawText("TOT.SPESE:", MARGIN + 10, yPosition + 8, PDType1Font.HELVETICA_BOLD, 10f)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}", PAGE_WIDTH - MARGIN - 5, yPosition + 8, PDType1Font.HELVETICA_BOLD, 11f, Color(0x15, 0x65, 0xC0))
                yPosition += 30f
            }

            drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition)
            yPosition += 25f

            // ===== RIEPILOGO + SPESE SOSTENUTE =====
            checkNewPage(150f)
            val halfWidth = (PAGE_WIDTH - MARGIN * 3) / 2
            val leftX = MARGIN
            val rightX = MARGIN + halfWidth + MARGIN

            drawText("RIEPILOGO CATEGORIA", leftX, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
            if (haSpeseDipendente) drawText("SPESE SOSTENUTE DA", rightX, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
            yPosition += 20f

            var catY = yPosition
            var catCount = 0
            for (cat in CategoriaSpesa.entries) {
                if (catCount >= 7) break
                val tot = notaSpeseConSpese.totaleByCategoria(cat)
                if (tot > 0) {
                    drawText(truncateText(cat.displayName, 12), leftX + 10, catY, PDType1Font.HELVETICA, 11f)
                    drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", tot)}", leftX + halfWidth - 5, catY, PDType1Font.HELVETICA, 11f)
                    catY += LINE_HEIGHT
                    catCount++
                }
            }
            if (haSpeseDipendente) {
                drawText("Azienda", rightX + 10, yPosition, PDType1Font.HELVETICA, 11f)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}", rightX + halfWidth - 5, yPosition, PDType1Font.HELVETICA, 11f)
                drawText("Dipendente", rightX + 10, yPosition + LINE_HEIGHT, PDType1Font.HELVETICA, 11f)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", rightX + halfWidth - 5, yPosition + LINE_HEIGHT, PDType1Font.HELVETICA, 11f)
                yPosition = maxOf(catY, yPosition + LINE_HEIGHT * 2)
            } else yPosition = catY

            yPosition += 15f
            drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition)
            yPosition += 25f

            // ===== CHILOMETRI =====
            if (nota.kmPercorsi > 0) {
                checkNewPage(120f)
                drawText("CHILOMETRI", MARGIN, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
                yPosition += 20f
                drawRect(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN * 2, 80f, Color(0xFF, 0xF3, 0xE0))
                drawText("Km Percorsi:", MARGIN + 20, yPosition + 10, PDType1Font.HELVETICA, 11f)
                drawText("${String.format(Locale.ITALY, "%.0f", nota.kmPercorsi)} km", MARGIN + 200, yPosition + 10, PDType1Font.HELVETICA_BOLD, 12f, Color(0x15, 0x65, 0xC0))
                if (nota.costoKmRimborso > 0) {
                    val kmLeftEdge = 275f
                    drawText("Costo/km rimborso:", MARGIN + 20, yPosition + 28, PDType1Font.HELVETICA, 11f)
                    drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", nota.costoKmRimborso)}", kmLeftEdge, yPosition + 28, PDType1Font.HELVETICA, 11f)
                    drawText("RIMBORSO TRASFERTISTA:", MARGIN + 20, yPosition + 46, PDType1Font.HELVETICA_BOLD, 12f)
                    drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}", kmLeftEdge, yPosition + 46, PDType1Font.HELVETICA_BOLD, 14f, Color(0x2E, 0x7D, 0x32))
                }
                if (nota.costoKmCliente > 0) {
                    drawText("Costo/km cliente:", MARGIN + 280, yPosition + 10, PDType1Font.HELVETICA, 11f)
                    drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", nota.costoKmCliente)}", PAGE_WIDTH - MARGIN - 5, yPosition + 10, PDType1Font.HELVETICA, 11f)
                    drawText("ADDEBITO CLIENTE:", MARGIN + 280, yPosition + 28, PDType1Font.HELVETICA_BOLD, 12f)
                    drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", nota.totaleCostoKmCliente)}", PAGE_WIDTH - MARGIN - 5, yPosition + 28, PDType1Font.HELVETICA_BOLD, 14f, Color(0x15, 0x65, 0xC0))
                }
                yPosition += 90f
                drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition)
                yPosition += 25f
            }

            // ===== RIMBORSO DIPENDENTE =====
            if (notaSpeseConSpese.haRimborsoDipendente || nota.anticipo > 0) {
                checkNewPage(120f)
                drawText("DA RIMBORSARE AL DIPENDENTE", MARGIN, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
                yPosition += 20f
                var boxH = 50f
                if (notaSpeseConSpese.totalePagatoDipendente > 0) boxH += 18f
                if (nota.totaleRimborsoKm > 0) boxH += 18f
                if (nota.anticipo > 0) boxH += 18f
                drawRect(MARGIN, yPosition - 5, PAGE_WIDTH - MARGIN * 2, boxH, Color(0xE8, 0xF5, 0xE9))
                var ry = yPosition + 10f
                if (notaSpeseConSpese.totalePagatoDipendente > 0) {
                    drawText("Spese dipendente:", MARGIN + 20, ry, PDType1Font.HELVETICA, 11f)
                    drawTextRight("+€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", PAGE_WIDTH - MARGIN - 5, ry, PDType1Font.HELVETICA, 11f)
                    ry += 18f
                }
                if (nota.totaleRimborsoKm > 0) {
                    drawText("Rimborso km:", MARGIN + 20, ry, PDType1Font.HELVETICA, 11f)
                    drawTextRight("+€ ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}", PAGE_WIDTH - MARGIN - 5, ry, PDType1Font.HELVETICA, 11f)
                    ry += 18f
                }
                if (nota.anticipo > 0) {
                    drawText("Anticipo:", MARGIN + 20, ry, PDType1Font.HELVETICA, 11f, Color(0xC6, 0x28, 0x28))
                    drawTextRight("-€ ${String.format(Locale.ITALY, "%.2f", nota.anticipo)}", PAGE_WIDTH - MARGIN - 5, ry, PDType1Font.HELVETICA, 11f, Color(0xC6, 0x28, 0x28))
                    ry += 18f
                }
                val totRimb = notaSpeseConSpese.totaleRimborsoDipendente
                val colorRimb = if (totRimb >= 0) Color(0x2E, 0x7D, 0x32) else Color(0xC6, 0x28, 0x28)
                drawText("TOTALE RIMBORSO:", MARGIN + 20, ry + 10, PDType1Font.HELVETICA_BOLD, 12f, colorRimb)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", totRimb)}", PAGE_WIDTH - MARGIN - 5, ry + 10, PDType1Font.HELVETICA_BOLD, 14f, colorRimb)
                yPosition += boxH + 25f
                drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition)
                yPosition += 30f
            }

            // ===== COSTO COMPLESSIVO =====
            checkNewPage(150f)
            drawText("COSTO COMPLESSIVO NOTA SPESE", MARGIN, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
            yPosition += 20f
            var boxHeight = 70f
            if (haSpeseDipendente) boxHeight += 18f
            if (nota.totaleRimborsoKm > 0) boxHeight += 18f
            drawRect(MARGIN, yPosition - 10, PAGE_WIDTH - MARGIN * 2, boxHeight, Color(0xE3, 0xF2, 0xFD))
            val costRight = PAGE_WIDTH - MARGIN - 5
            var cy = yPosition + 10f
            drawText("Spese Azienda:", MARGIN + 20, cy, PDType1Font.HELVETICA, 11f)
            drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}", costRight, cy, PDType1Font.HELVETICA, 11f)
            cy += 18f
            if (haSpeseDipendente) {
                drawText("Spese Dipendente:", MARGIN + 20, cy, PDType1Font.HELVETICA, 11f)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}", costRight, cy, PDType1Font.HELVETICA, 11f)
                cy += 18f
            }
            if (nota.totaleRimborsoKm > 0) {
                drawText("Rimborso Km:", MARGIN + 20, cy, PDType1Font.HELVETICA, 11f)
                drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}", costRight, cy, PDType1Font.HELVETICA, 11f)
                cy += 18f
            }
            cy += 5f
            drawText("COSTO TOTALE:", MARGIN + 20, cy, PDType1Font.HELVETICA_BOLD, 12f)
            drawTextRight("€ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.costoComplessivoNotaSpese)}", costRight, cy, PDType1Font.HELVETICA_BOLD, 14f, Color(0x15, 0x65, 0xC0))
            yPosition += boxHeight + 25f

            // ===== NOTE =====
            checkNewPage(180f)
            drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition)
            yPosition += 20f
            drawText("NOTE", MARGIN, yPosition, PDType1Font.HELVETICA_BOLD, 14f)
            yPosition += 20f
            contentStream.setNonStrokingColor(Color(0xFA, 0xFA, 0xFA))
            contentStream.addRect(MARGIN, yCoord(yPosition + 180), PAGE_WIDTH - MARGIN * 2, 180f)
            contentStream.fill()
            contentStream.setStrokingColor(Color(0xE0, 0xE0, 0xE0))
            contentStream.addRect(MARGIN, yCoord(yPosition + 180), PAGE_WIDTH - MARGIN * 2, 180f)
            contentStream.stroke()
            var noteY = yPosition + 10f
            for (spesa in notaSpeseConSpese.spese.filter { it.descrizione.isNotBlank() }.take(6)) {
                val descText = "• ${truncateText(spesa.descrizione, 70)} (${truncateText(spesa.categoria.displayName, 10)}, €${String.format(Locale.ITALY, "%.2f", spesa.importo)})"
                drawText(truncateText(descText, 95), MARGIN + 10, noteY, PDType1Font.HELVETICA, 9f, Color.DARK_GRAY)
                noteY += 16f
            }
            yPosition += 195f

            drawText("v${com.notaspese.desktop.data.model.APP_VERSION} - Pag. $pageNumber", PAGE_WIDTH - MARGIN - 80, yPosition, PDType1Font.HELVETICA, 8f, Color.GRAY)

            contentStream.close()

            val pdfFileName = baseName?.let { "$it.pdf" } ?: "NotaSpese_${nota.nomeCognome.replace(" ", "_")}_${fileNameDateFormatter.format(Date(nota.dataInizioTrasferta))}.pdf"
            val pdfFile = File(outputDir, pdfFileName)
            document.save(pdfFile)
            document.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
