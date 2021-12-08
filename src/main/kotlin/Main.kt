import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.div
import it.skrape.selects.html5.h1
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

const val firstValidVersion = "/wiki/Java_Edition_pre-Classic_rd-131655"
const val listLink = "https://minecraft.fandom.com/wiki/Java_Edition_version_history"

operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

fun String.remove(regex: Regex) = replace(regex, "")
fun String.remove(regex: String) = replace(regex, "")
fun String.get(regex: Regex) = replace(regex, "$1")
fun String.getIfMatches(regex: Regex) = if (matches(regex)) get(regex) else null

data class Snapshot(
	var name: String = "",
	var releaseTime: Long? = null,
	var description: String? = null,
	var downloadClient: String? = null,
	var downloadJSON: String? = null
)

data class Version(
	var name: String,
	var releaseTime: Long?,
	var imageUrl: String = "",
	var description: String,
	var importantDescription: String = description.split("\n").take(4).joinToString("\n"),
	var snapshots: List<Snapshot>
)


suspend fun main() {
	val client = HttpClient(CIO)
	val response: HttpResponse = client.get(listLink)
	val versions = mutableListOf<Version>()
	val snapshots = mutableListOf<Snapshot>()
	var validLinks = false
	
	response.readText().substringAfter("References").substringBefore("Categories").split("\n").forEach { it ->
		if (it.contains("<li>")) {
			val version = it.split("<li>")[1].split("</li>")[0]
			val versionSubPath = Regex("<a href=\"(/wiki/[\\w_.-]+)\"").find(version)?.groupValues?.get(1) ?: return@forEach
			if (versionSubPath == firstValidVersion) validLinks = true
			if (!validLinks) return@forEach
			val versionLink = "https://minecraft.fandom.com$versionSubPath"
			println(versionLink)
			
			val extracted = skrape(HttpFetcher) {
				request {
					url = versionLink
				}
				
				extractIt<Snapshot> {
					htmlDocument {
						relaxed = true
						
						it.name = h1(".page-header__title") {
							findFirst { ownText }
						}
						it.description = div(".mw-parser-output") {
							findFirst {
								children.first { it.tagName == "p" }.text
							}
						}
						
						val table = findFirst(".infobox-rows > tbody")
						val download = table.findFirst {
							findAll("tr").first {
								it.children.any { it.text.contains("Downloads") }
							}
						}
						
						it.downloadClient = download.findFirst("td > p").let {
							it.children.firstOrNull { it.tagName == "a" }?.attributes?.get("href")
						}
						it.downloadJSON = download.findFirst("td > p").let {
							it.children.firstOrNull {
								it.tagName == "a" && it.attributes["href"]?.endsWith(".json") == true
							}?.attributes?.get("href")
						}
						
						it.releaseTime = table.findFirst {
							findAll("tr").firstOrNull {
								it.children.any { it.text.contains("Release date") }
							}
						}?.findFirst("td")?.let { td ->
							td.children.firstOrNull { it.tagName == "p" }?.text?.let {
								var result = it
								if ("Original" in it) {
									Regex("<b>Original:</b>(.*?)<br />").find(it)?.groupValues?.get(1)?.let { result = it }
								} else if ("<sup" in it) {
									Regex("(.+?)(?><sup.+?</sup>)").find(it)?.groupValues?.get(1)?.let { result = it }
								}
								
								val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
								val formattedDate = try {
									LocalDate.parse(result.trim(), formatter)
								} catch (e: Exception) {
									println("Failed to parse date: $result")
									null
								}
								formattedDate?.toEpochSecond(LocalTime.now(), ZoneOffset.UTC)
							}
						}
					}
				}
			}
			
			println(extracted)
			
			snapshots.add(extracted)
		}
	}
}
