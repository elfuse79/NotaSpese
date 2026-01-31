package com.notaspese.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nota_spese")
data class NotaSpese(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numeroNota: String = "",  // Numero nota spese (opzionale, compilabile dall'amministrazione)
    val nomeCognome: String,
    val dataInizioTrasferta: Long,
    val oraInizioTrasferta: String = "",  // Ora inizio trasferta (formato HH:mm)
    val dataFineTrasferta: Long,
    val oraFineTrasferta: String = "",    // Ora fine trasferta (formato HH:mm)
    val luogoTrasferta: String,
    val cliente: String,
    val causale: String,
    val auto: String,
    val dataCompilazione: Long,
    val altriTrasfertisti: String = "",
    val anticipo: Double = 0.0,
    // Campi per i chilometri
    val kmPercorsi: Double = 0.0,
    val costoKmRimborso: Double = 0.0,  // Costo al km da rimborsare al trasfertista/dipendente
    val costoKmCliente: Double = 0.0     // Costo al km da addebitare al cliente
) {
    // Calcoli per i km
    val totaleRimborsoKm: Double get() = kmPercorsi * costoKmRimborso
    val totaleCostoKmCliente: Double get() = kmPercorsi * costoKmCliente
}
