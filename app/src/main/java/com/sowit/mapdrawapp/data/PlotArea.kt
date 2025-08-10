package com.sowit.mapdrawapp.data


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plots")
data class PlotArea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val vertices: String,           // "lat,lng;lat,lng;..."
    val centroidLat: Double,
    val centroidLng: Double
)
