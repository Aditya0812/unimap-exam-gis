package com.example.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollegeEntity
import com.example.data.model.CollegeType
import com.example.data.model.TransitBus
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveMapView(
    colleges: List<CollegeEntity>,
    filteredColleges: List<CollegeEntity>,
    selectedCollege: CollegeEntity?,
    radiusCenterCollege: CollegeEntity?,
    circleRadiusKm: Double?,
    collegesInRadius: List<CollegeEntity>,
    routeOriginCollege: CollegeEntity?,
    routeDestinationCollege: CollegeEntity?,
    activeBuses: List<TransitBus>,
    kmzBoundaries: List<DistrictBoundary> = emptyList(),
    isKmzVisible: Boolean = true,
    focusedDistrict: DistrictBoundary? = null,
    isFullScreen: Boolean = false,
    onToggleFullScreen: (() -> Unit)? = null,
    onOpenKmzDialog: (() -> Unit)? = null,
    onSelectCollege: (CollegeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Zoom scale from 0.2f up to 500.0f (Ultra-deep enlargement like Google Maps)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0.0f) }
    var panOffsetY by remember { mutableFloatStateOf(0.0f) }

    // Map style: 0 = Google Road Map (Day), 1 = Google Satellite / Night GIS, 2 = Google Topo Terrain
    var mapStyleMode by remember { mutableStateOf(0) }
    var useGoogleMaps by remember { mutableStateOf(true) }
    var showLegend by remember { mutableStateOf(false) }
    var hoveredCollegeId by remember { mutableStateOf<Long?>(null) }

    // Compute bounding box for projection considering colleges and user-inserted KMZ district boundaries
    val minLat = remember(colleges, kmzBoundaries) {
        val colMin = colleges.minOfOrNull { it.latitude }
        val kmzMin = kmzBoundaries.filter { it.minLat != 0.0 }.minOfOrNull { it.minLat }
        when {
            colMin != null && kmzMin != null -> minOf(colMin, kmzMin)
            colMin != null -> colMin
            kmzMin != null -> kmzMin
            else -> 28.45
        }
    }
    val maxLat = remember(colleges, kmzBoundaries) {
        val colMax = colleges.maxOfOrNull { it.latitude }
        val kmzMax = kmzBoundaries.filter { it.maxLat != 0.0 }.maxOfOrNull { it.maxLat }
        when {
            colMax != null && kmzMax != null -> maxOf(colMax, kmzMax)
            colMax != null -> colMax
            kmzMax != null -> kmzMax
            else -> 28.80
        }
    }
    val minLon = remember(colleges, kmzBoundaries) {
        val colMin = colleges.minOfOrNull { it.longitude }
        val kmzMin = kmzBoundaries.filter { it.minLon != 0.0 }.minOfOrNull { it.minLon }
        when {
            colMin != null && kmzMin != null -> minOf(colMin, kmzMin)
            colMin != null -> colMin
            kmzMin != null -> kmzMin
            else -> 77.00
        }
    }
    val maxLon = remember(colleges, kmzBoundaries) {
        val colMax = colleges.maxOfOrNull { it.longitude }
        val kmzMax = kmzBoundaries.filter { it.maxLon != 0.0 }.maxOfOrNull { it.maxLon }
        when {
            colMax != null && kmzMax != null -> maxOf(colMax, kmzMax)
            colMax != null -> colMax
            kmzMax != null -> kmzMax
            else -> 77.40
        }
    }

    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0
    val spanLat = (maxLat - minLat).coerceAtLeast(0.12) * 1.3
    val spanLon = (maxLon - minLon).coerceAtLeast(0.12) * 1.3

    LaunchedEffect(focusedDistrict) {
        if (focusedDistrict != null) {
            zoomScale = 2.5f
            panOffsetX = 0f
            panOffsetY = 0f
        }
    }

    // Precompute geographical features (rivers, expressways, landmarks)
    val rivers = remember(centerLat, centerLon, spanLat, spanLon) {
        GeographicalFeaturesProvider.getRivers(centerLat, centerLon, spanLat, spanLon)
    }
    val highways = remember(centerLat, centerLon, spanLat, spanLon) {
        GeographicalFeaturesProvider.getHighways(centerLat, centerLon, spanLat, spanLon)
    }
    val landmarks = remember(centerLat, centerLon, spanLat, spanLon) {
        GeographicalFeaturesProvider.getLandmarks(centerLat, centerLon, spanLat, spanLon)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (useGoogleMaps) {
            // Live Google Map with authentic rivers, waterways, national highways & road network
            GoogleMapView(
                colleges = colleges,
                selectedCollege = selectedCollege,
                radiusCenterCollege = radiusCenterCollege,
                circleRadiusKm = circleRadiusKm,
                kmzBoundaries = kmzBoundaries,
                isKmzVisible = isKmzVisible,
                routeOriginCollege = routeOriginCollege,
                routeDestinationCollege = routeDestinationCollege,
                focusedDistrict = focusedDistrict,
                mapStyleMode = mapStyleMode,
                onSelectCollege = onSelectCollege,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("google_map_live_view")
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("interactive_canvas_map")
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldZoom = zoomScale
                        val newZoom = (zoomScale * zoom).coerceIn(0.2f, 500.0f)
                        val zoomRatio = newZoom / oldZoom

                        // Smooth pivot zooming around touch centroid
                        panOffsetX = (panOffsetX - (centroid.x - size.width / 2f)) * zoomRatio + (centroid.x - size.width / 2f) + pan.x
                        panOffsetY = (panOffsetY - (centroid.y - size.height / 2f)) * zoomRatio + (centroid.y - size.height / 2f) + pan.y
                        zoomScale = newZoom
                    }
                }
                .pointerInput(colleges, zoomScale, panOffsetX, panOffsetY) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val pos = change.position
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()

                                var closestCol: CollegeEntity? = null
                                var minDist = Float.MAX_VALUE

                                for (col in colleges) {
                                    val pt = projectCoord(
                                        lat = col.latitude,
                                        lon = col.longitude,
                                        centerLat = centerLat,
                                        centerLon = centerLon,
                                        spanLat = spanLat,
                                        spanLon = spanLon,
                                        width = w,
                                        height = h,
                                        zoom = zoomScale,
                                        panX = panOffsetX,
                                        panY = panOffsetY
                                    )
                                    val dx = pt.x - pos.x
                                    val dy = pt.y - pos.y
                                    val dist = dx * dx + dy * dy
                                    // Hit radius for hover (~46dp)
                                    if (dist < 46f * 46f && dist < minDist) {
                                        minDist = dist
                                        closestCol = col
                                    }
                                }
                                hoveredCollegeId = closestCol?.id
                            }
                        }
                    }
                }
                .pointerInput(colleges, zoomScale, panOffsetX, panOffsetY) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        // Find closest college to tap point
                        var closest: CollegeEntity? = null
                        var minDist = Float.MAX_VALUE

                        for (col in colleges) {
                            val pt = projectCoord(
                                lat = col.latitude,
                                lon = col.longitude,
                                centerLat = centerLat,
                                centerLon = centerLon,
                                spanLat = spanLat,
                                spanLon = spanLon,
                                width = w,
                                height = h,
                                zoom = zoomScale,
                                panX = panOffsetX,
                                panY = panOffsetY
                            )
                            val dx = pt.x - tapOffset.x
                            val dy = pt.y - tapOffset.y
                            val dist = dx * dx + dy * dy
                            val threshold = 52f * 52f
                            if (dist < threshold && dist < minDist) {
                                minDist = dist
                                closest = col
                            }
                        }

                        if (closest != null) {
                            hoveredCollegeId = closest.id
                            onSelectCollege(closest)
                        } else {
                            hoveredCollegeId = null
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val isDark = mapStyleMode == 1

            // 1. Base Canvas Background
            val bgColor = when (mapStyleMode) {
                1 -> Color(0xFF0F172A) // Google Satellite / Dark
                2 -> Color(0xFFFBF9F1) // Google Terrain
                else -> Color(0xFFF8FAFC) // Google Road Map (Day)
            }
            drawRect(color = bgColor)

            // 2. Coordinate Grid Lines (Adaptive at high zoom levels)
            drawCoordinateGrid(centerLat, centerLon, spanLat, spanLon, w, h, zoomScale, panOffsetX, panOffsetY, isDark)

            // 3. District Boundaries & Layers from User-Inserted KMZ File
            // (Synthetic boundary map removed; plotted only when user inserts KMZ file)
            if (isKmzVisible && kmzBoundaries.isNotEmpty()) {
                drawDistrictBoundaries(kmzBoundaries, centerLat, centerLon, spanLat, spanLon, w, h, zoomScale, panOffsetX, panOffsetY, isDark)
            }

            // 4. Proximity Radius Circle Overlay (if chosen)
            if (radiusCenterCollege != null && circleRadiusKm != null && circleRadiusKm > 0) {
                drawProximityRadiusCircle(
                    centerCollege = radiusCenterCollege,
                    radiusKm = circleRadiusKm,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    spanLat = spanLat,
                    spanLon = spanLon,
                    width = w,
                    height = h,
                    zoom = zoomScale,
                    panX = panOffsetX,
                    panY = panOffsetY
                )
            }

            // 8. Route Planner Polyline (if route active)
            if (routeOriginCollege != null && routeDestinationCollege != null) {
                drawRoutePolyline(
                    origin = routeOriginCollege,
                    destination = routeDestinationCollege,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    spanLat = spanLat,
                    spanLon = spanLon,
                    width = w,
                    height = h,
                    zoom = zoomScale,
                    panX = panOffsetX,
                    panY = panOffsetY
                )
            }

            // 9. Draw College Markers (with color coding: GOVERNMENT, AIDED, SELF FINANCE, Autonomous)
            // Show hovering label with code and first three words; show full name when cursor hovered or clicked
            val filteredSet = filteredColleges.map { it.id }.toSet()
            val insideRadiusSet = collegesInRadius.map { it.id }.toSet()

            // Partition into unhighlighted and highlighted (hovered or selected) colleges so highlighted is rendered on top
            val (normalColleges, highlightedColleges) = colleges.partition {
                it.id != selectedCollege?.id && it.id != hoveredCollegeId
            }

            for (col in normalColleges) {
                val pt = projectCoord(
                    lat = col.latitude,
                    lon = col.longitude,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    spanLat = spanLat,
                    spanLon = spanLon,
                    width = w,
                    height = h,
                    zoom = zoomScale,
                    panX = panOffsetX,
                    panY = panOffsetY
                )

                val isFilteredIn = filteredSet.contains(col.id)
                val isRadiusCenter = radiusCenterCollege?.id == col.id
                val isInsideRadius = insideRadiusSet.contains(col.id)

                drawCollegeMarker(
                    point = pt,
                    college = col,
                    isSelected = false,
                    isHovered = false,
                    isRadiusCenter = isRadiusCenter,
                    isInsideRadius = isInsideRadius,
                    isFilteredIn = isFilteredIn,
                    zoom = zoomScale,
                    isDark = isDark
                )
            }

            for (col in highlightedColleges) {
                val pt = projectCoord(
                    lat = col.latitude,
                    lon = col.longitude,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    spanLat = spanLat,
                    spanLon = spanLon,
                    width = w,
                    height = h,
                    zoom = zoomScale,
                    panX = panOffsetX,
                    panY = panOffsetY
                )

                val isFilteredIn = filteredSet.contains(col.id)
                val isSelected = selectedCollege?.id == col.id
                val isHovered = hoveredCollegeId == col.id
                val isRadiusCenter = radiusCenterCollege?.id == col.id
                val isInsideRadius = insideRadiusSet.contains(col.id)

                drawCollegeMarker(
                    point = pt,
                    college = col,
                    isSelected = isSelected,
                    isHovered = isHovered,
                    isRadiusCenter = isRadiusCenter,
                    isInsideRadius = isInsideRadius,
                    isFilteredIn = isFilteredIn,
                    zoom = zoomScale,
                    isDark = isDark
                )
            }

            // 10. Active Simulated Transit Bus Markers
            for (bus in activeBuses) {
                val origin = colleges.find { it.id == bus.originCollegeId }
                val dest = colleges.find { it.id == bus.destinationExamCentreId }
                if (origin != null && dest != null) {
                    val pOrig = projectCoord(origin.latitude, origin.longitude, centerLat, centerLon, spanLat, spanLon, w, h, zoomScale, panOffsetX, panOffsetY)
                    val pDest = projectCoord(dest.latitude, dest.longitude, centerLat, centerLon, spanLat, spanLon, w, h, zoomScale, panOffsetX, panOffsetY)

                    val pr = bus.progressPercent
                    val bx = pOrig.x + (pDest.x - pOrig.x) * pr
                    val by = pOrig.y + (pDest.y - pOrig.y) * pr

                    drawTransitBusMarker(pos = Offset(bx, by), bus = bus)
                }
            }

            // 11. Metric Scale Bar (Google Maps style, adapting to zoom level)
            drawScaleBar(w, h, spanLon, zoomScale, isDark = isDark)
        }
    }

        // Top-Left Floating Info Bar: Quick Zoom Presets & GIS Badge
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status Chip with Google Maps Help Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zoom: %.1fx".format(zoomScale),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            val gmmUri = Uri.parse("geo:${centerLat},${centerLon}?z=${(kotlin.math.log2(zoomScale.toDouble()) + 11).toInt().coerceIn(3, 21)}&q=${centerLat},${centerLon}(Colleges GIS Map)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${centerLat},${centerLon}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2563EB).copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Google Maps",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }

            // Quick Zoom Magnification Presets
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        Pair("1x", 1.0f),
                        Pair("5x", 5.0f),
                        Pair("25x", 25.0f),
                        Pair("100x", 100.0f),
                        Pair("400x", 400.0f)
                    )
                    presets.forEach { (lbl, z) ->
                        Surface(
                            onClick = { zoomScale = z },
                            shape = RoundedCornerShape(8.dp),
                            color = if (abs(zoomScale - z) < z * 0.3f) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (abs(zoomScale - z) < z * 0.3f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Map Controls (Top-Right): Fullscreen, Engine, Style, Zoom +, Zoom -, Legend, Recenter
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Provision of Full Screen mode in map section
            SmallFloatingActionButton(
                onClick = { onToggleFullScreen?.invoke() },
                containerColor = if (isFullScreen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (isFullScreen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.testTag("toggle_fullscreen_map_button")
            ) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullScreen) "Exit Full Screen" else "Full Screen Map"
                )
            }

            // Google Map Live vs Canvas Topo Engine Switcher
            SmallFloatingActionButton(
                onClick = { useGoogleMaps = !useGoogleMaps },
                containerColor = if (useGoogleMaps) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (useGoogleMaps) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.testTag("toggle_map_engine_button")
            ) {
                Icon(
                    imageVector = if (useGoogleMaps) Icons.Default.Public else Icons.Default.Map,
                    contentDescription = if (useGoogleMaps) "Google Map Live" else "Canvas Mode"
                )
            }

            // Map Style Switcher (Road / Satellite / Terrain)
            SmallFloatingActionButton(
                onClick = { mapStyleMode = (mapStyleMode + 1) % 3 },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.testTag("toggle_map_style_button")
            ) {
                Icon(imageVector = Icons.Default.Layers, contentDescription = "Map Style")
            }

            // Legend toggle
            SmallFloatingActionButton(
                onClick = { showLegend = !showLegend },
                containerColor = if (showLegend) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (showLegend) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier.testTag("toggle_legend_button")
            ) {
                Icon(imageVector = Icons.Default.Explore, contentDescription = "Map Legend")
            }

            // Zoom In (Enlarges up to 500x)
            SmallFloatingActionButton(
                onClick = { zoomScale = (zoomScale * 1.6f).coerceAtMost(500.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier.testTag("zoom_in_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
            }

            // Zoom Out
            SmallFloatingActionButton(
                onClick = { zoomScale = (zoomScale / 1.6f).coerceAtLeast(0.2f) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                modifier = Modifier.testTag("zoom_out_button")
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            // Recenter
            SmallFloatingActionButton(
                onClick = {
                    zoomScale = 1.0f
                    panOffsetX = 0f
                    panOffsetY = 0f
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.testTag("recenter_map_button")
            ) {
                Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = "Recenter")
            }

            // District KMZ Boundary & Layer Control Button
            SmallFloatingActionButton(
                onClick = { onOpenKmzDialog?.invoke() },
                containerColor = if (kmzBoundaries.isNotEmpty() && isKmzVisible) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (kmzBoundaries.isNotEmpty() && isKmzVisible) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.tertiary,
                shape = CircleShape,
                modifier = Modifier.testTag("map_kmz_boundary_button")
            ) {
                Icon(imageVector = Icons.Default.Layers, contentDescription = "District KMZ Boundary Layer")
            }
        }

        // Full Screen Exit Banner
        if (isFullScreen) {
            Surface(
                onClick = { onToggleFullScreen?.invoke() },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .testTag("exit_fullscreen_banner_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Full Screen Map • Tap to Exit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Floating KMZ Active Badge (if loaded)
        if (kmzBoundaries.isNotEmpty()) {
            Surface(
                onClick = { onOpenKmzDialog?.invoke() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 56.dp)
                    .testTag("kmz_active_map_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isKmzVisible) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KMZ: ${kmzBoundaries.size} Districts (${if (isKmzVisible) "30% Shaded" else "Hidden"})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // On-Canvas Floating Map Legend Card (Toggleable)
        AnimatedVisibility(
            visible = showLegend,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 48.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                modifier = Modifier.width(260.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Map Features & Legend",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showLegend = false },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "COLLEGE TYPES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LegendItem(color = Color(0xFF1D4ED8), label = "GOVERNMENT (Blue)", glyph = "G")
                    LegendItem(color = Color(0xFF059669), label = "AIDED (Green)", glyph = "A")
                    LegendItem(color = Color(0xFF9333EA), label = "SELF FINANCE (Purple)", glyph = "SF")
                    LegendItem(color = Color(0xFFD97706), label = "Autonomous (Gold)", glyph = "AU")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "GEOGRAPHY & BOUNDARIES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LegendItem(color = Color(0xFF059669).copy(alpha = 0.45f), label = "District Boundaries (30% Shaded)", glyph = "⬡")
                    LegendItem(color = Color(0xFF38BDF8), label = "Rivers & Waterways (Blue)", glyph = "〰")
                    LegendItem(color = Color(0xFFFBBF24), label = "NH-44 / NH-48 Highway Network", glyph = "═")
                    LegendItem(color = Color(0xFFE11D48), label = "Railway, ISBT & Key Landmarks", glyph = "📍")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, glyph: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = glyph.take(1),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ----------------- DRAWING HELPERS -----------------

private fun DrawScope.drawCoordinateGrid(
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isDark: Boolean
) {
    val gridColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.7f)
    val labelPaint = android.graphics.Paint().apply {
        color = if (isDark) android.graphics.Color.GRAY else android.graphics.Color.DKGRAY
        textSize = 20f
        isAntiAlias = true
    }

    val steps = 6
    for (i in -steps..steps) {
        val lat = centerLat + (i * (spanLat / steps.toDouble()))
        val p1 = projectCoord(lat, centerLon - spanLon, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
        val p2 = projectCoord(lat, centerLon + spanLon, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
        drawLine(color = gridColor, start = p1, end = p2, strokeWidth = 1.0f)

        if (zoom <= 15f) {
            val latStr = "%.2f°N".format(lat)
            drawContext.canvas.nativeCanvas.drawText(latStr, 16f, p1.y - 4f, labelPaint)
        }
    }

    for (j in -steps..steps) {
        val lon = centerLon + (j * (spanLon / steps.toDouble()))
        val p1 = projectCoord(centerLat - spanLat, lon, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
        val p2 = projectCoord(centerLat + spanLat, lon, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
        drawLine(color = gridColor, start = p1, end = p2, strokeWidth = 1.0f)

        if (zoom <= 15f) {
            val lonStr = "%.2f°E".format(lon)
            drawContext.canvas.nativeCanvas.drawText(lonStr, p1.x + 4f, height - 24f, labelPaint)
        }
    }
}

/**
 * Draws the user-inserted KMZ District boundaries with 30% transparent shading as requested:
 * "Give provision to insert kmz file by me to plot district boundary and layer."
 * "shade with 30% transparent different colour scheme."
 */
private fun DrawScope.drawDistrictBoundaries(
    districts: List<DistrictBoundary>,
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isDark: Boolean
) {
    for (district in districts) {
        // Draw closed polygon if available
        if (district.polygon.isNotEmpty()) {
            val path = Path()
            district.polygon.forEachIndexed { index, coord ->
                val pt = projectCoord(coord.first, coord.second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
                if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()

            // 1. Shaded polygon with EXACTLY 30% opacity (alpha = 0.30f)
            drawPath(
                path = path,
                color = district.color.copy(alpha = 0.30f),
                style = Fill
            )

            // 2. High-definition district boundary stroke
            drawPath(
                path = path,
                color = district.strokeColor.copy(alpha = 0.85f),
                style = Stroke(
                    width = district.strokeWidth.coerceAtLeast(3.0f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), 0f)
                )
            )
        }

        // Draw linear boundary lines if any
        if (district.polylines.isNotEmpty()) {
            for (line in district.polylines) {
                if (line.size >= 2) {
                    val linePath = Path()
                    line.forEachIndexed { index, coord ->
                        val pt = projectCoord(coord.first, coord.second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
                        if (index == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = linePath,
                        color = district.strokeColor.copy(alpha = 0.90f),
                        style = Stroke(width = district.strokeWidth.coerceAtLeast(3.0f))
                    )
                }
            }
        }

        if (district.polygon.isEmpty() && district.polylines.isEmpty()) continue

        // 3. District Name Emblem Banner at centroid
        val centerPt = projectCoord(district.centerLat, district.centerLon, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)

        val bannerPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.rgb(15, 23, 42)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
            textSize = 16f
            isAntiAlias = true
        }

        val nameText = "📍 ${district.name.uppercase()}"
        val countText = if (district.collegeCount > 0) "${district.collegeCount} Colleges" else "District Boundary"
        val nameW = bannerPaint.measureText(nameText)
        val countW = subPaint.measureText(countText)
        val maxW = maxOf(nameW, countW)

        val padX = 12f
        val padY = 8f
        val left = centerPt.x - maxW / 2f - padX
        val top = centerPt.y - 24f - padY
        val right = centerPt.x + maxW / 2f + padX
        val bottom = centerPt.y + 20f + padY

        val bgPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.argb(225, 15, 23, 42) else android.graphics.Color.argb(235, 255, 255, 255)
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                240,
                (district.color.red * 255).toInt(),
                (district.color.green * 255).toInt(),
                (district.color.blue * 255).toInt()
            )
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        val rectF = android.graphics.RectF(left, top, right, bottom)
        drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
        drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 12f, 12f, borderPaint)

        drawContext.canvas.nativeCanvas.drawText(nameText, centerPt.x - nameW / 2f, centerPt.y - 4f, bannerPaint)
        drawContext.canvas.nativeCanvas.drawText(countText, centerPt.x - countW / 2f, centerPt.y + 16f, subPaint)
    }
}

/**
 * Draws natural rivers, tributaries, and water channels inspired by Google Maps.
 */
private fun DrawScope.drawGeographicalWaterways(
    rivers: List<RiverPath>,
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isDark: Boolean
) {
    val riverWaterColor = if (isDark) Color(0xFF1E3A8A) else Color(0xFF60A5FA)
    val riverBankColor = if (isDark) Color(0xFF172554) else Color(0xFF93C5FD)

    val riverLabelPaint = android.graphics.Paint().apply {
        color = if (isDark) android.graphics.Color.rgb(147, 197, 253) else android.graphics.Color.rgb(37, 99, 235)
        textSize = 18f
        isFakeBoldText = true
        isAntiAlias = true
    }

    for (river in rivers) {
        if (river.points.size < 2) continue
        val path = Path()

        val p0 = projectCoord(river.points[0].first, river.points[0].second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
        path.moveTo(p0.x, p0.y)

        for (i in 1 until river.points.size) {
            val pt = projectCoord(river.points[i].first, river.points[i].second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
            path.lineTo(pt.x, pt.y)
        }

        val strokeW = if (river.isMainStream) 22f * zoom.coerceIn(0.6f, 3.0f) else 12f * zoom.coerceIn(0.6f, 2.5f)

        // Outer water bank
        drawPath(
            path = path,
            color = riverBankColor.copy(alpha = 0.6f),
            style = Stroke(width = strokeW + 6f)
        )
        // Core river channel
        drawPath(
            path = path,
            color = riverWaterColor.copy(alpha = 0.85f),
            style = Stroke(width = strokeW)
        )

        // Water labels along mid-point
        val midIndex = river.points.size / 2
        val midPt = projectCoord(river.points[midIndex].first, river.points[midIndex].second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
        drawContext.canvas.nativeCanvas.drawText("〰 ${river.name}", midPt.x + 14f, midPt.y - 8f, riverLabelPaint)
    }
}

/**
 * Draws the road network:
 * - National Expressways (NH-44, NH-48) with Google Maps yellow fill and borders
 * - State Highways (SH-15) and Outer Ring Road
 * - Direct campus access roads connecting colleges to the highway grid
 */
private fun DrawScope.drawGeographicalRoads(
    highways: List<HighwayPath>,
    colleges: List<CollegeEntity>,
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isDark: Boolean
) {
    // 1. National and State Highways
    for (hw in highways) {
        if (hw.points.size < 2) continue
        val path = Path()
        val p0 = projectCoord(hw.points[0].first, hw.points[0].second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
        path.moveTo(p0.x, p0.y)

        for (i in 1 until hw.points.size) {
            val pt = projectCoord(hw.points[i].first, hw.points[i].second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
            path.lineTo(pt.x, pt.y)
        }

        if (hw.isExpressway) {
            // Google Maps Expressway: Dark casing + Warm Yellow/Gold fill
            val casingColor = if (isDark) Color(0xFF64748B) else Color(0xFFCBD5E1)
            val fillColor = if (isDark) Color(0xFFD97706) else Color(0xFFFBBF24)

            drawPath(path = path, color = casingColor, style = Stroke(width = 12f * zoom.coerceIn(0.8f, 2.5f)))
            drawPath(path = path, color = fillColor, style = Stroke(width = 8f * zoom.coerceIn(0.8f, 2.5f)))

            // Highway Shield Badge (NH-44 / NH-48)
            val midIdx = hw.points.size / 2
            val midPt = projectCoord(hw.points[midIdx].first, hw.points[midIdx].second, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)

            val shieldBg = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(30, 41, 59)
                style = android.graphics.Paint.Style.FILL
            }
            val shieldBorder = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(251, 191, 36)
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
            }
            val shieldText = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 15f
                isFakeBoldText = true
            }
            val sWidth = shieldText.measureText(hw.designation) + 12f
            val sRect = android.graphics.RectF(midPt.x - sWidth / 2f, midPt.y - 10f, midPt.x + sWidth / 2f, midPt.y + 10f)
            drawContext.canvas.nativeCanvas.drawRoundRect(sRect, 6f, 6f, shieldBg)
            drawContext.canvas.nativeCanvas.drawRoundRect(sRect, 6f, 6f, shieldBorder)
            drawContext.canvas.nativeCanvas.drawText(hw.designation, midPt.x - (sWidth - 12f) / 2f, midPt.y + 5f, shieldText)
        } else {
            // State Highway / Ring Road: White or Slate thoroughfare
            val casingColor = if (isDark) Color(0xFF334155) else Color(0xFF94A3B8)
            val fillColor = if (isDark) Color(0xFF475569) else Color(0xFFFFFFFF)

            drawPath(path = path, color = casingColor, style = Stroke(width = 8f * zoom.coerceIn(0.8f, 2.2f)))
            drawPath(path = path, color = fillColor, style = Stroke(width = 5f * zoom.coerceIn(0.8f, 2.2f)))
        }
    }
}

/**
 * Draws prominent civic and transport landmarks (Railway Junction, ISBT, Airport, Hospital, Secretariat, Eco Park).
 */
private fun DrawScope.drawGeographicalLandmarks(
    landmarks: List<GeoLandmark>,
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    isDark: Boolean
) {
    for (lm in landmarks) {
        val pt = projectCoord(lm.latitude, lm.longitude, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)

        val lmColor = Color(lm.type.colorHex)

        // Special rendering for Eco-Park polygon
        if (lm.type == LandmarkType.BOTANICAL_PARK) {
            drawCircle(
                color = Color(0xFF15803D).copy(alpha = 0.25f),
                radius = 36f * zoom.coerceIn(0.8f, 2.5f),
                center = pt
            )
        }

        // Special rendering for Airport Runway
        if (lm.type == LandmarkType.AIRPORT) {
            val rLen = 50f * zoom.coerceIn(0.8f, 2.5f)
            drawLine(
                color = if (isDark) Color(0xFF475569) else Color(0xFF334155),
                start = Offset(pt.x - rLen, pt.y),
                end = Offset(pt.x + rLen, pt.y),
                strokeWidth = 10f
            )
            drawLine(
                color = Color.White,
                start = Offset(pt.x - rLen + 5f, pt.y),
                end = Offset(pt.x + rLen - 5f, pt.y),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        }

        // Special hatched ties for Railway Junction
        if (lm.type == LandmarkType.RAILWAY_JUNCTION) {
            val railLen = 45f * zoom.coerceIn(0.8f, 2.5f)
            drawLine(
                color = if (isDark) Color.LightGray else Color.DarkGray,
                start = Offset(pt.x - railLen, pt.y + 6f),
                end = Offset(pt.x + railLen, pt.y - 6f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = if (isDark) Color.LightGray else Color.DarkGray,
                start = Offset(pt.x - railLen, pt.y - 2f),
                end = Offset(pt.x + railLen, pt.y - 14f),
                strokeWidth = 2.5f
            )
        }

        // Landmark Marker Pin Drop
        drawCircle(
            color = lmColor,
            radius = 12f,
            center = pt
        )
        drawCircle(
            color = Color.White,
            radius = 5.5f,
            center = pt
        )

        // Landmark Label & Glyph
        val paint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.rgb(30, 41, 59)
            textSize = 17f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val text = "${lm.type.iconGlyph} ${lm.name}"
        drawContext.canvas.nativeCanvas.drawText(text, pt.x + 16f, pt.y + 6f, paint)
    }
}

/**
 * Draws college markers using distinctive color icons for:
 * GOVERNMENT, AIDED, SELF FINANCE, and Autonomous colleges.
 * "Show icon with hovering label containing code and first three words of name (not full name).
 *  Show full name only when cursor is brought there are clicked over."
 */
private fun DrawScope.drawCollegeMarker(
    point: Offset,
    college: CollegeEntity,
    isSelected: Boolean,
    isHovered: Boolean,
    isRadiusCenter: Boolean,
    isInsideRadius: Boolean,
    isFilteredIn: Boolean,
    zoom: Float,
    isDark: Boolean
) {
    val alpha = if (isFilteredIn) 1.0f else 0.25f

    // 1. Radar pulse beacon if this college is the radius center
    if (isRadiusCenter) {
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.35f * alpha),
            radius = 32f * zoom.coerceIn(0.8f, 1.5f),
            center = point
        )
    }

    // 2. Selection ring or Hover halo
    if (isSelected) {
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = 0.6f),
            radius = 26f * zoom.coerceIn(0.8f, 1.4f),
            center = point
        )
    } else if (isHovered) {
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.45f),
            radius = 22f * zoom.coerceIn(0.8f, 1.3f),
            center = point
        )
    }

    // 3. Radius proximity ring
    if (isInsideRadius && !isRadiusCenter) {
        drawCircle(
            color = Color(0xFF10B981).copy(alpha = 0.4f),
            radius = 20f,
            center = point
        )
    }

    // 4. Color by College Management Type (4th column from uploaded Excel):
    // - GOVERNMENT: Royal Blue (0xFF1D4ED8)
    // - AIDED: Emerald Green (0xFF059669)
    // - SELF FINANCE: Vivid Purple (0xFF9333EA)
    // - AUTONOMOUS: Radiant Amber Gold (0xFFD97706)
    val managementTypeColor = Color(college.managementType.colorHex).copy(alpha = alpha)

    // Outer Pin Drop Body
    val pinRadius = if (isSelected || isHovered) {
        if (college.isExamCentre) 16f else 13f
    } else {
        if (college.isExamCentre) 13f else 10.5f
    }
    drawCircle(
        color = managementTypeColor,
        radius = pinRadius,
        center = point
    )

    // Inner White Pin Core
    val innerRadius = if (college.isExamCentre) 6.5f else 5f
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = innerRadius,
        center = point
    )

    // Emblem Letter ("G", "A", "SF", "AU") drawn sharply in the center of the pin
    val glyphText = when (college.managementType) {
        CollegeType.GOVERNMENT -> "G"
        CollegeType.AIDED -> "A"
        CollegeType.SELF_FINANCE -> "SF"
        CollegeType.AUTONOMOUS -> "AU"
    }
    val glyphPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (managementTypeColor.red * 255).toInt(),
            (managementTypeColor.green * 255).toInt(),
            (managementTypeColor.blue * 255).toInt()
        )
        textSize = if (glyphText.length > 1) 9f else 11f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(glyphText, point.x, point.y + 4f, glyphPaint)

    // Crown/Star on top if Exam Centre
    if (college.isExamCentre) {
        val starPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(245, 158, 11)
            textSize = 14f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("★", point.x, point.y - pinRadius - 2f, starPaint)
    }

    if (!isFilteredIn) return

    val isFullDetail = isHovered || isSelected

    if (!isFullDetail) {
        // DEFAULT UNHOVERED/UNCLICKED STATE:
        // "Show icon with hovering label containing code and first three words of name (not full name)."
        val words = college.name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val firstThreeWords = if (words.size <= 3) college.name.trim() else words.take(3).joinToString(" ")
        val codeTag = "[${college.code}] "

        val codePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(
                (managementTypeColor.red * 255).toInt(),
                (managementTypeColor.green * 255).toInt(),
                (managementTypeColor.blue * 255).toInt()
            )
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val namePaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.rgb(241, 245, 249) else android.graphics.Color.rgb(15, 23, 42)
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val codeWidth = codePaint.measureText(codeTag)
        val nameWidth = namePaint.measureText(firstThreeWords)
        val totalTextWidth = codeWidth + nameWidth

        val padX = 7f
        val pillLeft = point.x + pinRadius + 6f
        val pillTop = point.y - 12f
        val pillRight = pillLeft + totalTextWidth + (padX * 2)
        val pillBottom = point.y + 12f

        val rectF = android.graphics.RectF(pillLeft, pillTop, pillRight, pillBottom)

        val bgPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.argb(220, 15, 23, 42) else android.graphics.Color.argb(230, 255, 255, 255)
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                190,
                (managementTypeColor.red * 255).toInt(),
                (managementTypeColor.green * 255).toInt(),
                (managementTypeColor.blue * 255).toInt()
            )
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 6f, 6f, bgPaint)
        drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 6f, 6f, borderPaint)

        // Draw College Code
        drawContext.canvas.nativeCanvas.drawText(
            codeTag,
            pillLeft + padX,
            point.y + 4.5f,
            codePaint
        )

        // Draw First Three Words of College Name (NOT full name)
        drawContext.canvas.nativeCanvas.drawText(
            firstThreeWords,
            pillLeft + padX + codeWidth,
            point.y + 4.5f,
            namePaint
        )
    } else {
        // HOVERED OR CLICKED OVER STATE:
        // "Show full name only when cursor is brought there are clicked over."
        val fullNameText = college.name
        val subtitleText = "[${college.code}] • ${college.managementType.title} • District: ${college.district}"
        val coordsText = "📍 Lat: ${String.format(java.util.Locale.US, "%.5f", college.latitude)}, Lon: ${String.format(java.util.Locale.US, "%.5f", college.longitude)}"

        val fullNamePaint = android.graphics.Paint().apply {
            color = when {
                isSelected -> if (isDark) android.graphics.Color.rgb(254, 240, 138) else android.graphics.Color.rgb(30, 58, 138)
                else -> if (isDark) android.graphics.Color.rgb(241, 245, 249) else android.graphics.Color.rgb(15, 23, 42)
            }
            textSize = 21f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.rgb(203, 213, 225) else android.graphics.Color.rgb(51, 65, 85)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val coordsPaint = android.graphics.Paint().apply {
            color = if (isDark) android.graphics.Color.rgb(148, 163, 184) else android.graphics.Color.rgb(100, 116, 139)
            textSize = 13f
            isAntiAlias = true
        }

        val w1 = fullNamePaint.measureText(fullNameText)
        val w2 = subtitlePaint.measureText(subtitleText)
        val w3 = coordsPaint.measureText(coordsText)
        val maxCardWidth = maxOf(w1, maxOf(w2, w3))

        val cardPadX = 14f
        val cardPadY = 10f
        val cardLeft = point.x + pinRadius + 8f
        val cardTop = point.y - 38f
        val cardRight = cardLeft + maxCardWidth + (cardPadX * 2)
        val cardBottom = cardTop + 72f

        val rectF = android.graphics.RectF(cardLeft, cardTop, cardRight, cardBottom)

        // Drop shadow backing
        val shadowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(80, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawRoundRect(
            android.graphics.RectF(cardLeft + 2f, cardTop + 3f, cardRight + 2f, cardBottom + 3f),
            12f,
            12f,
            shadowPaint
        )

        // Card Fill
        val bgPaint = android.graphics.Paint().apply {
            color = if (isDark) {
                if (isSelected) android.graphics.Color.argb(245, 30, 41, 59) else android.graphics.Color.argb(240, 15, 23, 42)
            } else {
                if (isSelected) android.graphics.Color.argb(250, 254, 243, 199) else android.graphics.Color.argb(245, 255, 255, 255)
            }
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        // Highlight Glowing Border
        val borderPaint = android.graphics.Paint().apply {
            color = if (isSelected) {
                android.graphics.Color.argb(255, 245, 158, 11) // Amber
            } else {
                android.graphics.Color.argb(
                    255,
                    (managementTypeColor.red * 255).toInt(),
                    (managementTypeColor.green * 255).toInt(),
                    (managementTypeColor.blue * 255).toInt()
                )
            }
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 2.4f
            isAntiAlias = true
        }

        drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 10f, 10f, bgPaint)
        drawContext.canvas.nativeCanvas.drawRoundRect(rectF, 10f, 10f, borderPaint)

        // Line 1: Complete Full Name
        drawContext.canvas.nativeCanvas.drawText(
            fullNameText,
            cardLeft + cardPadX,
            cardTop + cardPadY + 14f,
            fullNamePaint
        )

        // Line 2: Code, Type, and District
        drawContext.canvas.nativeCanvas.drawText(
            subtitleText,
            cardLeft + cardPadX,
            cardTop + cardPadY + 33f,
            subtitlePaint
        )

        // Line 3: Exact Latitude and Longitude
        drawContext.canvas.nativeCanvas.drawText(
            coordsText,
            cardLeft + cardPadX,
            cardTop + cardPadY + 50f,
            coordsPaint
        )
    }
}

private fun DrawScope.drawProximityRadiusCircle(
    centerCollege: CollegeEntity,
    radiusKm: Double,
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float
) {
    val centerPt = projectCoord(centerCollege.latitude, centerCollege.longitude, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
    val latRadius = radiusKm / 111.0
    val edgePt = projectCoord(centerCollege.latitude + latRadius, centerCollege.longitude, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
    val pixelRadius = abs(edgePt.y - centerPt.y)

    drawCircle(
        color = Color(0xFF3B82F6).copy(alpha = 0.14f),
        radius = pixelRadius,
        center = centerPt
    )
    drawCircle(
        color = Color(0xFF2563EB),
        radius = pixelRadius,
        center = centerPt,
        style = Stroke(
            width = 3.0f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
        )
    )

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(37, 99, 235)
        textSize = 26f
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        "Radius: ${radiusKm.toInt()} km",
        centerPt.x + 8f,
        centerPt.y - pixelRadius - 8f,
        paint
    )
}

private fun DrawScope.drawRoutePolyline(
    origin: CollegeEntity,
    destination: CollegeEntity,
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float
) {
    val p1 = projectCoord(origin.latitude, origin.longitude, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)
    val p2 = projectCoord(destination.latitude, destination.longitude, centerLat, centerLon, spanLat, spanLon, width, height, zoom, panX, panY)

    val midX = (p1.x + p2.x) / 2f + (p2.y - p1.y) * 0.15f
    val midY = (p1.y + p2.y) / 2f - (p2.x - p1.x) * 0.15f

    val routePath = Path().apply {
        moveTo(p1.x, p1.y)
        quadraticTo(midX, midY, p2.x, p2.y)
    }

    drawPath(
        path = routePath,
        color = Color(0xFF6366F1).copy(alpha = 0.35f),
        style = Stroke(width = 12f)
    )
    drawPath(
        path = routePath,
        color = Color(0xFF4F46E5),
        style = Stroke(width = 5f)
    )

    drawCircle(color = Color(0xFF10B981), radius = 8f, center = p1)
    drawCircle(color = Color(0xFFE11D48), radius = 9f, center = p2)
}

private fun DrawScope.drawTransitBusMarker(pos: Offset, bus: TransitBus) {
    drawCircle(color = Color(0xFF8B5CF6).copy(alpha = 0.4f), radius = 16f, center = pos)
    drawCircle(color = Color(0xFF7C3AED), radius = 9f, center = pos)
    drawCircle(color = Color.White, radius = 3.5f, center = pos)

    val busPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(109, 40, 217)
        textSize = 18f
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(bus.busId, pos.x + 12f, pos.y - 8f, busPaint)
}

/**
 * Scale Bar dynamically adapts from 50 km down to 20 meters as map is enlarged.
 */
private fun DrawScope.drawScaleBar(
    width: Float,
    height: Float,
    spanLon: Double,
    zoom: Float,
    isDark: Boolean
) {
    val barColor = if (isDark) Color.White else Color(0xFF334155)
    val barWidth = 120f
    val startX = 24f
    val startY = height - 24f

    drawLine(color = barColor, start = Offset(startX, startY), end = Offset(startX + barWidth, startY), strokeWidth = 4f)
    drawLine(color = barColor, start = Offset(startX, startY - 6f), end = Offset(startX, startY + 6f), strokeWidth = 3f)
    drawLine(color = barColor, start = Offset(startX + barWidth, startY - 6f), end = Offset(startX + barWidth, startY + 6f), strokeWidth = 3f)

    val scalePaint = android.graphics.Paint().apply {
        color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
        textSize = 20f
        isAntiAlias = true
    }

    val approxMeters = (15000.0 / zoom)
    val scaleText = if (approxMeters >= 1000.0) {
        "~%.1f km".format(approxMeters / 1000.0)
    } else {
        "~%d m".format(approxMeters.toInt().coerceAtLeast(10))
    }

    drawContext.canvas.nativeCanvas.drawText(scaleText, startX + 20f, startY - 8f, scalePaint)
}

private fun projectCoord(
    lat: Double,
    lon: Double,
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double,
    width: Float,
    height: Float,
    zoom: Float,
    panX: Float,
    panY: Float
): Offset {
    val normX = (lon - (centerLon - spanLon / 2.0)) / spanLon
    val normY = ((centerLat + spanLat / 2.0) - lat) / spanLat

    val cx = width / 2f
    val cy = height / 2f

    val basePx = normX.toFloat() * width
    val basePy = normY.toFloat() * height

    val scaledX = (basePx - cx) * zoom + cx + panX
    val scaledY = (basePy - cy) * zoom + cy + panY

    return Offset(scaledX, scaledY)
}
