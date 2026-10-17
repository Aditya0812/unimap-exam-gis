package com.example.data.repository

import com.example.data.local.AuthDao
import com.example.data.local.CollegeDao
import com.example.data.model.CollegeEntity
import com.example.data.model.TransitBus
import com.example.data.model.TransitCalculationResult
import com.example.data.model.TransitCollegeLeg
import com.example.data.model.TransitStatusType
import com.example.data.model.UserCredential
import com.example.data.security.CryptoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CollegeRepository(
    private val collegeDao: CollegeDao,
    private val authDao: AuthDao
) {
    val allColleges: Flow<List<CollegeEntity>> = collegeDao.getAllColleges()
    val examCentres: Flow<List<CollegeEntity>> = collegeDao.getExamCentres()
    val selfCentres: Flow<List<CollegeEntity>> = collegeDao.getSelfCentres()

    // Live transit simulation state
    private val _activeTransitFleets = MutableStateFlow<List<TransitBus>>(emptyList())
    val activeTransitFleets: StateFlow<List<TransitBus>> = _activeTransitFleets.asStateFlow()

    private val _transitAdvisoryAlert = MutableStateFlow<String?>(
        "All academic transit convoys operating normally. Check exam hall seating capacity clearance."
    )
    val transitAdvisoryAlert: StateFlow<String?> = _transitAdvisoryAlert.asStateFlow()

    suspend fun initializeDefaultsIfNeeded() {
        // Initialize Default Allowed Credentials if empty
        if (authDao.getCount() == 0) {
            val adminSalt = CryptoUtils.generateSalt()
            val controllerSalt = CryptoUtils.generateSalt()
            val transitSalt = CryptoUtils.generateSalt()

            authDao.insertAll(
                listOf(
                    UserCredential(
                        username = "admin@university.edu",
                        passwordHash = CryptoUtils.hashPassword("UniAdmin@2026", adminSalt),
                        salt = adminSalt,
                        fullName = "Prof. Arvind Sharma (Registrar)",
                        role = "Master Administrator",
                        allowedByCreator = true
                    ),
                    UserCredential(
                        username = "controller@exams.edu",
                        passwordHash = CryptoUtils.hashPassword("ExamPass#2026", controllerSalt),
                        salt = controllerSalt,
                        fullName = "Dr. Neha Verma (Controller of Exams)",
                        role = "Exam Controller",
                        allowedByCreator = true
                    ),
                    UserCredential(
                        username = "transit@logistics.edu",
                        passwordHash = CryptoUtils.hashPassword("TransitSafe!2026", transitSalt),
                        salt = transitSalt,
                        fullName = "Rajesh Malhotra (Chief Transit Officer)",
                        role = "Transit Logistics Officer",
                        allowedByCreator = true
                    )
                )
            )
        }

        // Initialize Default Sample Colleges with Exam & Self Center pins if empty
        if (collegeDao.getCount() == 0) {
            seedDefaultColleges(replaceExisting = false)
        }
    }

    suspend fun seedDefaultColleges(replaceExisting: Boolean = false) {
        if (replaceExisting) {
            collegeDao.clearAll()
        }
        val defaults = listOf(
            CollegeEntity(
                name = "Delhi University North Campus Govt College",
                code = "DU-NC01",
                latitude = 28.6920,
                longitude = 77.2140,
                branches = "BSc, MSc, BCA, MCA, BA",
                seatingCapacity = 3400,
                studentPopulation = 2500,
                isExamCentre = true,
                isSelfCentre = false,
                address = "North Campus, University Enclave",
                district = "North Delhi",
                collegeType = "GOVERNMENT",
                contactPhone = "+91 11 2766 7011"
            ),
            CollegeEntity(
                name = "St. Stephen's Memorial Aided College",
                code = "SSMC-02",
                latitude = 28.6850,
                longitude = 77.2080,
                branches = "BA, MA, BSc, MSc, BCom",
                seatingCapacity = 1900,
                studentPopulation = 1600,
                isExamCentre = false,
                isSelfCentre = true,
                address = "University Road, Civil Lines",
                district = "North Delhi",
                collegeType = "AIDED",
                contactPhone = "+91 11 2766 7200"
            ),
            CollegeEntity(
                name = "Central Delhi Autonomous Institute of Law & Commerce",
                code = "CDAI-03",
                latitude = 28.6310,
                longitude = 77.2190,
                branches = "LLB, BALLB, BCom, MCom, BBA",
                seatingCapacity = 2900,
                studentPopulation = 2200,
                isExamCentre = true,
                isSelfCentre = true,
                address = "Barakhamba Road, Connaught Place",
                district = "Central Delhi",
                collegeType = "Autonomous",
                contactPhone = "+91 11 2331 4500"
            ),
            CollegeEntity(
                name = "South Delhi Govt Science & Technology Institute",
                code = "SDST-04",
                latitude = 28.5480,
                longitude = 77.1950,
                branches = "BSc, MSc, BCA, MCA",
                seatingCapacity = 3100,
                studentPopulation = 2400,
                isExamCentre = true,
                isSelfCentre = false,
                address = "Hauz Khas, Outer Ring Road",
                district = "South Delhi",
                collegeType = "GOVERNMENT",
                contactPhone = "+91 11 2659 1000"
            ),
            CollegeEntity(
                name = "Apex Self-Finance Degree College",
                code = "ASFC-05",
                latitude = 28.6300,
                longitude = 77.0850,
                branches = "BCA, MCA, BCom, MCom, BEd",
                seatingCapacity = 1700,
                studentPopulation = 1500,
                isExamCentre = false,
                isSelfCentre = true,
                address = "Janakpuri Institutional Area",
                district = "West Delhi",
                collegeType = "SELF FINANCE",
                contactPhone = "+91 11 2555 3300"
            ),
            CollegeEntity(
                name = "Rajdhani Autonomous College of Humanities",
                code = "RACH-06",
                latitude = 28.6520,
                longitude = 77.1250,
                branches = "BA, MA, BCom, MEd",
                seatingCapacity = 2200,
                studentPopulation = 1800,
                isExamCentre = false,
                isSelfCentre = true,
                address = "Raja Garden, Ring Road",
                district = "West Delhi",
                collegeType = "Autonomous",
                contactPhone = "+91 11 2593 1122"
            ),
            CollegeEntity(
                name = "Vivekananda Aided Institute of Arts & Science",
                code = "VIAS-07",
                latitude = 28.5360,
                longitude = 77.2580,
                branches = "BA, BSc, BCom, MA",
                seatingCapacity = 2600,
                studentPopulation = 1900,
                isExamCentre = true,
                isSelfCentre = false,
                address = "Kalkaji, Nehru Place Extension",
                district = "South East Delhi",
                collegeType = "AIDED",
                contactPhone = "+91 11 2643 8900"
            ),
            CollegeEntity(
                name = "Pragyan Self-Finance Technical Campus",
                code = "PSTC-08",
                latitude = 28.7050,
                longitude = 77.1050,
                branches = "BCom, BEd, MEd, BA",
                seatingCapacity = 1300,
                studentPopulation = 1100,
                isExamCentre = false,
                isSelfCentre = false,
                address = "Rohini Sector 14, Institutional Area",
                district = "North West Delhi",
                collegeType = "SELF FINANCE",
                contactPhone = "+91 11 2755 4400"
            ),
            CollegeEntity(
                name = "East Delhi Govt Girls Degree College",
                code = "EDGC-09",
                latitude = 28.6250,
                longitude = 77.2750,
                branches = "BA, MA, BSc, MSc, BEd",
                seatingCapacity = 1800,
                studentPopulation = 1400,
                isExamCentre = false,
                isSelfCentre = true,
                address = "Vivek Vihar, Shahdara",
                district = "East Delhi",
                collegeType = "GOVERNMENT",
                contactPhone = "+91 11 2215 6700"
            ),
            CollegeEntity(
                name = "Metro Aided Institute of Commerce & Management",
                code = "MAIC-10",
                latitude = 28.6680,
                longitude = 77.2900,
                branches = "BCom, MCom, BA, MA",
                seatingCapacity = 1200,
                studentPopulation = 950,
                isExamCentre = false,
                isSelfCentre = false,
                address = "Dilshad Garden, GT Road",
                district = "North East Delhi",
                collegeType = "AIDED",
                contactPhone = "+91 11 2228 9011"
            ),
            CollegeEntity(
                name = "Capital Autonomous Law & Policy Academy",
                code = "CALP-11",
                latitude = 28.5850,
                longitude = 77.2350,
                branches = "LLB, BALLB, BBA, BCom",
                seatingCapacity = 2700,
                studentPopulation = 2000,
                isExamCentre = true,
                isSelfCentre = false,
                address = "Lodhi Road, Institutional Area",
                district = "South Delhi",
                collegeType = "Autonomous",
                contactPhone = "+91 11 2461 5500"
            ),
            CollegeEntity(
                name = "Global Self-Finance Business Academy",
                code = "GSBA-12",
                latitude = 28.5720,
                longitude = 77.0680,
                branches = "BCA, MCA, BCom, MCom",
                seatingCapacity = 2400,
                studentPopulation = 1750,
                isExamCentre = true,
                isSelfCentre = true,
                address = "Dwarka Sector 9, Institutional Area",
                district = "South West Delhi",
                collegeType = "SELF FINANCE",
                contactPhone = "+91 11 2808 2211"
            )
        )
        collegeDao.insertAll(defaults)
    }

    suspend fun authenticateUser(username: String, passwordAttempt: String): UserCredential? {
        val user = authDao.getCredential(username.trim().lowercase()) ?: return null
        if (!user.allowedByCreator) return null
        val isValid = CryptoUtils.verifyPassword(passwordAttempt, user.salt, user.passwordHash)
        return if (isValid) user else null
    }

    suspend fun addAuthorizedCredential(username: String, rawPass: String, fullName: String, role: String): Boolean {
        val cleanUser = username.trim().lowercase()
        val salt = CryptoUtils.generateSalt()
        val hash = CryptoUtils.hashPassword(rawPass, salt)
        val cred = UserCredential(
            username = cleanUser,
            passwordHash = hash,
            salt = salt,
            fullName = fullName.ifBlank { cleanUser },
            role = role,
            allowedByCreator = true
        )
        authDao.insertCredential(cred)
        return true
    }

    suspend fun saveColleges(colleges: List<CollegeEntity>, replaceAll: Boolean = false) {
        if (replaceAll) {
            collegeDao.clearAll()
        }
        collegeDao.insertAll(colleges)
    }

    suspend fun insertCollege(college: CollegeEntity): Long {
        return collegeDao.insertCollege(college)
    }

    suspend fun updateCollege(college: CollegeEntity) {
        collegeDao.updateCollege(college)
    }

    suspend fun deleteCollege(college: CollegeEntity) {
        collegeDao.deleteCollege(college)
    }

    suspend fun clearAllColleges() {
        collegeDao.clearAll()
    }

    /**
     * Finds all colleges within a given radius (km) from a center college
     */
    fun findCollegesWithinRadius(
        center: CollegeEntity,
        allColleges: List<CollegeEntity>,
        radiusKm: Double
    ): List<Pair<CollegeEntity, Double>> {
        return allColleges.mapNotNull { col ->
            val dist = calculateHaversineDistanceKm(
                center.latitude, center.longitude,
                col.latitude, col.longitude
            )
            if (dist <= radiusKm) {
                col to dist
            } else {
                null
            }
        }.sortedBy { it.second }
    }

    /**
     * Calculates Exam Centre Seating Capacity vs Secondary College Student Population,
     * including road distance, bus requirements, convoy staggered dispatch, and gate clearance ETA.
     */
    fun calculateExamCentreTransit(
        examCentre: CollegeEntity,
        secondaryColleges: List<CollegeEntity>
    ): TransitCalculationResult {
        val totalStudents = secondaryColleges.sumOf { it.studentPopulation }
        val capacity = examCentre.seatingCapacity
        val isOverCapacity = totalStudents > capacity
        val overflow = if (isOverCapacity) totalStudents - capacity else 0
        val utilization = if (capacity > 0) (totalStudents.toFloat() / capacity.toFloat()) * 100f else 0f

        // Average 50 students per university chartered transit bus
        val totalBuses = ceil(totalStudents.toDouble() / 50.0).toInt().coerceAtLeast(1)

        val legs = secondaryColleges.map { col ->
            val directDist = calculateHaversineDistanceKm(
                col.latitude, col.longitude,
                examCentre.latitude, examCentre.longitude
            )
            // Road curvature factor: roads are ~1.28x geodesic distance
            val roadDist = directDist * 1.28
            // Average bus transit speed: 45 km/h in urban/suburban corridors
            val driveMinutes = ceil((roadDist / 45.0) * 60.0).toInt().coerceAtLeast(15)
            val busesNeeded = ceil(col.studentPopulation.toDouble() / 50.0).toInt().coerceAtLeast(1)

            // Stagger batch: 5 minutes per bus convoy
            val stagger = (busesNeeded - 1) * 4
            val departureOffset = driveMinutes + stagger + 40 // +40 mins for arrival & security check

            val hours = departureOffset / 60
            val mins = departureOffset % 60
            val formattedTime = if (hours > 0) "${hours}h ${mins}m before exam" else "$mins mins before exam"

            TransitCollegeLeg(
                college = col,
                students = col.studentPopulation,
                busesNeeded = busesNeeded,
                directDistanceKm = Math.round(directDist * 10.0) / 10.0,
                roadDistanceKm = Math.round(roadDist * 10.0) / 10.0,
                estimatedDriveMinutes = driveMinutes,
                recommendedBatchDeparture = formattedTime
            )
        }

        val maxRoadDist = legs.maxOfOrNull { it.roadDistanceKm } ?: 0.0
        val maxDriveMins = legs.maxOfOrNull { it.estimatedDriveMinutes } ?: 0

        // Ingress clearance: Biometric scanning gates throughput (~40 students/min across university entrance gates)
        val gateIngressMinutes = ceil(totalStudents.toDouble() / 40.0).toInt().coerceIn(15, 60)
        val convoyStagger = (totalBuses * 3).coerceAtMost(45)
        val totalTransitMins = maxDriveMins + convoyStagger + gateIngressMinutes

        val totalHours = totalTransitMins / 60
        val remainingMins = totalTransitMins % 60
        val recommendedSchedule = if (totalHours > 0) {
            "${totalHours}h ${remainingMins}m before exam start"
        } else {
            "$remainingMins mins before exam start"
        }

        return TransitCalculationResult(
            examCentre = examCentre,
            secondaryColleges = secondaryColleges,
            totalStudents = totalStudents,
            seatingCapacity = capacity,
            isOverCapacity = isOverCapacity,
            overflowCount = overflow,
            capacityUtilizationPercent = Math.round(utilization * 10.0) / 10.0f,
            totalBusesRequired = totalBuses,
            roadDistanceKm = Math.round(maxRoadDist * 10.0) / 10.0,
            roadTravelTimeMinutes = maxDriveMins,
            convoyStaggerMinutes = convoyStagger,
            securityIngressMinutes = gateIngressMinutes,
            totalTransitMinutes = totalTransitMins,
            recommendedDepartureBeforeExam = recommendedSchedule,
            legDetails = legs
        )
    }

    /**
     * Initializes simulated real-time transit bus status updates
     */
    fun startTransitSimulationForColleges(
        examCentre: CollegeEntity,
        secondaryColleges: List<CollegeEntity>
    ) {
        val fleets = mutableListOf<TransitBus>()
        var busNumber = 101

        secondaryColleges.forEach { col ->
            val directDist = calculateHaversineDistanceKm(col.latitude, col.longitude, examCentre.latitude, examCentre.longitude)
            val roadDist = directDist * 1.28
            val busesForCol = ceil(col.studentPopulation.toDouble() / 50.0).toInt().coerceAtMost(4)

            for (i in 1..busesForCol) {
                val progress = (0.2f + (i * 0.18f)).coerceAtMost(0.95f)
                val eta = ceil(((1.0 - progress) * (roadDist / 45.0) * 60.0)).toInt().coerceAtLeast(5)
                val status = when {
                    progress > 0.85f -> TransitStatusType.SECURITY_CHECK
                    progress > 0.70f -> TransitStatusType.TOLL_PLAZA
                    progress > 0.45f -> TransitStatusType.IN_TRANSIT
                    else -> TransitStatusType.BOARDING
                }
                fleets.add(
                    TransitBus(
                        busId = "BUS-UN#$busNumber",
                        originCollegeId = col.id,
                        originCollegeName = col.name,
                        destinationExamCentreId = examCentre.id,
                        destinationExamCentreName = examCentre.name,
                        studentCount = (col.studentPopulation / busesForCol).coerceAtMost(52),
                        busCapacity = 52,
                        currentSpeedKmh = if (status == TransitStatusType.IN_TRANSIT) (48..56).random() else if (status == TransitStatusType.TOLL_PLAZA) 18 else 0,
                        progressPercent = progress,
                        etaMinutes = eta,
                        status = status,
                        currentHighway = "State Arterial Corridor NH-${(busNumber % 9) + 40}",
                        remarks = "Convoy batch #$i | Radio link confirmed"
                    )
                )
                busNumber++
            }
        }
        _activeTransitFleets.value = fleets
    }

    fun updateTransitAdvisory(message: String) {
        _transitAdvisoryAlert.value = message
    }

    fun stepSimulationProgress() {
        val current = _activeTransitFleets.value
        val updated = current.map { bus ->
            val newProgress = (bus.progressPercent + 0.08f).coerceAtMost(1.0f)
            val newEta = (bus.etaMinutes - 3).coerceAtLeast(0)
            val newStatus = when {
                newProgress >= 1.0f -> TransitStatusType.ARRIVED
                newProgress > 0.85f -> TransitStatusType.SECURITY_CHECK
                newProgress > 0.70f -> TransitStatusType.TOLL_PLAZA
                newProgress > 0.30f -> TransitStatusType.IN_TRANSIT
                else -> TransitStatusType.BOARDING
            }
            bus.copy(
                progressPercent = newProgress,
                etaMinutes = newEta,
                status = newStatus,
                currentSpeedKmh = if (newProgress >= 1.0f) 0 else (42..58).random()
            )
        }
        _activeTransitFleets.value = updated
    }

    companion object {
        fun calculateHaversineDistanceKm(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val r = 6371.0 // Earth radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
