package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CollegeEntity
import com.example.data.model.TransitBus
import com.example.data.model.TransitCalculationResult
import com.example.data.model.UserCredential
import com.example.data.parser.KmzParser
import com.example.data.parser.SpreadsheetParser
import com.example.data.repository.CollegeRepository
import com.example.ui.map.DistrictBoundary
import com.example.ui.map.DistrictBoundaryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SheetMode {
    NONE,
    COLLEGE_DETAIL,
    RADIUS_TOOL,
    EXAM_TOOL,
    ROUTE_TOOL,
    TRANSIT_TELEMETRY
}

class MainViewModel(
    private val repository: CollegeRepository
) : ViewModel() {

    // Auth State
    private val _currentUser = MutableStateFlow<UserCredential?>(null)
    val currentUser: StateFlow<UserCredential?> = _currentUser.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // College DB Flow
    val allColleges: StateFlow<List<CollegeEntity>> = repository.allColleges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBranch = MutableStateFlow("ALL")
    val selectedBranch: StateFlow<String> = _selectedBranch.asStateFlow()

    private val _selectedCollegeType = MutableStateFlow("ALL")
    val selectedCollegeType: StateFlow<String> = _selectedCollegeType.asStateFlow()

    private val _examCentresOnly = MutableStateFlow(false)
    val examCentresOnly: StateFlow<Boolean> = _examCentresOnly.asStateFlow()

    // Filtered Colleges
    val filteredColleges: StateFlow<List<CollegeEntity>> = combine(
        allColleges,
        _searchQuery,
        _selectedBranch,
        _selectedCollegeType,
        _examCentresOnly
    ) { colleges, query, branch, colType, examOnly ->
        colleges.filter { col ->
            val matchesQuery = if (query.isBlank()) true else {
                col.name.contains(query, ignoreCase = true) ||
                col.code.contains(query, ignoreCase = true) ||
                col.district.contains(query, ignoreCase = true) ||
                col.address.contains(query, ignoreCase = true)
            }
            val matchesBranch = col.matchesBranch(branch)
            val matchesType = col.matchesType(colType)
            val matchesExam = if (examOnly) col.isExamCentre else true
            matchesQuery && matchesBranch && matchesType && matchesExam
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Sheet Mode
    private val _activeSheetMode = MutableStateFlow(SheetMode.NONE)
    val activeSheetMode: StateFlow<SheetMode> = _activeSheetMode.asStateFlow()

    // Selected College (Map Pin Tap)
    private val _selectedCollege = MutableStateFlow<CollegeEntity?>(null)
    val selectedCollege: StateFlow<CollegeEntity?> = _selectedCollege.asStateFlow()

    // Radius Circle Tool State
    private val _radiusCenterCollege = MutableStateFlow<CollegeEntity?>(null)
    val radiusCenterCollege: StateFlow<CollegeEntity?> = _radiusCenterCollege.asStateFlow()

    private val _circleRadiusKm = MutableStateFlow(15.0)
    val circleRadiusKm: StateFlow<Double> = _circleRadiusKm.asStateFlow()

    private val _collegesInRadius = MutableStateFlow<List<Pair<CollegeEntity, Double>>>(emptyList())
    val collegesInRadius: StateFlow<List<Pair<CollegeEntity, Double>>> = _collegesInRadius.asStateFlow()

    // Exam Transit Tool State
    private val _selectedExamCentre = MutableStateFlow<CollegeEntity?>(null)
    val selectedExamCentre: StateFlow<CollegeEntity?> = _selectedExamCentre.asStateFlow()

    private val _selectedSecondaryColleges = MutableStateFlow<List<CollegeEntity>>(emptyList())
    val selectedSecondaryColleges: StateFlow<List<CollegeEntity>> = _selectedSecondaryColleges.asStateFlow()

    private val _transitResult = MutableStateFlow<TransitCalculationResult?>(null)
    val transitResult: StateFlow<TransitCalculationResult?> = _transitResult.asStateFlow()

    // Route Planner State
    private val _routeOriginCollege = MutableStateFlow<CollegeEntity?>(null)
    val routeOriginCollege: StateFlow<CollegeEntity?> = _routeOriginCollege.asStateFlow()

    private val _routeDestinationCollege = MutableStateFlow<CollegeEntity?>(null)
    val routeDestinationCollege: StateFlow<CollegeEntity?> = _routeDestinationCollege.asStateFlow()

    // Live Transit Fleet State
    val activeTransitFleets: StateFlow<List<TransitBus>> = repository.activeTransitFleets
    val transitAdvisoryAlert: StateFlow<String?> = repository.transitAdvisoryAlert

    // Dialog States
    private val _showUploadDialog = MutableStateFlow(false)
    val showUploadDialog: StateFlow<Boolean> = _showUploadDialog.asStateFlow()

    private val _uploadLoading = MutableStateFlow(false)
    val uploadLoading: StateFlow<Boolean> = _uploadLoading.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()

    private val _uploadIsError = MutableStateFlow(false)
    val uploadIsError: StateFlow<Boolean> = _uploadIsError.asStateFlow()

    private val _collegeToEdit = MutableStateFlow<CollegeEntity?>(null)
    val collegeToEdit: StateFlow<CollegeEntity?> = _collegeToEdit.asStateFlow()

    private val _showEditCollegeDialog = MutableStateFlow(false)
    val showEditCollegeDialog: StateFlow<Boolean> = _showEditCollegeDialog.asStateFlow()

    // KMZ District Boundary Layer State
    private val _kmzDistrictBoundaries = MutableStateFlow<List<DistrictBoundary>>(emptyList())
    val kmzDistrictBoundaries: StateFlow<List<DistrictBoundary>> = _kmzDistrictBoundaries.asStateFlow()

    private val _kmzFileName = MutableStateFlow<String?>(null)
    val kmzFileName: StateFlow<String?> = _kmzFileName.asStateFlow()

    private val _isKmzBoundaryVisible = MutableStateFlow(true)
    val isKmzBoundaryVisible: StateFlow<Boolean> = _isKmzBoundaryVisible.asStateFlow()

    private val _kmzLoading = MutableStateFlow(false)
    val kmzLoading: StateFlow<Boolean> = _kmzLoading.asStateFlow()

    private val _kmzStatusMessage = MutableStateFlow<String?>(null)
    val kmzStatusMessage: StateFlow<String?> = _kmzStatusMessage.asStateFlow()

    private val _kmzIsError = MutableStateFlow(false)
    val kmzIsError: StateFlow<Boolean> = _kmzIsError.asStateFlow()

    private val _showKmzDialog = MutableStateFlow(false)
    val showKmzDialog: StateFlow<Boolean> = _showKmzDialog.asStateFlow()

    private val _focusedDistrict = MutableStateFlow<DistrictBoundary?>(null)
    val focusedDistrict: StateFlow<DistrictBoundary?> = _focusedDistrict.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
        }
    }

    // Auth actions
    fun login(username: String, pass: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val user = repository.authenticateUser(username, pass)
            _authLoading.value = false
            if (user != null) {
                _currentUser.value = user
            } else {
                _authError.value = "Access Denied: Invalid credentials or unapproved account by creator."
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeSheetMode.value = SheetMode.NONE
    }

    // Search & Filter actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedBranch(branch: String) {
        _selectedBranch.value = branch
    }

    fun setSelectedCollegeType(type: String) {
        _selectedCollegeType.value = type
    }

    fun toggleExamCentresOnly(enabled: Boolean) {
        _examCentresOnly.value = enabled
    }

    // Map selection
    fun onSelectCollege(college: CollegeEntity) {
        _selectedCollege.value = college
        _activeSheetMode.value = SheetMode.COLLEGE_DETAIL
    }

    fun closeSheet() {
        _activeSheetMode.value = SheetMode.NONE
        _selectedCollege.value = null
    }

    // Radius Circle Tool actions
    fun openRadiusTool(initialCenter: CollegeEntity? = null) {
        val center = initialCenter ?: _selectedCollege.value ?: allColleges.value.firstOrNull()
        if (center != null) {
            _radiusCenterCollege.value = center
            recalculateRadiusColleges(center, _circleRadiusKm.value)
        }
        _activeSheetMode.value = SheetMode.RADIUS_TOOL
    }

    fun setRadiusCenter(college: CollegeEntity) {
        _radiusCenterCollege.value = college
        recalculateRadiusColleges(college, _circleRadiusKm.value)
    }

    fun setCircleRadiusKm(radius: Double) {
        _circleRadiusKm.value = radius
        _radiusCenterCollege.value?.let { center ->
            recalculateRadiusColleges(center, radius)
        }
    }

    private fun recalculateRadiusColleges(center: CollegeEntity, radiusKm: Double) {
        _collegesInRadius.value = repository.findCollegesWithinRadius(center, allColleges.value, radiusKm)
    }

    fun clearRadiusCircle() {
        _radiusCenterCollege.value = null
        _collegesInRadius.value = emptyList()
    }

    // Exam Transit Tool actions
    fun openExamTransitTool(examCentre: CollegeEntity? = null) {
        val venue = examCentre ?: _selectedCollege.value ?: allColleges.value.firstOrNull { it.isExamCentre }
        _selectedExamCentre.value = venue
        if (venue != null && _selectedSecondaryColleges.value.isEmpty()) {
            // Pick 2-3 nearby colleges as default secondary colleges
            val nearby = repository.findCollegesWithinRadius(venue, allColleges.value, 30.0)
                .map { it.first }
                .filter { it.id != venue.id }
                .take(3)
            _selectedSecondaryColleges.value = nearby
        }
        recalculateExamTransit()
        _activeSheetMode.value = SheetMode.EXAM_TOOL
    }

    fun setExamCentre(college: CollegeEntity) {
        _selectedExamCentre.value = college
        // Remove from secondary list if present
        _selectedSecondaryColleges.value = _selectedSecondaryColleges.value.filter { it.id != college.id }
        recalculateExamTransit()
    }

    fun toggleSecondaryCollege(college: CollegeEntity) {
        val current = _selectedSecondaryColleges.value.toMutableList()
        val index = current.indexOfFirst { it.id == college.id }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(college)
        }
        _selectedSecondaryColleges.value = current
        recalculateExamTransit()
    }

    private fun recalculateExamTransit() {
        val centre = _selectedExamCentre.value
        val secondary = _selectedSecondaryColleges.value
        if (centre != null && secondary.isNotEmpty()) {
            _transitResult.value = repository.calculateExamCentreTransit(centre, secondary)
        } else {
            _transitResult.value = null
        }
    }

    fun startTransitSimulation() {
        val centre = _selectedExamCentre.value ?: return
        val secondary = _selectedSecondaryColleges.value
        if (secondary.isNotEmpty()) {
            repository.startTransitSimulationForColleges(centre, secondary)
            _activeSheetMode.value = SheetMode.TRANSIT_TELEMETRY
        }
    }

    fun openTransitTelemetry() {
        _activeSheetMode.value = SheetMode.TRANSIT_TELEMETRY
    }

    fun stepSimulation() {
        repository.stepSimulationProgress()
    }

    // Route Planner actions
    fun openRoutePlanner(origin: CollegeEntity? = null) {
        val orig = origin ?: _selectedCollege.value ?: allColleges.value.firstOrNull()
        _routeOriginCollege.value = orig
        if (_routeDestinationCollege.value == null) {
            val dest = allColleges.value.firstOrNull { it.id != orig?.id && it.isExamCentre }
            _routeDestinationCollege.value = dest
        }
        _activeSheetMode.value = SheetMode.ROUTE_TOOL
    }

    fun setRouteOrigin(college: CollegeEntity) {
        _routeOriginCollege.value = college
    }

    fun setRouteDestination(college: CollegeEntity) {
        _routeDestinationCollege.value = college
    }

    fun swapRouteColleges() {
        val temp = _routeOriginCollege.value
        _routeOriginCollege.value = _routeDestinationCollege.value
        _routeDestinationCollege.value = temp
    }

    fun clearRoute() {
        _routeOriginCollege.value = null
        _routeDestinationCollege.value = null
    }

    // Spreadsheet Upload actions
    fun openUploadDialog() {
        _showUploadDialog.value = true
        _uploadStatus.value = null
        _uploadIsError.value = false
    }

    fun closeUploadDialog() {
        _showUploadDialog.value = false
        _uploadStatus.value = null
    }

    fun uploadSpreadsheet(context: Context, uri: Uri, replaceAll: Boolean) {
        viewModelScope.launch {
            _uploadLoading.value = true
            _uploadStatus.value = null
            _uploadIsError.value = false

            val result = SpreadsheetParser.parseSpreadsheet(context, uri)
            _uploadLoading.value = false

            result.onSuccess { colleges ->
                if (colleges.isEmpty()) {
                    _uploadStatus.value = "Parsed spreadsheet contained no valid college rows."
                    _uploadIsError.value = true
                } else {
                    repository.saveColleges(colleges, replaceAll)
                    _uploadStatus.value = "Successfully imported ${colleges.size} colleges to university map!"
                    _uploadIsError.value = false
                }
            }.onFailure { err ->
                _uploadStatus.value = "Failed to parse: ${err.localizedMessage ?: "Unknown format"}"
                _uploadIsError.value = true
            }
        }
    }

    // KMZ District Boundary Actions
    fun openKmzDialog() {
        _showKmzDialog.value = true
        _kmzStatusMessage.value = null
        _kmzIsError.value = false
    }

    fun closeKmzDialog() {
        _showKmzDialog.value = false
        _kmzStatusMessage.value = null
    }

    fun uploadKmz(context: Context, uri: Uri) {
        viewModelScope.launch {
            _kmzLoading.value = true
            _kmzStatusMessage.value = null
            _kmzIsError.value = false

            val result = KmzParser.parseKmzOrKml(context, uri)
            _kmzLoading.value = false

            result.onSuccess { parseResult ->
                if (parseResult.boundaries.isEmpty()) {
                    _kmzStatusMessage.value = "No district boundary polygons found in ${parseResult.fileName}."
                    _kmzIsError.value = true
                } else {
                    val updated = DistrictBoundaryManager.updateCollegeCounts(parseResult.boundaries, allColleges.value)
                    _kmzDistrictBoundaries.value = updated
                    _kmzFileName.value = parseResult.fileName
                    _isKmzBoundaryVisible.value = true
                    _kmzStatusMessage.value = "Successfully imported ${parseResult.boundaries.size} district boundaries from ${parseResult.fileName} (${parseResult.totalVertices} vertices)!"
                    _kmzIsError.value = false
                }
            }.onFailure { err ->
                _kmzStatusMessage.value = "Failed to load KMZ: ${err.localizedMessage ?: "Invalid file format"}"
                _kmzIsError.value = true
            }
        }
    }

    fun toggleKmzVisibility(visible: Boolean) {
        _isKmzBoundaryVisible.value = visible
    }

    fun clearKmzBoundaries() {
        _kmzDistrictBoundaries.value = emptyList()
        _kmzFileName.value = null
        _kmzStatusMessage.value = "District KMZ layer removed."
        _kmzIsError.value = false
        _focusedDistrict.value = null
    }

    fun focusDistrict(district: DistrictBoundary?) {
        _focusedDistrict.value = district
    }

    fun clearAllColleges() {
        viewModelScope.launch {
            repository.clearAllColleges()
            _activeSheetMode.value = SheetMode.NONE
            _selectedCollege.value = null
            _radiusCenterCollege.value = null
            _collegesInRadius.value = emptyList()
            _selectedExamCentre.value = null
            _selectedSecondaryColleges.value = emptyList()
            _transitResult.value = null
            _routeOriginCollege.value = null
            _routeDestinationCollege.value = null
        }
    }

    // College Add/Edit Actions (Future Updation)
    fun openAddCollegeDialog() {
        _collegeToEdit.value = null
        _showEditCollegeDialog.value = true
    }

    fun openEditCollegeDialog(college: CollegeEntity) {
        _collegeToEdit.value = college
        _showEditCollegeDialog.value = true
    }

    fun closeEditCollegeDialog() {
        _showEditCollegeDialog.value = false
        _collegeToEdit.value = null
    }

    fun saveCollege(college: CollegeEntity) {
        viewModelScope.launch {
            if (college.id == 0L) {
                repository.insertCollege(college)
            } else {
                repository.updateCollege(college)
            }
            closeEditCollegeDialog()
            _selectedCollege.value = college
        }
    }

    fun deleteCollege(college: CollegeEntity) {
        viewModelScope.launch {
            repository.deleteCollege(college)
            closeEditCollegeDialog()
            if (_selectedCollege.value?.id == college.id) {
                closeSheet()
            }
        }
    }

    companion object {
        fun provideFactory(repository: CollegeRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
        }
    }
}
