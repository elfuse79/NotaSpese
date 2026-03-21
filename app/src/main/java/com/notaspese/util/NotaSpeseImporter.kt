package com.notaspese.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.notaspese.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Importa file .notaspese (ZIP contenente export.json + allegati).
 * Formato usato da versioni precedenti dell'app (es. v1.0.x).
 */
object NotaSpeseImporter {

    private val gson = Gson()

    fun importFromUri(context: Context, notaspeseUri: Uri): NotaSpeseConSpese? {
        return try {
            val extractDir = File(context.cacheDir, "notaspese_${System.currentTimeMillis()}")
            extractDir.mkdirs()
            try {
                context.contentResolver.openInputStream(notaspeseUri)?.use { input ->
                    extractZip(input, extractDir)
                } ?: return null
                val jsonFile = File(extractDir, "export.json")
                if (!jsonFile.exists()) return null
                val json = jsonFile.readText(Charsets.UTF_8)
                parseExportJson(json, extractDir)
            } finally {
                extractDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractZip(input: java.io.InputStream, destDir: File) {
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val destFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun parseExportJson(json: String, allegatiBaseDir: File): NotaSpeseConSpese? {
        val root = gson.fromJson(json, JsonObject::class.java) ?: return null
        val noteSpeseArr = root.getAsJsonArray("noteSpese") ?: return null
        val firstNota = noteSpeseArr.firstOrNull()?.asJsonObject ?: return null

        val nota = NotaSpese(
            numeroNota = firstNota.get("numeroNota")?.asString ?: "",
            nomeCognome = firstNota.get("nomeCognome")?.asString?.ifBlank { "Import" } ?: "Import",
            dataInizioTrasferta = firstNota.get("dataInizioTrasferta")?.asLong ?: System.currentTimeMillis(),
            oraInizioTrasferta = firstNota.get("oraInizioTrasferta")?.asString ?: "",
            dataFineTrasferta = firstNota.get("dataFineTrasferta")?.asLong ?: firstNota.get("dataInizioTrasferta")?.asLong ?: System.currentTimeMillis(),
            oraFineTrasferta = firstNota.get("oraFineTrasferta")?.asString ?: "",
            luogoTrasferta = firstNota.get("luogoTrasferta")?.asString?.ifBlank { "-" } ?: "-",
            cliente = firstNota.get("cliente")?.asString?.ifBlank { "-" } ?: "-",
            causale = firstNota.get("causale")?.asString ?: "",
            auto = firstNota.get("auto")?.asString ?: "",
            dataCompilazione = firstNota.get("dataCompilazione")?.asLong ?: System.currentTimeMillis(),
            altriTrasfertisti = firstNota.get("altriTrasfertisti")?.asString ?: "",
            anticipo = firstNota.get("anticipo")?.asDouble ?: 0.0,
            kmPercorsi = firstNota.get("kmPercorsi")?.asDouble ?: 0.0,
            costoKmRimborso = firstNota.get("costoKmRimborso")?.asDouble ?: 0.0,
            costoKmCliente = firstNota.get("costoKmCliente")?.asDouble ?: 0.60
        )

        val dataInizio = nota.dataInizioTrasferta
        val speseArr = firstNota.getAsJsonArray("spese") ?: return NotaSpeseConSpese(nota, emptyList())
        val spese = speseArr.mapNotNull { el ->
            if (el.isJsonObject) parseSpesa(el.asJsonObject, allegatiBaseDir, dataInizio) else null
        }

        return NotaSpeseConSpese(nota, spese)
    }

    private fun parseSpesa(s: JsonObject, allegatiBaseDir: File, defaultData: Long): Spesa? {
        val descrizione = s.get("descrizione")?.asString ?: ""
        val importo = s.get("importo")?.asDouble ?: 0.0
        val data = s.get("data")?.asLong ?: defaultData
        val metPagStr = s.get("metodoPagamento")?.asString ?: "CARTA_CREDITO"
        val catStr = s.get("categoria")?.asString ?: "ALTRO"
        val pagatoDaStr = s.get("pagatoDa")?.asString ?: "AZIENDA"
        val allegatoFile = s.get("allegatoFile")?.asString ?: ""

        val categoria = CategoriaSpesa.entries.find { it.name == catStr } ?: CategoriaSpesa.ALTRO
        val metodoPagamento = when {
            metPagStr.contains("CARTA") || metPagStr.contains("CREDITO") -> MetodoPagamento.CARTA_CREDITO
            metPagStr.contains("CONTANTI") || metPagStr.contains("ELETTRONICO") -> MetodoPagamento.CONTANTI
            else -> MetodoPagamento.ALTRO
        }
        val pagatoDa = if (pagatoDaStr.contains("DIPENDENTE")) PagatoDa.DIPENDENTE else PagatoDa.AZIENDA

        var photoPath: String? = null
        if (allegatoFile.isNotBlank()) {
            val attFile = File(allegatiBaseDir, allegatoFile)
            if (attFile.exists()) photoPath = attFile.absolutePath
        }

        return Spesa(
            notaSpeseId = 0,
            descrizione = descrizione,
            importo = importo,
            data = data,
            metodoPagamento = metodoPagamento,
            categoria = categoria,
            fotoScontrinoPath = photoPath,
            pagatoDa = pagatoDa
        )
    }
}
