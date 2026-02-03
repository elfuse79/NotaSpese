# NotaSpese - App Gestione Note Spese

**Versione:** 1.0.1

App Android per la gestione delle note spese aziendali, sviluppata con Kotlin e Jetpack Compose.

---

## 📱 Funzionalità Principali

### Gestione Note Spese
- Creazione e modifica di note spese per trasferte
- Dati trasferta: nome, cliente, luogo, date, orari
- Supporto per altri trasfertisti
- Gestione auto (targa/modello)
- Causale trasferta

### Registrazione Spese
- Categorie: Vitto, Alloggio, Pedaggi, Parcheggi, Carburante, Altri Mezzi, Altro
- Metodi pagamento: Carta di Credito (Azienda), Pagamento Elettronico (Dipendente)
- Distinzione spese Azienda/Dipendente
- Allegati foto scontrini con OCR automatico
- Supporto allegati PDF

### Gestione Chilometri
- Km percorsi
- Costo €/km rimborso al trasfertista
- Costo €/km da addebitare al cliente (default: €0.60)
- Il rimborso km viene sommato alle spese del dipendente
- L'addebito cliente non incide sul costo complessivo

### Generazione PDF
- Report completo nota spese in formato A4
- Intestazione con dati trasferta
- Dettaglio spese in due colonne (Azienda/Dipendente)
- Riepilogo per categoria
- Sezione chilometri
- Calcolo rimborso dipendente
- Costo complessivo nota spese
- Campo NOTE con descrizioni spese + righe vuote
- Allegati scontrini (foto e PDF)
- Versione SW su ogni pagina

### Export Dati
- Export CSV con allegati
- Condivisione PDF via app esterne

---

## 🔧 Tecnologie Utilizzate

- **Linguaggio:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Database:** Room (SQLite)
- **OCR:** Google ML Kit Text Recognition
- **Camera:** CameraX
- **Navigazione:** Navigation Compose
- **Immagini:** Coil

---

## 📋 Changelog

### v1.0.1 (Febbraio 2026)
- Aggiunto dettaglio rimborso km nella sezione "Spese Sostenute Da"
- Visualizzazione separata: Spese + Rimborso Km = Totale Dipendente

### v1.0.0 (Febbraio 2026)
- Data compilazione automatica (ultimo giorno del mese)
- PDF: testo troncato per evitare sovrapposizioni
- PDF: nascoste sezioni spese dipendente se assenti
- PDF: riepilogo categoria e spese sostenute sulla stessa linea
- PDF: aggiunto campo NOTE con descrizioni spese
- PDF: versione SW su ogni pagina
- Semplificato metodo pagamento (Carta Credito / Pag. Elettronico)
- Km: costo addebito cliente default €0.60
- Km: rimborso km sommato a spese dipendente
- Home: pulsante elimina nota (con eliminazione cartella)
- Home: nome nota con data inizio trasferta
- Versione SW visualizzata in app

---

## 📁 Struttura Progetto

```
app/src/main/java/com/notaspese/
├── MainActivity.kt              # Entry point
├── data/
│   ├── database/               # Room Database, DAO
│   ├── model/                  # Entità (NotaSpese, Spesa, Enums)
│   └── repository/             # Repository pattern
├── ui/
│   ├── components/             # Componenti riutilizzabili
│   ├── navigation/             # Routing app
│   ├── screens/                # Schermate (Home, Detail, AddSpesa, Create)
│   └── theme/                  # Tema Material 3
├── util/
│   ├── PdfGenerator.kt         # Generazione PDF
│   ├── CsvExporter.kt          # Export CSV
│   ├── TextRecognitionHelper.kt # OCR scontrini
│   └── FileStorageHelper.kt    # Gestione file
└── viewmodel/
    └── NotaSpeseViewModel.kt   # ViewModel principale
```

---

## 📲 Installazione

1. Scaricare il file APK
2. Abilitare "Installa da fonti sconosciute" nelle impostazioni Android
3. Installare l'APK
4. Concedere i permessi richiesti (Camera, Storage)

---

## 📄 Output Files

I file generati vengono salvati in:
```
Downloads/Innoval Nota Spese/[NomeCognome]_[Data]/
├── NotaSpese_[NomeCognome].pdf
├── spese.csv
└── allegati/
    ├── 001_[categoria].jpg
    └── ...
```

---

## 👨‍💻 Sviluppato da

Innoval S.r.l.

---

## 📜 Licenza

Proprietario - Tutti i diritti riservati
