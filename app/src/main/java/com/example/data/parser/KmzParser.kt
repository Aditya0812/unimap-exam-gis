package com.example.data.parser

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.ui.map.DistrictBoundary
import com.example.ui.map.DistrictBoundaryManager
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.math.abs

/**
 * Result data class returned when a user-provided KMZ or KML file is parsed.
 */
data class KmzParseResult(
    val fileName: String,
    val boundaries: List<DistrictBoundary>,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val totalPlacemarks: Int,
    val totalVertices: Int
)

data class KmlStyle(
    val id: String,
    val fillColor: Color? = null,
    val strokeColor: Color? = null,
    val strokeWidth: Float? = null
)

/**
 * Comprehensive parser for KMZ (compressed KML archives) and KML XML files.
 * Extracts district boundaries, polygons, multi-polygons, boundary polylines,
 * styles, and layer names.
 */
object KmzParser {

    private const val TAG = "KmzParser"

    fun parseKmzOrKml(context: Context, uri: Uri): Result<KmzParseResult> {
        return try {
            val fileName = queryFileName(context, uri) ?: "district_layer.kmz"
            val contentResolver = context.contentResolver

            contentResolver.openInputStream(uri)?.use { rawIn ->
                val bufferedIn = BufferedInputStream(rawIn)
                bufferedIn.mark(4)
                val magic = ByteArray(4)
                val readBytes = bufferedIn.read(magic)
                bufferedIn.reset()

                val isZip = readBytes == 4 && magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte()

                if (isZip) {
                    parseKmzStream(bufferedIn, fileName)
                } else {
                    // Plain KML file
                    val kmlBytes = bufferedIn.readBytes()
                    val result = parseKmlBytes(kmlBytes, fileName)
                    Result.success(result)
                }
            } ?: Result.failure(Exception("Unable to open input stream for selected KMZ file."))
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing KMZ/KML file", e)
            Result.failure(e)
        }
    }

