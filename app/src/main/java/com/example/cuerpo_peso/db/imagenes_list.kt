package com.example.cuerpo_peso.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imagenes_list")
data class imagenes_list(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    val dia: Int?,
    val mes: Int?,
    val imagen1: String,
    val imagen2: String,
    val imagen3: String,
    val peso: Double
)