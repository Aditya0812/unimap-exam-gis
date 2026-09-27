package com.example

import com.example.data.model.CollegeEntity
import com.example.data.parser.SpreadsheetParser
import com.example.data.repository.CollegeRepository
import com.example.data.security.CryptoUtils
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class ExampleUnitTest {

  @Test
  fun cryptoUtils_hashesAndVerifiesPasswordCorrectly() {
    val salt = CryptoUtils.generateSalt()
    val password = "SecretUniversityKey#2026"
    val hash = CryptoUtils.hashPassword(password, salt)

    assertTrue(CryptoUtils.verifyPassword(password, salt, hash))
    assertFalse(CryptoUtils.verifyPassword("WrongPassword123", salt, hash))
  }

  @Test
  fun haversineDistance_calculatesDistanceAccurately() {
    // Connaught Place (28.6315, 77.2167) to India Gate (28.6129, 77.2295) ~ 2.4 km
    val dist = CollegeRepository.calculateHaversineDistanceKm(
      28.6315, 77.2167,
      28.6129, 77.2295
    )
    assertTrue("Distance should be around 2.4km, got $dist", dist in 2.0..3.0)
  }

  @Test
  fun spreadsheetParser_parsesCsvCorrectly() {
    val csvData = """
      College Name,Code,Latitude,Longitude,Branches,Seating Capacity,Student Population,Is Exam Centre,Address,District
      Apex Engineering,APEX-01,28.6139,77.2090,"CSE, ECE",2500,1800,Yes,Main Campus,Central
      Metro Institute,METRO-02,28.5355,77.3910,"ME, Civil",1200,950,No,East Campus,East
    """.trimIndent()

    val stream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
    val parseResult = SpreadsheetParser.parseCsv(stream)
    assertTrue(parseResult.isSuccess)
    val colleges = parseResult.getOrThrow()

    assertEquals(2, colleges.size)
    assertEquals("Apex Engineering", colleges[0].name)
    assertEquals("APEX-01", colleges[0].code)
    assertEquals(2500, colleges[0].seatingCapacity)
    assertTrue(colleges[0].isExamCentre)
    assertEquals(28.6139, colleges[0].latitude, 0.0001)
  }

  @Test
  fun transitCalculation_computesBusRequirementsAndCapacityAccurately() {
    val examCentre = CollegeEntity(
      id = 1,
      name = "Central Exam Centre",
      code = "EXAM-01",
      latitude = 28.6139,
      longitude = 77.2090,
      branches = "CSE, ECE",
      seatingCapacity = 2000,
      studentPopulation = 1500,
      isExamCentre = true,
      address = "Central Campus, North Delhi"
    )

    val secondary1 = CollegeEntity(
      id = 2,
      name = "Sub College 1",
      code = "SUB-01",
      latitude = 28.5355,
      longitude = 77.3910,
      branches = "ME, Civil",
      seatingCapacity = 1000,
      studentPopulation = 600,
      isExamCentre = false,
      address = "South Campus, Noida"
    )

    val secondary2 = CollegeEntity(
      id = 3,
      name = "Sub College 2",
      code = "SUB-02",
      latitude = 28.7041,
      longitude = 77.1025,
      branches = "EE, IT",
      seatingCapacity = 800,
      studentPopulation = 500,
      isExamCentre = false,
      address = "West Campus, Rohini"
    )

    val dummyCollegeDao = object : com.example.data.local.CollegeDao {
      override fun getAllColleges() = kotlinx.coroutines.flow.flowOf(emptyList<CollegeEntity>())
      override fun getExamCentres() = kotlinx.coroutines.flow.flowOf(emptyList<CollegeEntity>())
      override suspend fun getCollegeById(id: Long): CollegeEntity? = null
      override suspend fun insertCollege(college: CollegeEntity) = 1L
      override suspend fun insertAll(colleges: List<CollegeEntity>) = Unit
      override suspend fun updateCollege(college: CollegeEntity) = Unit
      override suspend fun deleteCollege(college: CollegeEntity) = Unit
      override suspend fun deleteCollegeById(id: Long) = Unit
      override suspend fun clearAll() = Unit
      override suspend fun getCount() = 0
    }

    val dummyAuthDao = object : com.example.data.local.AuthDao {
      override suspend fun getCredential(username: String): com.example.data.model.UserCredential? = null
      override fun getAllCredentials() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.model.UserCredential>())
      override suspend fun insertCredential(credential: com.example.data.model.UserCredential) = Unit
      override suspend fun insertAll(credentials: List<com.example.data.model.UserCredential>) = Unit
      override suspend fun updateCredential(credential: com.example.data.model.UserCredential) = Unit
      override suspend fun setAllowedStatus(username: String, allowed: Boolean) = Unit
      override suspend fun getCount() = 0
    }

    val repo = CollegeRepository(
      collegeDao = dummyCollegeDao,
      authDao = dummyAuthDao
    )

    val result = repo.calculateExamCentreTransit(examCentre, listOf(secondary1, secondary2))

    // Total incoming students = 600 + 500 = 1100
    assertEquals(1100, result.totalStudents)
    assertEquals(2000, result.seatingCapacity)
    assertFalse(result.isOverCapacity)
    assertEquals(0, result.overflowCount)
    // 1100 students / 50 per bus = 22 buses
    assertEquals(22, result.totalBusesRequired)
    assertTrue(result.roadDistanceKm > 0)
    assertTrue(result.totalTransitMinutes > 0)
  }

  @Test
  fun districtBoundaryManager_computesPolygonsPerDistrict() {
    val colleges = listOf(
      CollegeEntity(
        id = 1, name = "College A", code = "A1",
        latitude = 25.31, longitude = 82.97,
        branches = "BA, BSc", seatingCapacity = 500, studentPopulation = 300,
        isExamCentre = true, address = "Varanasi", district = "Varanasi",
        collegeType = "GOVERNMENT"
      ),
      CollegeEntity(
        id = 2, name = "College B", code = "B1",
        latitude = 25.35, longitude = 83.01,
        branches = "BCom, MCom", seatingCapacity = 400, studentPopulation = 250,
        isExamCentre = false, address = "Varanasi", district = "Varanasi",
        collegeType = "AIDED"
      ),
      CollegeEntity(
        id = 3, name = "College C", code = "C1",
        latitude = 25.43, longitude = 81.84,
        branches = "BA, MA", seatingCapacity = 600, studentPopulation = 400,
        isExamCentre = false, address = "Prayagraj", district = "Prayagraj",
        collegeType = "SELF FINANCE"
      )
    )

    val boundaries = com.example.ui.map.DistrictBoundaryManager.computeBoundaries(colleges)
    assertEquals(2, boundaries.size)

    val varanasi = boundaries.find { it.name.equals("Varanasi", ignoreCase = true) }
    assertNotNull(varanasi)
    assertEquals(2, varanasi!!.collegeCount)
    assertTrue(varanasi.polygon.size >= 3) // Polygon has valid vertices

    val prayagraj = boundaries.find { it.name.equals("Prayagraj", ignoreCase = true) }
    assertNotNull(prayagraj)
    assertEquals(1, prayagraj!!.collegeCount)
  }

  @Test
  fun spreadsheetParser_parsesDistrictAndCollegeType() {
    val csvData = """
      College Name,Code,Latitude,Longitude,Branches,Maximum Student Count Per Day,Is Exam Centre,District,College Type
      Govt Degree College,GDC-01,25.32,82.98,"BA, MA, BSc",1200,Yes,Varanasi,GOVERNMENT
      St Xavier Aided,SXA-02,25.34,83.00,"BCom, MCom",800,No,Varanasi,AIDED
      Private Tech Institute,PTI-03,25.45,81.85,"BCA, MCA, LLB",650,No,Prayagraj,SELF FINANCE
      Autonomous Institute,AUT-04,25.46,81.86,"MSc, BEd, MEd",900,Yes,Prayagraj,Autonomous
    """.trimIndent()

    val stream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
    val parseResult = SpreadsheetParser.parseCsv(stream)
    assertTrue(parseResult.isSuccess)
    val list = parseResult.getOrThrow()

    assertEquals(4, list.size)
    assertEquals("Varanasi", list[0].district)
    assertEquals(com.example.data.model.CollegeType.GOVERNMENT, list[0].managementType)
    assertEquals(com.example.data.model.CollegeType.AIDED, list[1].managementType)
    assertEquals(com.example.data.model.CollegeType.SELF_FINANCE, list[2].managementType)
    assertEquals(com.example.data.model.CollegeType.AUTONOMOUS, list[3].managementType)
  }

  @Test
  fun geographicalFeaturesProvider_generatesRiversRoadsAndLandmarks() {
    val rivers = com.example.ui.map.GeographicalFeaturesProvider.getRivers(25.3, 82.9, 0.5, 0.5)
    assertTrue(rivers.isNotEmpty())
    assertTrue(rivers.any { it.isMainStream })

    val highways = com.example.ui.map.GeographicalFeaturesProvider.getHighways(25.3, 82.9, 0.5, 0.5)
    assertTrue(highways.isNotEmpty())
    assertTrue(highways.any { it.isExpressway })

    val landmarks = com.example.ui.map.GeographicalFeaturesProvider.getLandmarks(25.3, 82.9, 0.5, 0.5)
    assertTrue(landmarks.size >= 5)
  }
}

