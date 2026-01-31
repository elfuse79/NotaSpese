package com.notaspese.data.database

import androidx.room.TypeConverter
import com.notaspese.data.model.CategoriaSpesa
import com.notaspese.data.model.MetodoPagamento

class Converters {
    @TypeConverter fun fromMetodoPagamento(value: MetodoPagamento): String = value.name
    @TypeConverter fun toMetodoPagamento(value: String): MetodoPagamento = MetodoPagamento.valueOf(value)
    @TypeConverter fun fromCategoriaSpesa(value: CategoriaSpesa): String = value.name
    @TypeConverter fun toCategoriaSpesa(value: String): CategoriaSpesa = CategoriaSpesa.valueOf(value)
}
