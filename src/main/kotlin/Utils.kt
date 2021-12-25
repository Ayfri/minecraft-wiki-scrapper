import it.skrape.selects.DocElement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.absolutePathString

fun calculateDate(str: String): Long? {
	var result = str
	result = when {
		"[" in str -> result.remove(Regex("\\[.*?]"))
		else -> result
	}
	result = result.get(Regex("[\\w-]+: (.+?) \\w+: .+"))
	result = result.get(Regex(".+?\\((.+?)\\)"))
	
	val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
	val formattedDate = try {
		LocalDate.parse(result.trimEnd(), formatter)
	} catch (e: Exception) {
		printError("Failed to parse date: '$result' ($result)")
		null
	}
	return formattedDate?.toEpochSecond(LocalTime.now(), ZoneOffset.UTC)
}

fun DocElement.findFirstElementWithTableHeaderName(tableHeaderName: String) = findFirst {
	findAll("tr").firstOrNull {
		it.children.any { it.text.contains(tableHeaderName, true) }
	}
}

fun DocElement.findFirstElementWithTableHeaderRegex(tableHeaderName: Regex) = findFirst {
	findAll("tr").firstOrNull {
		it.children.any { it.text.contains(tableHeaderName) }
	}
}


fun saveToJSON() {
	val jsonSerializer = Json { prettyPrint = true }
	val json = jsonSerializer.encodeToString<List<Version>>(versions)
	val local = Paths.get("out")
	val file = File(local.absolutePathString(), "versions.json")
	file.writeText(json)
	
	println("Saved to ${file.normalize().absolutePath}")
}
