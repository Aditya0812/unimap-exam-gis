package com.example.ui.map

import androidx.compose.ui.graphics.Color
import com.example.data.model.CollegeEntity

/**
 * Represents a geographical district boundary imported from a user-provided KMZ or KML file.
 * Each district has its exact coordinates, color scheme, and is rendered with 30% transparent shading.
 */
data class DistrictBoundary(
    val name: String,
    val polygon: List<Pair<Double, Double>>, // (Latitude, Longitude) outer boundary coordinates
    val innerHoles: List<List<Pair<Double, Double>>> = emptyList(),
    val polylines: List<List<Pair<Double, Double>>> = emptyList(), // Linear boundary polylines if LineString
    val centerLat: Double,
    val centerLon: Double,
    val minLat: Double = 0.0,
    val maxLat: Double = 0.0,
    val minLon: Double = 0.0,
    val maxLon: Double = 0.0,
    val color: Color,
    val strokeColor: Color = color,
    val strokeWidth: Float = 2.5f,
    val description: String = "",
    val collegeCount: Int = 0
)

object DistrictBoundaryManager {

    // Distinct vibrant color palette for different districts
    val DistrictColorPalette = listOf(
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF059669), // Emerald Green
        Color(0xFF7C3AED), // Vivid Violet / Purple
        Color(0xFFD97706), // Warm Amber / Gold
        Color(0xFFE11D48), // Rose Red / Coral
        Color(0xFF0D9488), // Deep Teal
        Color(0xFF4F46E5), // Indigo
        Color(0xFFEA580C), // Tangelo Orange
        Color(0xFF9333EA), // Magenta Purple
        Color(0xFF0284C7)  // Ocean Cyan
    )

    /**
     * User requested removal of synthetic/generated district boundaries:
     * "Remove district map and boundary given by you. Give provision to insert kmz file by me to plot district boundary and layer."
     * This function now returns emptyList() so no synthetic boundaries are generated.
     */
    fun computeBoundaries(colleges: List<CollegeEntity>): List<DistrictBoundary> {
        return emptyList()
    }

    /**
     * Cross-references colleges with loaded KMZ district boundaries to update college counts.
     */
    fun updateCollegeCounts(
        boundaries: List<DistrictBoundary>,
        colleges: List<CollegeEntity>
    ): List<DistrictBoundary> {
        if (boundaries.isEmpty() || colleges.isEmpty()) return boundaries

        return boundaries.map { boundary ->
            val matchingColleges = colleges.filter { col ->
                col.district.trim().equals(boundary.name.trim(), ignoreCase = true) ||
                boundary.name.contains(col.district.trim(), ignoreCase = true) ||
                (col.district.isNotBlank() && col.district.trim().contains(boundary.name.trim(), ignoreCase = true)) ||
                isPointInPolygon(col.latitude, col.longitude, boundary.polygon)
            }
            boundary.copy(collegeCount = matchingColleges.size)
        }
    }

    /**
     * Ray-casting algorithm to test if a point (lat, lon) is inside a polygon.
     */
    fun isPointInPolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val (xi, yi) = polygon[i]
            val (xj, yj) = polygon[j]
            val intersect = ((yi > lon) != (yj > lon)) &&
                    (lat < (xj - xi) * (lon - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
