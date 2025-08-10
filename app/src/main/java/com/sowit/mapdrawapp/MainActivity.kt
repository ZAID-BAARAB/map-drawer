package com.sowit.mapdrawapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.sowit.mapdrawapp.data.GeoConverters
import com.sowit.mapdrawapp.data.PlotArea
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm by viewModels<MapViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { com.sowit.mapdrawapp.ui.theme.MapDrawAppTheme { AppScaffold(vm) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(vm: MapViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showNameDialog by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("test") }

    val saved by vm.savedPlots.collectAsState()
    val current by vm.current.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Saved areas",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                if (saved.isEmpty()) {
                    Text("No saved plots yet", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn {
                        items(saved) { plot ->
                            ListItem(
                                headlineContent = { Text(plot.name) },
                                supportingContent = {
                                    val ha = plot.areaSqM / 10_000.0
                                    Text("#${plot.id} • ${plot.centroidLat.format(4)}, ${plot.centroidLng.format(4)} • ${ha.format(2)} ha")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { SelectedPlotBus.select(plot) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = { Text("Map Draw - SOWIT") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = { showNameDialog = true },
                        text = { Text("Finish & Save") },
                        icon = {},
                        expanded = true,
                        containerColor = if (current.size >= 3)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (current.size >= 3)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { vm.clearDrawing() },
                        text = { Text("Clear") },
                        icon = {},
                        expanded = true,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                GoogleMapComposable(
                    onMapTap = { lat, lng -> vm.addPoint(lat, lng) },
                    current = current
                )
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Save area") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.savePolygon(nameText) { }
                        showNameDialog = false
                    },
                    enabled = current.size >= 3
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/** Google Maps Compose: draw current poly + react to saved selection (re-draw & color). */
@Composable
fun GoogleMapComposable(
    onMapTap: (lat: Double, lng: Double) -> Unit,
    current: List<Pair<Double, Double>>
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(33.5731, -7.5898), 14f)
    }

    // Keep the *selected/saved* polygon points to render them colored.
    var selectedPolygon by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    LaunchedEffect(Unit) {
        SelectedPlotBus.events.collect { plot ->
            val center = LatLng(plot.centroidLat, plot.centroidLng)
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(center, 15f),
                durationMs = 800
            )

            selectedPolygon = GeoConverters.fromStorage(plot.vertices)
                .map { (lat, lng) -> LatLng(lat, lng) }
        }
    }

    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) }
    val properties = remember { MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = false) }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        onMapClick = { latLng -> onMapTap(latLng.latitude, latLng.longitude) }
    ) {

        if (current.isNotEmpty()) {
            val pts = current.map { LatLng(it.first, it.second) }
            if (pts.size >= 2) {
                Polyline(points = pts, width = 6f)
            }
            if (pts.size >= 3) {
                Polygon(points = pts, fillColor = Color(0x33FF9800), strokeWidth = 0f)
            }
        }

        if (selectedPolygon.size >= 3) {
            Polygon(
                points = selectedPolygon,
                fillColor = Color(0x5534C759), // 0x55 alpha for a solid highlight
                strokeColor = Color(0xFF34C759),
                strokeWidth = 4f
            )
        }
    }
}

/** Event bus: saved list -> map */
object SelectedPlotBus {
    private val _events = MutableSharedFlow<PlotArea>(extraBufferCapacity = 1)
    val events: SharedFlow<PlotArea> = _events.asSharedFlow()
    fun select(plot: PlotArea) { _events.tryEmit(plot) }
}

private fun Double.format(n: Int) = "%.${n}f".format(this)

