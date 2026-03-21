# NotaSpese - App Gestione Note Spese

**Versione:** 1.2.0

App per la gestione delle note spese aziendali, disponibile per **Android** e **Windows 11**. Sviluppata con Kotlin, Jetpack Compose (Android) e Compose for Desktop (Windows). Interoperabilità completa tra le due piattaforme tramite export/import CSV e file .notaspese (anche da versioni precedenti).

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
- Allegati foto scontrini (con OCR su Android)
- Supporto allegati PDF

### Gestione Chilometri
- Km percorsi
- Costo €/km rimborso al trasfertista
- Costo €/km da addebitare al cliente (default: €0.60)
- Il rimborso km viene sommato alle spese del dipendente
- L'addebito cliente non incide sul costo complessivo

### Generazione PDF
- Report completo nota spese in formato A4 (layout identico su Android e Windows)
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
- Export file **.notaspese** (ZIP con JSON + allegati) per compatibilità cross-piattaforma
- Condivisione PDF via app esterne
- **Import**: CSV, cartella con CSV, o file .notaspese (anche da versioni precedenti v1.0.x)
- Android ↔ Windows: apri e modifica note create dall'altra app

---

## 🖥️ Piattaforme Supportate

| Piattaforma | Tecnologia | Output |
|-------------|------------|--------|
| **Android** | Kotlin, Jetpack Compose, Room | APK |
| **Windows 11** | Kotlin, Compose for Desktop, SQLite | .exe / .msi |

### Interoperabilità
- **Export**: Crea cartella `Downloads/Innoval Nota Spese/NomeCognome_Data/` con PDF, CSV, .notaspese e allegati
- **Import Android**: File CSV o .notaspese (anche da versioni precedenti)
- **Import Windows**: Cartella (CSV) o file .notaspese
- Stesso formato dati per modificare note su entrambe le piattaforme

---

## 🔧 Tecnologie Utilizzate

### Android
- **Linguaggio:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Database:** Room (SQLite)
- **OCR:** Google ML Kit Text Recognition
- **Camera:** CameraX

### Windows
- **Linguaggio:** Kotlin
- **UI:** Compose for Desktop + Material 3
- **Database:** SQLite (JDBC)
- **PDF:** Apache PDFBox

---

## 📋 Changelog

### v1.2.0 (Marzo 2026)
- **Import/Export .notaspese**: supporto file da versioni precedenti (v1.0.x)
- **Compatibilità bidirezionale**: Windows apre e modifica note Android e viceversa
- Export genera sempre CSV + .notaspese per massima interoperabilità

### v1.1.0 (Marzo 2026)
- **Nuovo:** App Windows 11 con stesse funzionalità di Android
- **Nuovo:** Import CSV su Android (carica note esportate da Windows)
- **Nuovo:** Import da cartella su Windows (carica note esportate da Android)
- Layout PDF identico su entrambe le piattaforme
- Interoperabilità completa tra Android e Windows

### v1.0.5 (Febbraio 2026)
- Versione pre-multipiattaforma

### v1.0.3 (Febbraio 2026)
- Fix generazione PDF: corretta logica visualizzazione colonne
- Separata logica spese dipendente da rimborso km

### v1.0.2 (Febbraio 2026)
- Corretto calcolo "Spese Sostenute Da"
- Il rimborso km appare solo nella sezione "Da Rimborsare al Dipendente"

### v1.0.1 (Febbraio 2026)
- Aggiunto dettaglio rimborso km
- Visualizzazione separata: Spese + Rimborso Km = Totale Dipendente

### v1.0.0 (Febbraio 2026)
- Release iniziale Android

---

## 📁 Struttura Progetto

```
NotaSpese/
├── app/                    # App Android
│   └── src/main/java/com/notaspese/
│       ├── data/           # Database Room, modelli
│       ├── ui/             # Schermate Compose
│       ├── util/           # PdfGenerator, CsvExporter, CsvImporter
│       └── viewmodel/
├── desktop/                # App Windows
│   └── src/jvmMain/kotlin/com/notaspese/desktop/
│       ├── data/           # Database SQLite, modelli
│       ├── ui/             # Schermate Compose
│       ├── util/           # PdfGenerator (PDFBox), CsvExporter, CsvImporter
│       └── viewmodel/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 📲 Installazione

### Android
1. Scaricare il file APK (es. `NotaSpese_1.1.0.apk`)
2. Abilitare "Installa da fonti sconosciute" nelle impostazioni
3. Installare l'APK
4. Concedere i permessi richiesti (Camera, Storage)

### Windows 11
**Opzione A – Distribuzione completa (consigliato, no Java richiesto):**
```bash
./gradlew :desktop:copyToDesktop
```
Copia su `Desktop/NotaSpese_v1.1.0/`. Avviare con:
- Doppio clic su `Avvia NotaSpese.bat`, oppure
- Doppio clic su `NotaSpese.exe`

**Opzione B – Solo JAR (richiede Java 17+):**
```bash
./gradlew :desktop:copyJarToDesktop
```
Usa `Avvia NotaSpese (JAR).bat` oppure:
```bash
java -jar NotaSpese-windows-x64-1.1.0.jar
```
⚠️ **ClassNotFoundException:** Usare il file `NotaSpese-windows-x64-1.1.0.jar` dalla cartella `desktop/build/compose/jars/`, non `desktop.jar`. Serve Java 17 o superiore.

---

## 🔨 Compilazione

**Nota:** Per compilare serve **Java 17** (Java 25 non compatibile). Impostare `JAVA_HOME` a un JDK 17 se necessario.

### Android
```bash
# Richiede ANDROID_HOME o local.properties con sdk.dir
./gradlew :app:assembleRelease
# APK in: app/build/outputs/apk/release/app-release.apk
```

### Windows
```bash
# Build completo (Android + Windows) e copia su Desktop
./gradlew buildAndCopyAll
# Output: Desktop/NotaSpese_v1.2.0/ con APK, JAR e script di avvio

# Solo distribuzione Windows con exe (chiudere l'app se già aperta)
./gradlew :desktop:createDistributable
./gradlew :desktop:copyToDesktop

# Solo JAR eseguibile
./gradlew :desktop:packageUberJarForCurrentOS
# Output: desktop/build/compose/jars/NotaSpese-windows-x64-1.2.0.jar
```

---

## 📄 Output Files

I file generati vengono salvati in:
```
Downloads/Innoval Nota Spese/[NomeCognome]_[Data]/
├── NotaSpese_[NomeCognome].pdf
├── NotaSpese_[NomeCognome].csv
└── scontrino_001_[categoria].jpg (allegati)
```

---

## 👨‍💻 Sviluppato da

Innoval S.r.l.

---

## 📜 Licenza

Proprietario - Tutti i diritti riservati
