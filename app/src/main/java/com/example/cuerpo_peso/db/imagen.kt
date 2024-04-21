package com.example.cuerpo_peso.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "imagen")
data class imagen(
    @PrimaryKey(autoGenerate = true) val id: Int?,
    val dia: Int?,
    val mes: Int?,
    val nombre_imagen: String,
    val uri: String,
    val orden: Int?
)
