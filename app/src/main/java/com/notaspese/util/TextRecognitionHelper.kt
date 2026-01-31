package com.notaspese.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume

object TextRecognitionHelper {
    
    private const val TAG = "OCR_TextRecognition"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    
    // Pattern per identificare righe di totale (ordine di priorità)
    private val HIGH_PRIORITY_PATTERNS = listOf(
        "TOTALE EURO",
        "TOTALE EUR",
        "TOTAL EURO",
        "TOTAL EUR",
        "TOTALE COMPLESSIVO",
        "TOTALE DA PAGARE",
        "IMPORTO DOVUTO",
        "DA PAGARE",
        "AMOUNT DUE",
        "TOTALE €",
        "TOTAL €"
    )
    
    private val MEDIUM_PRIORITY_PATTERNS = listOf(
        "TOTALE",
        "TOTAL",
        "TOT.EURO",
        "TOT. EURO",
        "TOT.EUR",
        "TOT. EUR",
        "IMPORTO TOTALE",
        "IMPORTO",
        "AMOUNT",
        "SALDO",
        "DOVUTO"
    )
    
    private val LOW_PRIORITY_PATTERNS = listOf(
        "TOT.",
        "TOT ",
        "PAGATO",
        "CONTANTI",
        "CONTANTE",
        "CARTA",
        "BANCOMAT",
        "POS",
        "CASH",
        "RESTO"
    )
    
    // Pattern da escludere (subtotali, IVA, etc.)
    private val EXCLUDE_PATTERNS = listOf(
        "SUBTOTALE",
        "SUBTOTAL",
        "SUB-TOTALE",
        "SUB TOTALE",
        "IVA",
        "VAT",
        "SCONTO",
        "DISCOUNT",
        "RESTO",
        "CHANGE",
        "PUNTI",
        "POINTS"
    )
    
