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
import it.skrape.selects.html5.img
import it.skrape.selects.html5.th
import it.skrape.selects.html5.tr
import it.skrape.selects.text
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

const val listLink = "https://minecraft.fandom.com/wiki/Java_Edition_version_history"

val versionsExceptions = listOf("April Fools updates")
// TODO : Handle exceptions

fun calculateDate(str: String): Long? {
	var result = str
	result = when {
		"[" in str -> result.remove(Regex("\\[.*?]"))
		else -> result
	}
	result = result.get(Regex("[\\w-]+: (.+?) \\w+: .+"))
	
	val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
	val formattedDate = try {
		LocalDate.parse(str.trimEnd(), formatter)
	} catch (e: Exception) {
		printError("Failed to parse date: '$str' ($str)")
		null
	}
	return formattedDate?.toEpochSecond(LocalTime.now(), ZoneOffset.UTC)
}

suspend fun main() {
	val client = HttpClient(CIO)
	val versions = mutableListOf<Version>()
	skrape(HttpFetcher) {
		request {
			url = listLink
		}
		
		extractIt<List<Version>> {
			htmlDocument {
				relaxed = true
				
				val versionsBody = findFirst("table.navbox.hlist.collapsible > tbody")
				versionsBody.children.filter { it.contains("span.navbox-title > a.mw-redirect > span.nowrap") }.forEach { el ->
					val version = skrape(HttpFetcher) {
						request {
							val subLink = el.findFirst("a").attributes["href"] ?: return@request
							url = "https://minecraft.fandom.com$subLink"
						}
						
						extractIt<Version> {
							htmlDocument {
								it.name = findFirst("h1#firstHeading").text
								it.description = findFirst("div.mw-parser-output > p").text
								findFirst("table.infobox-rows > tbody").findAny {
									th {
										findFirst {
											text.contains("Starting version")
											this
										}
									}
								}?.findFirst("td")?.text?.let { date ->
									it.releaseTime = calculateDate(date)
								}
								
								it.imageUrl = findFirst("div.infobox-imagearea animated-container > div > a.img > img").attributes["href"] ?: "Not found."
								it.snapshots = el.findAll("td > ul > li > i > a") { map { scrapSnapshot(it.attributes["href"] ?: "") }}
							}
						}
					}
					versions += version
				}
			}
		}
	}
	println(versions)
}

fun scrapSnapshot(link: String) = skrape(HttpFetcher) {
	println(link)
	request {
		url = link
	}
	
	extractIt<Snapshot> {
		htmlDocument {
			relaxed = true
			
			if (findFirst("ul.categories").children.map { it.text }.none { it == "Java Edition versions" }) {
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
			} catch (_: Exception) {
			}
			
			
			it.releaseTime = table.findFirst {
				findAll("tr").firstOrNull {
					it.children.any { it.text.contains("Release date") }
				}
			}?.findFirst("td")?.let { td ->
				td.children.firstOrNull { it.tagName == "p" }?.text?.let(::calculateDate)
			}
		}
	}
}
