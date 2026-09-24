package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollegeType(
    val title: String,
    val tag: String,
    val colorHex: Long
) {
    GOVERNMENT("Government", "GOVT", 0xFF1D4ED8),     // Royal Blue
    AIDED("Aided", "AIDED", 0xFF059669),             // Emerald Green
    SELF_FINANCE("Self Finance", "SFI", 0xFF9333EA), // Vivid Purple
    AUTONOMOUS("Autonomous", "AUTO", 0xFFD97706);    // Rich Amber Gold

    companion object {
        fun fromString(str: String?): CollegeType {
            if (str.isNullOrBlank()) return GOVERNMENT
            val s = str.trim().uppercase()
            return when {
                s.contains("AUTO") -> AUTONOMOUS
                s.contains("AIDED") || s.contains("GRANT") -> AIDED
                s.contains("SELF") || s.contains("SFI") || s.contains("PRIVATE") || s.contains("UNAIDED") -> SELF_FINANCE
                s.contains("GOV") -> GOVERNMENT
                else -> GOVERNMENT
            }
        }
    }
}

@Entity(tableName = "colleges")
data class CollegeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val latitude: Double,
    val longitude: Double,
    val branches: String, // Comma-separated: "BA, MA, BSc, MSc, BCom, MCom, BEd, MEd, LLB, BALLB"
    val seatingCapacity: Int,
    val studentPopulation: Int, // Maximum student count per day
    val isExamCentre: Boolean,
    val address: String,
    val contactPhone: String = "",
    val contactEmail: String = "",
    val district: String = "",
    val collegeType: String = "GOVERNMENT", // GOVERNMENT, AIDED, SELF FINANCE, Autonomous
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Alias matching user specification: "Maximum student count per day" */
    val maxStudentCountPerDay: Int
        get() = studentPopulation

    val managementType: CollegeType
        get() = CollegeType.fromString(collegeType)

    val branchList: List<String>
        get() = branches.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun matchesBranch(branchQuery: String): Boolean {
        if (branchQuery.equals("ALL", ignoreCase = true) || branchQuery.isBlank()) return true
        return branchList.any { it.equals(branchQuery, ignoreCase = true) }
    }

    fun matchesType(typeQuery: String): Boolean {
        if (typeQuery.equals("ALL", ignoreCase = true) || typeQuery.isBlank()) return true
        return managementType.title.equals(typeQuery, ignoreCase = true) ||
                managementType.name.equals(typeQuery, ignoreCase = true)
    }
}
