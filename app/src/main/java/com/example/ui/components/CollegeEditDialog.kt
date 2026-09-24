package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CollegeEntity
import com.example.ui.theme.AlertRed

@Composable
fun CollegeEditDialog(
    collegeToEdit: CollegeEntity?, // null if adding a new college
    onSave: (CollegeEntity) -> Unit,
    onDelete: ((CollegeEntity) -> Unit)?,
    onDismiss: () -> Unit
) {
    val isEditing = collegeToEdit != null

    var name by remember { mutableStateOf(collegeToEdit?.name ?: "") }
    var code by remember { mutableStateOf(collegeToEdit?.code ?: "") }
    var latitude by remember { mutableStateOf(collegeToEdit?.latitude?.toString() ?: "28.6139") }
    var longitude by remember { mutableStateOf(collegeToEdit?.longitude?.toString() ?: "77.2090") }
    var branches by remember { mutableStateOf(collegeToEdit?.branches ?: "BA, BSc, BCom, MA, LLB") }
    var seatingCapacity by remember { mutableStateOf(collegeToEdit?.seatingCapacity?.toString() ?: "1800") }
    var studentPopulation by remember { mutableStateOf(collegeToEdit?.studentPopulation?.toString() ?: "1200") }
    var isExamCentre by remember { mutableStateOf(collegeToEdit?.isExamCentre ?: false) }
    var collegeType by remember { mutableStateOf(collegeToEdit?.collegeType ?: "GOVERNMENT") }
    var address by remember { mutableStateOf(collegeToEdit?.address ?: "") }
    var district by remember { mutableStateOf(collegeToEdit?.district ?: "University Region") }
    var phone by remember { mutableStateOf(collegeToEdit?.contactPhone ?: "") }

    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("college_edit_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Update College" else "Add New College",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("College Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_college_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Code & District
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code (e.g. APEX-01) *") },
                        modifier = Modifier.weight(1f).testTag("edit_college_code"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("District/Zone") },
                        modifier = Modifier.weight(1f).testTag("edit_college_district"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Coordinates Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("edit_college_lat"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("edit_college_lon"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Seating Capacity & Student Population
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = seatingCapacity,
                        onValueChange = { seatingCapacity = it },
                        label = { Text("Seating Capacity *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_college_capacity"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = studentPopulation,
                        onValueChange = { studentPopulation = it },
                        label = { Text("Maximum student count per day *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_college_students"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Branches
                OutlinedTextField(
                    value = branches,
                    onValueChange = { branches = it },
                    label = { Text("Branches (e.g. BA, MA, BSc, MSc, BCom, MCom, BEd, MEd, LLB, BALLB)") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_college_branches"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Campus Address") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_college_address"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // College Management Type
                Text(
                    text = "College Category / Management Type *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf("GOVERNMENT", "AIDED", "SELF FINANCE", "Autonomous")
                    for (t in types) {
                        val isSelected = collegeType.equals(t, ignoreCase = true)
                        val typeColor = when {
                            t.contains("GOV", true) -> Color(0xFF1D4ED8)
                            t.contains("AID", true) -> Color(0xFF059669)
                            t.contains("SELF", true) -> Color(0xFF9333EA)
                            else -> Color(0xFFD97706)
                        }
                        Surface(
                            onClick = { collegeType = t },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) typeColor else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) typeColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = t,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Is Exam Centre Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isExamCentre,
                        onCheckedChange = { isExamCentre = it },
                        modifier = Modifier.testTag("edit_college_is_exam_centre")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Designated University Exam Centre",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Validation error
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = AlertRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Save & Delete Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEditing && onDelete != null && collegeToEdit != null) {
                        OutlinedButton(
                            onClick = { onDelete(collegeToEdit) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            modifier = Modifier.testTag("delete_college_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        }
                    }

                    Button(
                        onClick = {
                            val latVal = latitude.toDoubleOrNull()
                            val lonVal = longitude.toDoubleOrNull()
                            val capVal = seatingCapacity.toIntOrNull()
                            val stdVal = studentPopulation.toIntOrNull()

                            if (name.isBlank() || code.isBlank()) {
                                validationError = "Name and Code are required."
                                return@Button
                            }
                            if (latVal == null || lonVal == null) {
                                validationError = "Valid latitude and longitude are required."
                                return@Button
                            }
                            if (capVal == null || stdVal == null) {
                                validationError = "Valid seating capacity and maximum student count per day are required."
                                return@Button
                            }

                            val entity = CollegeEntity(
                                id = collegeToEdit?.id ?: 0L,
                                name = name.trim(),
                                code = code.trim().uppercase(),
                                latitude = latVal,
                                longitude = lonVal,
                                branches = branches.trim(),
                                seatingCapacity = capVal,
                                studentPopulation = stdVal,
                                isExamCentre = isExamCentre,
                                address = address.trim(),
                                district = district.trim(),
                                collegeType = collegeType,
                                contactPhone = phone.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(entity)
                        },
                        modifier = Modifier.weight(1f).testTag("save_college_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditing) "Save Updates" else "Add College")
                    }
                }
            }
        }
    }
}
