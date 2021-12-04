import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

const val firstValidVersion = "/wiki/Java_Edition_pre-Classic_rd-131655"
const val listLink = "https://minecraft.fandom.com/wiki/Java_Edition_version_history"

operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

suspend fun main() {
	val client = HttpClient(CIO)
	val response: HttpResponse = client.get(listLink)
	var validLinks = false
	response.readText().substringAfter("References").substringBefore("Categories").split("\n").forEach {
		if (it.contains("<li>")) {
			val version = it.split("<li>")[1].split("</li>")[0]
			val versionSubPath = Regex("<a href=\"(/wiki/[\\w_.-]+)\"").find(version)?.groupValues?.get(1) ?: return@forEach
			if (versionSubPath == firstValidVersion) validLinks = true
			if (!validLinks) return@forEach
			val versionLink = "https://minecraft.fandom.com$versionSubPath"
			println(versionLink)
			
			
			val versionResponse: HttpResponse = client.get(versionLink)
			val versionText = versionResponse.readText()
			val versionName = Regex("<h1.+?class=\"page-header__title\".+?>(.+?)</h1>", RegexOption.DOT_MATCHES_ALL).find(versionText)?.groupValues?.get(1) ?: "title"
			val versionDownloadName =
				Regex(
					"href=\"https://archive.org/download/Minecraft-JE-\\w+/(.+?)/\\1.jar\"|href=\"https://launcher.mojang.com/v1/objects/(\\w+)/client.jar\"",
					RegexOption.DOT_MATCHES_ALL
				).find(versionText)?.groupValues?.drop(1)?.first { it.isNotBlank() }
			val versionDownloadJSON =
				Regex(
					"href=\"https://archive.org/download/Minecraft-JE-\\w+/(.+?)/\\1.json\"|href=\"https://launcher.mojang.com/v1/objects/(\\w+/[\\w.-_]+).json\"",
					RegexOption.DOT_MATCHES_ALL
				).find(versionText)?.groupValues?.drop(1)?.first { it.isNotBlank() }
			
			var versionDate =
				Regex(
					"<tr>.+?<th>Release date.+?</th>.+?<td>.+?<p>(.+?)</p>.+?</td></tr>",
					RegexOption.DOT_MATCHES_ALL
				).find(versionText)?.groupValues?.get(1) ?: "date"
			if ("Original" in versionDate) {
				Regex("<b>Original:</b>(.*?)<br />").find(versionDate)?.groupValues?.get(1)?.let { versionDate = it }
			} else if ("<sup" in versionDate) {
				Regex("(.+?)(?><sup.+?</sup>)").find(versionDate)?.groupValues?.get(1)?.let { versionDate = it }
			}
			
			val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
			val formattedDate = try {
				LocalDate.parse(versionDate.trim(), formatter)
			} catch (e: Exception) {
				println("Failed to parse date: $versionDate")
				null
			}
			val snapshot = Snapshot(versionName.trim(), formattedDate?.toEpochSecond(LocalTime.now(), ZoneOffset.UTC), versionDownloadName, versionDownloadJSON)
			
			println(snapshot)
		}
	}
}

data class Snapshot(
	val name: String,
	val releaseTime: Long?,
	val download: String?,
	val downloadJSON: String?
)
