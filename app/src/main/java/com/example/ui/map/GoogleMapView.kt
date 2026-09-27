package com.example.ui.map

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CollegeEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-definition Google Map view embedding authentic Google Maps cartography:
 * - All real Rivers & Waterways worldwide taken directly from Google Map
 * - All National Highways (NH), State Highways (SH), Expressways, and Road Networks
 * - Authentic Google Road Map, Satellite Hybrid, and Topo Terrain tiles
 * - Plots user-inserted KMZ district boundaries with 30% transparent color shading
 * - Interactive College marker pins with Exam Centre badges and tap selection
 * - Proximity Radius Circle overlay (2, 5, 8, 10, 12, 15, 18, 20, 25 km)
 * - Route Planner polyline
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapView(
    colleges: List<CollegeEntity>,
    selectedCollege: CollegeEntity?,
    radiusCenterCollege: CollegeEntity?,
    circleRadiusKm: Double?,
    kmzBoundaries: List<DistrictBoundary>,
    isKmzVisible: Boolean,
    routeOriginCollege: CollegeEntity?,
    routeDestinationCollege: CollegeEntity?,
    focusedDistrict: DistrictBoundary?,
    mapStyleMode: Int, // 0 = Road, 1 = Satellite Hybrid, 2 = Terrain
    onSelectCollege: (CollegeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Map bridge for JavaScript callbacks to Compose
    class WebAppInterface {
        @JavascriptInterface
        fun onCollegeClicked(collegeId: Long) {
            mainHandler.post {
                val found = colleges.find { it.id == collegeId }
                if (found != null) {
                    onSelectCollege(found)
                }
            }
        }
    }

    val webAppInterface = remember(colleges) { WebAppInterface() }

    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
            }
            addJavascriptInterface(webAppInterface, "AndroidBridge")
        }
    }

    // Cleanup webview on disposal
    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.destroy()
            } catch (_: Exception) {}
        }
    }

    // Initial HTML setup
    LaunchedEffect(Unit) {
        val html = buildGoogleMapHtml()
        webView.loadDataWithBaseURL("https://maps.google.com", html, "text/html", "UTF-8", null)
    }

    // Update map style when changed (Road / Satellite / Terrain)
    LaunchedEffect(mapStyleMode) {
        webView.evaluateJavascript("setMapStyle($mapStyleMode);", null)
    }

    // Sync data (Colleges, KMZ District Boundaries, Radius Circle, Route)
    LaunchedEffect(
        colleges,
        selectedCollege,
        radiusCenterCollege,
        circleRadiusKm,
        kmzBoundaries,
        isKmzVisible,
        routeOriginCollege,
        routeDestinationCollege
    ) {
        val payload = buildMapPayload(
            colleges = colleges,
            selectedCollege = selectedCollege,
            radiusCenterCollege = radiusCenterCollege,
            circleRadiusKm = circleRadiusKm,
            kmzBoundaries = kmzBoundaries,
            isKmzVisible = isKmzVisible,
            routeOriginCollege = routeOriginCollege,
            routeDestinationCollege = routeDestinationCollege
        )
        webView.evaluateJavascript("updateMapData($payload);", null)
    }

    // Handle focusing on a specific district from KMZ
    LaunchedEffect(focusedDistrict) {
        if (focusedDistrict != null && focusedDistrict.polygon.isNotEmpty()) {
            val minLat = focusedDistrict.minLat
            val maxLat = focusedDistrict.maxLat
            val minLon = focusedDistrict.minLon
            val maxLon = focusedDistrict.maxLon
            if (minLat != 0.0 && maxLat != 0.0 && minLon != 0.0 && maxLon != 0.0) {
                webView.evaluateJavascript("fitBounds($minLat, $minLon, $maxLat, $maxLon);", null)
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

private fun buildGoogleMapHtml(): String {
    return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <style>
            html, body, #map {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                background: #f8fafc;
            }
            .leaflet-popup-content-wrapper {
                border-radius: 12px;
                padding: 6px;
                box-shadow: 0 4px 16px rgba(0,0,0,0.18);
            }
            .leaflet-popup-content {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                font-size: 13px;
                line-height: 1.4;
                margin: 8px 12px;
            }
            .college-title {
                font-weight: 700;
                font-size: 14px;
                color: #0f172a;
                margin-bottom: 4px;
            }
            .college-subtitle {
                font-size: 12px;
                color: #64748b;
                margin-bottom: 6px;
            }
            .badge-row {
                display: flex;
                gap: 6px;
                margin-bottom: 8px;
            }
            .badge {
                display: inline-block;
                padding: 2px 8px;
                border-radius: 6px;
                font-size: 11px;
                font-weight: 600;
            }
            .badge-exam {
                background: #fef08a;
                color: #854d0e;
                border: 1px solid #facc15;
            }
            .badge-type {
                background: #e2e8f0;
                color: #334155;
            }
            .select-btn {
                display: block;
                width: 100%;
                background: #2563eb;
                color: #ffffff;
                border: none;
                border-radius: 8px;
                padding: 8px 12px;
                font-weight: 600;
                font-size: 12px;
                cursor: pointer;
                text-align: center;
                box-sizing: border-box;
            }
            .select-btn:active {
                background: #1d4ed8;
            }
            .district-label {
                background: rgba(255, 255, 255, 0.88);
                border: 1px solid #94a3b8;
                border-radius: 6px;
                padding: 2px 6px;
                font-size: 11px;
                font-weight: bold;
                color: #1e293b;
                box-shadow: 0 2px 6px rgba(0,0,0,0.15);
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
            // 1. Google Maps Authentic Tile Layers
            // These load directly from Google Maps tile servers with authentic rivers, waterways, national highways, and road networks
            var googleRoad = L.tileLayer('https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
                subdomains: ['0', '1', '2', '3'],
                maxZoom: 22,
                attribution: '&copy; Google Maps'
            });

            var googleSatellite = L.tileLayer('https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
                subdomains: ['0', '1', '2', '3'],
                maxZoom: 22,
                attribution: '&copy; Google Maps'
            });

            var googleTerrain = L.tileLayer('https://mt{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}', {
                subdomains: ['0', '1', '2', '3'],
                maxZoom: 22,
                attribution: '&copy; Google Maps'
            });

            var map = L.map('map', {
                center: [28.6139, 77.2090],
                zoom: 10,
                zoomControl: false,
                layers: [googleRoad]
            });

            var currentStyle = 0;
            function setMapStyle(styleIndex) {
                currentStyle = styleIndex;
                map.removeLayer(googleRoad);
                map.removeLayer(googleSatellite);
                map.removeLayer(googleTerrain);

                if (styleIndex === 1) {
                    map.addLayer(googleSatellite);
                } else if (styleIndex === 2) {
                    map.addLayer(googleTerrain);
                } else {
                    map.addLayer(googleRoad);
                }
            }

            // Layer Groups
            var collegesLayer = L.layerGroup().addTo(map);
            var kmzBoundaryLayer = L.layerGroup().addTo(map);
            var radiusCircleLayer = L.layerGroup().addTo(map);
            var routeLayer = L.layerGroup().addTo(map);

            function getTypeColor(type) {
                var t = (type || '').toUpperCase();
                if (t.indexOf('AUTO') !== -1) return '#D97706';
                if (t.indexOf('NON-GRANT') !== -1 || t.indexOf('UNAIDED') !== -1 || t.indexOf('SELF') !== -1 || t.indexOf('SFI') !== -1 || t.indexOf('PRIVATE') !== -1 || t === 'SF') return '#9333EA';
                if (t.indexOf('AIDED') !== -1 || t.indexOf('GRANT') !== -1 || t === 'A') return '#059669';
                return '#1D4ED8';
            }

            function createCollegeIcon(col, isSelected) {
                var isExam = col.isExamCentre;
                var t = (col.type || 'Government').toUpperCase();
                var color = '#1D4ED8';
                var symbol = '🏛';
                var shapeStyle = 'border-radius: 50%;';
                var isDiamond = false;

                if (t.indexOf('AUTO') !== -1) {
                    color = '#D97706'; // Golden Amber
                    symbol = '👑';
                    shapeStyle = 'border-radius: 50%; border: 2px solid #FEF3C7;';
                } else if (t.indexOf('NON-GRANT') !== -1 || t.indexOf('UNAIDED') !== -1 || t.indexOf('SELF') !== -1 || t.indexOf('SFI') !== -1 || t.indexOf('PRIVATE') !== -1 || t === 'SF') {
                    color = '#9333EA'; // Vivid Purple
                    symbol = '🎓';
                    shapeStyle = 'border-radius: 6px; transform: rotate(45deg);';
                    isDiamond = true;
                } else if (t.indexOf('AIDED') !== -1 || t.indexOf('GRANT') !== -1 || t === 'A') {
                    color = '#059669'; // Emerald Green
                    symbol = '🎖';
                    shapeStyle = 'border-radius: 8px;';
                } else {
                    color = '#1D4ED8'; // Royal Blue
                    symbol = '🏛';
                    shapeStyle = 'border-radius: 50%;';
                }

                if (isSelected) {
                    color = '#EF4444';
                }

                var size = isSelected ? 34 : 28;
                var examBadge = isExam ? '<span style="position: absolute; top: -5px; right: -5px; background: #DC2626; color: #FFF; width: 14px; height: 14px; border-radius: 50%; font-size: 8px; display: flex; align-items: center; justify-content: center; border: 1.5px solid #FFF; font-weight: bold; box-shadow: 0 1px 3px rgba(0,0,0,0.4); z-index: 10;">★</span>' : '';

                var html = '<div style="position: relative; width: ' + size + 'px; height: ' + size + 'px;">' +
                    '<div style="background-color: ' + color + '; width: ' + size + 'px; height: ' + size + 'px; ' + shapeStyle + ' display: flex; align-items: center; justify-content: center; border: 2px solid #ffffff; box-shadow: 0 3px 8px rgba(0,0,0,0.35); cursor: pointer;">' +
                    '<span style="' + (isDiamond ? 'transform: rotate(-45deg); ' : '') + 'color: #ffffff; font-size: ' + (size * 0.46) + 'px; line-height: 1;">' + symbol + '</span>' +
                    '</div>' +
                    examBadge +
                    '</div>';

                return L.divIcon({
                    html: html,
                    className: '',
                    iconSize: [size, size],
                    iconAnchor: [size / 2, size / 2],
                    popupAnchor: [0, -size / 2]
                });
            }

            function selectCollegeFromJs(id) {
                if (window.AndroidBridge && window.AndroidBridge.onCollegeClicked) {
                    window.AndroidBridge.onCollegeClicked(id);
                }
            }

            function updateMapData(data) {
                // Clear existing dynamic layers
                collegesLayer.clearLayers();
                kmzBoundaryLayer.clearLayers();
                radiusCircleLayer.clearLayers();
                routeLayer.clearLayers();

                var bounds = [];

                // 1. Draw KMZ District Boundaries with 30% transparent color shading
                if (data.isKmzVisible && data.kmzBoundaries && data.kmzBoundaries.length > 0) {
                    data.kmzBoundaries.forEach(function(b) {
                        var strokeColor = b.strokeColor || '#2563EB';
                        var fillColor = b.color || '#3B82F6';

                        // Draw polygon if available
                        if (b.polygon && b.polygon.length > 0) {
                            var latLngs = b.polygon.map(function(p) { return [p[0], p[1]]; });
                            var poly = L.polygon(latLngs, {
                                color: strokeColor,
                                weight: 3,
                                opacity: 0.85,
                                fillColor: fillColor,
                                fillOpacity: 0.30, // 30% transparent shading as requested
                                dashArray: '6, 6'
                            });

                            var countText = b.collegeCount > 0 ? (b.collegeCount + ' Colleges') : 'Boundary';
                            poly.bindTooltip('<b>' + b.name + '</b><br>' + countText, {
                                permanent: false,
                                direction: 'center',
                                className: 'district-label'
                            });

                            poly.addTo(kmzBoundaryLayer);

                            latLngs.forEach(function(ll) { bounds.push(ll); });
                        }

                        // Draw polylines if any
                        if (b.polylines && b.polylines.length > 0) {
                            b.polylines.forEach(function(line) {
                                var lineLatLngs = line.map(function(p) { return [p[0], p[1]]; });
                                L.polyline(lineLatLngs, {
                                    color: strokeColor,
                                    weight: 3,
                                    opacity: 0.90
                                }).addTo(kmzBoundaryLayer);
                            });
                        }
                    });
                }

                // 2. Draw Proximity Radius Circle & Direction Rays
                if (data.radiusCircle) {
                    var r = data.radiusCircle;
                    var circle = L.circle([r.lat, r.lon], {
                        radius: r.radiusKm * 1000,
                        color: '#2563EB',
                        weight: 2.5,
                        opacity: 0.9,
                        fillColor: '#3B82F6',
                        fillOpacity: 0.18,
                        dashArray: '8, 6'
                    }).addTo(radiusCircleLayer);

                    // Add radius center marker
                    L.circleMarker([r.lat, r.lon], {
                        radius: 6,
                        color: '#1D4ED8',
                        fillColor: '#FFFFFF',
                        fillOpacity: 1
                    }).addTo(radiusCircleLayer);

                    // Draw Direction Rays from center to all colleges within radius periphery
                    if (data.colleges && data.colleges.length > 0) {
                        data.colleges.forEach(function(col) {
                            if (col.id === data.radiusCenterCollegeId) return;
                            var dLat = (col.lat - r.lat) * Math.PI / 180;
                            var dLon = (col.lon - r.lon) * Math.PI / 180;
                            var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                                    Math.cos(r.lat * Math.PI / 180) * Math.cos(col.lat * Math.PI / 180) *
                                    Math.sin(dLon/2) * Math.sin(dLon/2);
                            var dist = 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                            if (dist <= r.radiusKm) {
                                var y = Math.sin(dLon) * Math.cos(col.lat * Math.PI / 180);
                                var x = Math.cos(r.lat * Math.PI / 180) * Math.sin(col.lat * Math.PI / 180) -
                                        Math.sin(r.lat * Math.PI / 180) * Math.cos(col.lat * Math.PI / 180) * Math.cos(dLon);
                                var brng = (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
                                var headings = ['N','NNE','NE','ENE','E','ESE','SE','SSE','S','SSW','SW','WSW','W','WNW','NW','NNW'];
                                var heading = headings[Math.round(brng / 22.5) % 16];
                                var roadDist = Math.max(dist * 1.28, dist + 0.4);

                                var ray = L.polyline([[r.lat, r.lon], [col.lat, col.lon]], {
                                    color: '#2563EB',
                                    weight: 2,
                                    opacity: 0.8,
                                    dashArray: '5, 5'
                                }).addTo(radiusCircleLayer);

                                ray.bindTooltip(
                                    '<b>' + col.name + '</b><br/>' +
                                    '🧭 Direction: <b>' + heading + ' (' + Math.round(brng) + '°)</b><br/>' +
                                    '✈️ Aerial: <b>' + dist.toFixed(1) + ' km</b><br/>' +
                                    '🚗 Road: <b>' + roadDist.toFixed(1) + ' km</b>',
                                    { sticky: true }
                                );
                            }
                        });
                    }
                }

                // 3. Draw Route Polyline
                if (data.route) {
                    var routeLine = L.polyline([
                        [data.route.origLat, data.route.origLon],
                        [data.route.destLat, data.route.destLon]
                    ], {
                        color: '#2563EB',
                        weight: 5,
                        opacity: 0.85,
                        dashArray: '10, 8'
                    }).addTo(routeLayer);
                }

                // 4. Plot College Pins
                if (data.colleges && data.colleges.length > 0) {
                    data.colleges.forEach(function(col) {
                        var isSel = (data.selectedCollegeId === col.id);
                        var marker = L.marker([col.lat, col.lon], {
                            icon: createCollegeIcon(col, isSel)
                        });

                        var popupHtml = '<div class="college-title">' + col.name + '</div>' +
                            '<div class="college-subtitle">Code: <b>' + col.code + '</b> • ' + col.district + '</div>' +
                            '<div class="badge-row">' +
                            '<span class="badge" style="background: ' + getTypeColor(col.type) + '; color: #ffffff; font-weight: bold; border-radius: 4px; padding: 2px 6px;">' + (col.type || 'Government') + '</span>' +
                            (col.isExamCentre ? '<span class="badge badge-exam">★ Exam Centre</span>' : '') +
                            '</div>' +
                            '<div style="font-size: 11px; color: #475569; margin-bottom: 6px;">Seating Capacity: <b>' + col.capacity + '</b></div>' +
                            '<button class="select-btn" onclick="selectCollegeFromJs(' + col.id + ')">Select College</button>';

                        marker.bindPopup(popupHtml);
                        marker.on('click', function() {
                            selectCollegeFromJs(col.id);
                        });

                        marker.addTo(collegesLayer);
                        bounds.push([col.lat, col.lon]);
                    });
                }

                // If bounds exist and we haven't locked zoom, fit bounds gracefully
                if (bounds.length > 0 && !window.hasFitInitially) {
                    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
                    window.hasFitInitially = true;
                }
            }

            function fitBounds(minLat, minLon, maxLat, maxLon) {
                map.fitBounds([[minLat, minLon], [maxLat, maxLon]], { padding: [30, 30] });
            }

            function recenterMap() {
                var allBounds = [];
                collegesLayer.eachLayer(function(layer) {
                    if (layer.getLatLng) allBounds.push(layer.getLatLng());
                });
                kmzBoundaryLayer.eachLayer(function(layer) {
                    if (layer.getBounds) {
                        var b = layer.getBounds();
                        allBounds.push(b.getNorthEast());
                        allBounds.push(b.getSouthWest());
                    }
                });
                if (allBounds.length > 0) {
                    map.fitBounds(allBounds, { padding: [40, 40] });
                }
            }

            function zoomIn() { map.zoomIn(); }
            function zoomOut() { map.zoomOut(); }
        </script>
    </body>
    </html>
    """.trimIndent()
}

private fun buildMapPayload(
    colleges: List<CollegeEntity>,
    selectedCollege: CollegeEntity?,
    radiusCenterCollege: CollegeEntity?,
    circleRadiusKm: Double?,
    kmzBoundaries: List<DistrictBoundary>,
    isKmzVisible: Boolean,
    routeOriginCollege: CollegeEntity?,
    routeDestinationCollege: CollegeEntity?
): String {
    val root = JSONObject()

    root.put("selectedCollegeId", selectedCollege?.id ?: -1L)
    root.put("isKmzVisible", isKmzVisible)

    // Colleges
    val colsArr = JSONArray()
    for (col in colleges) {
        val obj = JSONObject()
        obj.put("id", col.id)
        obj.put("name", col.name)
        obj.put("code", col.code)
        obj.put("district", col.district)
        obj.put("lat", col.latitude)
        obj.put("lon", col.longitude)
        obj.put("isExamCentre", col.isExamCentre)
        obj.put("type", col.collegeType)
        obj.put("capacity", col.seatingCapacity)
        colsArr.put(obj)
    }
    root.put("colleges", colsArr)

    // KMZ Boundaries with 30% transparent colored scheme
    val kmzArr = JSONArray()
    for (b in kmzBoundaries) {
        val bObj = JSONObject()
        bObj.put("name", b.name)
        bObj.put("collegeCount", b.collegeCount)

        val hexColor = "#%02x%02x%02x".format(
            (b.color.red * 255).toInt(),
            (b.color.green * 255).toInt(),
            (b.color.blue * 255).toInt()
        )
        val strokeHex = "#%02x%02x%02x".format(
            (b.strokeColor.red * 255).toInt(),
            (b.strokeColor.green * 255).toInt(),
            (b.strokeColor.blue * 255).toInt()
        )
        bObj.put("color", hexColor)
        bObj.put("strokeColor", strokeHex)

        val polyArr = JSONArray()
        for (coord in b.polygon) {
            val pt = JSONArray()
            pt.put(coord.first)
            pt.put(coord.second)
            polyArr.put(pt)
        }
        bObj.put("polygon", polyArr)

        val linesArr = JSONArray()
        for (line in b.polylines) {
            val lineArr = JSONArray()
            for (c in line) {
                val pt = JSONArray()
                pt.put(c.first)
                pt.put(c.second)
                lineArr.put(pt)
            }
            linesArr.put(lineArr)
        }
        bObj.put("polylines", linesArr)

        kmzArr.put(bObj)
    }
    root.put("kmzBoundaries", kmzArr)

    // Radius Circle
    if (radiusCenterCollege != null && circleRadiusKm != null && circleRadiusKm > 0) {
        val rObj = JSONObject()
        rObj.put("lat", radiusCenterCollege.latitude)
        rObj.put("lon", radiusCenterCollege.longitude)
        rObj.put("radiusKm", circleRadiusKm)
        root.put("radiusCircle", rObj)
        root.put("radiusCenterCollegeId", radiusCenterCollege.id)
    }

    // Route
    if (routeOriginCollege != null && routeDestinationCollege != null) {
        val routeObj = JSONObject()
        routeObj.put("origLat", routeOriginCollege.latitude)
        routeObj.put("origLon", routeOriginCollege.longitude)
        routeObj.put("destLat", routeDestinationCollege.latitude)
        routeObj.put("destLon", routeDestinationCollege.longitude)
        root.put("route", routeObj)
    }

    return root.toString()
}
