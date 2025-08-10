package com.sowit.mapdrawapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.sowit.mapdrawapp.data.AppDatabase
import com.sowit.mapdrawapp.data.GeoConverters
import com.sowit.mapdrawapp.data.PlotArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).plotDao()

    private val _current = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val current: StateFlow<List<Pair<Double, Double>>> = _current

    val savedPlots: StateFlow<List<PlotArea>> =
        dao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPoint(lat: Double, lng: Double) {
        _current.value = _current.value + (lat to lng)
    }

    fun clearDrawing() {
        _current.value = emptyList()
    }

    fun savePolygon(name: String, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val pts = _current.value
            if (pts.size < 3) return@launch

            val (cLat, cLng) = com.sowit.mapdrawapp.data.GeoConverters.centroidOf(pts)

            // area in m² using Maps Utils
            val latLngs = pts.map { LatLng(it.first, it.second) }
            val areaSqM = SphericalUtil.computeArea(latLngs)

            val id = dao.insert(
                PlotArea(
                    name = name.ifBlank { "Unnamed" },
                    vertices = GeoConverters.toStorage(pts),
                    centroidLat = cLat,
                    centroidLng = cLng,
                    areaSqM = areaSqM
                )
            )
            onDone(id)
            _current.value = emptyList()
        }
    }
}
