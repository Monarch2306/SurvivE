package com.example.myapplication.ui.components

import android.location.Location
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.bitchat.location.GeohashUtils

@Composable
fun LocatorBar(currentLocation: Location?, peerCount: Int = 0) {
    val geohash = currentLocation?.let { GeohashUtils.encode(it.latitude, it.longitude, 7) } ?: "Scanning..."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "BitChat Locator",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (currentLocation != null)
                        "Lat: %.4f, Lon: %.4f".format(currentLocation.latitude, currentLocation.longitude)
                    else "Waiting for GPS...",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "Geohash: #$geohash ($peerCount peers nearby)",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
            ) {
                WorldPoliticalMap(
                    lat = currentLocation?.latitude ?: 20.0,
                    lon = currentLocation?.longitude ?: 0.0,
                    zoom = if (currentLocation != null) 5 else 1
                )
            }
        }
    }
}

@Composable
fun WorldPoliticalMap(lat: Double, lon: Double, zoom: Int, modifier: Modifier = Modifier.fillMaxSize()) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.invalidate()
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(0x00000000)

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                        <style>
                            #map { height: 100vh; width: 100vw; margin: 0; padding: 0; }
                            body { margin: 0; }
                        </style>
                    </head>
                    <body>
                        <div id="map"></div>
                        <script>
                            var map = L.map('map', {
                                attributionControl: false,
                                zoomControl: false
                            }).setView([$lat, $lon], $zoom);
                            
                            L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
                                maxZoom: 19
                            }).addTo(map);

                            var marker = L.marker([$lat, $lon]).addTo(map);
                            
                            window.updateLocation = function(newLat, newLon, newZoom) {
                                map.setView([newLat, newLon], newZoom);
                                marker.setLatLng([newLat, newLon]);
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                loadDataWithBaseURL("https://leafletjs.com", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.evaluateJavascript("""
                if (typeof updateLocation === 'function') {
                    updateLocation($lat, $lon, $zoom);
                } else {
                    setTimeout(function() {
                        if (typeof updateLocation === 'function') {
                            updateLocation($lat, $lon, $zoom);
                        }
                    }, 100);
                }
            """.trimIndent(), null)
        },
        modifier = modifier
    )
}
