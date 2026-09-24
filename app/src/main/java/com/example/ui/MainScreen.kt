package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.auth.LoginScreen
import com.example.ui.components.AppTopBar
import com.example.ui.components.CollegeDetailBottomSheet
import com.example.ui.components.CollegeEditDialog
import com.example.ui.components.KmzBoundaryDialog
import com.example.ui.components.SpreadsheetUploadDialog
import com.example.ui.exam.ExamTransitToolSheet
import com.example.ui.map.InteractiveMapView
import com.example.ui.radius.RadiusCircleToolSheet
import com.example.ui.route.RoutePlannerSheet
import com.example.ui.transit.TransitStatusDashboardSheet

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    // If not authenticated, display the encrypted login portal
    if (currentUser == null) {
        LoginScreen(
            isLoading = authLoading,
            errorMessage = authError,
            onLogin = { u, p -> viewModel.login(u, p) },
            modifier = modifier
        )
        return
    }

    val allColleges by viewModel.allColleges.collectAsState()
    val filteredColleges by viewModel.filteredColleges.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBranch by viewModel.selectedBranch.collectAsState()
    val selectedCollegeType by viewModel.selectedCollegeType.collectAsState()
    val examCentresOnly by viewModel.examCentresOnly.collectAsState()

    val activeSheetMode by viewModel.activeSheetMode.collectAsState()
    val selectedCollege by viewModel.selectedCollege.collectAsState()

    val radiusCenterCollege by viewModel.radiusCenterCollege.collectAsState()
    val circleRadiusKm by viewModel.circleRadiusKm.collectAsState()
    val collegesInRadius by viewModel.collegesInRadius.collectAsState()

    val selectedExamCentre by viewModel.selectedExamCentre.collectAsState()
    val selectedSecondaryColleges by viewModel.selectedSecondaryColleges.collectAsState()
    val transitResult by viewModel.transitResult.collectAsState()

    val routeOriginCollege by viewModel.routeOriginCollege.collectAsState()
    val routeDestinationCollege by viewModel.routeDestinationCollege.collectAsState()

    val activeTransitFleets by viewModel.activeTransitFleets.collectAsState()
    val transitAdvisoryAlert by viewModel.transitAdvisoryAlert.collectAsState()

    val showUploadDialog by viewModel.showUploadDialog.collectAsState()
    val uploadLoading by viewModel.uploadLoading.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    val uploadIsError by viewModel.uploadIsError.collectAsState()

    val showEditCollegeDialog by viewModel.showEditCollegeDialog.collectAsState()
    val collegeToEdit by viewModel.collegeToEdit.collectAsState()

    // KMZ District Boundary States
    val kmzDistrictBoundaries by viewModel.kmzDistrictBoundaries.collectAsState()
    val kmzFileName by viewModel.kmzFileName.collectAsState()
    val isKmzBoundaryVisible by viewModel.isKmzBoundaryVisible.collectAsState()
    val kmzLoading by viewModel.kmzLoading.collectAsState()
    val kmzStatusMessage by viewModel.kmzStatusMessage.collectAsState()
    val kmzIsError by viewModel.kmzIsError.collectAsState()
    val showKmzDialog by viewModel.showKmzDialog.collectAsState()
    val focusedDistrict by viewModel.focusedDistrict.collectAsState()

    var isMapFullScreen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            if (!isMapFullScreen) {
                AppTopBar(
                    currentUser = currentUser,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    selectedBranch = selectedBranch,
                    onSelectBranch = { viewModel.setSelectedBranch(it) },
                    selectedCollegeType = selectedCollegeType,
                    onSelectCollegeType = { viewModel.setSelectedCollegeType(it) },
                    examCentresOnly = examCentresOnly,
                    onToggleExamCentresOnly = { viewModel.toggleExamCentresOnly(it) },
                    onOpenUploadDialog = { viewModel.openUploadDialog() },
                    onOpenKmzDialog = { viewModel.openKmzDialog() },
                    hasKmzLoaded = kmzDistrictBoundaries.isNotEmpty(),
                    onOpenAddCollegeDialog = { viewModel.openAddCollegeDialog() },
                    onLogout = { viewModel.logout() },
                    totalCollegesCount = allColleges.size,
                    filteredCollegesCount = filteredColleges.size
                )
            }
        },
        bottomBar = {
            if (!isMapFullScreen) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                // 1. Map View
                NavigationBarItem(
                    selected = activeSheetMode == SheetMode.NONE,
                    onClick = { viewModel.closeSheet() },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") },
                    modifier = Modifier.testTag("nav_map_tab")
                )

                // 2. Radius Tool
                NavigationBarItem(
                    selected = activeSheetMode == SheetMode.RADIUS_TOOL,
                    onClick = { viewModel.openRadiusTool() },
                    icon = { Icon(Icons.Default.Radar, contentDescription = "Radius") },
                    label = { Text("Radius") },
                    modifier = Modifier.testTag("nav_radius_tab")
                )

                // 3. Transit ETA & Exam Logistics
                NavigationBarItem(
                    selected = activeSheetMode == SheetMode.EXAM_TOOL,
                    onClick = { viewModel.openExamTransitTool() },
                    icon = { Icon(Icons.Default.School, contentDescription = "Transit ETA") },
                    label = { Text("Transit ETA") },
                    modifier = Modifier.testTag("nav_exam_transit_tab")
                )

                // 4. Route Planner
                NavigationBarItem(
                    selected = activeSheetMode == SheetMode.ROUTE_TOOL,
                    onClick = { viewModel.openRoutePlanner() },
                    icon = { Icon(Icons.Default.AltRoute, contentDescription = "Routes") },
                    label = { Text("Route") },
                    modifier = Modifier.testTag("nav_route_tab")
                )

                // 5. Live Fleet & Telemetry
                NavigationBarItem(
                    selected = activeSheetMode == SheetMode.TRANSIT_TELEMETRY,
                    onClick = { viewModel.openTransitTelemetry() },
                    icon = {
                        if (activeTransitFleets.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge { Text("${activeTransitFleets.size}") }
                                }
                            ) {
                                Icon(Icons.Default.DirectionsBus, contentDescription = "Fleet")
                            }
                        } else {
                            Icon(Icons.Default.DirectionsBus, contentDescription = "Fleet")
                        }
                    },
                    label = { Text("Fleet") },
                    modifier = Modifier.testTag("nav_fleet_tab")
                )
            }
        }
    }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive Map Canvas (Offline Vector Topo & Corridors)
            InteractiveMapView(
                colleges = allColleges,
                filteredColleges = filteredColleges,
                selectedCollege = selectedCollege,
                radiusCenterCollege = radiusCenterCollege,
                circleRadiusKm = if (radiusCenterCollege != null) circleRadiusKm else null,
                collegesInRadius = collegesInRadius.map { it.first },
                routeOriginCollege = routeOriginCollege,
                routeDestinationCollege = routeDestinationCollege,
                activeBuses = activeTransitFleets,
                kmzBoundaries = kmzDistrictBoundaries,
                isKmzVisible = isKmzBoundaryVisible,
                focusedDistrict = focusedDistrict,
                isFullScreen = isMapFullScreen,
                onToggleFullScreen = { isMapFullScreen = !isMapFullScreen },
                onOpenKmzDialog = { viewModel.openKmzDialog() },
                onSelectCollege = { viewModel.onSelectCollege(it) },
                modifier = Modifier.fillMaxSize()
            )

            // Empty State overlay when no spreadsheet has been uploaded yet
            if (allColleges.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Upload College Dataset",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please upload an Excel (.xlsx / .xls) or CSV file with affiliated colleges to visualize and navigate them on the map.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.openUploadDialog() },
                            modifier = Modifier.testTag("empty_state_upload_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Excel File")
                        }
                    }
                }
            }

            // Dynamic Bottom Sheets based on activeSheetMode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // College Detail Sheet
                AnimatedVisibility(
                    visible = activeSheetMode == SheetMode.COLLEGE_DETAIL && selectedCollege != null,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    selectedCollege?.let { college ->
                        CollegeDetailBottomSheet(
                            college = college,
                            onDismiss = { viewModel.closeSheet() },
                            onSetAsRadiusCenter = { viewModel.openRadiusTool(it) },
                            onSetAsExamCentre = { viewModel.openExamTransitTool(it) },
                            onPlanRoute = { viewModel.openRoutePlanner(it) },
                            onEditCollege = { viewModel.openEditCollegeDialog(it) },
                            onDeleteCollege = { viewModel.deleteCollege(it) }
                        )
                    }
                }

                // Radius Circle Tool Sheet
                AnimatedVisibility(
                    visible = activeSheetMode == SheetMode.RADIUS_TOOL,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    RadiusCircleToolSheet(
                        colleges = allColleges,
                        selectedCenterCollege = radiusCenterCollege,
                        onSelectCenterCollege = { viewModel.setRadiusCenter(it) },
                        currentRadiusKm = circleRadiusKm,
                        onSelectRadiusKm = { viewModel.setCircleRadiusKm(it) },
                        collegesInRadius = collegesInRadius,
                        onCollegeClick = { viewModel.onSelectCollege(it) },
                        onClearRadiusCircle = { viewModel.clearRadiusCircle() },
                        onClose = { viewModel.closeSheet() }
                    )
                }

                // Exam Transit Tool Sheet
                AnimatedVisibility(
                    visible = activeSheetMode == SheetMode.EXAM_TOOL,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    ExamTransitToolSheet(
                        colleges = allColleges,
                        selectedExamCentre = selectedExamCentre,
                        onSelectExamCentre = { viewModel.setExamCentre(it) },
                        selectedSecondaryColleges = selectedSecondaryColleges,
                        onToggleSecondaryCollege = { viewModel.toggleSecondaryCollege(it) },
                        transitResult = transitResult,
                        onStartTransitSimulation = { viewModel.startTransitSimulation() },
                        onClose = { viewModel.closeSheet() }
                    )
                }

                // Route Planner Sheet
                AnimatedVisibility(
                    visible = activeSheetMode == SheetMode.ROUTE_TOOL,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    RoutePlannerSheet(
                        colleges = allColleges,
                        originCollege = routeOriginCollege,
                        destinationCollege = routeDestinationCollege,
                        onSelectOrigin = { viewModel.setRouteOrigin(it) },
                        onSelectDestination = { viewModel.setRouteDestination(it) },
                        onSwapColleges = { viewModel.swapRouteColleges() },
                        onClearRoute = { viewModel.clearRoute() },
                        onClose = { viewModel.closeSheet() }
                    )
                }

                // Real-time Transit Telemetry Sheet
                AnimatedVisibility(
                    visible = activeSheetMode == SheetMode.TRANSIT_TELEMETRY,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    TransitStatusDashboardSheet(
                        activeBuses = activeTransitFleets,
                        advisoryAlert = transitAdvisoryAlert,
                        onStepSimulation = { viewModel.stepSimulation() },
                        onClose = { viewModel.closeSheet() }
                    )
                }
            }

            // Spreadsheet Upload Dialog
            if (showUploadDialog) {
                SpreadsheetUploadDialog(
                    isLoading = uploadLoading,
                    statusMessage = uploadStatus,
                    isError = uploadIsError,
                    onUploadUri = { uri, replaceAll ->
                        viewModel.uploadSpreadsheet(context, uri, replaceAll)
                    },
                    onDismiss = { viewModel.closeUploadDialog() }
                )
            }

            // College Add/Edit Dialog (Future Updation)
            if (showEditCollegeDialog) {
                CollegeEditDialog(
                    collegeToEdit = collegeToEdit,
                    onSave = { entity -> viewModel.saveCollege(entity) },
                    onDelete = { entity -> viewModel.deleteCollege(entity) },
                    onDismiss = { viewModel.closeEditCollegeDialog() }
                )
            }

            // KMZ District Boundary & Layer Dialog
            if (showKmzDialog) {
                KmzBoundaryDialog(
                    isOpen = showKmzDialog,
                    kmzFileName = kmzFileName,
                    boundaries = kmzDistrictBoundaries,
                    isVisible = isKmzBoundaryVisible,
                    isLoading = kmzLoading,
                    statusMessage = kmzStatusMessage,
                    isError = kmzIsError,
                    onUploadKmz = { ctx, uri -> viewModel.uploadKmz(ctx, uri) },
                    onToggleVisibility = { viewModel.toggleKmzVisibility(it) },
                    onClearKmz = { viewModel.clearKmzBoundaries() },
                    onZoomToDistrict = { district -> viewModel.focusDistrict(district) },
                    onZoomToAllKmz = { viewModel.focusDistrict(null) },
                    onDismiss = { viewModel.closeKmzDialog() }
                )
            }
        }
    }
}