    private fun parseKmzStream(inputStream: InputStream, fileName: String): Result<KmzParseResult> {
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry
        val boundaries = mutableListOf<DistrictBoundary>()
        var foundKml = false
        var totalPlacemarks = 0
        var totalVertices = 0

        while (entry != null) {
            val entryName = entry.name.lowercase()
            if (!entry.isDirectory && entryName.endsWith(".kml")) {
                foundKml = true
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var len: Int
                while (zipIn.read(buffer).also { len = it } > 0) {
                    out.write(buffer, 0, len)
                }
                val kmlBytes = out.toByteArray()
                val parsed = parseKmlBytes(kmlBytes, entry.name)
                boundaries.addAll(parsed.boundaries)
                totalPlacemarks += parsed.totalPlacemarks
                totalVertices += parsed.totalVertices
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        if (!foundKml) {
            return Result.failure(Exception("No valid .kml document found inside the selected KMZ archive."))
        }

        if (boundaries.isEmpty()) {
            return Result.failure(Exception("KMZ file was parsed, but no boundary polygons or placemarks were found."))
        }

        val allPoints = boundaries.flatMap { it.polygon }
        val minLat = allPoints.minOfOrNull { it.first } ?: 0.0
        val maxLat = allPoints.maxOfOrNull { it.first } ?: 0.0
        val minLon = allPoints.minOfOrNull { it.second } ?: 0.0
        val maxLon = allPoints.maxOfOrNull { it.second } ?: 0.0

        return Result.success(
            KmzParseResult(
                fileName = fileName,
                boundaries = boundaries,
                minLat = minLat,
                maxLat = maxLat,
                minLon = minLon,
                maxLon = maxLon,
                totalPlacemarks = totalPlacemarks,
                totalVertices = totalVertices
            )
        )
    }

    fun parseKmlBytes(bytes: ByteArray, sourceName: String): KmzParseResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        val styles = mutableMapOf<String, KmlStyle>()
        val boundaries = mutableListOf<DistrictBoundary>()

        var eventType = parser.eventType
        var currentTag = ""

        // Style parsing temp variables
        var currentStyleId: String? = null
        var inLineStyle = false
        var inPolyStyle = false
        var styleLineColor: Color? = null
        var styleLineWidth: Float? = null
        var stylePolyColor: Color? = null

        // Placemark parsing temp variables
        var inPlacemark = false
        var placemarkName = ""
        var placemarkDesc = ""
        var placemarkStyleUrl = ""
        val placemarkPolygons = mutableListOf<List<Pair<Double, Double>>>()
        val placemarkPolylines = mutableListOf<List<Pair<Double, Double>>>()

        // Geometry tracking
        var inCoordinates = false
        var inOuterBoundary = false
        var inLineString = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.lowercase()
                    when (currentTag) {
                        "style" -> {
                            currentStyleId = parser.getAttributeValue(null, "id")
                            styleLineColor = null
                            styleLineWidth = null
                            stylePolyColor = null
                        }
                        "linestyle" -> inLineStyle = true
                        "polystyle" -> inPolyStyle = true
                        "placemark" -> {
                            inPlacemark = true
                            placemarkName = ""
                            placemarkDesc = ""
                            placemarkStyleUrl = ""
                            placemarkPolygons.clear()
                            placemarkPolylines.clear()
                        }
                        "outerboundaryis" -> inOuterBoundary = true
                        "linestring" -> inLineString = true
                        "coordinates" -> inCoordinates = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when {
                            inCoordinates -> {
                                val points = parseCoordinatesString(text)
                                if (points.isNotEmpty()) {
                                    if (inOuterBoundary) {
                                        placemarkPolygons.add(points)
                                    } else if (inLineString) {
                                        placemarkPolylines.add(points)
                                    } else {
                                        // Default polygon if outerboundaryis tag was omitted
                                        placemarkPolygons.add(points)
                                    }
                                }
                            }
                            inPlacemark && currentTag == "name" && placemarkName.isEmpty() -> {
                                placemarkName = text
                            }
                            inPlacemark && currentTag == "description" && placemarkDesc.isEmpty() -> {
                                placemarkDesc = text
                            }
                            inPlacemark && currentTag == "styleurl" && placemarkStyleUrl.isEmpty() -> {
                                placemarkStyleUrl = text.removePrefix("#")
                            }
                            currentStyleId != null && inLineStyle && currentTag == "color" -> {
                                styleLineColor = parseKmlColor(text, Color(0xFF2563EB))
                            }
                            currentStyleId != null && inLineStyle && currentTag == "width" -> {
                                styleLineWidth = text.toFloatOrNull()
                            }
                            currentStyleId != null && inPolyStyle && currentTag == "color" -> {
                                stylePolyColor = parseKmlColor(text, Color(0xFF2563EB))
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val endTag = parser.name.lowercase()
                    when (endTag) {
                        "style" -> {
                            if (currentStyleId != null) {
                                styles[currentStyleId] = KmlStyle(
                                    id = currentStyleId,
                                    fillColor = stylePolyColor,
                                    strokeColor = styleLineColor,
                                    strokeWidth = styleLineWidth
                                )
                            }
                            currentStyleId = null
                            inLineStyle = false
                            inPolyStyle = false
                        }
                        "linestyle" -> inLineStyle = false
                        "polystyle" -> inPolyStyle = false
                        "outerboundaryis" -> inOuterBoundary = false
                        "linestring" -> inLineString = false
                        "coordinates" -> inCoordinates = false
                        "placemark" -> {
                            if (inPlacemark) {
                                val matchedStyle = styles[placemarkStyleUrl]
                                val defaultColorIndex = abs((placemarkName.ifEmpty { "District" }).hashCode()) % DistrictBoundaryManager.DistrictColorPalette.size
                                val baseColor = matchedStyle?.fillColor ?: DistrictBoundaryManager.DistrictColorPalette[defaultColorIndex]
                                val strokeColor = matchedStyle?.strokeColor ?: baseColor
                                val strokeWidth = matchedStyle?.strokeWidth ?: 2.5f

                                val displayName = placemarkName.ifEmpty { "District Layer ${boundaries.size + 1}" }

                                // If polygon exists, add boundary
                                for (poly in placemarkPolygons) {
                                    if (poly.size >= 3) {
                                        val minLt = poly.minOf { it.first }
                                        val maxLt = poly.maxOf { it.first }
                                        val minLn = poly.minOf { it.second }
                                        val maxLn = poly.maxOf { it.second }
                                        val cLat = (minLt + maxLt) / 2.0
                                        val cLon = (minLn + maxLn) / 2.0

                                        boundaries.add(
                                            DistrictBoundary(
                                                name = displayName,
                                                polygon = poly,
                                                polylines = emptyList(),
                                                centerLat = cLat,
                                                centerLon = cLon,
                                                minLat = minLt,
                                                maxLat = maxLt,
                                                minLon = minLn,
                                                maxLon = maxLn,
                                                color = baseColor,
                                                strokeColor = strokeColor,
                                                strokeWidth = strokeWidth,
                                                description = placemarkDesc,
                                                collegeCount = 0
                                            )
                                        )
                                    }
                                }

                                // If only polylines exist (boundary border lines)
                                if (placemarkPolygons.isEmpty() && placemarkPolylines.isNotEmpty()) {
                                    val allLinePts = placemarkPolylines.flatten()
                                    if (allLinePts.isNotEmpty()) {
                                        val minLt = allLinePts.minOf { it.first }
                                        val maxLt = allLinePts.maxOf { it.first }
                                        val minLn = allLinePts.minOf { it.second }
                                        val maxLn = allLinePts.maxOf { it.second }
                                        val cLat = (minLt + maxLt) / 2.0
                                        val cLon = (minLn + maxLn) / 2.0

                                        boundaries.add(
                                            DistrictBoundary(
                                                name = displayName,
                                                polygon = emptyList(),
                                                polylines = placemarkPolylines.toList(),
                                                centerLat = cLat,
                                                centerLon = cLon,
                                                minLat = minLt,
                                                maxLat = maxLt,
                                                minLon = minLn,
                                                maxLon = maxLn,
                                                color = baseColor,
                                                strokeColor = strokeColor,
                                                strokeWidth = strokeWidth,
                                                description = placemarkDesc,
                                                collegeCount = 0
                                            )
                                        )
                                    }
                                }
                            }
                            inPlacemark = false
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        val allPoints = boundaries.flatMap { if (it.polygon.isNotEmpty()) it.polygon else it.polylines.flatten() }
        val minLat = allPoints.minOfOrNull { it.first } ?: 0.0
        val maxLat = allPoints.maxOfOrNull { it.first } ?: 0.0
        val minLon = allPoints.minOfOrNull { it.second } ?: 0.0
        val maxLon = allPoints.maxOfOrNull { it.second } ?: 0.0

        return KmzParseResult(
            fileName = sourceName,
            boundaries = boundaries,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
            totalPlacemarks = boundaries.size,
            totalVertices = allPoints.size
        )
    }

    /**
     * Parses KML coordinate strings formatted as:
     * "lon,lat,alt lon,lat,alt ..." or newline separated.
     * Returns List of (Latitude, Longitude) coordinates.
     */
    fun parseCoordinatesString(coordString: String): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        val tokens = coordString.trim().split(Regex("\\s+"))
        for (token in tokens) {
            val parts = token.trim().split(",")
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull()
                val lat = parts[1].toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    result.add(Pair(lat, lon))
                }
            }
        }
        return result
    }

    /**
     * KML hex color format is AABBGGRR (Alpha, Blue, Green, Red)
     */
    fun parseKmlColor(hex: String, defaultColor: Color): Color {
        val clean = hex.trim().replace("#", "")
        return try {
            if (clean.length == 8) {
                val a = clean.substring(0, 2).toInt(16)
                val b = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val r = clean.substring(6, 8).toInt(16)
                Color(red = r, green = g, blue = b, alpha = a)
            } else if (clean.length == 6) {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(red = r, green = g, blue = b, alpha = 255)
            } else {
                defaultColor
            }
        } catch (_: Exception) {
            defaultColor
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) return cursor.getString(index)
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment
    }
}
