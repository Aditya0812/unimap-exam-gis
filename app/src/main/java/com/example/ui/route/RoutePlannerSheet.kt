package com.example.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import com.example.data.repository.CollegeRepository
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerSheet(
    colleges: List<CollegeEntity>,
    originCollege: CollegeEntity?,
    destinationCollege: CollegeEntity?,
    onSelectOrigin: (CollegeEntity) -> Unit,
    onSelectDestination: (CollegeEntity) -> Unit,
    onSwapColleges: () -> Unit,
    onClearRoute: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var originDropdownExpanded by remember { mutableStateOf(false) }
    var destDropdownExpanded by remember { mutableStateOf(false) }

    val roadStats = remember(originCollege, destinationCollege) {
        if (originCollege != null && destinationCollege != null) {
            val direct = CollegeRepository.calculateHaversineDistanceKm(
                originCollege.latitude, originCollege.longitude,
                destinationCollege.latitude, destinationCollege.longitude
            )
            val roadKm = direct * 1.28
            val busMins = ceil((roadKm / 45.0) * 60.0).toInt().coerceAtLeast(10)
            val carMins = ceil((roadKm / 65.0) * 60.0).toInt().coerceAtLeast(8)
            Triple(roadKm, busMins, carMins)
        } else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("route_planner_sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
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
                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Campus Route Planner",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Optimal highway navigation between affiliated colleges",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Origin College Selector
            ExposedDropdownMenuBox(
                expanded = originDropdownExpanded,
                onExpandedChange = { originDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = originCollege?.name ?: "Select Starting College (Origin)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Origin College (A)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = originDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("route_origin_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = originDropdownExpanded,
                    onDismissRequest = { originDropdownExpanded = false }
                ) {
                    colleges.forEach { col ->
                        DropdownMenuItem(
                            text = { Text("${col.code} - ${col.name}") },
                            onClick = {
                                onSelectOrigin(col)
                                originDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Swap Button Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSwapColleges,
                    modifier = Modifier.testTag("swap_route_colleges_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap Origin and Destination",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Destination College Selector
            ExposedDropdownMenuBox(
                expanded = destDropdownExpanded,
                onExpandedChange = { destDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = destinationCollege?.name ?: "Select Destination College (Exam Centre)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Destination College (B)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFE11D48)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("route_destination_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = destDropdownExpanded,
                    onDismissRequest = { destDropdownExpanded = false }
                ) {
                    colleges.forEach { col ->
                        DropdownMenuItem(
                            text = { Text("${col.code} - ${col.name}") },
                            onClick = {
                                onSelectDestination(col)
                                destDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Road Metrics Breakdown
            if (roadStats != null) {
                val (roadKm, busMins, carMins) = roadStats

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Estimated Road Distance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "%.1f km".format(roadKm),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "$busMins mins",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(text = "Transit Bus", style = MaterialTheme.typography.labelSmall)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "$carMins mins",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(text = "Escort Car", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = "Route Navigation Waypoints:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Depart ${originCollege?.code} -> North/Central Arterial Highway (8.4 km)\n2. Merge onto University Expressway NH-48 (12.2 km)\n3. Take Exam Corridor Exit towards ${destinationCollege?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onClearRoute,
                    modifier = Modifier.fillMaxWidth().testTag("clear_route_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear Route Polyline")
                }
            }
        }
    }
}
