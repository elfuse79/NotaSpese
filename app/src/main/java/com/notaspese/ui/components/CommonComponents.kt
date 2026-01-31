package com.notaspese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notaspese.data.model.CategoriaSpesa
import com.notaspese.data.model.MetodoPagamento
import com.notaspese.data.model.PagatoDa
import com.notaspese.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(label: String, selectedDate: Long?, onDateSelected: (Long) -> Unit, modifier: Modifier = Modifier) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    
    OutlinedTextField(
        value = selectedDate?.let { dateFormatter.format(Date(it)) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, null) } },
        modifier = modifier.fillMaxWidth()
    )
    
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onDateSelected(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annulla") } }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(label: String, selectedTime: String, onTimeSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Parsing dell'ora esistente
    val initialHour = selectedTime.split(":").getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = selectedTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    
    OutlinedTextField(
        value = selectedTime,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = { IconButton(onClick = { showTimePicker = true }) { Icon(Icons.Default.Schedule, null) } },
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("HH:mm") }
    )
    
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(label) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val hour = String.format("%02d", timePickerState.hour)
                    val minute = String.format("%02d", timePickerState.minute)
                    onTimeSelected("$hour:$minute")
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (selectedTime.isNotBlank()) {
                        TextButton(onClick = {
                            onTimeSelected("")
                            showTimePicker = false
                        }) { Text("Cancella") }
                    }
                    TextButton(onClick = { showTimePicker = false }) { Text("Annulla") }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetodoPagamentoSelector(selected: MetodoPagamento, onSelect: (MetodoPagamento) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(modifier = modifier) {
        Text("Metodo di Pagamento", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetodoPagamento.entries.forEach { metodo ->
                val color = when (metodo) { MetodoPagamento.CARTA_CREDITO -> ColorCarta; MetodoPagamento.CONTANTI -> ColorContanti; MetodoPagamento.ALTRO -> ColorAltro }
                FilterChip(
                    selected = metodo == selected,
                    onClick = { if (enabled) onSelect(metodo) },
                    label = { Text(metodo.displayName, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = if (metodo == selected) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = if (enabled) 0.2f else 0.1f), 
                        selectedLabelColor = if (enabled) color else color.copy(alpha = 0.5f)
                    ),
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagatoDaSelector(selected: PagatoDa, onSelect: (PagatoDa) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Pagato da", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PagatoDa.entries.forEach { pagatore ->
                val color = when (pagatore) { 
                    PagatoDa.AZIENDA -> ColorCarta 
                    PagatoDa.DIPENDENTE -> ColorContanti 
                }
                val icon = when (pagatore) {
                    PagatoDa.AZIENDA -> Icons.Default.Business
                    PagatoDa.DIPENDENTE -> Icons.Default.Person
                }
                FilterChip(
                    selected = pagatore == selected,
                    onClick = { onSelect(pagatore) },
                    label = { Text(pagatore.displayName, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f), 
                        selectedLabelColor = color
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CategoriaSpesaSelector(selected: CategoriaSpesa, onSelect: (CategoriaSpesa) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Categoria", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Prima riga: prime 3 categorie
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CategoriaSpesa.entries.take(3).forEach { cat -> CategoriaChip(cat, cat == selected, { onSelect(cat) }, Modifier.weight(1f)) }
            }
            // Seconda riga: categorie 4-6
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CategoriaSpesa.entries.drop(3).take(3).forEach { cat -> CategoriaChip(cat, cat == selected, { onSelect(cat) }, Modifier.weight(1f)) }
            }
            // Terza riga: categoria Altro (centrata)
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                CategoriaSpesa.entries.drop(6).forEach { cat -> CategoriaChip(cat, cat == selected, { onSelect(cat) }, Modifier.width(120.dp)) }
            }
        }
    }
}

@Composable
private fun CategoriaChip(categoria: CategoriaSpesa, isSelected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val color = getCategoriaColor(categoria)
    val icon = when (categoria) {
        CategoriaSpesa.VITTO -> Icons.Default.Restaurant
        CategoriaSpesa.ALLOGGIO -> Icons.Default.Hotel
        CategoriaSpesa.PEDAGGI -> Icons.Default.Toll
        CategoriaSpesa.PARCHEGGI -> Icons.Default.LocalParking
        CategoriaSpesa.CARBURANTE -> Icons.Default.LocalGasStation
        CategoriaSpesa.ALTRI_MEZZI -> Icons.Default.DirectionsBus
        CategoriaSpesa.ALTRO -> Icons.Default.MoreHoriz
    }
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier.height(56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(4.dp)) {
            Icon(icon, null, tint = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Text(categoria.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun TotaleCard(totale: Double, anticipo: Double, totaleDovuto: Double, rimborsoKm: Double = 0.0, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Riepilogo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Totale Spese:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text("€ %.2f".format(totale), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            if (rimborsoKm > 0) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rimborso Km:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text("+ € %.2f".format(rimborsoKm), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorContanti)
                }
            }
            if (anticipo > 0) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Anticipo:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text("- € %.2f".format(anticipo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Divider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            
            // Totale dovuto (spese + rimborso km - anticipo)
            val totaleConKm = totaleDovuto + rimborsoKm
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Totale Dovuto:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("€ %.2f".format(totaleConKm), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (totaleConKm >= 0) MaterialTheme.colorScheme.onPrimaryContainer else Error)
            }
        }
    }
}

fun getCategoriaColor(categoria: CategoriaSpesa): Color = when (categoria) {
    CategoriaSpesa.VITTO -> ColorVitto
    CategoriaSpesa.ALLOGGIO -> ColorAlloggio
    CategoriaSpesa.PEDAGGI -> ColorPedaggi
    CategoriaSpesa.PARCHEGGI -> ColorParcheggi
    CategoriaSpesa.CARBURANTE -> ColorCarburante
    CategoriaSpesa.ALTRI_MEZZI -> ColorAltriMezzi
    CategoriaSpesa.ALTRO -> ColorAltroCategoria
}
