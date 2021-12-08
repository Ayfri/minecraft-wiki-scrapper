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
import it.skrape.selects.html5.map
import java.awt.Color.red
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

fun printError(message: Any) = println("\u001B[31m$message\u001B[0m")

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
			if (!validLinks || "Java_Edition" !in versionSubPath) return@forEach
			val versionLink = "https://minecraft.fandom.com$versionSubPath"
			println(versionLink)
			
			val extracted = skrape(HttpFetcher) {
				request {
					url = versionLink
				}
				
				extractIt<Snapshot> {
					htmlDocument {
						relaxed = true
						
						if (findFirst("ul.categories").children.map { it.text }.none { it == "Java Edition versions"}) {
							return@htmlDocument
						}
						
						it.name = h1(".page-header__title") { findFirst { ownText } }
						it.description = div(".mw-parser-output") {
							findFirst { children.first { it.tagName == "p" }.text }
						}
						
						val table = findFirst(".infobox-rows > tbody")
						
						try {
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
						} catch (_: Exception) {}
						
						
						it.releaseTime = table.findFirst {
							findAll("tr").firstOrNull {
								it.children.any { it.text.contains("Release date") }
							}
						}?.findFirst("td")?.let { td ->
							td.children.firstOrNull { it.tagName == "p" }?.text?.let {
								var result = it
								result = when {
									"[" in it -> result.remove(Regex("\\[.*?]"))
									else -> result
								}
								result = result.get(Regex("[\\w-]+: (.+?) \\w+: .+"))
								
								
								val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
								val formattedDate = try {
									LocalDate.parse(result.trimEnd(), formatter)
								} catch (e: Exception) {
									printError("Failed to parse date: '$it' ($result)")
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
