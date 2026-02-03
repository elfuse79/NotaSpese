package com.notaspese.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.notaspese.data.model.CategoriaSpesa
import com.notaspese.data.model.MetodoPagamento
import com.notaspese.data.model.PagatoDa
import com.notaspese.data.model.Spesa
import com.notaspese.ui.components.*
import com.notaspese.util.FileStorageHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpesaScreen(notaSpeseId: Long, onNavigateBack: () -> Unit, onSave: (Spesa) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var descrizione by remember { mutableStateOf("") }
    var importo by remember { mutableStateOf("") }
    var data by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var metodoPagamento by remember { mutableStateOf(MetodoPagamento.CARTA_CREDITO) }
    var categoria by remember { mutableStateOf(CategoriaSpesa.VITTO) }
    var fotoPath by remember { mutableStateOf<String?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var isOcrProcessing by remember { mutableStateOf(false) }
    var ocrResultMessage by remember { mutableStateOf<String?>(null) }
    var pdfPath by remember { mutableStateOf<String?>(null) }
    var isPdf by remember { mutableStateOf(false) }
    var pagatoDa by remember { mutableStateOf(PagatoDa.AZIENDA) }
    
    // Se pagato dal dipendente, forza metodo pagamento a Pagamento Elettronico (CONTANTI nel enum)
    LaunchedEffect(pagatoDa) {
        if (pagatoDa == PagatoDa.DIPENDENTE) {
            metodoPagamento = MetodoPagamento.CONTANTI  // "Pag. Elettronico Dip."
        } else {
            metodoPagamento = MetodoPagamento.CARTA_CREDITO
        }
    }
    
    val isFormValid = importo.isNotBlank() && importo.toDoubleOrNull() != null && importo.toDouble() > 0 && data != null
    
    // Launcher per PDF
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Salva il PDF nella cartella della nota spese
            val savedPath = FileStorageHelper.saveFileToNotaFolder(context, it, notaSpeseId, "pdf")
            if (savedPath != null) {
                pdfPath = savedPath
                fotoPath = savedPath  // Usiamo lo stesso campo per semplicità
                isPdf = true
                ocrResultMessage = "PDF caricato correttamente"
            } else {
                Toast.makeText(context, "Errore nel salvataggio del PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> 
        if (success && tempPhotoUri != null) {
            pendingImageUri = tempPhotoUri
            showCropDialog = true
            isPdf = false
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> 
        uri?.let { 
            pendingImageUri = it
            showCropDialog = true
            isPdf = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir("receipts")
            val photoFile = File.createTempFile("SCONTRINO_${timeStamp}_", ".jpg", storageDir)
            tempPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempPhotoUri?.let { cameraLauncher.launch(it) }
        } else { Toast.makeText(context, "Permesso fotocamera necessario", Toast.LENGTH_SHORT).show() }
    }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Nuova Spesa", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Indietro") } }) },
        bottomBar = { Surface(tonalElevation = 3.dp) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) { Text("Annulla") }; Button(onClick = { onSave(Spesa(notaSpeseId = notaSpeseId, descrizione = descrizione, importo = importo.toDouble(), data = data!!, metodoPagamento = metodoPagamento, categoria = categoria, fotoScontrinoPath = fotoPath, pagatoDa = pagatoDa)) }, enabled = isFormValid, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Salva") } } } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Banner OCR result
            ocrResultMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("rilevato:")) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (message.contains("rilevato:")) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (message.contains("rilevato:")) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.contains("rilevato:")) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Allegato Scontrino", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    if (fotoPath != null) {
                        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                            if (isPdf) {
                                // Mostra un placeholder per il PDF
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "PDF Allegato",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                Image(painter = rememberAsyncImagePainter(fotoPath), contentDescription = "Foto", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            IconButton(
                                onClick = { 
                                    fotoPath = null
                                    pdfPath = null
                                    isPdf = false
                                }, 
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(50))
                            ) { 
                                Icon(Icons.Default.Close, "Rimuovi", tint = MaterialTheme.colorScheme.error) 
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).clickable { showPhotoOptions = true }, contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddAPhoto, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)); Spacer(Modifier.height(8.dp)); Text("Tocca per aggiungere foto o PDF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } }
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dettagli Spesa", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = importo, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) importo = it }, label = { Text("Importo (EUR) *") }, leadingIcon = { Icon(Icons.Default.Euro, null) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    DatePickerField("Data Spesa *", data, { data = it })
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = descrizione, onValueChange = { descrizione = it }, label = { Text("Descrizione (opzionale)") }, leadingIcon = { Icon(Icons.Default.Description, null) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            }
            // Selettore Pagato Da
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { 
                Column(Modifier.padding(16.dp)) { 
                    PagatoDaSelector(
                        selected = pagatoDa, 
                        onSelect = { pagatoDa = it }
                    ) 
                } 
            }
            
            // Metodo pagamento (disabilitato se pagato dal dipendente)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { 
                Column(Modifier.padding(16.dp)) { 
                    MetodoPagamentoSelector(
                        selected = metodoPagamento, 
                        onSelect = { if (pagatoDa == PagatoDa.AZIENDA) metodoPagamento = it },
                        enabled = pagatoDa == PagatoDa.AZIENDA,
                        showOnlyCarta = pagatoDa == PagatoDa.AZIENDA  // Mostra solo Carta di Credito per azienda
                    )
                    if (pagatoDa == PagatoDa.DIPENDENTE) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Metodo: Pagamento Elettronico Dipendente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } 
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { CategoriaSpesaSelector(categoria, { categoria = it }) } }
            Spacer(Modifier.height(80.dp))
        }
    }
    
    if (showPhotoOptions) {
        ModalBottomSheet(onDismissRequest = { showPhotoOptions = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Aggiungi Allegato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 16.dp))
                ListItem(headlineContent = { Text("Scatta Foto") }, leadingContent = { Icon(Icons.Default.CameraAlt, null) }, modifier = Modifier.clickable { showPhotoOptions = false; permissionLauncher.launch(Manifest.permission.CAMERA) })
                ListItem(headlineContent = { Text("Scegli Immagine dalla Galleria") }, leadingContent = { Icon(Icons.Default.PhotoLibrary, null) }, modifier = Modifier.clickable { showPhotoOptions = false; galleryLauncher.launch("image/*") })
                Divider(Modifier.padding(vertical = 8.dp))
                ListItem(
                    headlineContent = { Text("Carica PDF") }, 
                    supportingContent = { Text("Seleziona un documento PDF", style = MaterialTheme.typography.bodySmall) },
                    leadingContent = { Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.error) }, 
                    modifier = Modifier.clickable { 
                        showPhotoOptions = false
                        pdfLauncher.launch("application/pdf") 
                    }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // Dialog per ritagliare l'immagine
    if (showCropDialog && pendingImageUri != null) {
        ImageCropperDialog(
            imageUri = pendingImageUri!!,
            onDismiss = { 
                showCropDialog = false
                pendingImageUri = null
            },
            onCropComplete = { croppedUri, extractedTotal ->
                fotoPath = croppedUri.toString()
                showCropDialog = false
                pendingImageUri = null
                
                // Se l'OCR ha trovato un totale, lo inserisce nel campo importo
                if (extractedTotal != null && extractedTotal > 0) {
                    importo = String.format(java.util.Locale.US, "%.2f", extractedTotal)
                    ocrResultMessage = "Totale rilevato: €${String.format("%.2f", extractedTotal)}"
                } else {
                    ocrResultMessage = "Totale non rilevato. Inseriscilo manualmente."
                }
            }
        )
    }
    
    // Snackbar per mostrare il risultato dell'OCR
    ocrResultMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            ocrResultMessage = null
        }
    }
}
