package com.example.myapplication.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.bitchat.location.GeohashUtils
import com.example.myapplication.bitchat.mesh.PeerNode
import com.example.myapplication.bluetooth.BluetoothViewModel
import com.example.myapplication.ui.components.LocatorBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocatorScreen() {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter
    val viewModel: BluetoothViewModel = viewModel { BluetoothViewModel(context, adapter) }

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Peer Radar, 1: Offline Map

    val meshPeers by viewModel.meshPeers.collectAsState()

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            val geohash7 = GeohashUtils.encode(loc.latitude, loc.longitude, 7)
            viewModel.updateLocation(loc.latitude, loc.longitude, geohash7)
        }
    }

    if (permissionState.allPermissionsGranted) {
        GPSProvider { currentLocation = it }

        Column(modifier = Modifier.fillMaxSize()) {
            LocatorBar(currentLocation, meshPeers.size)

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Peer Radar", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Offline Map", modifier = Modifier.padding(12.dp))
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> PeerRadarView(currentLocation, meshPeers)
                    1 -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            OfflineMapView(
                                userLocation = currentLocation?.let { LatLong(it.latitude, it.longitude) },
                                onMapViewReady = { mapViewInstance = it }
                            )

                            FloatingActionButton(
                                onClick = {
                                    currentLocation?.let { loc ->
                                        mapViewInstance?.apply {
                                            setCenter(LatLong(loc.latitude, loc.longitude))
                                            setZoomLevel(15.toByte())
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My Location")
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Location Permission Required for Locator")
                Button(
                    onClick = { permissionState.launchMultiplePermissionRequest() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun PeerRadarView(currentLocation: Location?, meshPeers: List<PeerNode>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "BitChat Off-Grid Mesh Radar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${meshPeers.size} active mesh peers in radio / hop range",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            PeerRadarCanvas(currentLocation, meshPeers)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Nearby Mesh Nodes:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())

        if (meshPeers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Scanning surrounding spectrum for BitChat nodes...", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(meshPeers) { peer ->
                    val distStr = if (currentLocation != null && peer.latitude != null && peer.longitude != null) {
                        val d = GeohashUtils.haversineDistanceMeters(
                            currentLocation.latitude, currentLocation.longitude,
                            peer.latitude, peer.longitude
                        )
                        val b = GeohashUtils.calculateBearing(
                            currentLocation.latitude, currentLocation.longitude,
                            peer.latitude, peer.longitude
                        )
                        "%.0fm (%s)".format(d, GeohashUtils.bearingToCardinal(b))
                    } else {
                        "~%.1fm (RSSI)".format(peer.estimatedDistanceMeters)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(peer.nickname, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("ID: ${peer.nodeId} • Hop: ${peer.hopCount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(distStr, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeerRadarCanvas(currentLocation: Location?, meshPeers: List<PeerNode>) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanAngle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.width / 2f) - 16.dp.toPx()

        val sweepGradient = Brush.sweepGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x0522C55E),
                Color(0x2222C55E),
                Color(0x7722C55E),
                Color(0xFF22C55E)
            ),
            center = center
        )

        rotate(degrees = scanAngle, pivot = center) {
            drawCircle(
                brush = sweepGradient,
                radius = maxRadius,
                center = center
            )
            drawLine(
                color = Color(0xFF22C55E),
                start = center,
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 2.5f
            )
        }

        val rings = listOf(0.25f, 0.5f, 0.75f, 1.0f)
        val ringLabels = listOf("10m", "50m", "100m", "500m")

        rings.forEachIndexed { idx, factor ->
            val r = maxRadius * factor
            drawCircle(
                color = Color(0x3322C55E),
                radius = r,
                center = center,
                style = Stroke(width = 1.5f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = ringLabels[idx],
                topLeft = Offset(center.x + 4f, center.y - r - 12f),
                style = TextStyle(color = Color(0xFF86EFAC), fontSize = 9.sp)
            )
        }

        drawLine(
            color = Color(0x4422C55E),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0x4422C55E),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.5f
        )

        drawCircle(color = Color(0xFF38BDF8), radius = 6.dp.toPx(), center = center)

        meshPeers.forEachIndexed { index, peer ->
            val distanceMeters = if (currentLocation != null && peer.latitude != null && peer.longitude != null) {
                GeohashUtils.haversineDistanceMeters(
                    currentLocation.latitude, currentLocation.longitude,
                    peer.latitude, peer.longitude
                )
            } else {
                peer.estimatedDistanceMeters
            }

            val bearing = if (currentLocation != null && peer.latitude != null && peer.longitude != null) {
                GeohashUtils.calculateBearing(
                    currentLocation.latitude, currentLocation.longitude,
                    peer.latitude, peer.longitude
                )
            } else {
                (index * 137.5f) % 360f
            }

            val maxRadarRange = 200.0
            val clampedDist = distanceMeters.coerceIn(5.0, maxRadarRange)
            val normR = (clampedDist / maxRadarRange) * maxRadius

            val angleRad = Math.toRadians((bearing - 90).toDouble())
            val px = center.x + (normR * cos(angleRad)).toFloat()
            val py = center.y + (normR * sin(angleRad)).toFloat()
            val peerOffset = Offset(px, py)

            drawCircle(color = Color(0x5522C55E), radius = 10.dp.toPx(), center = peerOffset)
            drawCircle(color = Color(0xFF22C55E), radius = 5.dp.toPx(), center = peerOffset)

            drawText(
                textMeasurer = textMeasurer,
                text = peer.nickname,
                topLeft = Offset(px + 8f, py - 10f),
                style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GPSProvider(onLocationChanged: (Location) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager

        val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        (lastGps ?: lastNetwork)?.let { onLocationChanged(it) }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocationChanged(location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, listener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, listener)
            }
        } catch (_: Exception) {
        }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }
}

@Composable
fun OfflineMapView(
    userLocation: LatLong?,
    onMapViewReady: (MapView) -> Unit
) {
    val context = LocalContext.current
    val mapsDir = remember { getMapsDirectory(context) }

    var mapListRefreshKey by remember { mutableIntStateOf(0) }
    val mapFiles = remember(mapListRefreshKey) {
        mapsDir.listFiles { file -> file.isFile && file.name.endsWith(".map", ignoreCase = true) }?.toList()?.sortedBy { it.name } ?: emptyList()
    }

    var selectedMapFile by remember { mutableStateOf<File?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val imported = importMapFileFromUri(context, it, mapsDir)
            if (imported != null) {
                mapListRefreshKey++
                selectedMapFile = imported
            }
        }
    }

    if (selectedMapFile == null) {
        // --- OFFLINE MAP MANAGER PAGE ---
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Downloaded Country Maps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${mapFiles.size} country map(s) saved offline in storage", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://download.mapsforge.org/maps/v5/"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download More", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Map File", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (mapFiles.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No Maps Downloaded Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("1. Tap 'Download More' to open the Mapsforge server repository.", style = MaterialTheme.typography.bodySmall)
                                Text("2. Select your continent & country map (e.g. India.map).", style = MaterialTheme.typography.bodySmall)
                                Text("3. Return here and tap 'Import Map File' to select your downloaded map!", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                Text("Select a map to view:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 6.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(mapFiles) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedMapFile = file },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = "Map",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(file.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("Size: ${formatFileSize(file.length())}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        file.delete()
                                        mapListRefreshKey++
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Map",
                                        tint = Color.Red.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- FULL INTERACTIVE MAP VIEW PAGE ---
        val currentMap = selectedMapFile!!
        var userMarker by remember { mutableStateOf<Marker?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            key(currentMap.absolutePath) {
                AndroidView(
                    factory = { ctx ->
                        AndroidGraphicFactory.createInstance(ctx.applicationContext)
                        MapView(ctx).apply {
                            isClickable = true
                            setBuiltInZoomControls(true)

                            val tileCache = AndroidUtil.createTileCache(
                                ctx, "mapcache", model.displayModel.tileSize, 1f,
                                model.frameBufferModel.overdrawFactor
                            )
                            val mapStore = org.mapsforge.map.reader.MapFile(currentMap)
                            val rendererLayer = TileRendererLayer(
                                tileCache, mapStore, model.mapViewPosition, AndroidGraphicFactory.INSTANCE
                            )
                            rendererLayer.setXmlRenderTheme(MapsforgeThemes.OSMARENDER)
                            layerManager.layers.add(rendererLayer)

                            val mapCenter = mapStore.mapFileInfo.boundingBox?.centerPoint ?: mapStore.mapFileInfo.startPosition ?: LatLong(20.0, 0.0)
                            val initialCenter = userLocation ?: mapCenter
                            setCenter(initialCenter)

                            val startZoom = mapStore.mapFileInfo.startZoomLevel ?: 10.toByte()
                            setZoomLevel(if (startZoom in 5..16) startZoom else 10.toByte())

                            onMapViewReady(this)
                        }
                    },
                    update = { view ->
                        userLocation?.let { latLong ->
                            if (userMarker == null) {
                                val drawable = androidx.core.content.ContextCompat.getDrawable(context, android.R.drawable.presence_online)
                                val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
                                val marker = Marker(latLong, bitmap, 0, 0)
                                userMarker = marker
                                view.layerManager.layers.add(marker)
                            } else {
                                userMarker?.latLong = latLong
                                userMarker?.requestRedraw()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Action Bar Overlay over Map
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = { selectedMapFile = null }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Maps List")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(currentMap.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Offline Map View", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = { selectedMapFile = null },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Exit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit Map", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun getMapsDirectory(context: Context): File {
    val mapsDir = File(context.filesDir, "maps")
    if (!mapsDir.exists()) {
        mapsDir.mkdirs()
    }
    val legacyActive = File(context.filesDir, "active_map.map")
    val legacyWorld = File(context.filesDir, "world.map")
    if (legacyActive.exists() && legacyActive.length() > 0) {
        val savedName = getSavedMapName(context)
        val targetName = if (savedName.endsWith(".map", ignoreCase = true)) savedName else "Country.map"
        val dest = File(mapsDir, targetName)
        if (!dest.exists()) {
            legacyActive.copyTo(dest, overwrite = true)
        }
        legacyActive.delete()
    }
    if (legacyWorld.exists() && legacyWorld.length() > 0) {
        val dest = File(mapsDir, "World.map")
        if (!dest.exists()) {
            legacyWorld.copyTo(dest, overwrite = true)
        }
        legacyWorld.delete()
    }
    return mapsDir
}

fun importMapFileFromUri(context: Context, uri: Uri, mapsDir: File): File? {
    return try {
        var fileName = "Country.map"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val queried = cursor.getString(nameIndex)
                if (queried.isNotBlank()) {
                    fileName = queried
                }
            }
        }
        if (!fileName.endsWith(".map", ignoreCase = true)) {
            fileName = "$fileName.map"
        }

        val destFile = File(mapsDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile
    } catch (e: Exception) {
        null
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1024) {
        "%.2f GB".format(mb / 1024)
    } else {
        "%.1f MB".format(mb)
    }
}

fun saveMapName(context: Context, name: String) {
    val prefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("active_map_name", name).apply()
}

fun getSavedMapName(context: Context): String {
    val prefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
    return prefs.getString("active_map_name", "Country.map") ?: "Country.map"
}
