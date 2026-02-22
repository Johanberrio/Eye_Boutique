package com.example.lentespro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messengers")
data class MessengerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String? = null
)
