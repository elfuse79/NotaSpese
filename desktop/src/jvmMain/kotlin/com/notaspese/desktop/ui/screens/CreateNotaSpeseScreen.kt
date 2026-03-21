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
import com.notaspese.desktop.data.model.NotaSpese
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotaSpeseScreen(
    onNavigateBack: () -> Unit,
    onSave: (NotaSpese) -> Unit,
    existingNota: NotaSpese?
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ITALY) }
    var numeroNota by remember { mutableStateOf(existingNota?.numeroNota ?: "") }
    var nomeCognome by remember { mutableStateOf(existingNota?.nomeCognome ?: "") }
    var dataInizioStr by remember { mutableStateOf(existingNota?.let { dateFormatter.format(Date(it.dataInizioTrasferta)) } ?: "") }
    var oraInizio by remember { mutableStateOf(existingNota?.oraInizioTrasferta ?: "") }
    var dataFineStr by remember { mutableStateOf(existingNota?.let { dateFormatter.format(Date(it.dataFineTrasferta)) } ?: "") }
    var oraFine by remember { mutableStateOf(existingNota?.oraFineTrasferta ?: "") }
    var luogo by remember { mutableStateOf(existingNota?.luogoTrasferta ?: "") }
    var cliente by remember { mutableStateOf(existingNota?.cliente ?: "") }
    var causale by remember { mutableStateOf(existingNota?.causale ?: "") }
    var auto by remember { mutableStateOf(existingNota?.auto ?: "") }
    var altriTrasfertisti by remember { mutableStateOf(existingNota?.altriTrasfertisti ?: "") }
    var anticipo by remember { mutableStateOf(if ((existingNota?.anticipo ?: 0.0) > 0) "%.2f".format(existingNota?.anticipo) else "") }
    var kmPercorsi by remember { mutableStateOf(if ((existingNota?.kmPercorsi ?: 0.0) > 0) "%.0f".format(existingNota?.kmPercorsi) else "") }
    var costoKmRimborso by remember { mutableStateOf(if ((existingNota?.costoKmRimborso ?: 0.0) > 0) "%.2f".format(existingNota?.costoKmRimborso) else "") }
    var costoKmCliente by remember { mutableStateOf(if ((existingNota?.costoKmCliente ?: 0.0) > 0) "%.2f".format(existingNota?.costoKmCliente) else "0.60") }

    fun parseDate(s: String): Long? = try { dateFormatter.parse(s)?.time } catch (_: Exception) { null }
    fun getLastDayOfMonth(ts: Long): Long = Calendar.getInstance().apply { timeInMillis = ts; set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }.timeInMillis

    val dataInizio = parseDate(dataInizioStr)
    val dataFine = parseDate(dataFineStr)
    val isFormValid = nomeCognome.isNotBlank() && dataInizio != null && luogo.isNotBlank() && cliente.isNotBlank()
    val isEditing = existingNota != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Modifica Nota Spese" else "Nuova Nota Spese", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Indietro") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) { Text("Annulla") }
                    Button(
                        onClick = {
                            val dc = if (existingNota != null) existingNota.dataCompilazione else getLastDayOfMonth(dataInizio!!)
                            val nota = NotaSpese(
                                id = existingNota?.id ?: 0,
                                numeroNota = numeroNota,
                                nomeCognome = nomeCognome,
                                dataInizioTrasferta = dataInizio!!,
                                oraInizioTrasferta = oraInizio,
                                dataFineTrasferta = dataFine ?: dataInizio!!,
                                oraFineTrasferta = oraFine,
                                luogoTrasferta = luogo,
                                cliente = cliente,
                                causale = causale,
                                auto = auto,
                                dataCompilazione = dc,
                                altriTrasfertisti = altriTrasfertisti,
                                anticipo = anticipo.toDoubleOrNull() ?: 0.0,
                                kmPercorsi = kmPercorsi.toDoubleOrNull() ?: 0.0,
                                costoKmRimborso = costoKmRimborso.toDoubleOrNull() ?: 0.0,
                                costoKmCliente = costoKmCliente.toDoubleOrNull() ?: 0.60
                            )
                            onSave(nota)
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
            Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Numero Nota Spese", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = numeroNota, onValueChange = { numeroNota = it }, label = { Text("N° Nota (opzionale)") }, leadingIcon = { Icon(Icons.Default.Numbers, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dati Personali", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = nomeCognome, onValueChange = { nomeCognome = it }, label = { Text("Nome e Cognome *") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Dettagli Trasferta", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = dataInizioStr, onValueChange = { dataInizioStr = it }, label = { Text("Data Inizio *") }, placeholder = { Text("gg/mm/aaaa") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = dataFineStr, onValueChange = { dataFineStr = it }, label = { Text("Data Fine") }, placeholder = { Text("gg/mm/aaaa") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = oraInizio, onValueChange = { oraInizio = it }, label = { Text("Ora Inizio") }, placeholder = { Text("HH:mm") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = oraFine, onValueChange = { oraFine = it }, label = { Text("Ora Fine") }, placeholder = { Text("HH:mm") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = luogo, onValueChange = { luogo = it }, label = { Text("Luogo Trasferta *") }, leadingIcon = { Icon(Icons.Default.LocationOn, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = cliente, onValueChange = { cliente = it }, label = { Text("Cliente *") }, leadingIcon = { Icon(Icons.Default.Business, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = causale, onValueChange = { causale = it }, label = { Text("Causale") }, leadingIcon = { Icon(Icons.Default.Description, null) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Informazioni Aggiuntive", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = auto, onValueChange = { auto = it }, label = { Text("Auto (targa/modello)") }, leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = altriTrasfertisti, onValueChange = { altriTrasfertisti = it }, label = { Text("Altri Trasfertisti") }, leadingIcon = { Icon(Icons.Default.Group, null) }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Chilometri", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = kmPercorsi, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) kmPercorsi = it }, label = { Text("Km Percorsi") }, leadingIcon = { Icon(Icons.Default.Route, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = costoKmRimborso, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) costoKmRimborso = it }, label = { Text("€/km Rimborso") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = costoKmCliente, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) costoKmCliente = it }, label = { Text("€/km Cliente") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Anticipo", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = anticipo, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) anticipo = it }, label = { Text("Anticipo Ricevuto (EUR)") }, leadingIcon = { Icon(Icons.Default.Euro, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
