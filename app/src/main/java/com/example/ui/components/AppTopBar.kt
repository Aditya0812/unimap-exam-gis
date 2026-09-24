package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserCredential
import com.example.ui.theme.ExamGold
import com.example.ui.theme.PrimaryNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentUser: UserCredential?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedBranch: String,
    onSelectBranch: (String) -> Unit,
    selectedCollegeType: String = "ALL",
    onSelectCollegeType: (String) -> Unit = {},
    examCentresOnly: Boolean,
    onToggleExamCentresOnly: (Boolean) -> Unit,
    onOpenUploadDialog: () -> Unit,
    onOpenKmzDialog: () -> Unit = {},
    hasKmzLoaded: Boolean = false,
    onOpenAddCollegeDialog: () -> Unit,
    onLogout: () -> Unit,
    totalCollegesCount: Int,
    filteredCollegesCount: Int,
    modifier: Modifier = Modifier
) {
    val branchOptions = listOf(
        "ALL", "BA", "MA", "BSc", "MSc", "BCom", "MCom", "BEd", "MEd", "LLB", "BALLB", "BCA", "MCA"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Top Bar Row
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "UniMap",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "GIS & Transit",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Affiliated Colleges: $filteredCollegesCount / $totalCollegesCount active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Upload Excel Button
                    IconButton(
                        onClick = onOpenUploadDialog,
                        modifier = Modifier.testTag("topbar_upload_excel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Upload Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Insert KMZ District Boundary Layer Button
                    IconButton(
                        onClick = onOpenKmzDialog,
                        modifier = Modifier.testTag("topbar_kmz_boundary_button")
                    ) {
                        if (hasKmzLoaded) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "District KMZ Boundary Layer",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "District KMZ Boundary Layer",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    // Add College (+) Button for future updates
                    IconButton(
                        onClick = onOpenAddCollegeDialog,
                        modifier = Modifier.testTag("topbar_add_college_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add College",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("topbar_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Search Bar & Filter Options Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .widthIn(min = 220.dp, max = 290.dp)
                        .testTag("search_colleges_textfield"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    placeholder = { Text("Search college, code, district...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // College Category / Management Type Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val types = listOf(
                        Triple("ALL", "All Types", Color(0xFF475569)),
                        Triple("GOVERNMENT", "Government", Color(0xFF1D4ED8)),
                        Triple("AIDED", "Aided", Color(0xFF059669)),
                        Triple("SELF FINANCE", "Self Finance", Color(0xFF9333EA)),
                        Triple("Autonomous", "Autonomous", Color(0xFFD97706))
                    )
                    types.forEach { (typeKey, typeLabel, color) ->
                        val isSelected = selectedCollegeType.equals(typeKey, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCollegeType(typeKey) },
                            label = { Text(typeLabel, fontSize = 12.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(color, CircleShape)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color
                            ),
                            modifier = Modifier.testTag("filter_type_$typeKey")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Branch Filters & Exam Centre Toggle Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exam Centres Only filter
                    FilterChip(
                        selected = examCentresOnly,
                        onClick = { onToggleExamCentresOnly(!examCentresOnly) },
                        label = { Text("Exam Centres Only") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ExamGold, CircleShape)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExamGold.copy(alpha = 0.2f),
                            selectedLabelColor = ExamGold
                        ),
                        modifier = Modifier.testTag("filter_exam_centres_only")
                    )

                    // Branch Chips
                    branchOptions.forEach { branch ->
                        val isSelected = selectedBranch.equals(branch, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectBranch(branch) },
                            label = { Text(branch) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_branch_$branch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
