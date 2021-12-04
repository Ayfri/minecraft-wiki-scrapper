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
			val versionDownload = Regex("<a href=\"(.+)\" class=\"external text\"").find(versionText)?.groupValues?.get(1) ?: "download"
			// TODO : Set download method
			// Attention c'est relou, c'est un tableau tr/td donc faudra faire comme la date
			
			var versionDate =
				Regex("<tr>.+?<th>Release date.+?</th>.+?<td>.+?<p>(.+?)</p>.+?</td></tr>", RegexOption.DOT_MATCHES_ALL).find(versionText)?.groupValues?.get(1) ?: "date"
			if (versionDate.contains("Original")) {
				versionDate.apply { Regex("<b>Original:</b>(.*?)<br />").find(versionText)?.groupValues?.get(1)?.let { versionDate = it } }
			}
			val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
			val formattedDate = try {
				LocalDate.parse(versionDate.trim(), formatter)
			} catch (e: Exception) {
				println("Failed to parse date: $versionDate")
				null
			}
			
			println("${versionName.trim()} ${formattedDate?.toEpochSecond(LocalTime.now(), ZoneOffset.UTC)} $versionDownload")
		}
	}
}
