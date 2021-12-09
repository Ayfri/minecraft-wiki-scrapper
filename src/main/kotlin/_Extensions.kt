import it.skrape.selects.CssSelectable
import it.skrape.selects.DocElement

operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

fun String.remove(regex: Regex) = replace(regex, "")
fun String.remove(regex: String) = replace(regex, "")
fun String.get(regex: Regex) = replace(regex, "$1")
fun String.getIfMatches(regex: Regex) = if (matches(regex)) get(regex) else null

fun DocElement.contains(cssSelector: String) = try {
	findFirst(cssSelector)
	true
} catch (e: Exception) {
	false
}

inline fun <T> CssSelectable.contains(
	cssSelector: String = "",
	init: DocElement.() -> T
) = try {
	findFirst(cssSelector).init()
	true
} catch (e: Exception) {
	false
}

fun DocElement.findAny(cssSelector: String) = try {
	findFirst(cssSelector)
} catch (e: Exception) {
	null
}

inline fun <T> DocElement.findAny(
	cssSelector: String = "",
	init: DocElement.() -> T
) = try {
	findFirst(cssSelector).init()
} catch (e: Exception) {
	null
}

fun printError(message: Any) = println("\u001B[31m$message\u001B[0m")
