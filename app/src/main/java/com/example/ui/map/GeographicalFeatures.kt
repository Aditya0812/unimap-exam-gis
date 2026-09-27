package com.example.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.model.CollegeEntity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geographical features inspired by Google Maps cartography:
 * - Rivers and waterways
 * - National Highways (NH-44, NH-48) & State Highways
 * - Key civic and transport landmarks
 */
enum class LandmarkType(val title: String, val iconGlyph: String, val colorHex: Long) {
    RAILWAY_JUNCTION("Central Railway Junction", "🚉", 0xFF0284C7),
    BUS_TERMINAL("Inter-District Bus Terminal (ISBT)", "🚌", 0xFF0D9488),
    SECRETARIAT("University Administrative Secretariat", "🏛️", 0xFF4338CA),
    AIRPORT("Regional Airport Runway", "✈️", 0xFF334155),
    CIVIL_HOSPITAL("District Civil Hospital", "🏥", 0xFFDC2626),
    BOTANICAL_PARK("Botanical Reserve & Eco Park", "🌲", 0xFF15803D),
    HERITAGE_TOWER("Historic Civic Clock Tower", "📍", 0xFFB45309)
}

data class GeoLandmark(
    val name: String,
    val type: LandmarkType,
    val latitude: Double,
    val longitude: Double,
    val description: String
)

data class RiverPath(
    val name: String,
    val points: List<Pair<Double, Double>>, // Lat, Lon
    val isMainStream: Boolean
)

data class HighwayPath(
    val name: String,
    val designation: String, // e.g., "NH-44", "NH-48", "SH-15"
    val points: List<Pair<Double, Double>>,
    val isExpressway: Boolean
)

object GeographicalFeaturesProvider {

    /**
     * Generates a coherent geographical context (river system, highway corridors,
     * railway tracks, and landmarks) centered around the user's uploaded dataset.
     */
    fun getRivers(centerLat: Double, centerLon: Double, spanLat: Double, spanLon: Double): List<RiverPath> {
        val riverPoints = mutableListOf<Pair<Double, Double>>()
        // Meandering river from North-West to South-East
        val steps = 30
        val startLat = centerLat + spanLat * 0.55
        val startLon = centerLon - spanLon * 0.65
        val endLat = centerLat - spanLat * 0.55
        val endLon = centerLon + spanLon * 0.70

        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val baseLat = startLat + (endLat - startLat) * t
            val baseLon = startLon + (endLon - startLon) * t
            // Natural meander bends
            val meander = sin(t * Math.PI * 3.5) * (spanLon * 0.08)
            riverPoints.add(Pair(baseLat - meander * 0.4, baseLon + meander))
        }

        // Tributary joining the main river
        val tributaryPoints = mutableListOf<Pair<Double, Double>>()
        val tribStartLat = centerLat + spanLat * 0.45
        val tribStartLon = centerLon + spanLon * 0.55
        val tribEndLat = centerLat
        val tribEndLon = centerLon + spanLon * 0.15
        for (i in 0..15) {
            val t = i.toDouble() / 15
            val lat = tribStartLat + (tribEndLat - tribStartLat) * t
            val lon = tribStartLon + (tribEndLon - tribStartLon) * t + sin(t * Math.PI * 2) * (spanLon * 0.03)
            tributaryPoints.add(Pair(lat, lon))
        }

