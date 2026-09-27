package com.example.data.parser

import android.content.Context
import android.net.Uri
import com.example.data.model.CollegeEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object SpreadsheetParser {

    fun parseSpreadsheet(context: Context, uri: Uri): Result<List<CollegeEntity>> {
        return try {
            val contentResolver = context.contentResolver
            val type = contentResolver.getType(uri) ?: ""
            val fileName = uri.lastPathSegment?.lowercase() ?: ""

            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file stream"))

            if (fileName.endsWith(".xlsx") || type.contains("spreadsheetml")) {
                parseXlsx(inputStream)
            } else {
                // Parse as CSV or text delimited
                parseCsv(inputStream)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseCsv(inputStream: InputStream): Result<List<CollegeEntity>> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = mutableListOf<List<String>>()
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    lines.add(parseCsvLine(line))
                }
                line = reader.readLine()
            }
            reader.close()

            if (lines.isEmpty()) {
                return Result.failure(Exception("Uploaded file is empty"))
            }

            val firstLine = lines.first().map { it.trim() }
            val col4Dbl = firstLine.getOrNull(4)?.replace(",", ".")?.toDoubleOrNull()
            val col5Dbl = firstLine.getOrNull(5)?.replace(",", ".")?.toDoubleOrNull()
            val isFirstLineData = col4Dbl != null && col5Dbl != null &&
                    !firstLine[0].lowercase().contains("district") &&
                    !firstLine[1].lowercase().contains("code")

            val isHeaderRow = !isFirstLineData && firstLine.any {
                val l = it.lowercase()
                l.contains("district") || l.contains("code") || l.contains("name") || l.contains("college") ||
                l.contains("type") || l.contains("lat") || l.contains("lon") || l.contains("lng")
            }

            var districtCol = -1
            var codeCol = -1
            var nameCol = -1
            var typeCol = -1
            var latCol = -1
            var lonCol = -1
            var branchCol = -1
            var capacityCol = -1
            var studentCol = -1
            var examCentreCol = -1
            var addressCol = -1
            var phoneCol = -1
            var emailCol = -1

            if (isHeaderRow) {
                val header = firstLine.map { it.lowercase() }
                districtCol = header.indexOfFirst { it.contains("district") || it.contains("zone") || it.contains("region") }
                codeCol = header.indexOfFirst { it.contains("code") }
                nameCol = header.indexOfFirst { (it.contains("name") || it.contains("college")) && !it.contains("code") && !it.contains("district") && !it.contains("type") }
                if (nameCol == -1) nameCol = header.indexOfFirst { it.contains("name") || it.contains("college") }
                typeCol = header.indexOfFirst { it.contains("type") || it.contains("management") || it.contains("category") || it.contains("status") || it.contains("affiliation") }
                latCol = header.indexOfFirst { it.contains("lat") }
                lonCol = header.indexOfFirst { it.contains("lon") || it.contains("lng") }
                branchCol = header.indexOfFirst { it.contains("branch") || it.contains("course") || it.contains("dept") || it.contains("degree") || it.contains("stream") }
                capacityCol = header.indexOfFirst { it.contains("capacity") || it.contains("seating") || it.contains("seat") }
                studentCol = header.indexOfFirst {
                    it.contains("maximum student count per day") || it.contains("max student") || it.contains("max count") ||
                    it.contains("per day") || it.contains("student") || it.contains("population")
                }
                examCentreCol = header.indexOfFirst { it.contains("exam") || it.contains("centre") || it.contains("center") }
                addressCol = header.indexOfFirst { it.contains("address") || it.contains("loc") }
                phoneCol = header.indexOfFirst { it.contains("phone") || it.contains("contact") }
                emailCol = header.indexOfFirst { it.contains("email") }
            }

            // Column mappings with fallbacks:
            // Column 4 (index 3) titled 'type'
            // Column 7 (index 6) Seating Capacity
            if (districtCol == -1) districtCol = 0
            if (codeCol == -1) codeCol = 1
            if (nameCol == -1) nameCol = 2
            if (typeCol == -1) typeCol = 3
            if (latCol == -1) latCol = 4
            if (lonCol == -1) lonCol = 5
            if (capacityCol == -1) capacityCol = 6

            val rows = if (isHeaderRow) lines.drop(1) else lines

            val list = mutableListOf<CollegeEntity>()
            rows.forEachIndexed { index, cols ->
                if (cols.isEmpty() || cols.all { it.isBlank() }) return@forEachIndexed
                val district = if (districtCol in cols.indices && cols[districtCol].isNotBlank()) cols[districtCol].trim() else "District ${(index % 3) + 1}"
                val code = if (codeCol in cols.indices && cols[codeCol].isNotBlank()) cols[codeCol].trim() else "COL-${index + 101}"
                val name = if (nameCol in cols.indices && cols[nameCol].isNotBlank()) cols[nameCol].trim() else "College #${index + 1}"
                
                // Read type from column 4 titled 'type' or fallbacks
                var rawType = if (typeCol in cols.indices && cols[typeCol].isNotBlank()) cols[typeCol].trim() else ""
                if (rawType.isBlank() && 3 in cols.indices) rawType = cols[3].trim()
                if (rawType.isBlank() || !isKnownType(rawType)) {
                    val fallbackType = cols.firstOrNull { isKnownType(it) }
                    if (fallbackType != null) rawType = fallbackType.trim()
                }
                val colType = resolveCollegeType(rawType, name)
                
                var rawLat = if (latCol in cols.indices) parseCoordinate(cols[latCol]) else null
                var rawLon = if (lonCol in cols.indices) parseCoordinate(cols[lonCol]) else null

                // Auto-swap check for India (Lat ~15-25, Lon ~72-85)
                if (rawLat != null && rawLon != null && rawLat > 50.0 && rawLon < 45.0) {
                    val tmp = rawLat
                    rawLat = rawLon
                    rawLon = tmp
                }

                val lat = rawLat ?: (21.1458 + (index % 10) * 0.01)
                val lon = rawLon ?: (79.0882 + (index % 10) * 0.01)
                val branches = if (branchCol != -1 && branchCol in cols.indices && cols[branchCol].isNotBlank()) cols[branchCol].trim() else "BA, BSc, BCom, MA"
                
                // Read seating capacity from Column 7 (index 6) or capacityCol
                val rawCap = when {
                    capacityCol in cols.indices && cols[capacityCol].isNotBlank() -> cols[capacityCol]
                    6 in cols.indices && cols[6].isNotBlank() -> cols[6]
                    7 in cols.indices && cols[7].isNotBlank() -> cols[7]
                    else -> ""
                }
                val capacity = rawCap.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1500
                val students = if (studentCol != -1 && studentCol in cols.indices) cols[studentCol].toIntOrNull() ?: 1000 else 1000
                val examStr = if (examCentreCol != -1 && examCentreCol in cols.indices) cols[examCentreCol].lowercase() else ""
                val isExam = examStr.contains("yes") || examStr.contains("true") || examStr == "1" || capacity >= 2000
                val address = if (addressCol != -1 && addressCol in cols.indices && cols[addressCol].isNotBlank()) cols[addressCol].trim() else "$district, Campus Zone"
                val phone = if (phoneCol != -1 && phoneCol in cols.indices) cols[phoneCol].trim() else ""
                val email = if (emailCol != -1 && emailCol in cols.indices) cols[emailCol].trim() else ""

                list.add(
                    CollegeEntity(
                        id = (index + 1).toLong(),
                        name = name,
                        code = code,
                        latitude = lat,
                        longitude = lon,
                        branches = branches,
                        seatingCapacity = capacity,
                        studentPopulation = students,
                        isExamCentre = isExam,
                        address = address,
                        district = district,
                        collegeType = colType,
                        contactPhone = phone,
                        contactEmail = email
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                (c == ',' || c == '\t' || c == ';') && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Reads an .xlsx file by unzipping xl/sharedStrings.xml and xl/worksheets/sheet1.xml
     * without needing external heavy Apache POI dependencies!
     */
    private fun parseXlsx(inputStream: InputStream): Result<List<CollegeEntity>> {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        val entriesData = mutableMapOf<String, ByteArray>()

        while (entry != null) {
            val lower = entry.name.lowercase().replace('\\', '/')
            if (lower.contains("sharedstrings.xml")) {
                entriesData["sharedStrings"] = zip.readBytes()
            } else if (lower.contains("worksheets/sheet") && !entriesData.containsKey("sheet")) {
                entriesData["sheet"] = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        zip.close()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false

        // Parse sharedStrings.xml
        entriesData["sharedStrings"]?.let { bytes ->
            val parser = factory.newPullParser()
            parser.setInput(bytes.inputStream(), "UTF-8")
            var eventType = parser.eventType
            var inT = false
            val currentStr = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.equals("si", ignoreCase = true)) {
                            currentStr.setLength(0)
                        } else if (parser.name.equals("t", ignoreCase = true)) {
                            inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) currentStr.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inT = false
                        } else if (parser.name.equals("si", ignoreCase = true)) {
                            sharedStrings.add(currentStr.toString().trim())
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        // Parse worksheet XML
        entriesData["sheet"]?.let { bytes ->
            val parser = factory.newPullParser()
            parser.setInput(bytes.inputStream(), "UTF-8")
            var eventType = parser.eventType
            var currentRow = mutableListOf<String>()
            var isStringCell = false
            var isInlineStr = false
            val cellVal = StringBuilder()
            var inV = false
            var inT = false
            var cellColIndex = -1
            var nextImplicitCol = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "row" -> {
                                currentRow = mutableListOf()
                                nextImplicitCol = 0
                            }
                            "c" -> {
                                val rAttr = parser.getAttributeValue(null, "r")
                                cellColIndex = if (rAttr != null) colLetterToIndex(rAttr) else nextImplicitCol
                                val tAttr = parser.getAttributeValue(null, "t")?.lowercase()
                                isStringCell = (tAttr == "s")
                                isInlineStr = (tAttr == "inlinestr")
                                cellVal.setLength(0)
                                inV = false
                                inT = false
                            }
                            "v" -> inV = true
                            "t" -> inT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV || (isInlineStr && inT)) {
                            cellVal.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "v" -> inV = false
                            "t" -> inT = false
                            "c" -> {
                                val raw = cellVal.toString().trim()
                                val resolved = if (isStringCell) {
                                    val index = raw.toIntOrNull() ?: -1
                                    if (index in sharedStrings.indices) sharedStrings[index] else raw
                                } else {
                                    raw
                                }
                                if (cellColIndex >= 0) {
                                    while (currentRow.size <= cellColIndex) {
                                        currentRow.add("")
                                    }
                                    currentRow[cellColIndex] = resolved
                                    nextImplicitCol = cellColIndex + 1
                                } else {
                                    currentRow.add(resolved)
                                    nextImplicitCol++
                                }
                            }
                            "row" -> {
                                if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                    sheetRows.add(currentRow)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        if (sheetRows.isEmpty()) {
            return Result.failure(Exception("No readable data found in sheet.xml"))
        }

        // Scan first 10 rows for the header row
        var headerRowIdx = -1
        for (rIdx in 0 until minOf(10, sheetRows.size)) {
            val r = sheetRows[rIdx].map { it.lowercase().trim() }
            val hasType = r.any { it == "type" || it.contains("type") || it.contains("management") || it.contains("category") }
            val hasName = r.any { it.contains("name") || it.contains("college") }
            val hasCode = r.any { it.contains("code") }
            val hasLat = r.any { it.contains("lat") }
            val hasCap = r.any { it.contains("seat") || it.contains("cap") }
            if (hasType || (hasName && (hasCode || hasLat || hasCap))) {
                headerRowIdx = rIdx
                break
            }
        }

        var districtCol = -1
        var codeCol = -1
        var nameCol = -1
        var typeCol = -1
        var latCol = -1
        var lonCol = -1
        var branchCol = -1
        var capCol = -1
        var studentCol = -1
        var examCol = -1
        var addressCol = -1

        if (headerRowIdx != -1) {
            val header = sheetRows[headerRowIdx].map { it.lowercase().trim() }
            districtCol = header.indexOfFirst { it.contains("district") || it.contains("zone") || it.contains("region") || it.contains("city") }
            codeCol = header.indexOfFirst { it.contains("code") }
            nameCol = header.indexOfFirst { (it.contains("name") || it.contains("college") || it.contains("institute")) && !it.contains("code") && !it.contains("district") && !it.contains("type") }
            if (nameCol == -1) nameCol = header.indexOfFirst { it.contains("name") || it.contains("college") }
            typeCol = header.indexOfFirst { it == "type" || it.contains("type") || it.contains("management") || it.contains("category") || it.contains("status") || it.contains("affiliation") }
            latCol = header.indexOfFirst { it.contains("lat") }
            lonCol = header.indexOfFirst { it.contains("lon") || it.contains("lng") }
            branchCol = header.indexOfFirst { it.contains("branch") || it.contains("course") || it.contains("dept") || it.contains("stream") }
            capCol = header.indexOfFirst { it.contains("capacity") || it.contains("seating") || it.contains("seat") || it.contains("intake") }
            studentCol = header.indexOfFirst {
                it.contains("maximum student count per day") || it.contains("max student") || it.contains("max count") ||
                it.contains("per day") || it.contains("student") || it.contains("population")
            }
            examCol = header.indexOfFirst { it.contains("exam") }
            addressCol = header.indexOfFirst { it.contains("address") }
        }

        // Column fallback assignments:
        // Column 4 (index 3) titled 'type'
        // Column 7 (index 6) Seating Capacity
        // Column 5 (index 4) Latitude
        // Column 6 (index 5) Longitude
        if (typeCol == -1) typeCol = 3
        if (capCol == -1) capCol = 6
        if (latCol == -1) latCol = 4
        if (lonCol == -1) lonCol = 5
        if (districtCol == -1) districtCol = 0
        if (codeCol == -1) codeCol = 1
        if (nameCol == -1) nameCol = 2

        val rows = if (headerRowIdx != -1) sheetRows.drop(headerRowIdx + 1) else sheetRows

        val list = mutableListOf<CollegeEntity>()
        rows.forEachIndexed { i, r ->
            if (r.isEmpty() || r.all { it.isBlank() }) return@forEachIndexed
            val district = if (districtCol in r.indices && r[districtCol].isNotBlank()) r[districtCol].trim() else "District ${(i % 3) + 1}"
            val code = if (codeCol in r.indices && r[codeCol].isNotBlank()) r[codeCol].trim() else "COL-${i + 101}"
            val name = if (nameCol in r.indices && r[nameCol].isNotBlank()) r[nameCol].trim() else "College #${i + 1}"
            
            // Read type from column 4 titled 'type' (index 3) or fallbacks
            var rawType = if (typeCol in r.indices && r[typeCol].isNotBlank()) r[typeCol].trim() else ""
            if (rawType.isBlank() && 3 in r.indices) rawType = r[3].trim()
            if (rawType.isBlank() || !isKnownType(rawType)) {
                val fallbackType = r.firstOrNull { isKnownType(it) }
                if (fallbackType != null) rawType = fallbackType.trim()
            }
            val colType = resolveCollegeType(rawType, name)
            
            var rawLat = if (latCol in r.indices) parseCoordinate(r[latCol]) else null
            var rawLon = if (lonCol in r.indices) parseCoordinate(r[lonCol]) else null

            // Auto-swap check for India (Lat ~15-25, Lon ~72-85)
            if (rawLat != null && rawLon != null && rawLat > 50.0 && rawLon < 45.0) {
                val tmp = rawLat
                rawLat = rawLon
                rawLon = tmp
            }

            val lat = rawLat ?: (21.1458 + (i % 10) * 0.01)
            val lon = rawLon ?: (79.0882 + (i % 10) * 0.01)
            val branches = if (branchCol != -1 && branchCol in r.indices && r[branchCol].isNotBlank()) r[branchCol].trim() else "BA, BSc, BCom, MA"
            
            // Read seating capacity from Column 7 (index 6) or capCol
            val rawCap = when {
                capCol in r.indices && r[capCol].isNotBlank() -> r[capCol]
                6 in r.indices && r[6].isNotBlank() -> r[6]
                7 in r.indices && r[7].isNotBlank() -> r[7]
                else -> ""
            }
            val cap = rawCap.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1500
            val std = if (studentCol != -1 && studentCol in r.indices) r[studentCol].replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1000 else 1000
            val isExam = if (examCol != -1 && examCol in r.indices) {
                val e = r[examCol].lowercase()
                e.contains("yes") || e.contains("true") || e == "1" || cap >= 2000
            } else cap >= 2000
            val addr = if (addressCol != -1 && addressCol in r.indices && r[addressCol].isNotBlank()) r[addressCol].trim() else "$district, Affiliated Campus"

            list.add(
                CollegeEntity(
                    id = (i + 1).toLong(),
                    name = name,
                    code = code,
                    latitude = lat,
                    longitude = lon,
                    branches = branches,
                    seatingCapacity = cap,
                    studentPopulation = std,
                    isExamCentre = isExam,
                    address = addr,
                    district = district,
                    collegeType = colType
                )
            )
        }

        return Result.success(list)
    }

    private fun colLetterToIndex(ref: String?): Int {
        if (ref == null) return -1
        val letters = ref.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return -1
        var col = 0
        for (ch in letters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return col - 1
    }

    private fun parseCoordinate(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().replace(",", ".")
        // Check DMS e.g. 21° 7' 31.8" N
        val dmsRegex = Regex("""([0-9.]+)\s*[°d]\s*([0-9.]+)?\s*['m]?\s*([0-9.]+)?\s*["s]?\s*([NSEWnsew])?""")
        val match = dmsRegex.find(s)
        if (match != null && (s.contains("°") || s.contains("'") || s.contains("\"") || s.contains("d"))) {
            val deg = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            val min = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            val sec = match.groupValues.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val dir = match.groupValues.getOrNull(4)?.uppercase() ?: ""
            var dec = deg + (min / 60.0) + (sec / 3600.0)
            if (dir == "S" || dir == "W") dec = -dec
            return dec
        }

        val isNegative = s.contains("S", ignoreCase = true) || s.contains("W", ignoreCase = true) || s.startsWith("-")
        val clean = s.replace(Regex("[^0-9.]"), "")
        val num = clean.toDoubleOrNull() ?: return null
        return if (isNegative) -num else num
    }

    private fun isKnownType(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        val s = str.trim().uppercase()
        return s.contains("AUTO") || s.contains("AIDED") || s.contains("GRANT") ||
               s.contains("SELF") || s.contains("SFI") || s.contains("PRIVATE") ||
               s.contains("UNAIDED") || s.contains("NON-GRANT") || s.contains("NON GRANT") ||
               s.contains("GOV") || s == "G" || s == "A" || s == "SF" || s == "AUTO"
    }

    private fun resolveCollegeType(rawType: String, name: String): String {
        val t = rawType.trim().uppercase()
        val n = name.uppercase()
        return when {
            // Autonomous
            t.contains("AUTO") || n.contains("AUTONOMOUS") || n.contains("AUTONOMY") -> "Autonomous"
            // Self-Finance / Unaided / Non-Grant / Private (checked before Aided!)
            t.contains("NON-GRANT") || t.contains("NON GRANT") || t.contains("UNAIDED") ||
            t.contains("UN-AIDED") || t.contains("SELF") || t.contains("SFI") ||
            t.contains("PRIVATE") || t.contains("PVT") || t == "SF" || t == "S.F." ||
            n.contains("SELF FINANCE") || n.contains("SELF-FINANCE") || n.contains("SELF FINANCED") || n.contains("UNAIDED") -> "SELF FINANCE"
            // Aided / Grant-in-aid
            t.contains("AIDED") || t.contains("GRANT") || t == "A" || t == "GIA" || n.contains("AIDED") || n.contains("GRANT-IN-AID") -> "AIDED"
            // Government
            t.contains("GOV") || t.contains("STATE") || t.contains("CENTRAL") || t == "G" || n.contains("GOVT") || n.contains("GOVERNMENT") -> "GOVERNMENT"
            t.startsWith("SF") || t.startsWith("SELF") || t.startsWith("UN") -> "SELF FINANCE"
            t.startsWith("AID") || t.startsWith("GR") -> "AIDED"
            t.startsWith("GOV") -> "GOVERNMENT"
            n.contains("GOVT") || n.contains("GOVERNMENT") || n.startsWith("GCOE") -> "GOVERNMENT"
            n.contains("AIDED") -> "AIDED"
            n.contains("PRIVATE") -> "SELF FINANCE"
            else -> "GOVERNMENT"
        }
    }
}