    /**
     * Estrae il totale da un'immagine di scontrino
     */
    suspend fun extractTotalFromReceipt(context: Context, imageUri: Uri): Double? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        Log.d(TAG, "Testo riconosciuto:\n${visionText.text}")
                        val total = findTotalInText(visionText.text)
                        Log.d(TAG, "Totale estratto: $total")
                        continuation.resume(total)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Errore OCR: ${e.message}")
                        continuation.resume(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione OCR: ${e.message}")
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Estrae il totale da un Bitmap (per immagini ritagliate)
     */
    suspend fun extractTotalFromBitmap(bitmap: Bitmap): Double? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        Log.d(TAG, "Testo riconosciuto (bitmap):\n${visionText.text}")
                        val total = findTotalInText(visionText.text)
                        Log.d(TAG, "Totale estratto: $total")
                        continuation.resume(total)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Errore OCR bitmap: ${e.message}")
                        continuation.resume(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione OCR bitmap: ${e.message}")
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Cerca il totale nel testo riconosciuto con logica avanzata
     */
    private fun findTotalInText(text: String): Double? {
        // Pre-processing: pulisci e normalizza il testo
        val cleanedText = cleanText(text)
        val lines = cleanedText.lines().filter { it.isNotBlank() }
        
        Log.d(TAG, "Linee dopo pulizia: ${lines.size}")
        
        // Struttura per salvare i candidati trovati con priorità
        data class TotalCandidate(val amount: Double, val priority: Int, val lineIndex: Int, val source: String)
        val candidates = mutableListOf<TotalCandidate>()
        
        // Analizza ogni riga
        for ((index, line) in lines.withIndex()) {
            val upperLine = line.uppercase()
            
            // Salta righe che contengono pattern da escludere
            if (EXCLUDE_PATTERNS.any { upperLine.contains(it) && !upperLine.contains("TOTALE") }) {
                Log.d(TAG, "Riga esclusa: $line")
                continue
            }
            
            // Estrai importi dalla riga
            val amounts = extractAmounts(line)
            
            for (amount in amounts) {
                // Determina la priorità basata sui pattern trovati
                val priority = when {
                    HIGH_PRIORITY_PATTERNS.any { upperLine.contains(it) } -> 3
                    MEDIUM_PRIORITY_PATTERNS.any { upperLine.contains(it) } -> 2
                    LOW_PRIORITY_PATTERNS.any { upperLine.contains(it) } -> 1
                    else -> 0
                }
                
                if (priority > 0 || amount > 0) {
                    candidates.add(TotalCandidate(amount, priority, index, line))
                    Log.d(TAG, "Candidato trovato: €$amount, priorità=$priority, riga=$line")
                }
            }
        }
        
        // Strategia di selezione del totale
        
        // 1. Se abbiamo candidati ad alta priorità, prendi quello con l'importo più alto
        val highPriorityCandidates = candidates.filter { it.priority == 3 }
        if (highPriorityCandidates.isNotEmpty()) {
            val best = highPriorityCandidates.maxByOrNull { it.amount }
            Log.d(TAG, "Selezionato da alta priorità: €${best?.amount}")
            return best?.amount
        }
        
        // 2. Se abbiamo candidati a media priorità, prendi quello con l'importo più alto
        val mediumPriorityCandidates = candidates.filter { it.priority == 2 }
        if (mediumPriorityCandidates.isNotEmpty()) {
            val best = mediumPriorityCandidates.maxByOrNull { it.amount }
            Log.d(TAG, "Selezionato da media priorità: €${best?.amount}")
            return best?.amount
        }
        
        // 3. Se abbiamo candidati a bassa priorità, prendi quello con l'importo più alto
        val lowPriorityCandidates = candidates.filter { it.priority == 1 }
        if (lowPriorityCandidates.isNotEmpty()) {
            val best = lowPriorityCandidates.maxByOrNull { it.amount }
            Log.d(TAG, "Selezionato da bassa priorità: €${best?.amount}")
            return best?.amount
        }
        
        // 4. Fallback: prendi l'importo più alto dalle ultime 5 righe (spesso il totale è in fondo)
        val lastLines = lines.takeLast(5)
        val lastLineAmounts = mutableListOf<Double>()
        for (line in lastLines) {
            lastLineAmounts.addAll(extractAmounts(line))
        }
        if (lastLineAmounts.isNotEmpty()) {
            val maxAmount = lastLineAmounts.maxOrNull()
            Log.d(TAG, "Fallback ultime righe: €$maxAmount")
            return maxAmount
        }
        
        // 5. Ultimo fallback: l'importo più alto trovato in tutto il testo
        val allAmounts = lines.flatMap { extractAmounts(it) }
        val maxOverall = allAmounts.filter { it > 1.0 && it < 10000.0 }.maxOrNull()
        Log.d(TAG, "Fallback generale: €$maxOverall")
        return maxOverall
    }
    
    /**
     * Pulisce e normalizza il testo OCR
     */
    private fun cleanText(text: String): String {
        return text
            // Sostituisci caratteri OCR comuni errati
            .replace("O", "0")  // Solo in contesti numerici, gestito dopo
            .replace("|", "1")
            .replace("l", "1")  // 'l' minuscola spesso confusa con 1 nei numeri
            .replace("€", " € ")
            .replace("EUR", " EUR ")
            .replace("  ", " ")
            // Normalizza separatori
            .replace("'", "")
            .replace("´", "")
            .replace("`", "")
            // Gestisci spazi intorno ai numeri
            .trim()
    }
    
    /**
     * Estrae tutti gli importi monetari da una stringa
     * Supporta vari formati: 12,50 | 12.50 | € 12,50 | EUR 12.50 | 1.234,56 | 1,234.56
     */
    private fun extractAmounts(text: String): List<Double> {
        val amounts = mutableListOf<Double>()
        
        // Pattern per importi con migliaia e decimali (formato italiano: 1.234,56)
        val patternItaWithThousands = Regex("""(\d{1,3})\.(\d{3}),(\d{2})""")
        patternItaWithThousands.findAll(text).forEach { match ->
            val thousands = match.groupValues[1]
            val hundreds = match.groupValues[2]
            val decimals = match.groupValues[3]
            val amount = "$thousands$hundreds.$decimals".toDoubleOrNull()
            if (amount != null && amount > 0) {
                amounts.add(amount)
            }
        }
        
        // Pattern per importi con migliaia e decimali (formato US: 1,234.56)
        val patternUsWithThousands = Regex("""(\d{1,3}),(\d{3})\.(\d{2})""")
        patternUsWithThousands.findAll(text).forEach { match ->
            val thousands = match.groupValues[1]
            val hundreds = match.groupValues[2]
            val decimals = match.groupValues[3]
            val amount = "$thousands$hundreds.$decimals".toDoubleOrNull()
            if (amount != null && amount > 0 && !amounts.contains(amount)) {
                amounts.add(amount)
            }
        }
        
        // Pattern per importi semplici con virgola decimale (formato italiano: 12,50)
        val patternItaSimple = Regex("""(?<!\d[.,])(\d{1,4}),(\d{2})(?![.,]\d)""")
        patternItaSimple.findAll(text).forEach { match ->
            val intPart = match.groupValues[1]
            val decPart = match.groupValues[2]
            val amount = "$intPart.$decPart".toDoubleOrNull()
            if (amount != null && amount > 0 && !amounts.contains(amount)) {
                amounts.add(amount)
            }
        }
        
        // Pattern per importi semplici con punto decimale (formato US: 12.50)
        val patternUsSimple = Regex("""(?<!\d[.,])(\d{1,4})\.(\d{2})(?![.,]\d)""")
        patternUsSimple.findAll(text).forEach { match ->
            val intPart = match.groupValues[1]
            val decPart = match.groupValues[2]
            val amount = "$intPart.$decPart".toDoubleOrNull()
            if (amount != null && amount > 0 && !amounts.contains(amount)) {
                amounts.add(amount)
            }
        }
        
        // Pattern per importi interi preceduti da € o EUR
        val patternEuroInt = Regex("""[€E][UR]*\s*(\d{1,4})(?![.,]\d)""", RegexOption.IGNORE_CASE)
        patternEuroInt.findAll(text).forEach { match ->
            val amount = match.groupValues[1].toDoubleOrNull()
            if (amount != null && amount > 0 && !amounts.contains(amount)) {
                amounts.add(amount)
            }
        }
        
        return amounts.filter { it >= 0.01 && it <= 99999.99 }
    }
    
    /**
     * Carica e ruota correttamente un'immagine da Uri
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Correggi l'orientamento basandosi sui dati EXIF
            val rotatedBitmap = correctBitmapOrientation(context, uri, bitmap)
            rotatedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Errore caricamento bitmap: ${e.message}")
            null
        }
    }
    
    private fun correctBitmapOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()
            
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Errore correzione orientamento: ${e.message}")
            bitmap
        }
    }
}
