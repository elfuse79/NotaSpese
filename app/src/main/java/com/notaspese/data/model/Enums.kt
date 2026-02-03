package com.notaspese.data.model

// Versione software dell'applicazione
const val APP_VERSION = "1.0.4"

enum class MetodoPagamento(val displayName: String) {
    CARTA_CREDITO("Carta di Credito"),
    CONTANTI("Pag. Elettronico Dip."),
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
