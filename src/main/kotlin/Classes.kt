data class Snapshot(
	var name: String = "",
	var releaseTime: Long? = null,
	var description: String = "",
	var downloadClient: String? = null,
	var downloadJSON: String? = null,

	var snapshotFor: String? = null
)

data class Version(
	var name: String = "",
	var releaseTime: Long? = null,
	var description: String = "",
	var importantDescription: String = description.split("\n").take(4).joinToString("\n"),
	var snapshots: List<Snapshot> = emptyList()
)
