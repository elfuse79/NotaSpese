package com.notaspese.util

import android.content.Context
import android.net.Uri
import com.notaspese.data.model.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object CsvImporter {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)

    /**
     * Importa una nota spese da un file CSV.
     * @param context Context per accedere ai file
     * @param csvUri URI del file CSV (content://)
     * @param attachmentsFolderUri URI della cartella allegati (opzionale, se null usa la cartella del CSV)
     * @return NotaSpeseConSpese o null in caso di errore
     */
    @Suppress("UNUSED_PARAMETER")
    fun importFromCsv(context: Context, csvUri: Uri, _attachmentsFolderUri: Uri? = null): NotaSpeseConSpese? {
        return try {
            val lines = context.contentResolver.openInputStream(csvUri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readLines()
            } ?: return null

            val map = lines.filter { it.contains(";") }.associate { line ->
                val idx = line.indexOf(";")
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }

            fun get(key: String) = map[key] ?: ""

            val dataInizio = parseDate(get("Data Inizio")) ?: System.currentTimeMillis()
            val dataFine = parseDate(get("Data Fine")) ?: dataInizio
            val dataComp = parseDate(get("Data Compilazione")) ?: dataInizio

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

            var kmPercorsi = 0.0
            var costoKmRimborso = 0.0
            var costoKmCliente = 0.60
            var anticipo = 0.0

            val kmStart = lines.indexOfFirst { it.contains("CHILOMETRI") }
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

            val rimborsoStart = lines.indexOfFirst { it.contains("DA RIMBORSARE") }
            if (rimborsoStart >= 0) {
                for (j in rimborsoStart + 1 until minOf(rimborsoStart + 10, lines.size)) {
                    val l = lines[j]
                    if (l.contains("Anticipo")) {
                        anticipo = l.split(";").getOrNull(1)?.replace(",", ".")?.replace("-", "")?.replace("+", "")?.trim()?.toDoubleOrNull() ?: 0.0
                        break
                    }
                }
            }

            val notaWithKm = nota.copy(kmPercorsi = kmPercorsi, costoKmRimborso = costoKmRimborso, costoKmCliente = costoKmCliente, anticipo = anticipo)

            val spese = mutableListOf<Spesa>()
            val dettaglioStart = lines.indexOfFirst { it.contains("DETTAGLIO SPESE") }
            if (dettaglioStart >= 0) {
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

                        spese.add(Spesa(
                            notaSpeseId = 0,
                            descrizione = descrizione,
                            importo = importo,
                            data = data,
                            metodoPagamento = metodoPagamento,
                            categoria = categoria,
                            fotoScontrinoPath = photoFile,
                            pagatoDa = pagatoDa
                        ))
                    }
                    i++
                }
            }

            NotaSpeseConSpese(notaWithKm, spese)
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
