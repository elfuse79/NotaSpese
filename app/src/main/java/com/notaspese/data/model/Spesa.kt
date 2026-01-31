package com.notaspese.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "spesa",
    foreignKeys = [ForeignKey(entity = NotaSpese::class, parentColumns = ["id"], childColumns = ["notaSpeseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("notaSpeseId")]
)
data class Spesa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notaSpeseId: Long,
    val descrizione: String,
    val importo: Double,
    val data: Long,
    val metodoPagamento: MetodoPagamento,
    val categoria: CategoriaSpesa,
    val fotoScontrinoPath: String? = null,
    val pagatoDa: PagatoDa = PagatoDa.AZIENDA  // Default: pagato dall'azienda
)
