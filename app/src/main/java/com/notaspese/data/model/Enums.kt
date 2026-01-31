package com.notaspese.data.model

enum class MetodoPagamento(val displayName: String) {
    CARTA_CREDITO("Carta di Credito"),
    CONTANTI("Contanti"),
    ALTRO("Altro")
}

enum class CategoriaSpesa(val displayName: String) {
    VITTO("Vitto"),
    ALLOGGIO("Alloggio"),
    PEDAGGI("Pedaggi"),
    PARCHEGGI("Parcheggi"),
    CARBURANTE("Carburante"),
    ALTRI_MEZZI("Altri Mezzi"),
    ALTRO("Altro")
}

enum class PagatoDa(val displayName: String) {
    AZIENDA("Azienda"),
    DIPENDENTE("Dipendente")
}
