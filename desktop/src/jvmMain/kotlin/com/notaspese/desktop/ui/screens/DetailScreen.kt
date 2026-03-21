package com.notaspese.desktop.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notaspese.desktop.data.model.*
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    notaSpeseConSpese: NotaSpeseConSpese?,
    onNavigateBack: () -> Unit,
    onAddSpesa: () -> Unit,
    onEditSpesa: (Spesa) -> Unit,
    onDeleteSpesa: (Spesa) -> Unit,
    onEditAnticipo: (Double) -> Unit,
    onEditNota: () -> Unit,
    onExport: (NotaSpeseConSpese) -> File?
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALY) }
    var showAnticipoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Spesa?>(null) }
    var exportFolder by remember { mutableStateOf<File?>(null) }

    if (notaSpeseConSpese == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val nota = notaSpeseConSpese.notaSpese

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nota.nomeCognome, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Indietro") } },
                actions = {
                    IconButton(onClick = onEditNota) { Icon(Icons.Default.Edit, "Modifica nota") }
                    IconButton(onClick = { showAnticipoDialog = true }) { Icon(Icons.Default.Euro, "Modifica anticipo") }
                    IconButton(onClick = {
                        val folder = onExport(notaSpeseConSpese)
                        folder?.let { exportFolder = it; Desktop.getDesktop().open(it) }
                    }) { Icon(Icons.Default.Share, "Esporta PDF e CSV") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddSpesa, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Aggiungi Spesa") })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(end = 12.dp)
            ) {
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
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Orario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("${nota.oraInizioTrasferta.ifBlank { "--:--" }} - ${nota.oraFineTrasferta.ifBlank { "--:--" }}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Luogo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(nota.luogoTrasferta, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Cliente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(nota.cliente, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (nota.auto.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text("Auto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)); Text(nota.auto, style = MaterialTheme.typography.bodyMedium) }
                        if (nota.causale.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text("Causale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)); Text(nota.causale, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }

            if (nota.kmPercorsi > 0) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Chilometri", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Km: %.0f | Rimborso: € %.2f | Cliente: € %.2f".format(nota.kmPercorsi, nota.totaleRimborsoKm, nota.totaleCostoKmCliente), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Riepilogo", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Totale Spese:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text("€ %.2f".format(notaSpeseConSpese.totaleSpese), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        if (nota.totaleRimborsoKm > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rimborso Km:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Text("+ € %.2f".format(nota.totaleRimborsoKm), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (nota.anticipo > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Anticipo:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Text("- € %.2f".format(nota.anticipo), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Divider(Modifier.padding(vertical = 12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Costo Complessivo:", style = MaterialTheme.typography.titleMedium)
                            Text("€ %.2f".format(notaSpeseConSpese.costoComplessivoNotaSpese), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("Spese (${notaSpeseConSpese.spese.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            items(notaSpeseConSpese.spese) { spesa ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(spesa.descrizione.ifBlank { spesa.categoria.displayName }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(dateFormatter.format(Date(spesa.data)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${spesa.categoria.displayName} | ${spesa.pagatoDa.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("€ %.2f".format(spesa.importo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onEditSpesa(spesa) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, "Modifica", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showDeleteDialog = spesa }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState)
            )
        }
    }

    if (showAnticipoDialog) {
        var anticipoInput by remember { mutableStateOf(if (nota.anticipo > 0) "%.2f".format(nota.anticipo) else "") }
        AlertDialog(
            onDismissRequest = { showAnticipoDialog = false },
            title = { Text("Modifica Anticipo") },
            text = {
                OutlinedTextField(
                    value = anticipoInput,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) anticipoInput = it },
                    label = { Text("Anticipo (EUR)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditAnticipo(anticipoInput.toDoubleOrNull() ?: 0.0)
                    showAnticipoDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showAnticipoDialog = false }) { Text("Annulla") }
            }
        )
    }

    showDeleteDialog?.let { spesa ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Elimina Spesa") },
            text = { Text("Eliminare questa spesa?") },
            confirmButton = {
                TextButton(onClick = { onDeleteSpesa(spesa); showDeleteDialog = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Annulla") }
            }
        )
    }
}
