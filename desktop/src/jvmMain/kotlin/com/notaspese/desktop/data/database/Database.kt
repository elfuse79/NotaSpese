package com.notaspese.desktop.data.database

import com.notaspese.desktop.data.model.*
import java.io.File
import java.sql.DriverManager
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class NotaSpeseDatabase {
    private val dbDir = File(System.getProperty("user.home"), ".notaspese")
    private val dbFile = File(dbDir, "nota_spese.db")
    
    private val connection get() = run {
        dbDir.mkdirs()
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").apply {
            autoCommit = false
        }
    }
    
    init {
        initSchema()
    }
    
    private fun initSchema() {
        kotlinx.coroutines.runBlocking {
            withContext(Dispatchers.IO) {
            connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS nota_spese (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            numeroNota TEXT,
                            nomeCognome TEXT NOT NULL,
                            dataInizioTrasferta INTEGER NOT NULL,
                            oraInizioTrasferta TEXT,
                            dataFineTrasferta INTEGER NOT NULL,
                            oraFineTrasferta TEXT,
                            luogoTrasferta TEXT NOT NULL,
                            cliente TEXT NOT NULL,
                            causale TEXT,
                            auto TEXT,
                            dataCompilazione INTEGER NOT NULL,
                            altriTrasfertisti TEXT,
                            anticipo REAL,
                            kmPercorsi REAL,
                            costoKmRimborso REAL,
                            costoKmCliente REAL
                        )
                    """.trimIndent())
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS spesa (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            notaSpeseId INTEGER NOT NULL,
                            descrizione TEXT,
                            importo REAL NOT NULL,
                            data INTEGER NOT NULL,
                            metodoPagamento TEXT NOT NULL,
                            categoria TEXT NOT NULL,
                            fotoScontrinoPath TEXT,
                            pagatoDa TEXT NOT NULL,
                            FOREIGN KEY (notaSpeseId) REFERENCES nota_spese(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_spesa_nota ON spesa(notaSpeseId)")
                }
                conn.commit()
            }
        }
        }
    }
    
    suspend fun insertNotaSpese(nota: NotaSpese): Long = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("""
                INSERT INTO nota_spese (numeroNota, nomeCognome, dataInizioTrasferta, oraInizioTrasferta,
                    dataFineTrasferta, oraFineTrasferta, luogoTrasferta, cliente, causale, auto,
                    dataCompilazione, altriTrasfertisti, anticipo, kmPercorsi, costoKmRimborso, costoKmCliente)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()).use { ps ->
                ps.setString(1, nota.numeroNota)
                ps.setString(2, nota.nomeCognome)
                ps.setLong(3, nota.dataInizioTrasferta)
                ps.setString(4, nota.oraInizioTrasferta)
                ps.setLong(5, nota.dataFineTrasferta)
                ps.setString(6, nota.oraFineTrasferta)
                ps.setString(7, nota.luogoTrasferta)
                ps.setString(8, nota.cliente)
                ps.setString(9, nota.causale)
                ps.setString(10, nota.auto)
                ps.setLong(11, nota.dataCompilazione)
                ps.setString(12, nota.altriTrasfertisti)
                ps.setDouble(13, nota.anticipo)
                ps.setDouble(14, nota.kmPercorsi)
                ps.setDouble(15, nota.costoKmRimborso)
                ps.setDouble(16, nota.costoKmCliente)
                ps.executeUpdate()
                val rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()")
                if (rs.next()) rs.getLong(1) else 0L
            }.also { conn.commit() }
        }
    }
    
    suspend fun updateNotaSpese(nota: NotaSpese) = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("""
                UPDATE nota_spese SET numeroNota=?, nomeCognome=?, dataInizioTrasferta=?, oraInizioTrasferta=?,
                    dataFineTrasferta=?, oraFineTrasferta=?, luogoTrasferta=?, cliente=?, causale=?, auto=?,
                    dataCompilazione=?, altriTrasfertisti=?, anticipo=?, kmPercorsi=?, costoKmRimborso=?, costoKmCliente=?
                WHERE id=?
            """.trimIndent()).use { ps ->
                ps.setString(1, nota.numeroNota)
                ps.setString(2, nota.nomeCognome)
                ps.setLong(3, nota.dataInizioTrasferta)
                ps.setString(4, nota.oraInizioTrasferta)
                ps.setLong(5, nota.dataFineTrasferta)
                ps.setString(6, nota.oraFineTrasferta)
                ps.setString(7, nota.luogoTrasferta)
                ps.setString(8, nota.cliente)
                ps.setString(9, nota.causale)
                ps.setString(10, nota.auto)
                ps.setLong(11, nota.dataCompilazione)
                ps.setString(12, nota.altriTrasfertisti)
                ps.setDouble(13, nota.anticipo)
                ps.setDouble(14, nota.kmPercorsi)
                ps.setDouble(15, nota.costoKmRimborso)
                ps.setDouble(16, nota.costoKmCliente)
                ps.setLong(17, nota.id)
                ps.executeUpdate()
            }
            conn.commit()
        }
    }
    
    suspend fun deleteNotaSpese(nota: NotaSpese) = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("DELETE FROM spesa WHERE notaSpeseId=?").use { ps ->
                ps.setLong(1, nota.id)
                ps.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM nota_spese WHERE id=?").use { ps ->
                ps.setLong(1, nota.id)
                ps.executeUpdate()
            }
            conn.commit()
        }
    }
    
    fun getAllNoteSpeseConSpese(): Flow<List<NotaSpeseConSpese>> = flow {
        val list = withContext(Dispatchers.IO) {
            connection.use { conn ->
                val result = mutableListOf<NotaSpeseConSpese>()
                conn.createStatement().executeQuery("SELECT * FROM nota_spese ORDER BY dataCompilazione DESC").use { rs ->
                    while (rs.next()) {
                        val nota = mapNotaSpese(rs)
                        val spese = getSpeseByNotaId(conn, nota.id)
                        result.add(NotaSpeseConSpese(nota, spese))
                    }
                }
                result
            }
        }
        emit(list)
    }.flowOn(Dispatchers.IO)
    
    suspend fun getNotaSpeseConSpese(id: Long): NotaSpeseConSpese? = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("SELECT * FROM nota_spese WHERE id=?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val nota = mapNotaSpese(rs)
                        val spese = getSpeseByNotaId(conn, nota.id)
                        NotaSpeseConSpese(nota, spese)
                    } else null
                    }
            }
        }
    }
    
    suspend fun getNotaSpeseById(id: Long): NotaSpese? = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("SELECT * FROM nota_spese WHERE id=?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapNotaSpese(rs) else null
                }
            }
        }
    }
    
    private fun getSpeseByNotaId(conn: java.sql.Connection, notaSpeseId: Long): List<Spesa> {
        val list = mutableListOf<Spesa>()
        conn.prepareStatement("SELECT * FROM spesa WHERE notaSpeseId=? ORDER BY data").use { ps ->
            ps.setLong(1, notaSpeseId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(Spesa(
                        id = rs.getLong("id"),
                        notaSpeseId = rs.getLong("notaSpeseId"),
                        descrizione = rs.getString("descrizione") ?: "",
                        importo = rs.getDouble("importo"),
                        data = rs.getLong("data"),
                        metodoPagamento = MetodoPagamento.valueOf(rs.getString("metodoPagamento")),
                        categoria = CategoriaSpesa.valueOf(rs.getString("categoria")),
                        fotoScontrinoPath = rs.getString("fotoScontrinoPath"),
                        pagatoDa = PagatoDa.valueOf(rs.getString("pagatoDa"))
                    ))
                }
            }
        }
        return list
    }
    
    private fun mapNotaSpese(rs: java.sql.ResultSet): NotaSpese = NotaSpese(
        id = rs.getLong("id"),
        numeroNota = rs.getString("numeroNota") ?: "",
        nomeCognome = rs.getString("nomeCognome")!!,
        dataInizioTrasferta = rs.getLong("dataInizioTrasferta"),
        oraInizioTrasferta = rs.getString("oraInizioTrasferta") ?: "",
        dataFineTrasferta = rs.getLong("dataFineTrasferta"),
        oraFineTrasferta = rs.getString("oraFineTrasferta") ?: "",
        luogoTrasferta = rs.getString("luogoTrasferta")!!,
        cliente = rs.getString("cliente")!!,
        causale = rs.getString("causale") ?: "",
        auto = rs.getString("auto") ?: "",
        dataCompilazione = rs.getLong("dataCompilazione"),
        altriTrasfertisti = rs.getString("altriTrasfertisti") ?: "",
        anticipo = rs.getDouble("anticipo"),
        kmPercorsi = rs.getDouble("kmPercorsi"),
        costoKmRimborso = rs.getDouble("costoKmRimborso"),
        costoKmCliente = rs.getDouble("costoKmCliente")
    )
    
    suspend fun insertSpesa(spesa: Spesa): Long = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("""
                INSERT INTO spesa (notaSpeseId, descrizione, importo, data, metodoPagamento, categoria, fotoScontrinoPath, pagatoDa)
                VALUES (?,?,?,?,?,?,?,?)
            """.trimIndent()).use { ps ->
                ps.setLong(1, spesa.notaSpeseId)
                ps.setString(2, spesa.descrizione)
                ps.setDouble(3, spesa.importo)
                ps.setLong(4, spesa.data)
                ps.setString(5, spesa.metodoPagamento.name)
                ps.setString(6, spesa.categoria.name)
                ps.setString(7, spesa.fotoScontrinoPath)
                ps.setString(8, spesa.pagatoDa.name)
                ps.executeUpdate()
                val rs = conn.createStatement().executeQuery("SELECT last_insert_rowid()")
                if (rs.next()) rs.getLong(1) else 0L
            }.also { conn.commit() }
        }
    }
    
    suspend fun updateSpesa(spesa: Spesa) = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("""
                UPDATE spesa SET descrizione=?, importo=?, data=?, metodoPagamento=?, categoria=?, fotoScontrinoPath=?, pagatoDa=?
                WHERE id=?
            """.trimIndent()).use { ps ->
                ps.setString(1, spesa.descrizione)
                ps.setDouble(2, spesa.importo)
                ps.setLong(3, spesa.data)
                ps.setString(4, spesa.metodoPagamento.name)
                ps.setString(5, spesa.categoria.name)
                ps.setString(6, spesa.fotoScontrinoPath)
                ps.setString(7, spesa.pagatoDa.name)
                ps.setLong(8, spesa.id)
                ps.executeUpdate()
            }
            conn.commit()
        }
    }
    
    suspend fun deleteSpesa(spesa: Spesa) = withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.prepareStatement("DELETE FROM spesa WHERE id=?").use { ps ->
                ps.setLong(1, spesa.id)
                ps.executeUpdate()
            }
            conn.commit()
        }
    }
    
    fun getDataDirectory(): File = dbDir
    fun getNotaFolder(notaSpeseId: Long): File = File(dbDir, "nota_$notaSpeseId")
}
