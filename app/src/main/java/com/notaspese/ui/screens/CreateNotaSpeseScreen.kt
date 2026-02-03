package com.notaspese.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.notaspese.data.model.NotaSpese
import com.notaspese.ui.components.DatePickerField
import com.notaspese.ui.components.TimePickerField
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNotaSpeseScreen(onNavigateBack: () -> Unit, onSave: (NotaSpese) -> Unit, existingNota: NotaSpese? = null) {
    var numeroNota by remember { mutableStateOf(existingNota?.numeroNota ?: "") }
    var nomeCognome by remember { mutableStateOf(existingNota?.nomeCognome ?: "") }
    var dataInizio by remember { mutableStateOf<Long?>(existingNota?.dataInizioTrasferta) }
    var oraInizio by remember { mutableStateOf(existingNota?.oraInizioTrasferta ?: "") }
    var dataFine by remember { mutableStateOf<Long?>(existingNota?.dataFineTrasferta) }
    var oraFine by remember { mutableStateOf(existingNota?.oraFineTrasferta ?: "") }
    var luogo by remember { mutableStateOf(existingNota?.luogoTrasferta ?: "") }
    var cliente by remember { mutableStateOf(existingNota?.cliente ?: "") }
    var causale by remember { mutableStateOf(existingNota?.causale ?: "") }
    var auto by remember { mutableStateOf(existingNota?.auto ?: "") }
    var altriTrasfertisti by remember { mutableStateOf(existingNota?.altriTrasfertisti ?: "") }
    var anticipo by remember { mutableStateOf(if ((existingNota?.anticipo ?: 0.0) > 0) "%.2f".format(existingNota?.anticipo) else "") }
    
    // Campi per i chilometri
    var kmPercorsi by remember { mutableStateOf(if ((existingNota?.kmPercorsi ?: 0.0) > 0) "%.0f".format(existingNota?.kmPercorsi) else "") }
    var costoKmRimborso by remember { mutableStateOf(if ((existingNota?.costoKmRimborso ?: 0.0) > 0) "%.2f".format(existingNota?.costoKmRimborso) else "") }
    var costoKmCliente by remember { mutableStateOf(if ((existingNota?.costoKmCliente ?: 0.0) > 0) "%.2f".format(existingNota?.costoKmCliente) else "0.60") }
    
    // Funzione per calcolare l'ultimo giorno del mese
    fun getLastDayOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return calendar.timeInMillis
    }
    
    val isEditing = existingNota != null
    val isFormValid = nomeCognome.isNotBlank() && dataInizio != null && luogo.isNotBlank() && cliente.isNotBlank()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (isEditing) "Modifica Nota Spese" else "Nuova Nota Spese", fontWeight = FontWeight.SemiBold) }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Indietro") } }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) { Text("Annulla") }
                    Button(onClick = { 
                        // Calcola la data di compilazione come ultimo giorno del mese della data inizio
                        val dataCompilazione = if (existingNota != null) {
                            existingNota.dataCompilazione
                        } else {
                            getLastDayOfMonth(dataInizio!!)
                        }
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
                            dataCompilazione = dataCompilazione, 
                            altriTrasfertisti = altriTrasfertisti, 
                            anticipo = anticipo.toDoubleOrNull() ?: 0.0,
                            kmPercorsi = kmPercorsi.toDoubleOrNull() ?: 0.0,
                            costoKmRimborso = costoKmRimborso.toDoubleOrNull() ?: 0.0,
                            costoKmCliente = costoKmCliente.toDoubleOrNull() ?: 0.60
                        )
                        onSave(nota) 
                    }, enabled = isFormValid, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Salva") }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Numero Nota Spese", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = numeroNota, 
                        onValueChange = { numeroNota = it }, 
                        label = { Text("N° Nota (opzionale)") }, 
                        leadingIcon = { Icon(Icons.Default.Numbers, null) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true,
                        supportingText = { Text("Lasciare vuoto per compilazione successiva") }
                    )
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
                        DatePickerField("Data Inizio *", dataInizio, { dataInizio = it }, Modifier.weight(1f))
                        DatePickerField("Data Fine", dataFine, { dataFine = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimePickerField("Ora Inizio", oraInizio, { oraInizio = it }, Modifier.weight(1f))
                        TimePickerField("Ora Fine", oraFine, { oraFine = it }, Modifier.weight(1f))
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
                    OutlinedTextField(value = altriTrasfertisti, onValueChange = { altriTrasfertisti = it }, label = { Text("Altri Trasfertisti") }, leadingIcon = { Icon(Icons.Default.Group, null) }, modifier = Modifier.fillMaxWidth(), supportingText = { Text("Separare i nomi con virgola") }, maxLines = 2)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Chilometri", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = kmPercorsi, 
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) kmPercorsi = it }, 
                        label = { Text("Km Percorsi") }, 
                        leadingIcon = { Icon(Icons.Default.Route, null) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = costoKmRimborso, 
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) costoKmRimborso = it }, 
                            label = { Text("€/km Rimborso") }, 
                            leadingIcon = { Icon(Icons.Default.Euro, null) }, 
                            modifier = Modifier.weight(1f), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                            singleLine = true,
                            supportingText = { Text("Al trasfertista") }
                        )
                        OutlinedTextField(
                            value = costoKmCliente, 
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) costoKmCliente = it }, 
                            label = { Text("€/km Cliente") }, 
                            leadingIcon = { Icon(Icons.Default.Euro, null) }, 
                            modifier = Modifier.weight(1f), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                            singleLine = true,
                            supportingText = { Text("Da addebitare") }
                        )
                    }
                    // Preview dei totali km
                    val kmVal = kmPercorsi.toDoubleOrNull() ?: 0.0
                    val rimborsoVal = costoKmRimborso.toDoubleOrNull() ?: 0.0
                    val clienteVal = costoKmCliente.toDoubleOrNull() ?: 0.0
                    if (kmVal > 0 && (rimborsoVal > 0 || clienteVal > 0)) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                if (rimborsoVal > 0) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Rimborso trasfertista:", style = MaterialTheme.typography.bodySmall)
                                        Text("€ %.2f".format(kmVal * rimborsoVal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (clienteVal > 0) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Addebito cliente:", style = MaterialTheme.typography.bodySmall)
                                        Text("€ %.2f".format(kmVal * clienteVal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Anticipo", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = anticipo, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) anticipo = it }, label = { Text("Anticipo Ricevuto (EUR)") }, leadingIcon = { Icon(Icons.Default.Euro, null) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, supportingText = { Text("Verra sottratto dal totale spese") })
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
