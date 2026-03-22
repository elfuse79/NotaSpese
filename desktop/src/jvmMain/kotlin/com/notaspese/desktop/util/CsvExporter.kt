package com.notaspese.desktop.util

import com.notaspese.desktop.data.model.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportToCsv(notaSpeseConSpese: NotaSpeseConSpese, outputDir: File, baseName: String? = null): File? {
        return try {
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
            val nota = notaSpeseConSpese.notaSpese

            if (!outputDir.exists()) outputDir.mkdirs()

            val sb = StringBuilder()
            sb.appendLine("NOTA SPESE")
            if (nota.numeroNota.isNotBlank()) sb.appendLine("Numero Nota;${nota.numeroNota}")
            sb.appendLine("Nome e Cognome;${nota.nomeCognome}")
            sb.appendLine("Cliente;${nota.cliente}")
            sb.appendLine("Luogo Trasferta;${nota.luogoTrasferta}")
            sb.appendLine("Data Inizio;${dateFormatter.format(Date(nota.dataInizioTrasferta))}")
            if (nota.oraInizioTrasferta.isNotBlank()) sb.appendLine("Ora Inizio;${nota.oraInizioTrasferta}")
            if (nota.dataFineTrasferta != nota.dataInizioTrasferta) sb.appendLine("Data Fine;${dateFormatter.format(Date(nota.dataFineTrasferta))}")
            if (nota.oraFineTrasferta.isNotBlank()) sb.appendLine("Ora Fine;${nota.oraFineTrasferta}")
            sb.appendLine("Causale;${nota.causale}")
            sb.appendLine("Auto;${nota.auto}")
            if (nota.altriTrasfertisti.isNotBlank()) sb.appendLine("Altri Trasfertisti;${nota.altriTrasfertisti}")
            sb.appendLine("Data Compilazione;${dateFormatter.format(Date(nota.dataCompilazione))}")
            sb.appendLine()

            sb.appendLine("DETTAGLIO SPESE")
            sb.appendLine("Data;Descrizione;Categoria;Metodo Pagamento;Sostenuto Da;Importo;Foto Scontrino")

            var photoCounter = 1
            for (spesa in notaSpeseConSpese.spese) {
                val desc = spesa.descrizione.ifBlank { spesa.categoria.displayName }
                var photoFileName = ""
                val pagatoDaStr = if (spesa.pagatoDa == PagatoDa.AZIENDA) "Azienda" else "Dipendente"
                if (spesa.fotoScontrinoPath != null) {
                    val src = File(spesa.fotoScontrinoPath)
                    if (src.exists()) {
                        val ext = src.extension.ifBlank { "jpg" }
                        photoFileName = "scontrino_${String.format("%03d", photoCounter)}_${spesa.categoria.name.lowercase()}.$ext"
                        src.copyTo(File(outputDir, photoFileName), overwrite = true)
                        photoCounter++
                    }
                }
                sb.appendLine("${dateFormatter.format(Date(spesa.data))};$desc;${spesa.categoria.displayName};${spesa.metodoPagamento.displayName};$pagatoDaStr;${String.format(Locale.ITALY, "%.2f", spesa.importo)};$photoFileName")
            }
            sb.appendLine()

            sb.appendLine("RIEPILOGO PER CATEGORIA")
            sb.appendLine("Categoria;Importo")
            for (cat in CategoriaSpesa.entries) {
                val tot = notaSpeseConSpese.totaleByCategoria(cat)
                if (tot > 0) sb.appendLine("${cat.displayName};${String.format(Locale.ITALY, "%.2f", tot)}")
            }
            sb.appendLine()

            sb.appendLine("RIEPILOGO PER METODO PAGAMENTO")
            sb.appendLine("Metodo;Importo")
            sb.appendLine("Carta di Credito;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleByCarta)}")
            sb.appendLine("Contanti;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleContanti)}")
            sb.appendLine("Altro;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleAltro)}")
            sb.appendLine()

            sb.appendLine("SPESE SOSTENUTE DA")
            sb.appendLine("Sostenute da;Importo")
            sb.appendLine("Azienda;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}")
            sb.appendLine("Dipendente;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}")
            sb.appendLine()

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

            if (notaSpeseConSpese.totaleRimborsoDipendenteLordo > 0 || nota.anticipo > 0) {
                sb.appendLine("DA RIMBORSARE AL DIPENDENTE")
                if (notaSpeseConSpese.totalePagatoDipendente > 0) sb.appendLine("Spese pagate dal dipendente;+ ${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}")
                if (nota.totaleRimborsoKm > 0) sb.appendLine("Rimborso chilometri;+ ${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}")
                if (nota.anticipo > 0) sb.appendLine("Anticipo ricevuto;- ${String.format(Locale.ITALY, "%.2f", nota.anticipo)}")
                sb.appendLine("TOTALE RIMBORSO DIPENDENTE;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totaleRimborsoDipendente)}")
                sb.appendLine()
            }

            sb.appendLine("RIEPILOGO FINALE")
            sb.appendLine("Spese pagate dall'Azienda;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoAzienda)}")
            sb.appendLine("Spese pagate dal Dipendente;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.totalePagatoDipendente)}")
            if (nota.totaleRimborsoKm > 0) sb.appendLine("Rimborso Km;${String.format(Locale.ITALY, "%.2f", nota.totaleRimborsoKm)}")
            sb.appendLine("COSTO COMPLESSIVO NOTA SPESE;${String.format(Locale.ITALY, "%.2f", notaSpeseConSpese.costoComplessivoNotaSpese)}")

            val csvFileName = baseName?.let { "$it.csv" } ?: "NotaSpese_${nota.nomeCognome.replace(" ", "_")}.csv"
            val csvFile = File(outputDir, csvFileName)
            FileWriter(csvFile).use { it.write(sb.toString()) }
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
