import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.div
import it.skrape.selects.html5.h1
import it.skrape.selects.html5.th
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

const val JAVA_EDITION = "Java Edition"
const val JAVA_LINK = "https://minecraft.fandom.com/wiki/Java_Edition_"
const val FANDOM_URL = "https://minecraft.fandom.com"
const val LIST_LINK = "https://minecraft.fandom.com/wiki/Java_Edition_version_history"

val snapshotListSkip = listOf("Version history", "Development versions", "Full Release")
val versionsExceptions = listOf("April Fools updates")

val versionsSkip = listOf(Regex("Java_Edition_[a-z]+_server_.+", RegexOption.IGNORE_CASE), Regex("Version_history"))
val versions = mutableListOf<Version>()

suspend fun main() {
	scrap()
	fixMainReleaseVersion()
	saveToJSON()
}

suspend fun scrap() {
	skrape(HttpFetcher) {
		request {
			url = LIST_LINK
		}
		
		extractIt<Any> {
			htmlDocument {
				val versionsBody = findFirst("div[style*=\"overflow-x: hidden\"] > table.navbox.hlist.collapsible > tbody")
				versionsBody.children.filter {
					it.contains("span.navbox-title > a")
				}.distinctBy {
					it.findFirst("a").href
				}.forEach { el ->
					val link = el.findFirst("a").href ?: return@forEach
					if (versionsSkip.any { link.matches(it) }) return@forEach
					println("Version : $FANDOM_URL$link")
					
					val version = skrape(HttpFetcher) {
						request {
							url = FANDOM_URL + link
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
								}?.parent?.findFirst("td")?.text?.let { date ->
									it.releaseTime = calculateDate(date)
								}
								
								var selector = "td > ul > li a"
								if (it.name == JAVA_EDITION) selector += ", tr > th > a"
								
								it.snapshots = el.findAll(selector) {
									filter {
										snapshotListSkip.none { exception -> it.text.contains(exception) }
									}.map {
										scrapSnapshot(it.href ?: "")
									}
								}
							}
						}
					}
					println("${"-".repeat(40)} Parsed version : ${version.name}")
					versions += version
				}
			}
		}
	}
}

fun fixMainReleaseVersion() {
	println("${"=".repeat(20)} FIXING VERSIONS ${"=".repeat(20)}")
	
	val releaseVersion = versions.find { JAVA_EDITION == it.name } ?: run {
		println("Can't find main version, can't fix versions.")
		return@fixMainReleaseVersion
	}
	versions.removeIf { JAVA_EDITION == it.name }
	
	val firstReleases = releaseVersion.snapshots.groupBy { it.snapshotFor?.get(Regex("(\\d\\.\\d{1,2})\\.\\d{1,2}")) }.filterKeys { it != null } as Map<String, List<Snapshot>>
	val links = firstReleases.map { "/wiki/Java_Edition_" + it.key }
	links.forEach { link ->
		println("Fixing release : ${link.remove("/wiki/").replace("_", " ")}")
		val release = scrapSnapshot(link).run {
			Version(name, releaseTime, description = description, snapshots = firstReleases[link.remove("/wiki/Java_Edition_")] ?: listOf())
		}
		
		versions.add(release)
	}
}

fun saveToJSON() {
	val json = Json.encodeToString<List<Version>>(versions)
	val local = Paths.get(".")
	val file = File(local.absolutePathString(), "versions.json")
	file.writeText(json)
	
	println("Saved to ${file.normalize().absolutePath}")
}


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

fun scrapSnapshot(link: String) = skrape(HttpFetcher) {
	println("Snapshot: $FANDOM_URL$link")
	request {
		url = FANDOM_URL + link
		timeout = 30_000
	}
	
	extractIt<Snapshot> { it ->
		htmlDocument {
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
						it.tagName == "a" && it.href?.endsWith(".json") == true
					}?.href
				}
				
				table.findFirst {
					findAll("tr").firstOrNull {
						it.children.any { it.text.contains(Regex("(Snapshot|Pre-Release|Release Candidate) for", RegexOption.IGNORE_CASE)) }
					}
				}?.findFirst("td > p > a")?.text?.let { f -> it.snapshotFor = f.replace(" ", "_") }
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
