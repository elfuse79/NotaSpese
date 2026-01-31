package com.notaspese.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class NotaSpeseConSpese(
    @Embedded val notaSpese: NotaSpese,
    @Relation(parentColumn = "id", entityColumn = "notaSpeseId") val spese: List<Spesa>
) {
    // Totali generali
    val totaleSpese: Double get() = spese.sumOf { it.importo }
    val totaleDovuto: Double get() = totaleSpese - notaSpese.anticipo
    
    // Totali per metodo pagamento
    val totaleByCarta: Double get() = spese.filter { it.metodoPagamento == MetodoPagamento.CARTA_CREDITO }.sumOf { it.importo }
    val totaleContanti: Double get() = spese.filter { it.metodoPagamento == MetodoPagamento.CONTANTI }.sumOf { it.importo }
    val totaleAltro: Double get() = spese.filter { it.metodoPagamento == MetodoPagamento.ALTRO }.sumOf { it.importo }
    
    // Totali per chi ha pagato
    val totalePagatoAzienda: Double get() = spese.filter { it.pagatoDa == PagatoDa.AZIENDA }.sumOf { it.importo }
    val totalePagatoDipendente: Double get() = spese.filter { it.pagatoDa == PagatoDa.DIPENDENTE }.sumOf { it.importo }
    
    // Totale da rimborsare al dipendente (spese pagate dal dipendente + rimborso km - anticipo)
    val totaleRimborsoDipendente: Double get() = totalePagatoDipendente + notaSpese.totaleRimborsoKm - notaSpese.anticipo
    
    // Totale lordo rimborso dipendente (senza anticipo, per visualizzazione)
    val totaleRimborsoDipendenteLordo: Double get() = totalePagatoDipendente + notaSpese.totaleRimborsoKm
    
    // Costo complessivo della nota spese (tutte le spese + rimborso km)
    val costoComplessivoNotaSpese: Double get() = totaleSpese + notaSpese.totaleRimborsoKm
    
    fun totaleByCategoria(categoria: CategoriaSpesa): Double = spese.filter { it.categoria == categoria }.sumOf { it.importo }
    
    // Spese per pagatore
    val speseAzienda: List<Spesa> get() = spese.filter { it.pagatoDa == PagatoDa.AZIENDA }
    val speseDipendente: List<Spesa> get() = spese.filter { it.pagatoDa == PagatoDa.DIPENDENTE }
}
