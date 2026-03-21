package com.notaspese.desktop.data.model

data class Spesa(
    val id: Long = 0,
    val notaSpeseId: Long,
    val descrizione: String,
    val importo: Double,
    val data: Long,
    val metodoPagamento: MetodoPagamento,
    val categoria: CategoriaSpesa,
    val fotoScontrinoPath: String? = null,
    val pagatoDa: PagatoDa = PagatoDa.AZIENDA
)
