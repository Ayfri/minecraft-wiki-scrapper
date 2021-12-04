import java.text.SimpleDateFormat
import java.util.*

fun main() {
	val versionDate = "May 5, 2009"
	val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(versionDate)
	println(formattedDate)
}
