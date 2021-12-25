import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Snapshot(
	var name: String = "",
	var releaseTime: Long? = null,
	var description: String = "",
	var downloadClient: String? = null,
	var downloadJSON: String? = null,

	@Transient
	var snapshotFor: String? = null
) {
	init {
		description = description.replace("\n", "\\n")
	}
}

@Serializable
data class Version(
	var name: String = "",
	var releaseTime: Long? = null,
	var description: String = "",
	var snapshots: List<Snapshot> = emptyList()
) {
	init {
		description = description.replace("\n", "\\n")
	}
}
