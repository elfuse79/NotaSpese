package com.notaspese.desktop.data.model

data class NotaSpeseConSpese(
    val notaSpese: NotaSpese,
    val spese: List<Spesa>
) {
    val totaleSpese: Double get() = spese.sumOf { it.importo }
    val totaleDovuto: Double get() = totaleSpese - notaSpese.anticipo
    val totaleByCarta: Double get() = spese.filter { it.metodoPagamento == MetodoPagamento.CARTA_CREDITO }.sumOf { it.importo }
    val totaleContanti: Double get() = spese.filter { it.metodoPagamento == MetodoPagamento.CONTANTI }.sumOf { it.importo }
    val totaleAltro: Double get() = spese.filter { it.metodoPagamento == MetodoPagamento.ALTRO }.sumOf { it.importo }
    val totalePagatoAzienda: Double get() = spese.filter { it.pagatoDa == PagatoDa.AZIENDA }.sumOf { it.importo }
    val totalePagatoDipendente: Double get() = spese.filter { it.pagatoDa == PagatoDa.DIPENDENTE }.sumOf { it.importo }
    val totalePagatoDipendenteConKm: Double get() = totalePagatoDipendente + notaSpese.totaleRimborsoKm
    val totaleRimborsoDipendente: Double get() = totalePagatoDipendente + notaSpese.totaleRimborsoKm - notaSpese.anticipo
    val totaleRimborsoDipendenteLordo: Double get() = totalePagatoDipendente + notaSpese.totaleRimborsoKm
    val costoComplessivoNotaSpese: Double get() = totaleSpese + notaSpese.totaleRimborsoKm
    fun totaleByCategoria(categoria: CategoriaSpesa): Double = spese.filter { it.categoria == categoria }.sumOf { it.importo }
    val speseAzienda: List<Spesa> get() = spese.filter { it.pagatoDa == PagatoDa.AZIENDA }
    val speseDipendente: List<Spesa> get() = spese.filter { it.pagatoDa == PagatoDa.DIPENDENTE }
    val haSpeseDipendente: Boolean get() = speseDipendente.isNotEmpty()
    val haRimborsoDipendente: Boolean get() = speseDipendente.isNotEmpty() || notaSpese.totaleRimborsoKm > 0
}
