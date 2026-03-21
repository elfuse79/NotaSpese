package com.notaspese.desktop.data.model

data class NotaSpese(
    val id: Long = 0,
    val numeroNota: String = "",
    val nomeCognome: String,
    val dataInizioTrasferta: Long,
    val oraInizioTrasferta: String = "",
    val dataFineTrasferta: Long,
    val oraFineTrasferta: String = "",
    val luogoTrasferta: String,
    val cliente: String,
    val causale: String,
    val auto: String,
    val dataCompilazione: Long,
    val altriTrasfertisti: String = "",
    val anticipo: Double = 0.0,
    val kmPercorsi: Double = 0.0,
    val costoKmRimborso: Double = 0.0,
    val costoKmCliente: Double = 0.0
) {
    val totaleRimborsoKm: Double get() = kmPercorsi * costoKmRimborso
    val totaleCostoKmCliente: Double get() = kmPercorsi * costoKmCliente
}
