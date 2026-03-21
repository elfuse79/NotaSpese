package com.notaspese.desktop.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.notaspese.desktop.data.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpesaScreen(
    notaSpeseId: Long,
    notaFolder: File,
    onNavigateBack: () -> Unit,
    onSave: (Spesa) -> Unit,
    existingSpesa: Spesa? = null
) {
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALY) }
    var descrizione by remember { mutableStateOf(existingSpesa?.descrizione ?: "") }
    var importo by remember { mutableStateOf(if (existingSpesa != null) "%.2f".format(Locale.ITALY, existingSpesa.importo) else "") }
    var dataStr by remember { mutableStateOf(if (existingSpesa != null) dateFmt.format(Date(existingSpesa.data)) else dateFmt.format(Date())) }
    var metodoPagamento by remember { mutableStateOf(existingSpesa?.metodoPagamento ?: MetodoPagamento.CARTA_CREDITO) }
    var categoria by remember { mutableStateOf(existingSpesa?.categoria ?: CategoriaSpesa.VITTO) }
    var fotoPath by remember { mutableStateOf(existingSpesa?.fotoScontrinoPath) }
    var pagatoDa by remember { mutableStateOf(existingSpesa?.pagatoDa ?: PagatoDa.AZIENDA) }

    LaunchedEffect(pagatoDa) {
        metodoPagamento = if (pagatoDa == PagatoDa.DIPENDENTE) MetodoPagamento.CONTANTI else MetodoPagamento.CARTA_CREDITO
    }

    fun parseDate(s: String): Long = try {
        SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).parse(s)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) { System.currentTimeMillis() }

    val data = parseDate(dataStr)
    val isFormValid = importo.isNotBlank() && importo.toDoubleOrNull() != null && importo.toDouble() > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingSpesa != null) "Modifica Spesa" else "Nuova Spesa", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Indietro") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) { Text("Annulla") }
                    Button(
                        onClick = {
                            val destPath = if (fotoPath != null) {
                                val src = File(fotoPath!!)
                                if (src.exists() && !src.parentFile?.absolutePath.orEmpty().startsWith(notaFolder.absolutePath)) {
                                    notaFolder.mkdirs()
                                    val dest = File(notaFolder, "scontrino_${System.currentTimeMillis()}.${src.extension.ifBlank { "jpg" }}")
                                    src.copyTo(dest, overwrite = true)
                                    dest.absolutePath
                                } else fotoPath
                            } else null
                            onSave(Spesa(id = existingSpesa?.id ?: 0, notaSpeseId = notaSpeseId, descrizione = descrizione, importo = importo.toDouble(), data = data, metodoPagamento = metodoPagamento, categoria = categoria, fotoScontrinoPath = destPath, pagatoDa = pagatoDa))
                        },
                        enabled = isFormValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Salva")
                    }
                }
            }
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Allegato", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedButton(
                        onClick = {
                            val chooser = JFileChooser()
                            chooser.addChoosableFileFilter(FileNameExtensionFilter("Immagini e PDF", "jpg", "jpeg", "png", "pdf"))
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                chooser.selectedFile?.let { fotoPath = it.absolutePath }
                            }
                        }
                    ) {
                        Icon(Icons.Default.AttachFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (fotoPath != null) "File: ${File(fotoPath!!).name}" else "Scegli foto o PDF")
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dettagli Spesa", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = importo, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) importo = it }, label = { Text("Importo *") }, leadingIcon = { Icon(Icons.Default.Euro, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = dataStr, onValueChange = { dataStr = it }, label = { Text("Data *") }, placeholder = { Text("gg/mm/aaaa") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = descrizione, onValueChange = { descrizione = it }, label = { Text("Descrizione") }, leadingIcon = { Icon(Icons.Default.Description, null) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pagato da", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PagatoDa.entries.forEach { pd ->
                            FilterChip(
                                selected = pagatoDa == pd,
                                onClick = { pagatoDa = pd },
                                label = { Text(pd.displayName) },
                                leadingIcon = if (pagatoDa == pd) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                            )
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Categoria", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoriaSpesa.entries.take(4).forEach { cat ->
                            FilterChip(selected = categoria == cat, onClick = { categoria = cat }, label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoriaSpesa.entries.drop(4).forEach { cat ->
                            FilterChip(selected = categoria == cat, onClick = { categoria = cat }, label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }
}
