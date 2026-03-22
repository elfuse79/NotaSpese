package com.notaspese.desktop.util

import com.notaspese.desktop.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

/**
 * Esporta nota spese in formato .notaspese (ZIP con export.json + allegati).
 * Formato compatibile con versioni precedenti e import su Android.
 */
object NotaSpeseExporter {

    fun exportToNotaSpeseFile(notaConSpese: NotaSpeseConSpese, outputDir: File, baseName: String? = null): File? {
        return try {
            val nota = notaConSpese.notaSpese
            val defaultBaseName = "${nota.nomeCognome.replace(" ", "_")}_${java.text.SimpleDateFormat("yyyy-MM-dd_HHmm", java.util.Locale.ITALY).format(java.util.Date(nota.dataInizioTrasferta))}"
            val name = baseName ?: defaultBaseName
            val notaspeseFile = File(outputDir, "$name.notaspese")

            val json = buildExportJson(notaConSpese)
            val allegatiDir = File(System.getProperty("java.io.tmpdir"), "notaspese_export_${System.currentTimeMillis()}")
            allegatiDir.mkdirs()
            try {
                val allegatiMap = mutableMapOf<String, File>()
                var spesaIdx = 0
                for (spesa in notaConSpese.spese) {
                    spesa.fotoScontrinoPath?.let { path ->
                        val src = File(path)
                        if (src.exists()) {
                            val ext = src.extension.ifBlank { "pdf" }
                            val relPath = "allegati/nota_0/spesa_$spesaIdx.$ext"
                            val dest = File(allegatiDir, relPath)
                            dest.parentFile?.mkdirs()
                            src.copyTo(dest, overwrite = true)
                            allegatiMap[relPath] = dest
                        }
                    }
                    spesaIdx++
                }

                ZipOutputStream(FileOutputStream(notaspeseFile)).use { zos ->
                    addZipEntry(zos, "export.json", json.toByteArray(Charsets.UTF_8))
                    for ((relPath, file) in allegatiMap) {
                        addZipEntry(zos, relPath, file.readBytes())
                    }
                }
                notaspeseFile
            } finally {
                allegatiDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addZipEntry(zos: ZipOutputStream, name: String, data: ByteArray) {
        val entry = java.util.zip.ZipEntry(name)
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }

    private fun buildExportJson(notaConSpese: NotaSpeseConSpese): String {
        val nota = notaConSpese.notaSpese
        val sb = StringBuilder()
        sb.append("""{"version":1,"appVersion":"1.3.7","exportDate":${System.currentTimeMillis()},"noteSpese":[{""")
        sb.append(""""numeroNota":"${escape(nota.numeroNota)}",""")
        sb.append(""""nomeCognome":"${escape(nota.nomeCognome)}",""")
        sb.append(""""dataInizioTrasferta":${nota.dataInizioTrasferta},""")
        sb.append(""""oraInizioTrasferta":"${escape(nota.oraInizioTrasferta)}",""")
        sb.append(""""dataFineTrasferta":${nota.dataFineTrasferta},""")
        sb.append(""""oraFineTrasferta":"${escape(nota.oraFineTrasferta)}",""")
        sb.append(""""luogoTrasferta":"${escape(nota.luogoTrasferta)}",""")
        sb.append(""""cliente":"${escape(nota.cliente)}",""")
        sb.append(""""causale":"${escape(nota.causale)}",""")
        sb.append(""""auto":"${escape(nota.auto)}",""")
        sb.append(""""dataCompilazione":${nota.dataCompilazione},""")
        sb.append(""""altriTrasfertisti":"${escape(nota.altriTrasfertisti)}",""")
        sb.append(""""anticipo":${nota.anticipo},""")
        sb.append(""""kmPercorsi":${nota.kmPercorsi},""")
        sb.append(""""costoKmRimborso":${nota.costoKmRimborso},""")
        sb.append(""""costoKmCliente":${nota.costoKmCliente},""")
        sb.append(""""spese":[""")
        var first = true
        for ((idx, spesa) in notaConSpese.spese.withIndex()) {
            if (!first) sb.append(",")
            first = false
            val allegato = if (spesa.fotoScontrinoPath != null) {
                val src = File(spesa.fotoScontrinoPath)
                val ext = src.extension.ifBlank { "pdf" }
                "allegati/nota_0/spesa_$idx.$ext"
            } else ""
            sb.append("""{"descrizione":"${escape(spesa.descrizione)}","importo":${spesa.importo},"data":${spesa.data},""")
            sb.append(""""metodoPagamento":"${spesa.metodoPagamento.name}","categoria":"${spesa.categoria.name}",""")
            sb.append(""""allegatoFile":"${escape(allegato)}","pagatoDa":"${spesa.pagatoDa.name}"}""")
        }
        sb.append("]}]}")
        return sb.toString()
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
