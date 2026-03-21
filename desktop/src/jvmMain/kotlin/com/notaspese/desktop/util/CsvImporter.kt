package com.notaspese.desktop.util

import com.notaspese.desktop.data.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvImporter {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    private val dateFormatterFile = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.ITALY)

    /**
     * Importa una nota spese da una cartella contenente il CSV e gli allegati.
     * La cartella deve avere il formato: NomeCognome_yyyy-MM-dd_HHmm
     * e contenere NotaSpese_NomeCognome.csv più i file allegati.
     */
    fun importFromFolder(folder: File): NotaSpeseConSpese? {
        val csvFiles = folder.listFiles { f -> f.extension.equals("csv", ignoreCase = true) } ?: return null
        val csvFile = csvFiles.firstOrNull() ?: return null
        return importFromCsv(csvFile, folder)
    }

    fun importFromCsv(csvFile: File, attachmentsDir: File = csvFile.parentFile!!): NotaSpeseConSpese? {
        return try {
            val lines = csvFile.readLines(Charsets.UTF_8)
            val map = lines.filter { it.contains(";") }.associate { line ->
                val idx = line.indexOf(";")
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }

            fun get(key: String) = map[key] ?: ""

            val dataInizioStr = get("Data Inizio")
            val dataFineStr = get("Data Fine")
            val dataCompStr = get("Data Compilazione")
            val dataInizio = parseDate(dataInizioStr) ?: System.currentTimeMillis()
            val dataFine = parseDate(dataFineStr) ?: dataInizio
            val dataComp = parseDate(dataCompStr) ?: dataInizio

            val nota = NotaSpese(
                numeroNota = get("Numero Nota"),
                nomeCognome = get("Nome e Cognome").ifBlank { "Import" },
                dataInizioTrasferta = dataInizio,
                oraInizioTrasferta = get("Ora Inizio"),
                dataFineTrasferta = dataFine,
                oraFineTrasferta = get("Ora Fine"),
                luogoTrasferta = get("Luogo Trasferta").ifBlank { "-" },
                cliente = get("Cliente").ifBlank { "-" },
                causale = get("Causale"),
                auto = get("Auto"),
                dataCompilazione = dataComp,
                altriTrasfertisti = get("Altri Trasfertisti"),
                anticipo = 0.0,
                kmPercorsi = 0.0,
                costoKmRimborso = 0.0,
                costoKmCliente = 0.60
            )

            val spese = mutableListOf<Spesa>()
            val dettaglioStart = lines.indexOfFirst { it.contains("DETTAGLIO SPESE") }
            if (dettaglioStart >= 0) {
                val header = lines.getOrNull(dettaglioStart + 1) ?: ""
                var i = dettaglioStart + 2
                while (i < lines.size) {
                    val line = lines[i]
                    if (line.isBlank() || line.startsWith("RIEPILOGO") || line.startsWith("SPESE SOSTENUTE") || line.startsWith("CHILOMETRI") || line.startsWith("DA RIMBORSARE") || line.startsWith("RIEPILOGO FINALE")) break
                    val parts = line.split(";")
                    if (parts.size >= 6) {
                        val data = parseDate(parts[0]) ?: dataInizio
                        val descrizione = parts.getOrNull(1) ?: ""
                        val catName = parts.getOrNull(2) ?: "ALTRO"
                        val metPag = parts.getOrNull(3) ?: "Carta di Credito"
                        val sostenuto = parts.getOrNull(4) ?: "Azienda"
                        val importo = parts.getOrNull(5)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                        val photoFile = parts.getOrNull(6)?.takeIf { it.isNotBlank() }

                        val categoria = CategoriaSpesa.entries.find { it.displayName == catName } ?: CategoriaSpesa.ALTRO
                        val metodoPagamento = when {
                            metPag.contains("Carta") || metPag.contains("Credito") -> MetodoPagamento.CARTA_CREDITO
                            metPag.contains("Contanti") || metPag.contains("Elettronico") -> MetodoPagamento.CONTANTI
                            else -> MetodoPagamento.ALTRO
                        }
                        val pagatoDa = if (sostenuto.contains("Dipendente")) PagatoDa.DIPENDENTE else PagatoDa.AZIENDA

                        var photoPath: String? = null
                        if (photoFile != null) {
                            val attFile = File(attachmentsDir, photoFile)
                            if (attFile.exists()) photoPath = attFile.absolutePath
                        }

                        spese.add(Spesa(
                            notaSpeseId = 0,
                            descrizione = descrizione,
                            importo = importo,
                            data = data,
                            metodoPagamento = metodoPagamento,
                            categoria = categoria,
                            fotoScontrinoPath = photoPath,
                            pagatoDa = pagatoDa
                        ))
                    }
                    i++
                }
            }

            // Parse km e anticipo da RIEPILOGO se presenti
            val kmStart = lines.indexOfFirst { it.contains("CHILOMETRI") }

            // Leggi CHILOMETRI
            var kmPercorsi = 0.0
            var costoKmRimborso = 0.0
            var costoKmCliente = 0.60
            if (kmStart >= 0) {
                for (j in kmStart + 1 until minOf(kmStart + 10, lines.size)) {
                    val l = lines[j]
                    if (l.isBlank() || !l.contains(";")) break
                    val parts = l.split(";", limit = 2)
                    val k = parts[0].trim()
                    val v = parts.getOrNull(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    when {
                        k.contains("Km Percorsi") -> kmPercorsi = v
                        k.contains("Costo/km Rimborso") -> costoKmRimborso = v
                        k.contains("Costo/km Cliente") -> costoKmCliente = v
                    }
                }
            }

            // Leggi anticipo da DA RIMBORSARE
            var anticipo = 0.0
            val rimborsoStart = lines.indexOfFirst { it.contains("DA RIMBORSARE") }
            if (rimborsoStart >= 0) {
                for (j in rimborsoStart + 1 until minOf(rimborsoStart + 10, lines.size)) {
                    val l = lines[j]
                    if (l.contains("Anticipo")) {
                        val v = l.split(";").getOrNull(1)?.replace(",", ".")?.replace("-", "")?.replace("+", "")?.trim()?.toDoubleOrNull() ?: 0.0
                        anticipo = v
                        break
                    }
                }
            }

            NotaSpeseConSpese(
                nota.copy(kmPercorsi = kmPercorsi, costoKmRimborso = costoKmRimborso, costoKmCliente = costoKmCliente, anticipo = anticipo),
                spese
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseDate(s: String): Long? {
        if (s.isBlank()) return null
        return try {
            dateFormatter.parse(s)?.time
        } catch (_: Exception) {
            null
        }
    }
}
