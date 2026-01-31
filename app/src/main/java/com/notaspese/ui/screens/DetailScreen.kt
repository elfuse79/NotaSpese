package com.notaspese.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notaspese.data.model.*
import com.notaspese.ui.components.TotaleCard
import com.notaspese.ui.components.getCategoriaColor
import com.notaspese.ui.theme.*
import com.notaspese.util.CsvExporter
import com.notaspese.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(notaSpeseConSpese: NotaSpeseConSpese?, onNavigateBack: () -> Unit, onAddSpesa: () -> Unit, onDeleteSpesa: (Spesa) -> Unit, onEditAnticipo: (Double) -> Unit, onEditNota: () -> Unit) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    var showAnticipoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Spesa?>(null) }
    var showExportOptions by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<java.io.File?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }
    
    if (notaSpeseConSpese == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    
    val nota = notaSpeseConSpese.notaSpese
    val numFoto = notaSpeseConSpese.spese.count { it.fotoScontrinoPath != null }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nota.nomeCognome, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Indietro") } },
                actions = {
                    IconButton(onClick = onEditNota) { Icon(Icons.Default.Edit, "Modifica nota") }
                    IconButton(onClick = { showAnticipoDialog = true }) { Icon(Icons.Default.Euro, "Modifica anticipo") }
                    IconButton(onClick = { showExportOptions = true }) { 
                        Icon(Icons.Default.Share, "Esporta") 
                    }
                }
            )
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAddSpesa, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Aggiungi Spesa") }) }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { 
                                Text("Periodo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                val dataFineStr = if (nota.dataFineTrasferta != nota.dataInizioTrasferta) " - ${dateFormatter.format(Date(nota.dataFineTrasferta))}" else ""
                                Text("${dateFormatter.format(Date(nota.dataInizioTrasferta))}$dataFineStr", style = MaterialTheme.typography.bodyMedium) 
                            }
                            if (nota.oraInizioTrasferta.isNotBlank() || nota.oraFineTrasferta.isNotBlank()) {
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text("Orario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    val oraInizio = nota.oraInizioTrasferta.ifBlank { "--:--" }
                                    val oraFine = nota.oraFineTrasferta.ifBlank { "--:--" }
                                    Text("$oraInizio - $oraFine", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f)) { Text("Luogo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)); Text(nota.luogoTrasferta, style = MaterialTheme.typography.bodyMedium) }
                            Column(Modifier.weight(1f)) { Text("Cliente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)); Text(nota.cliente, style = MaterialTheme.typography.bodyMedium) }
                        }
                        if (nota.auto.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text("Auto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)); Text(nota.auto, style = MaterialTheme.typography.bodyMedium) }
                        if (nota.causale.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text("Causale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)); Text(nota.causale, style = MaterialTheme.typography.bodyMedium) }
                        if (nota.altriTrasfertisti.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text("Altri trasfertisti", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)); Text(nota.altriTrasfertisti, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            
            // Card Chilometri (se presenti)
            if (nota.kmPercorsi > 0) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("Chilometri", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            // Km percorsi
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Km Percorsi:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("%.0f km".format(nota.kmPercorsi), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            
                            if (nota.costoKmRimborso > 0) {
                                Spacer(Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                
                                // Costo al km rimborso
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Costo/km rimborso:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    Text("€ %.2f".format(nota.costoKmRimborso), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                                Spacer(Modifier.height(4.dp))
                                
                                // Totale rimborso trasfertista
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Rimborso Trasfertista:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("€ %.2f".format(nota.totaleRimborsoKm), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorContanti)
                                }
                            }
                            
                            if (nota.costoKmCliente > 0) {
                                Spacer(Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                
                                // Costo al km cliente
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Costo/km cliente:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    Text("€ %.2f".format(nota.costoKmCliente), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                                Spacer(Modifier.height(4.dp))
                                
                                // Totale addebito cliente
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Addebito Cliente:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("€ %.2f".format(nota.totaleCostoKmCliente), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorCarta)
                                }
                            }
                        }
                    }
                }
            }
            
            item { TotaleCard(notaSpeseConSpese.totaleSpese, nota.anticipo, notaSpeseConSpese.totaleDovuto, nota.totaleRimborsoKm) }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Dettaglio per Categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                        CategoriaSpesa.entries.forEach { cat ->
                            val tot = notaSpeseConSpese.totaleByCategoria(cat)
                            if (tot > 0) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(getCategoriaColor(cat))); Spacer(Modifier.width(8.dp)); Text(cat.displayName, style = MaterialTheme.typography.bodyMedium) }; Text("EUR %.2f".format(tot), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) } }
                        }
                        Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text("Per Metodo Pagamento", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Carta", style = MaterialTheme.typography.labelSmall); Text("EUR %.2f".format(notaSpeseConSpese.totaleByCarta), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorCarta) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Contanti", style = MaterialTheme.typography.labelSmall); Text("EUR %.2f".format(notaSpeseConSpese.totaleContanti), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorContanti) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Altro", style = MaterialTheme.typography.labelSmall); Text("EUR %.2f".format(notaSpeseConSpese.totaleAltro), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorAltro) }
                        }
                        
                        // Sezione spese sostenute
                        Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text("Spese Sostenute Da", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Business, null, Modifier.size(14.dp), tint = ColorCarta)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Azienda", style = MaterialTheme.typography.labelSmall)
                                }
                                Text("EUR %.2f".format(notaSpeseConSpese.totalePagatoAzienda), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorCarta) 
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = ColorContanti)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Dipendente", style = MaterialTheme.typography.labelSmall)
                                }
                                Text("EUR %.2f".format(notaSpeseConSpese.totalePagatoDipendente), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorContanti) 
                            }
                        }
                    }
                }
            }
            
            // Card Rimborso Dipendente (se ci sono spese pagate dal dipendente o rimborso km o anticipo)
            if (notaSpeseConSpese.totaleRimborsoDipendenteLordo > 0 || nota.anticipo > 0) {
                item {
                    Card(
                        Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(16.dp), 
                        colors = CardDefaults.cardColors(containerColor = ColorContanti.copy(alpha = 0.15f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, null, tint = ColorContanti)
                                Spacer(Modifier.width(8.dp))
                                Text("Da Rimborsare al Dipendente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ColorContanti)
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            if (notaSpeseConSpese.totalePagatoDipendente > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Spese pagate:", style = MaterialTheme.typography.bodyMedium)
                                    Text("+ EUR %.2f".format(notaSpeseConSpese.totalePagatoDipendente), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            if (nota.totaleRimborsoKm > 0) {
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Rimborso Km:", style = MaterialTheme.typography.bodyMedium)
                                    Text("+ EUR %.2f".format(nota.totaleRimborsoKm), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            if (nota.anticipo > 0) {
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Anticipo ricevuto:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                    Text("- EUR %.2f".format(nota.anticipo), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            
                            Divider(Modifier.padding(vertical = 8.dp), color = ColorContanti.copy(alpha = 0.3f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTALE RIMBORSO:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (notaSpeseConSpese.totaleRimborsoDipendente >= 0) ColorContanti else MaterialTheme.colorScheme.error)
                                Text("EUR %.2f".format(notaSpeseConSpese.totaleRimborsoDipendente), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (notaSpeseConSpese.totaleRimborsoDipendente >= 0) ColorContanti else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            // Card Costo Complessivo
            item {
                Card(
                    Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Costo Complessivo Nota Spese", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totale Spese:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                            Text("EUR %.2f".format(notaSpeseConSpese.totaleSpese), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        if (nota.totaleRimborsoKm > 0) {
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rimborso Km:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                Text("EUR %.2f".format(nota.totaleRimborsoKm), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                        Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("COSTO TOTALE:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("EUR %.2f".format(notaSpeseConSpese.costoComplessivoNotaSpese), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
            item { Text("Spese (${notaSpeseConSpese.spese.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp)) }
            if (notaSpeseConSpese.spese.isEmpty()) {
                item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)); Spacer(Modifier.height(12.dp)); Text("Nessuna spesa registrata", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } } }
            } else {
                items(notaSpeseConSpese.spese) { spesa ->
                    val catColor = getCategoriaColor(spesa.categoria)
                    val catIcon = when (spesa.categoria) { 
                        CategoriaSpesa.VITTO -> Icons.Default.Restaurant
                        CategoriaSpesa.ALLOGGIO -> Icons.Default.Hotel
                        CategoriaSpesa.PEDAGGI -> Icons.Default.Toll
                        CategoriaSpesa.PARCHEGGI -> Icons.Default.LocalParking
                        CategoriaSpesa.CARBURANTE -> Icons.Default.LocalGasStation
                        CategoriaSpesa.ALTRI_MEZZI -> Icons.Default.DirectionsBus
                        CategoriaSpesa.ALTRO -> Icons.Default.MoreHoriz
                    }
                    val isPdf = spesa.fotoScontrinoPath?.endsWith(".pdf", ignoreCase = true) == true
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(catColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(catIcon, null, tint = catColor, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(spesa.descrizione.ifBlank { spesa.categoria.displayName }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) { 
                                    Text(dateFormatter.format(Date(spesa.data)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(" - ", style = MaterialTheme.typography.labelSmall)
                                    Text(spesa.metodoPagamento.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    // Badge pagato da
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (spesa.pagatoDa == PagatoDa.AZIENDA) ColorCarta.copy(alpha = 0.15f) else ColorContanti.copy(alpha = 0.15f)
                                    ) {
                                        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (spesa.pagatoDa == PagatoDa.AZIENDA) Icons.Default.Business else Icons.Default.Person,
                                                null,
                                                Modifier.size(10.dp),
                                                tint = if (spesa.pagatoDa == PagatoDa.AZIENDA) ColorCarta else ColorContanti
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                if (spesa.pagatoDa == PagatoDa.AZIENDA) "Az" else "Dip",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (spesa.pagatoDa == PagatoDa.AZIENDA) ColorCarta else ColorContanti
                                            )
                                        }
                                    }
                                    if (spesa.fotoScontrinoPath != null) { 
                                        Spacer(Modifier.width(4.dp))
                                        if (isPdf) {
                                            Icon(Icons.Default.PictureAsPdf, "PDF", Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                        } else {
                                            Icon(Icons.Default.CameraAlt, "Foto", Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    } 
                                }
                            }
                            Text("EUR %.2f".format(spesa.importo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showDeleteDialog = spesa }) { Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    
    if (showAnticipoDialog) {
        var anticipo by remember { mutableStateOf(if (nota.anticipo > 0) "%.2f".format(nota.anticipo) else "") }
        AlertDialog(onDismissRequest = { showAnticipoDialog = false }, title = { Text("Modifica Anticipo") }, text = { OutlinedTextField(value = anticipo, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) anticipo = it }, label = { Text("Importo (EUR)") }, leadingIcon = { Icon(Icons.Default.Euro, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }, confirmButton = { TextButton(onClick = { onEditAnticipo(anticipo.toDoubleOrNull() ?: 0.0); showAnticipoDialog = false }) { Text("Salva") } }, dismissButton = { TextButton(onClick = { showAnticipoDialog = false }) { Text("Annulla") } })
    }
    
    showDeleteDialog?.let { spesa -> AlertDialog(onDismissRequest = { showDeleteDialog = null }, title = { Text("Elimina Spesa") }, text = { Text("Eliminare questa spesa di EUR %.2f?".format(spesa.importo)) }, confirmButton = { TextButton(onClick = { onDeleteSpesa(spesa); showDeleteDialog = null }, colors = ButtonDefaults.textButtonColors(contentColor = Error)) { Text("Elimina") } }, dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Annulla") } }) }
    
    // Bottom sheet per opzioni di esportazione
    if (showExportOptions) {
        ModalBottomSheet(onDismissRequest = { showExportOptions = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Esporta Nota Spese", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold, 
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Genera PDF + CSV + Allegati
                ListItem(
                    headlineContent = { Text("Genera PDF + CSV + Allegati") },
                    supportingContent = { Text("Crea PDF completo con riepilogo, scontrini, CSV e allegati") },
                    leadingContent = { 
                        Icon(
                            Icons.Default.PictureAsPdf, 
                            null, 
                            tint = MaterialTheme.colorScheme.error
                        ) 
                    },
                    modifier = Modifier.clickable {
                        showExportOptions = false
                        isGeneratingPdf = true
                        
                        // Genera PDF
                        val pdfFile = PdfGenerator.generatePdf(context, notaSpeseConSpese)
                        
                        // Esporta anche CSV + allegati nella stessa cartella
                        CsvExporter.exportToCsv(context, notaSpeseConSpese)
                        
                        isGeneratingPdf = false
                        
                        if (pdfFile != null) {
                            generatedPdfFile = pdfFile
                            showPdfDialog = true
                        } else {
                            Toast.makeText(context, "Errore nella generazione", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                Divider(Modifier.padding(vertical = 8.dp))
                
                // Solo CSV con allegati
                ListItem(
                    headlineContent = { Text("Solo CSV + Allegati") },
                    supportingContent = { Text("Esporta solo i dati in CSV con foto e PDF") },
                    leadingContent = { 
                        Icon(
                            Icons.Default.TableChart, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier.clickable {
                        showExportOptions = false
                        val folder = CsvExporter.exportToCsv(context, notaSpeseConSpese)
                        if (folder != null) {
                            val path = CsvExporter.getExportFolderPath(notaSpeseConSpese)
                            val numAllegati = notaSpeseConSpese.spese.count { it.fotoScontrinoPath != null }
                            val allegatoMsg = if (numAllegati > 0) " + $numAllegati allegati" else ""
                            Toast.makeText(context, "Salvato in:\n$path\n(CSV$allegatoMsg)", Toast.LENGTH_LONG).show()
                            CsvExporter.shareFile(context, folder)
                        } else {
                            Toast.makeText(context, "Errore durante esportazione", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // Loading overlay per generazione PDF
    if (isGeneratingPdf) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Generazione PDF in corso...")
                }
            }
        }
    }
    
    // Dialog per PDF generato con opzioni Apri/Condividi
    if (showPdfDialog && generatedPdfFile != null) {
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            icon = { Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("PDF Generato con Successo") },
            text = { 
                Column {
                    Text("Il PDF e il CSV sono stati generati nella cartella:")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        generatedPdfFile?.parentFile?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    val numAllegati = notaSpeseConSpese.spese.count { it.fotoScontrinoPath != null }
                    if (numAllegati > 0) {
                        Text(
                            "Inclusi $numAllegati allegati",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pulsante Apri
                    TextButton(onClick = {
                        generatedPdfFile?.let { file ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context, 
                                "${context.packageName}.fileprovider", 
                                file
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Nessuna app per aprire PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showPdfDialog = false
                    }) {
                        Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apri")
                    }
                    
                    // Pulsante Condividi
                    Button(onClick = {
                        generatedPdfFile?.let { file ->
                            PdfGenerator.sharePdf(context, file)
                        }
                        showPdfDialog = false
                    }) {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Condividi")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }
}
