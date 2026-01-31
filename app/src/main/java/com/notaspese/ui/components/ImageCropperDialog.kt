package com.notaspese.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.notaspese.util.TextRecognitionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

private const val TAG = "ImageCropperDialog"
private const val MAX_DISPLAY_SIZE = 1200 // Max dimension for display bitmap
private const val MAX_CROP_SIZE = 2000 // Max dimension for cropped image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropperDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropComplete: (Uri, Double?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Stati
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isProcessing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Colore primario (salvato fuori dal Canvas)
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Crop rect in percentuale (0-1)
    var cropLeft by remember { mutableFloatStateOf(0.1f) }
    var cropTop by remember { mutableFloatStateOf(0.1f) }
    var cropRight by remember { mutableFloatStateOf(0.9f) }
    var cropBottom by remember { mutableFloatStateOf(0.9f) }
    
    // Handle attivo
    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }
    
    // Carica l'immagine in modo sicuro
    LaunchedEffect(imageUri) {
        isLoading = true
        errorMessage = null
        
        withContext(Dispatchers.IO) {
            try {
                val result = loadAndScaleBitmap(context, imageUri)
                if (result != null) {
                    displayBitmap = result.first
                    originalBitmap = result.second
                } else {
                    errorMessage = "Impossibile caricare l'immagine"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore nel caricamento dell'immagine", e)
                errorMessage = "Errore: ${e.message ?: "Sconosciuto"}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Cleanup quando il dialog viene chiuso
    DisposableEffect(Unit) {
        onDispose {
            try {
                displayBitmap?.recycle()
                // Non riciclare originalBitmap qui se potrebbe essere ancora in uso
            } catch (e: Exception) {
                Log.e(TAG, "Errore nel cleanup", e)
            }
        }
    }
    
    Dialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = { 
                        Text(
                            "Ritaglia Scontrino", 
                            color = Color.White, 
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (!isProcessing) onDismiss() },
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Default.Close, "Chiudi", tint = Color.White)
                        }
                    },
                    actions = {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else if (originalBitmap != null && !isLoading) {
                            TextButton(
                                onClick = {
                                    originalBitmap?.let { bmp ->
                                        scope.launch {
                                            isProcessing = true
                                            try {
                                                val result = cropAndProcessSafely(
                                                    context,
                                                    bmp,
                                                    cropLeft,
                                                    cropTop,
                                                    cropRight,
                                                    cropBottom
                                                )
                                                if (result != null) {
                                                    onCropComplete(result.first, result.second)
                                                } else {
                                                    errorMessage = "Errore nel ritaglio dell'immagine"
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Errore nel crop", e)
                                                errorMessage = "Errore: ${e.message}"
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Conferma", color = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                
                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Caricamento immagine...",
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        errorMessage != null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    errorMessage ?: "Errore sconosciuto",
                                    color = Color.White
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onDismiss) {
                                    Text("Chiudi")
                                }
                            }
                        }
                        displayBitmap != null -> {
                            val bmp = displayBitmap!!
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { canvasSize = it }
                            ) {
                                // Calcola dimensioni sicure
                                val imageAspectRatio = if (bmp.height > 0) {
                                    bmp.width.toFloat() / bmp.height.toFloat()
                                } else 1f
                                
                                val canvasAspectRatio = if (canvasSize.height > 0 && canvasSize.width > 0) {
                                    canvasSize.width.toFloat() / canvasSize.height.toFloat()
                                } else 1f
                                
                                val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = remember(canvasSize, bmp.width, bmp.height) {
                                    if (canvasSize.width <= 0 || canvasSize.height <= 0) {
                                        listOf(100f, 100f, 0f, 0f)
                                    } else if (imageAspectRatio > canvasAspectRatio) {
                                        val w = canvasSize.width.toFloat()
                                        val h = if (imageAspectRatio > 0) w / imageAspectRatio else w
                                        val offsetY = (canvasSize.height - h) / 2
                                        listOf(w, h, 0f, offsetY)
                                    } else {
                                        val h = canvasSize.height.toFloat()
                                        val w = h * imageAspectRatio
                                        val offsetX = (canvasSize.width - w) / 2
                                        listOf(w, h, offsetX, 0f)
                                    }
                                }
                                
                                // ImageBitmap cached
                                val imageBitmap = remember(bmp) {
                                    try {
                                        bmp.asImageBitmap()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Errore conversione bitmap", e)
                                        null
                                    }
                                }
                                
                                if (imageBitmap != null && imageWidth > 0 && imageHeight > 0) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(imageWidth, imageHeight) {
                                                detectDragGestures(
                                                    onDragStart = { offset ->
                                                        if (imageWidth > 0 && imageHeight > 0) {
                                                            activeHandle = findNearestHandle(
                                                                offset,
                                                                cropLeft, cropTop, cropRight, cropBottom,
                                                                imageWidth, imageHeight, imageOffsetX, imageOffsetY
                                                            )
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        
                                                        if (imageWidth > 0 && imageHeight > 0) {
                                                            val deltaX = dragAmount.x / imageWidth
                                                            val deltaY = dragAmount.y / imageHeight
                                                            
                                                            when (activeHandle) {
                                                                CropHandle.TOP_LEFT -> {
                                                                    cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - 0.1f)
                                                                    cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - 0.1f)
                                                                }
                                                                CropHandle.TOP_RIGHT -> {
                                                                    cropRight = (cropRight + deltaX).coerceIn(cropLeft + 0.1f, 1f)
                                                                    cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - 0.1f)
                                                                }
                                                                CropHandle.BOTTOM_LEFT -> {
                                                                    cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - 0.1f)
                                                                    cropBottom = (cropBottom + deltaY).coerceIn(cropTop + 0.1f, 1f)
                                                                }
                                                                CropHandle.BOTTOM_RIGHT -> {
                                                                    cropRight = (cropRight + deltaX).coerceIn(cropLeft + 0.1f, 1f)
                                                                    cropBottom = (cropBottom + deltaY).coerceIn(cropTop + 0.1f, 1f)
                                                                }
                                                                CropHandle.CENTER -> {
                                                                    val width = cropRight - cropLeft
                                                                    val height = cropBottom - cropTop
                                                                    
                                                                    var newLeft = (cropLeft + deltaX).coerceIn(0f, 1f - width)
                                                                    var newTop = (cropTop + deltaY).coerceIn(0f, 1f - height)
                                                                    
                                                                    cropLeft = newLeft
                                                                    cropTop = newTop
                                                                    cropRight = newLeft + width
                                                                    cropBottom = newTop + height
                                                                }
                                                                null -> {}
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        activeHandle = null
                                                    }
                                                )
                                            }
                                    ) {
                                        // Disegna l'immagine
                                        try {
                                            drawImage(
                                                image = imageBitmap,
                                                dstOffset = IntOffset(
                                                    imageOffsetX.toInt(),
                                                    imageOffsetY.toInt()
                                                ),
                                                dstSize = IntSize(
                                                    imageWidth.toInt(),
                                                    imageHeight.toInt()
                                                )
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Errore disegno immagine", e)
                                        }
                                        
                                        // Calcola coordinate crop rect
                                        val cropRectLeft = imageOffsetX + cropLeft * imageWidth
                                        val cropRectTop = imageOffsetY + cropTop * imageHeight
                                        val cropRectRight = imageOffsetX + cropRight * imageWidth
                                        val cropRectBottom = imageOffsetY + cropBottom * imageHeight
                                        
                                        // Overlay scuro
                                        val overlayColor = Color.Black.copy(alpha = 0.6f)
                                        
                                        // Top overlay
                                        if (cropRectTop > imageOffsetY) {
                                            drawRect(
                                                color = overlayColor,
                                                topLeft = Offset(imageOffsetX, imageOffsetY),
                                                size = Size(imageWidth, cropRectTop - imageOffsetY)
                                            )
                                        }
                                        // Bottom overlay
                                        if (cropRectBottom < imageOffsetY + imageHeight) {
                                            drawRect(
                                                color = overlayColor,
                                                topLeft = Offset(imageOffsetX, cropRectBottom),
                                                size = Size(imageWidth, imageOffsetY + imageHeight - cropRectBottom)
                                            )
                                        }
                                        // Left overlay
                                        if (cropRectLeft > imageOffsetX) {
                                            drawRect(
                                                color = overlayColor,
                                                topLeft = Offset(imageOffsetX, cropRectTop),
                                                size = Size(cropRectLeft - imageOffsetX, cropRectBottom - cropRectTop)
                                            )
                                        }
                                        // Right overlay
                                        if (cropRectRight < imageOffsetX + imageWidth) {
                                            drawRect(
                                                color = overlayColor,
                                                topLeft = Offset(cropRectRight, cropRectTop),
                                                size = Size(imageOffsetX + imageWidth - cropRectRight, cropRectBottom - cropRectTop)
                                            )
                                        }
                                        
                                        // Bordo crop
                                        val cropWidth = cropRectRight - cropRectLeft
                                        val cropHeight = cropRectBottom - cropRectTop
                                        if (cropWidth > 0 && cropHeight > 0) {
                                            drawRect(
                                                color = Color.White,
                                                topLeft = Offset(cropRectLeft, cropRectTop),
                                                size = Size(cropWidth, cropHeight),
                                                style = Stroke(width = 3f)
                                            )
                                            
                                            // Griglia
                                            val thirdWidth = cropWidth / 3
                                            val thirdHeight = cropHeight / 3
                                            
                                            for (i in 1..2) {
                                                drawLine(
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    start = Offset(cropRectLeft + thirdWidth * i, cropRectTop),
                                                    end = Offset(cropRectLeft + thirdWidth * i, cropRectBottom),
                                                    strokeWidth = 1f
                                                )
                                                drawLine(
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    start = Offset(cropRectLeft, cropRectTop + thirdHeight * i),
                                                    end = Offset(cropRectRight, cropRectTop + thirdHeight * i),
                                                    strokeWidth = 1f
                                                )
                                            }
                                        }
                                        
                                        // Handle agli angoli
                                        val handleRadius = 12f
                                        listOf(
                                            Offset(cropRectLeft, cropRectTop),
                                            Offset(cropRectRight, cropRectTop),
                                            Offset(cropRectLeft, cropRectBottom),
                                            Offset(cropRectRight, cropRectBottom)
                                        ).forEach { pos ->
                                            drawCircle(
                                                color = Color.White,
                                                radius = handleRadius,
                                                center = pos
                                            )
                                            drawCircle(
                                                color = primaryColor,
                                                radius = handleRadius - 3f,
                                                center = pos
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Istruzioni
                if (!isLoading && errorMessage == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "Trascina gli angoli o l'area per ritagliare lo scontrino",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

private fun findNearestHandle(
    touchPoint: Offset,
    cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float,
    imageWidth: Float, imageHeight: Float, imageOffsetX: Float, imageOffsetY: Float
): CropHandle {
    val threshold = 60f
    
    val cropRectLeft = imageOffsetX + cropLeft * imageWidth
    val cropRectTop = imageOffsetY + cropTop * imageHeight
    val cropRectRight = imageOffsetX + cropRight * imageWidth
    val cropRectBottom = imageOffsetY + cropBottom * imageHeight
    
    val handles = listOf(
        CropHandle.TOP_LEFT to Offset(cropRectLeft, cropRectTop),
        CropHandle.TOP_RIGHT to Offset(cropRectRight, cropRectTop),
        CropHandle.BOTTOM_LEFT to Offset(cropRectLeft, cropRectBottom),
        CropHandle.BOTTOM_RIGHT to Offset(cropRectRight, cropRectBottom)
    )
    
    val nearest = handles.minByOrNull { (_, pos) ->
        (touchPoint - pos).getDistance()
    }
    
    return if (nearest != null && (touchPoint - nearest.second).getDistance() < threshold) {
        nearest.first
    } else {
        CropHandle.CENTER
    }
}

/**
 * Carica e scala il bitmap per la visualizzazione e per il crop
 * Ritorna una coppia: (bitmap per display, bitmap originale per crop)
 */
private fun loadAndScaleBitmap(context: Context, uri: Uri): Pair<Bitmap, Bitmap>? {
    return try {
        // Prima ottieni le dimensioni senza caricare l'immagine
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        
        if (originalWidth <= 0 || originalHeight <= 0) {
            Log.e(TAG, "Dimensioni immagine non valide: ${originalWidth}x${originalHeight}")
            return null
        }
        
        // Calcola il sample size per l'immagine originale (per il crop)
        val cropSampleSize = calculateInSampleSize(originalWidth, originalHeight, MAX_CROP_SIZE)
        
        // Carica l'immagine originale ridimensionata per il crop
        val cropOptions = BitmapFactory.Options().apply {
            inSampleSize = cropSampleSize
            inPreferredConfig = Bitmap.Config.RGB_565 // Usa meno memoria
        }
        
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, cropOptions)
        } ?: return null
        
        // Correggi l'orientamento
        val rotatedOriginal = correctOrientation(context, uri, originalBitmap)
        
        // Crea versione per display ancora più piccola
        val displayBitmap = if (rotatedOriginal.width > MAX_DISPLAY_SIZE || rotatedOriginal.height > MAX_DISPLAY_SIZE) {
            val scale = MAX_DISPLAY_SIZE.toFloat() / max(rotatedOriginal.width, rotatedOriginal.height)
            val newWidth = (rotatedOriginal.width * scale).toInt()
            val newHeight = (rotatedOriginal.height * scale).toInt()
            Bitmap.createScaledBitmap(rotatedOriginal, newWidth, newHeight, true)
        } else {
            rotatedOriginal
        }
        
        Pair(displayBitmap, rotatedOriginal)
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OutOfMemoryError nel caricamento", e)
        System.gc() // Prova a liberare memoria
        null
    } catch (e: Exception) {
        Log.e(TAG, "Errore nel caricamento bitmap", e)
        null
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
    var inSampleSize = 1
    val maxDimension = max(width, height)
    
    while (maxDimension / inSampleSize > maxSize) {
        inSampleSize *= 2
    }
    
    return inSampleSize
}

private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
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
            else -> return bitmap
        }
        
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        rotated
    } catch (e: Exception) {
        Log.e(TAG, "Errore nella correzione orientamento", e)
        bitmap
    }
}

/**
 * Ritaglia e processa l'immagine in modo sicuro
 */
private suspend fun cropAndProcessSafely(
    context: Context,
    bitmap: Bitmap,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float
): Pair<Uri, Double?>? = withContext(Dispatchers.IO) {
    try {
        // Valida i parametri di crop
        val safeLeft = cropLeft.coerceIn(0f, 1f)
        val safeTop = cropTop.coerceIn(0f, 1f)
        val safeRight = cropRight.coerceIn(0f, 1f)
        val safeBottom = cropBottom.coerceIn(0f, 1f)
        
        // Calcola le coordinate in pixel
        val x = (safeLeft * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (safeTop * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = ((safeRight - safeLeft) * bitmap.width).toInt()
            .coerceIn(1, bitmap.width - x)
        val height = ((safeBottom - safeTop) * bitmap.height).toInt()
            .coerceIn(1, bitmap.height - y)
        
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Dimensioni crop non valide: ${width}x${height}")
            return@withContext null
        }
        
        // Ritaglia
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
        
        // Salva
        val storageDir = context.getExternalFilesDir("receipts")
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        val croppedFile = File(storageDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(croppedFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        
        // OCR
        val total = try {
            TextRecognitionHelper.extractTotalFromBitmap(croppedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Errore OCR", e)
            null
        }
        
        // Cleanup
        if (croppedBitmap != bitmap) {
            croppedBitmap.recycle()
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            croppedFile
        )
        
        Pair(uri, total)
    } catch (e: OutOfMemoryError) {
        Log.e(TAG, "OutOfMemoryError nel crop", e)
        System.gc()
        null
    } catch (e: Exception) {
        Log.e(TAG, "Errore nel crop", e)
        null
    }
}
