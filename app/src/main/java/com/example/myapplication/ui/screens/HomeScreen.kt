package com.example.myapplication.ui.screens

import android.content.Context
import android.content.Intent
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.example.myapplication.bitchat.location.GeohashUtils
import com.example.myapplication.ui.navigation.Screen
import java.util.TimeZone

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current

    val services = listOf(
        Screen.Manual,
        Screen.AI,
        Screen.Locator,
        Screen.Bluetooth
    )

    var currentLocation by remember { mutableStateOf<Location?>(null) }

    val timeZoneName = remember { TimeZone.getDefault().id }
    val timeZoneOffset = remember {
        val offsetMs = TimeZone.getDefault().rawOffset
        val hours = offsetMs / (1000 * 60 * 60)
        val minutes = Math.abs((offsetMs / (1000 * 60)) % 60)
        "GMT%+d:%02d".format(hours, minutes)
    }

    GPSProvider { currentLocation = it }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_emergence_ai_vector),
                contentDescription = "Emergence AI Logo",
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SurvivE",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Emergence AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Location & Timezone Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Field Coordinates & Timezone",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = if (currentLocation != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (currentLocation != null) "GPS Fixed" else "Acquiring GPS",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Coordinates", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = if (currentLocation != null)
                                "%.4f, %.4f".format(currentLocation?.latitude, currentLocation?.longitude)
                            else "Locating...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentLocation != null) {
                            val geohash = GeohashUtils.encode(currentLocation!!.latitude, currentLocation!!.longitude, 7)
                            Text("Geohash: #$geohash", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Time Zone", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = timeZoneName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(timeZoneOffset, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(services) { service ->
                ServiceCard(service) {
                    navController.navigate(service.route)
                }
            }
        }

        // Launch Native System FM Radio App Button
        Button(
            onClick = {
                val launched = launchSystemFmApp(context)
                if (!launched) {
                    Toast.makeText(context, "No native FM Radio app found on this device.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch Emergency Hardware FM", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun launchSystemFmApp(context: Context): Boolean {
    val fmPackages = listOf(
        "com.caf.fmradio",                   // Qualcomm Snapdragon
        "com.sec.android.app.fm",            // Samsung
        "com.miui.fm",                       // Xiaomi / Redmi
        "com.motorola.fmplayer",             // Motorola
        "com.mediatek.fmradio",              // MediaTek
        "com.android.fmradio",               // AOSP Stock FM
        "com.realme.fmradio",                // Realme
        "com.oppo.fmradio",                  // Oppo
        "com.transsion.fmradio"              // Tecno / Infinix
    )

    for (pkg in fmPackages) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    try {
        val fmIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("android.intent.category.APP_FM")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (fmIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(fmIntent)
            return true
        }
    } catch (_: Exception) {}

    return false
}

@Composable
fun ServiceCard(service: Screen, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (service == Screen.AI) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_emergence_ai_vector),
                    contentDescription = "Emergence AI",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = service.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = service.title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
