package com.example.ui.radius

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollegeEntity
import com.example.ui.theme.ExamGold
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadiusCircleToolSheet(
    colleges: List<CollegeEntity>,
    selectedCenterCollege: CollegeEntity?,
    onSelectCenterCollege: (CollegeEntity) -> Unit,
    currentRadiusKm: Double,
    onSelectRadiusKm: (Double) -> Unit,
    collegesInRadius: List<Pair<CollegeEntity, Double>>,
    onCollegeClick: (CollegeEntity) -> Unit,
    onClearRadiusCircle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radiusOptions = listOf(2.0, 5.0, 8.0, 10.0, 12.0, 15.0, 18.0, 20.0, 25.0)

    var centerDropdownExpanded by remember { mutableStateOf(false) }
    var radiusDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 460.dp, max = 580.dp)
            .testTag("radius_circle_tool_sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Proximity Circle Tool",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nearby affiliated colleges within radius",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("close_radius_tool_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Drop-down selectors: Center College & Circle Radius with reduced font size and compact layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Center College Dropdown (reduced font size)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Center College:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    ExposedDropdownMenuBox(
                        expanded = centerDropdownExpanded,
                        onExpandedChange = { centerDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCenterCollege?.name ?: "Select center college",
                            onValueChange = {},
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = centerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("center_college_dropdown_field"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = centerDropdownExpanded,
                            onDismissRequest = { centerDropdownExpanded = false }
                        ) {
                            colleges.forEach { col ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${col.code} - ${col.name}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            )
                                            Text(
                                                text = "${col.district} • Cap: ${col.seatingCapacity}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectCenterCollege(col)
                                        centerDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        if (col.isExamCentre) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = ExamGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Circle Radius Dropdown (reduced font size, options: 2, 5, 8, 10, 12, 15, 18, 20, 25)
                Column(modifier = Modifier.width(115.dp)) {
                    Text(
                        text = "Circle Radius:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    ExposedDropdownMenuBox(
                        expanded = radiusDropdownExpanded,
                        onExpandedChange = { radiusDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "${currentRadiusKm.toInt()} km",
                            onValueChange = {},
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = radiusDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("radius_select_dropdown_field"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = radiusDropdownExpanded,
                            onDismissRequest = { radiusDropdownExpanded = false }
                        ) {
                            radiusOptions.forEach { r ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${r.toInt()} km",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        )
                                    },
                                    onClick = {
                                        onSelectRadiusKm(r)
                                        radiusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Statistics Summary Card
            if (selectedCenterCollege != null) {
                val totalCap = collegesInRadius.sumOf { it.first.seatingCapacity }
                val totalStd = collegesInRadius.sumOf { it.first.studentPopulation }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${collegesInRadius.size}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = "In Radius", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%,d".format(totalCap),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SuccessGreen
                            )
                            Text(text = "Total Seats", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%,d".format(totalStd),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ExamGold
                            )
                            Text(text = "Students/Day", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nearby Colleges (Sorted by Road/Air Distance) Header & Scrollable List
                Text(
                    text = "Nearby Colleges (sorted by Road/Air Distance):",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 160.dp, max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(collegesInRadius) { (col, distance) ->
                        Surface(
                            onClick = { onCollegeClick(col) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (col.id == selectedCenterCollege.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = col.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "${col.code} • ${col.district} • Cap: ${col.seatingCapacity}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    if (col.id != selectedCenterCollege.id) {
                                        val brng = calculateBearingDegrees(
                                            selectedCenterCollege.latitude, selectedCenterCollege.longitude,
                                            col.latitude, col.longitude
                                        )
                                        val (heading, arrow) = getCompassHeading(brng)
                                        Text(
                                            text = "🧭 $arrow $heading (${brng.toInt()}°)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                if (distance == 0.0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "CENTER",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    val roadDist = maxOf(distance * 1.28, distance + 0.4)
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "✈️ %.1f km".format(distance),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SuccessGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "🚗 %.1f km".format(roadDist),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = SuccessGreen,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Clear Circle Button
                OutlinedButton(
                    onClick = onClearRadiusCircle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("clear_radius_circle_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear Circle Overlay", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                }
            }
        }
    }
}

fun calculateBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val y = Math.sin(dLon) * Math.cos(lat2Rad)
    val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon)
    val brng = Math.toDegrees(Math.atan2(y, x))
    return (brng + 360.0) % 360.0
}

fun getCompassHeading(deg: Double): Pair<String, String> {
    val directions = listOf(
        "N" to "↑", "NNE" to "↗", "NE" to "↗", "ENE" to "↗",
        "E" to "→", "ESE" to "↘", "SE" to "↘", "SSE" to "↘",
        "S" to "↓", "SSW" to "↙", "SW" to "↙", "WSW" to "↙",
        "W" to "←", "WNW" to "↖", "NW" to "↖", "NNW" to "↖"
    )
    val index = (Math.round(deg / 22.5).toInt()) % 16
    return directions[index]
}
