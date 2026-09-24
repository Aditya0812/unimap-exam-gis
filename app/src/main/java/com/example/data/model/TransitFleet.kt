package com.example.data.model

data class TransitBus(
    val busId: String,
    val originCollegeId: Long,
    val originCollegeName: String,
    val destinationExamCentreId: Long,
    val destinationExamCentreName: String,
    val studentCount: Int,
    val busCapacity: Int = 50,
    val currentSpeedKmh: Int,
    val progressPercent: Float, // 0.0 to 1.0
    val etaMinutes: Int,
    val status: TransitStatusType,
    val currentHighway: String,
    val remarks: String
)

enum class TransitStatusType(val label: String, val isDelay: Boolean = false) {
    SCHEDULED("Scheduled"),
    BOARDING("Boarding Students"),
    IN_TRANSIT("En Route (Normal)"),
    CONGESTION("Moderate Traffic", isDelay = true),
    TOLL_PLAZA("Toll Plaza Clearance"),
    ARRIVED("Arrived at Centre"),
    SECURITY_CHECK("Frisking & Ingress")
}

data class TransitCalculationResult(
    val examCentre: CollegeEntity,
    val secondaryColleges: List<CollegeEntity>,
    val totalStudents: Int,
    val seatingCapacity: Int,
    val isOverCapacity: Boolean,
    val overflowCount: Int,
    val capacityUtilizationPercent: Float,
    val totalBusesRequired: Int,
    val roadDistanceKm: Double,
    val roadTravelTimeMinutes: Int,
    val convoyStaggerMinutes: Int,
    val securityIngressMinutes: Int,
    val totalTransitMinutes: Int,
    val recommendedDepartureBeforeExam: String,
    val legDetails: List<TransitCollegeLeg>
)

data class TransitCollegeLeg(
    val college: CollegeEntity,
    val students: Int,
    val busesNeeded: Int,
    val directDistanceKm: Double,
    val roadDistanceKm: Double,
    val estimatedDriveMinutes: Int,
    val recommendedBatchDeparture: String
)