        return listOf(
            RiverPath("Yamuna River Basin", riverPoints, isMainStream = true),
            RiverPath("Central Canal Aqueduct", tributaryPoints, isMainStream = false)
        )
    }

    fun getHighways(centerLat: Double, centerLon: Double, spanLat: Double, spanLon: Double): List<HighwayPath> {
        // NH-44: North to South arterial corridor
        val nh44 = mutableListOf<Pair<Double, Double>>()
        val nhSteps = 20
        for (i in 0..nhSteps) {
            val t = i.toDouble() / nhSteps
            val lat = (centerLat + spanLat * 0.6) - (spanLat * 1.2) * t
            val lon = (centerLon - spanLon * 0.12) + (spanLon * 0.18) * sin(t * Math.PI)
            nh44.add(Pair(lat, lon))
        }

        // NH-48: West to East Express highway
        val nh48 = mutableListOf<Pair<Double, Double>>()
        for (i in 0..nhSteps) {
            val t = i.toDouble() / nhSteps
            val lon = (centerLon - spanLon * 0.6) + (spanLon * 1.2) * t
            val lat = (centerLat - spanLat * 0.08) + (spanLat * 0.15) * sin(t * Math.PI * 1.5)
            nh48.add(Pair(lat, lon))
        }

        // State Highway SH-15: Diagonal connecting artery
        val sh15 = mutableListOf<Pair<Double, Double>>()
        for (i in 0..15) {
            val t = i.toDouble() / 15
            val lat = (centerLat + spanLat * 0.4) - (spanLat * 0.8) * t
            val lon = (centerLon + spanLon * 0.4) - (spanLon * 0.8) * t
            sh15.add(Pair(lat, lon))
        }

        // University Outer Ring Road Loop
        val ringRoad = mutableListOf<Pair<Double, Double>>()
        val ringSteps = 32
        val rLat = spanLat * 0.28
        val rLon = spanLon * 0.35
        for (i in 0..ringSteps) {
            val angle = (2 * Math.PI * i) / ringSteps
            val lat = centerLat + rLat * cos(angle)
            val lon = centerLon + rLon * sin(angle)
            ringRoad.add(Pair(lat, lon))
        }

        return listOf(
            HighwayPath("National Highway 44 (North-South)", "NH-44", nh44, isExpressway = true),
            HighwayPath("National Highway 48 (East-West)", "NH-48", nh48, isExpressway = true),
            HighwayPath("State Highway 15 (University Corridor)", "SH-15", sh15, isExpressway = false),
            HighwayPath("University Campus Outer Ring Road", "RING-1", ringRoad, isExpressway = false)
        )
    }

    fun getLandmarks(centerLat: Double, centerLon: Double, spanLat: Double, spanLon: Double): List<GeoLandmark> {
        return listOf(
            GeoLandmark(
                name = "Central Railway Junction & Terminal",
                type = LandmarkType.RAILWAY_JUNCTION,
                latitude = centerLat + spanLat * 0.18,
                longitude = centerLon - spanLon * 0.15,
                description = "Primary rail connectivity hub & transit inter-change"
            ),
            GeoLandmark(
                name = "Inter-District Bus Terminal (ISBT)",
                type = LandmarkType.BUS_TERMINAL,
                latitude = centerLat - spanLat * 0.16,
                longitude = centerLon - spanLon * 0.18,
                description = "State transport hub with express routes to all districts"
            ),
            GeoLandmark(
                name = "Administrative Secretariat & Raj Bhavan",
                type = LandmarkType.SECRETARIAT,
                latitude = centerLat + spanLat * 0.22,
                longitude = centerLon + spanLon * 0.12,
                description = "Government state offices & university oversight secretariat"
            ),
            GeoLandmark(
                name = "Regional Airport & Air Cargo Hub",
                type = LandmarkType.AIRPORT,
                latitude = centerLat - spanLat * 0.38,
                longitude = centerLon + spanLon * 0.32,
                description = "Regional commercial airport with 3200m runway"
            ),
            GeoLandmark(
                name = "District Civil Hospital & Medical College",
                type = LandmarkType.CIVIL_HOSPITAL,
                latitude = centerLat + spanLat * 0.05,
                longitude = centerLon - spanLon * 0.22,
                description = "Apex tertiary medical center and university emergency hospital"
            ),
            GeoLandmark(
                name = "Green Ridge Botanical Reserve & Eco Park",
                type = LandmarkType.BOTANICAL_PARK,
                latitude = centerLat - spanLat * 0.26,
                longitude = centerLon - spanLon * 0.32,
                description = "Protected regional forest park & ecological buffer"
            ),
            GeoLandmark(
                name = "Historic Clock Tower & Civic Plaza",
                type = LandmarkType.HERITAGE_TOWER,
                latitude = centerLat + spanLat * 0.02,
                longitude = centerLon + spanLon * 0.03,
                description = "City heritage monument and central academic assembly square"
            )
        )
    }
}
