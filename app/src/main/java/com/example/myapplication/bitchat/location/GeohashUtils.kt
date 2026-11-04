package com.example.myapplication.bitchat.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeohashUtils {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    private val BITS = intArrayOf(16, 8, 4, 2, 1)

    fun encode(latitude: Double, longitude: Double, precision: Int = 7): String {
        var isEven = true
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        var bit = 0
        var ch = 0
        val geohash = StringBuilder()

        while (geohash.length < precision) {
            if (isEven) {
                val lonMid = (lonMin + lonMax) / 2
                if (longitude >= lonMid) {
                    ch = ch or BITS[bit]
                    lonMin = lonMid
                } else {
                    lonMax = lonMid
                }
            } else {
                val latMid = (latMin + latMax) / 2
                if (latitude >= latMid) {
                    ch = ch or BITS[bit]
                    latMin = latMid
                } else {
                    latMax = latMid
                }
            }

            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return geohash.toString()
    }

    fun decode(geohash: String): Pair<Double, Double> {
        var isEven = true
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        for (i in geohash.indices) {
            val c = geohash[i]
            val cd = BASE32.indexOf(c)
            if (cd == -1) continue

            for (j in 0..4) {
                val mask = BITS[j]
                if (isEven) {
                    val lonMid = (lonMin + lonMax) / 2
                    if ((cd and mask) != 0) {
                        lonMin = lonMid
                    } else {
                        lonMax = lonMid
                    }
                } else {
                    val latMid = (latMin + latMax) / 2
                    if ((cd and mask) != 0) {
                        latMin = latMid
                    } else {
                        latMax = latMid
                    }
                }
                isEven = !isEven
            }
        }
        val lat = (latMin + latMax) / 2
        val lon = (lonMin + lonMax) / 2
        return Pair(lat, lon)
    }

    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x)).toFloat()
        return (brng + 360) % 360
    }

    fun bearingToCardinal(bearing: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        val index = Math.round(bearing / 45.0).toInt() % 8
        return directions[index]
    }

    fun getPrecisionLabel(precision: Int): String {
        return when (precision) {
            7 -> "Block (~150m)"
            6 -> "Neighborhood (~1km)"
            5 -> "City (~5km)"
            4 -> "Region (~35km)"
            else -> "Global"
        }
    }
}
